import java.util.List;
import java.util.Map;
import java.util.Scanner;
 
public class Main {
 
    private static final Scanner scanner = new Scanner(System.in);
    private static final MatchService service = new MatchService();
 
    public static void main(String[] args) {
        System.out.println("=== Organ Donor Matching System ===");
        boolean running = true;
 
        while (running) {
            printMenu();
            int choice = readInt("Choose an option: ");
            try {
                switch (choice) {
                    case 1: registerDonor(); break;
                    case 2: registerRecipient(); break;
                    case 3: viewWaitingList(); break;
                    case 4: viewAvailableDonors(); break;
                    case 5: findBestMatch(); break;
                    case 6: confirmDirectly(); break;
                    case 7: viewHistory(); break;
                    case 8: showReports(); break;
                    case 0: running = false; break;
                    default: System.out.println("Please choose a number from the menu.");
                }
            } catch (IllegalArgumentException e) {
                System.out.println("Invalid input: " + e.getMessage());
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
        System.out.println("Goodbye!");
    }
 
    // ---------- menu ----------
 
    private static void printMenu() {
        System.out.println("\n----------- MENU -----------");
        System.out.println("1. Register donor");
        System.out.println("2. Register recipient");
        System.out.println("3. View waiting list");
        System.out.println("4. View available donors");
        System.out.println("5. Find best match for a donor");
        System.out.println("6. Confirm a match (by IDs)");
        System.out.println("7. View match history");
        System.out.println("8. Reports");
        System.out.println("0. Exit");
    }
 
    // ---------- actions ----------
 
    private static void registerDonor() throws Exception {
        System.out.println("\n--- Register donor ---");
        String name = readText("Name: ");
        int age = readInt("Age (18-65): ");
        String blood = readText("Blood group " + MatchService.BLOOD_GROUPS + ": ");
        String organ = readText("Organ " + MatchService.ORGANS + ": ");
        String city = readText("City: ");
 
        Donor donor = service.registerDonor(name, age, blood, organ, city);
        System.out.println("Donor registered with ID " + donor.getDonorId() + ".");
    }
 
    private static void registerRecipient() throws Exception {
        System.out.println("\n--- Register recipient ---");
        String name = readText("Name: ");
        int age = readInt("Age: ");
        String blood = readText("Blood group " + MatchService.BLOOD_GROUPS + ": ");
        String organ = readText("Organ needed " + MatchService.ORGANS + ": ");
        String city = readText("City: ");
        int urgency = readInt("Urgency (1 = low ... 5 = critical): ");
 
        Recipient recipient = service.registerRecipient(name, age, blood, organ, city, urgency);
        System.out.println("Recipient registered with ID " + recipient.getRecipientId() + ".");
    }
 
    private static void viewWaitingList() throws Exception {
        System.out.println("\n--- Waiting list ---");
        String organ = readText("Organ to filter by (press Enter for all): ");
        List<Recipient> list = service.getWaitingList(organ);
        if (list.isEmpty()) {
            System.out.println("Nobody is waiting.");
            return;
        }
        printRecipients(list, false);
    }
 
    private static void viewAvailableDonors() throws Exception {
        System.out.println("\n--- Available donors ---");
        List<Donor> donors = service.getAvailableDonors();
        if (donors.isEmpty()) {
            System.out.println("No donors are available.");
            return;
        }
        for (Donor d : donors) {
            System.out.println(d);
        }
    }
 
    private static void findBestMatch() throws Exception {
        System.out.println("\n--- Find best match ---");
        List<Donor> donors = service.getAvailableDonors();
        if (donors.isEmpty()) {
            System.out.println("No donors are available.");
            return;
        }
        for (Donor d : donors) {
            System.out.println(d);
        }
 
        int donorId = readInt("\nEnter donor ID: ");
        List<Recipient> matches = service.findBestMatches(donorId);
        Donor donor = service.getDonor(donorId);
 
        System.out.println("\nSelected: " + donor);
        if (matches.isEmpty()) {
            System.out.println("No compatible recipient found for this donor right now.");
            return;
        }
 
        System.out.println("Ranked candidates (best match first):");
        printRecipients(matches, true);
        System.out.println("\nBest match: " + matches.get(0).getName()
                + " (ID " + matches.get(0).getRecipientId() + ")");
 
        int recipientId = readInt("Enter recipient ID to confirm the match (0 to go back): ");
        if (recipientId == 0) {
            return;
        }
        int matchId = service.confirmMatch(donorId, recipientId);
        System.out.println("Match confirmed! Match ID " + matchId + ".");
    }
 
    private static void confirmDirectly() throws Exception {
        System.out.println("\n--- Confirm a match ---");
        int donorId = readInt("Donor ID: ");
        int recipientId = readInt("Recipient ID: ");
        int matchId = service.confirmMatch(donorId, recipientId);
        System.out.println("Match confirmed! Match ID " + matchId + ".");
    }
 
    private static void viewHistory() throws Exception {
        System.out.println("\n--- Match history ---");
        List<String> lines = service.getMatchHistory();
        if (lines.isEmpty()) {
            System.out.println("No matches yet.");
            return;
        }
        for (String line : lines) {
            System.out.println(line);
        }
    }
 
    private static void showReports() throws Exception {
        System.out.println("\n--- Reports ---");
        printCounts("Recipients waiting, by organ", service.waitingCountByOrgan());
        System.out.printf("%nAverage waiting time (current waiting list): %.1f days%n",
                service.averageWaitingDays());
        printCounts("Available donors, by blood group", service.availableDonorsByBloodGroup());
        printCounts("Confirmed matches, by organ", service.matchesByOrgan());
    }
 
    // ---------- printing helpers ----------
 
    private static void printRecipients(List<Recipient> list, boolean ranked) {
        String header = String.format("%s%-4s %-22s %-4s %-6s %-10s %-12s %-8s %s",
                ranked ? "Rank  " : "", "ID", "Name", "Age", "Blood", "Organ", "City", "Urgency", "Days waiting");
        System.out.println(header);
 
        int rank = 1;
        for (Recipient r : list) {
            String prefix = ranked ? String.format("#%-5d", rank++) : "";
            System.out.println(prefix + String.format("%-4d %-22s %-4d %-6s %-10s %-12s %-8d %d",
                    r.getRecipientId(), r.getName(), r.getAge(), r.getBloodGroup(),
                    r.getOrganNeeded(), r.getCity(), r.getUrgency(), MatchService.daysWaiting(r)));
        }
    }
 
    private static void printCounts(String title, Map<String, Integer> counts) {
        System.out.println("\n" + title + ":");
        if (counts.isEmpty()) {
            System.out.println("  (none)");
            return;
        }
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            System.out.println("  " + e.getKey() + ": " + e.getValue());
        }
    }
 
    // ---------- input helpers ----------
 
    private static int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim();
            try {
                return Integer.parseInt(line);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a whole number.");
            }
        }
    }
 
    private static String readText(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine();
    }
}
