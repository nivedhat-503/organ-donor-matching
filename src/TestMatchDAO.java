import java.util.List;

public class TestMatchDAO {

    private static void show(String title, List<Recipient> list) {
        System.out.println("\n" + title + " -> " + list.size() + " candidate(s)");
        int rank = 1;
        for (Recipient r : list) {
            System.out.println("  " + rank++ + ". " + r + " | waiting since " + r.getRegisteredDate());
        }
    }

    public static void main(String[] args) {
        try {
            MatchDAO dao = new MatchDAO();


            show("Donor 1 (O-, Kidney, Chennai)", dao.findCompatibleRecipients(1));
            show("Donor 5 (AB+, Kidney)", dao.findCompatibleRecipients(5));
            show("Donor 12 (B+, Pancreas)", dao.findCompatibleRecipients(12));

            System.out.println("\nMatch history:");
            for (String line : dao.getMatchHistoryReport()) {
                System.out.println("  " + line);
            }


            try {
                dao.confirmMatch(16, 21);   // donor 16 is already MATCHED
            } catch (Exception e) {
                System.out.println("\nExpected error 1: " + e.getMessage());
            }
            try {
                dao.confirmMatch(12, 20);   // B+ donor, O+ recipient: incompatible
            } catch (Exception e) {
                System.out.println("Expected error 2: " + e.getMessage());
            }

            // WARNING: uncommenting this CHANGES your data (matches donor 1 with recipient 7).
            // To undo it, run schema.sql and sample_data.sql again.
            System.out.println("New match ID: " + dao.confirmMatch(1, 7));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
