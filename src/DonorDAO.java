import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class DonorDAO {

    
    public void addDonor(Donor donor) throws Exception {
        String sql = "INSERT INTO donors (name, age, blood_group, organ_type, city) VALUES (?, ?, ?, ?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, donor.getName());
            ps.setInt(2, donor.getAge());
            ps.setString(3, donor.getBloodGroup());
            ps.setString(4, donor.getOrganType());
            ps.setString(5, donor.getCity());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    donor.setDonorId(keys.getInt(1));
                }
            }
        }
    }

    
    public List<Donor> getAllDonors() throws Exception {
        String sql = "SELECT * FROM donors ORDER BY donor_id";
        List<Donor> donors = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                donors.add(mapRow(rs));
            }
        }
        return donors;
    }

    
    public List<Donor> getAvailableDonors() throws Exception {
        String sql = "SELECT * FROM donors WHERE status = 'AVAILABLE' ORDER BY donor_id";
        List<Donor> donors = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                donors.add(mapRow(rs));
            }
        }
        return donors;
    }

    
    public Donor getDonorById(int donorId) throws Exception {
        String sql = "SELECT * FROM donors WHERE donor_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, donorId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    
    public boolean updateStatus(int donorId, String status) throws Exception {
        String sql = "UPDATE donors SET status = ? WHERE donor_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, donorId);
            return ps.executeUpdate() > 0;
        }
    }

    
    private Donor mapRow(ResultSet rs) throws Exception {
        return new Donor(
                rs.getInt("donor_id"),
                rs.getString("name"),
                rs.getInt("age"),
                rs.getString("blood_group"),
                rs.getString("organ_type"),
                rs.getString("city"),
                rs.getString("status"),
                rs.getDate("registered_date").toLocalDate());
    }
}
