import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class RecipientDAO {

    public void addRecipient(Recipient recipient) throws Exception {
        String sql = "INSERT INTO recipients (name, age, blood_group, organ_needed, city, urgency) "
                   + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, recipient.getName());
            ps.setInt(2, recipient.getAge());
            ps.setString(3, recipient.getBloodGroup());
            ps.setString(4, recipient.getOrganNeeded());
            ps.setString(5, recipient.getCity());
            ps.setInt(6, recipient.getUrgency());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    recipient.setRecipientId(keys.getInt(1));
                }
            }
        }
    }


    public List<Recipient> getAllRecipients() throws Exception {
        String sql = "SELECT * FROM recipients ORDER BY recipient_id";
        List<Recipient> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

 
    public List<Recipient> getWaitingList(String organ) throws Exception {
        boolean filter = organ != null && !organ.trim().isEmpty();
        String sql = "SELECT * FROM recipients WHERE status = 'WAITING'"
                   + (filter ? " AND organ_needed = ?" : "")
                   + " ORDER BY urgency DESC, registered_date ASC, recipient_id ASC";
        List<Recipient> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (filter) {
                ps.setString(1, organ.trim());
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }


    public Recipient getRecipientById(int recipientId) throws Exception {
        String sql = "SELECT * FROM recipients WHERE recipient_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, recipientId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }


    public boolean updateStatus(int recipientId, String status) throws Exception {
        String sql = "UPDATE recipients SET status = ? WHERE recipient_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, recipientId);
            return ps.executeUpdate() > 0;
        }
    }


    static Recipient mapRow(ResultSet rs) throws Exception {
        return new Recipient(
                rs.getInt("recipient_id"),
                rs.getString("name"),
                rs.getInt("age"),
                rs.getString("blood_group"),
                rs.getString("organ_needed"),
                rs.getString("city"),
                rs.getInt("urgency"),
                rs.getString("status"),
                rs.getDate("registered_date").toLocalDate());
    }
}
