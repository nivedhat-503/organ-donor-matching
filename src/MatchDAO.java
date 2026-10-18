import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class MatchDAO {


    public List<Recipient> findCompatibleRecipients(int donorId) throws Exception {
        String sql =
                "SELECT r.* FROM donors d "
              + "JOIN blood_compatibility bc ON bc.donor_group = d.blood_group "
              + "JOIN recipients r ON r.blood_group = bc.recipient_group "
              + "                 AND r.organ_needed = d.organ_type "
              + "                 AND r.status = 'WAITING' "
              + "WHERE d.donor_id = ? AND d.status = 'AVAILABLE' "
              + "ORDER BY r.urgency DESC, r.registered_date ASC, (r.city = d.city) DESC, r.recipient_id ASC";
        List<Recipient> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, donorId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(RecipientDAO.mapRow(rs));
                }
            }
        }
        return list;
    }


    public int confirmMatch(int donorId, int recipientId) throws Exception {
        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                String checkSql =
                        "SELECT COUNT(*) FROM donors d "
                      + "JOIN recipients r ON r.organ_needed = d.organ_type "
                      + "JOIN blood_compatibility bc ON bc.donor_group = d.blood_group "
                      + "                           AND bc.recipient_group = r.blood_group "
                      + "WHERE d.donor_id = ? AND r.recipient_id = ?";
                try (PreparedStatement ps = con.prepareStatement(checkSql)) {
                    ps.setInt(1, donorId);
                    ps.setInt(2, recipientId);
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        if (rs.getInt(1) == 0) {
                            throw new Exception("Not compatible: different organ, incompatible blood group, "
                                    + "or the donor/recipient does not exist.");
                        }
                    }
                }

                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE donors SET status = 'MATCHED' WHERE donor_id = ? AND status = 'AVAILABLE'")) {
                    ps.setInt(1, donorId);
                    if (ps.executeUpdate() == 0) {
                        throw new Exception("Donor is not available (already matched).");
                    }
                }

                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE recipients SET status = 'MATCHED' WHERE recipient_id = ? AND status = 'WAITING'")) {
                    ps.setInt(1, recipientId);
                    if (ps.executeUpdate() == 0) {
                        throw new Exception("Recipient is not waiting (already matched).");
                    }
                }

                int matchId = 0;
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO matches (donor_id, recipient_id) VALUES (?, ?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, donorId);
                    ps.setInt(2, recipientId);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (keys.next()) {
                            matchId = keys.getInt(1);
                        }
                    }
                }

                con.commit();
                return matchId;
            } catch (Exception e) {
                con.rollback();  
                throw e;
            }
        }
    }


    public List<MatchRecord> getAllMatches() throws Exception {
        String sql = "SELECT * FROM matches ORDER BY match_date DESC, match_id DESC";
        List<MatchRecord> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new MatchRecord(
                        rs.getInt("match_id"),
                        rs.getInt("donor_id"),
                        rs.getInt("recipient_id"),
                        rs.getTimestamp("match_date").toLocalDateTime()));
            }
        }
        return list;
    }

    
    public List<String> getMatchHistoryReport() throws Exception {
        String sql =
                "SELECT m.match_id, m.match_date, d.name AS donor_name, d.blood_group AS donor_bg, "
              + "       d.organ_type, r.name AS recipient_name, r.blood_group AS recipient_bg, r.city "
              + "FROM matches m "
              + "JOIN donors d ON d.donor_id = m.donor_id "
              + "JOIN recipients r ON r.recipient_id = m.recipient_id "
              + "ORDER BY m.match_date DESC, m.match_id DESC";
        List<String> lines = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lines.add(String.format("Match #%d | %s | %s: %s (%s) -> %s (%s), %s",
                        rs.getInt("match_id"),
                        rs.getTimestamp("match_date").toLocalDateTime(),
                        rs.getString("organ_type"),
                        rs.getString("donor_name"), rs.getString("donor_bg"),
                        rs.getString("recipient_name"), rs.getString("recipient_bg"),
                        rs.getString("city")));
            }
        }
        return lines;
    }
}
