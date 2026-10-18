import java.util.List;

public class TestDonorDAO {
    public static void main(String[] args) {
        try {
            DonorDAO dao = new DonorDAO();

            List<Donor> all = dao.getAllDonors();
            System.out.println("Total donors: " + all.size());
            for (Donor d : all) {
                System.out.println(d);
            }

            System.out.println("\nDonor with ID 1: " + dao.getDonorById(1));
            System.out.println("Donor with ID 999: " + dao.getDonorById(999));
            System.out.println("Available donors: " + dao.getAvailableDonors().size());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
