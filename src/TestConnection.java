import java.sql.Connection;

public class TestConnection {
    public static void main(String[] args) {
        try (Connection con = DBConnection.getConnection()) {
            System.out.println("Connected to: " + con.getCatalog());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}