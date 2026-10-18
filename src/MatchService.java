import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Business rules live here: validation, duplicate checks, "no match" handling and reports.
// The menu (Main) talks to this class; this class talks to the DAOs.
public class MatchService {

    public static final List<String> BLOOD_GROUPS =
            Arrays.asList("O-", "O+", "A-", "A+", "B-", "B+", "AB-", "AB+");
    public static final List<String> ORGANS =
            Arrays.asList("Kidney", "Liver", "Heart", "Lung", "Pancreas");

    private final DonorDAO donorDAO = new DonorDAO();
    private final RecipientDAO recipientDAO = new RecipientDAO();
    private final MatchDAO matchDAO = new MatchDAO();

    // ---------- validation helpers ----------

    private String requireText(String value, String field, int maxLength) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " cannot be empty.");
        }
        String text = value.trim();
        if (text.length() > maxLength) {
            throw new IllegalArgumentException(field + " is too long (maximum " + maxLength + " characters).");
        }
        return text;
    }

    private String cleanBloodGroup(String input) {
        String bg = input == null ? "" : input.trim().toUpperCase();
        if (!BLOOD_GROUPS.contains(bg)) {
            throw new IllegalArgumentException("Blood group must be one of " + BLOOD_GROUPS + ".");
        }
        return bg;
    }

    private String cleanOrgan(String input) {
        String text = input == null ? "" : input.trim();
        for (String organ : ORGANS) {
            if (organ.equalsIgnoreCase(text)) {
                return organ;   // stored in the exact spelling used by the database
            }
        }
        throw new IllegalArgumentException("Organ must be one of " + ORGANS + ".");
    }

    // ---------- registration ----------

    public Donor registerDonor(String name, int age, String bloodGroup, String organ, String city)
            throws Exception {
        name = requireText(name, "Name", 100);
        city = requireText(city, "City", 50);
        bloodGroup = cleanBloodGroup(bloodGroup);
        organ = cleanOrgan(organ);
        if (age < 18 || age > 65) {
            throw new IllegalArgumentException("Donor age must be between 18 and 65.");
        }

        for (Donor d : donorDAO.getAllDonors()) {
            if (d.getName().equalsIgnoreCase(name)
                    && d.getBloodGroup().equals(bloodGroup)
                    && d.getOrganType().equals(organ)) {
                throw new IllegalArgumentException(
                        "This donor is already registered (donor #" + d.getDonorId() + ").");
            }
        }

        Donor donor = new Donor(name, age, bloodGroup, organ, city);
        donorDAO.addDonor(donor);
        return donor;
    }

    public Recipient registerRecipient(String name, int age, String bloodGroup, String organ,
                                       String city, int urgency) throws Exception {
        name = requireText(name, "Name", 100);
        city = requireText(city, "City", 50);
        bloodGroup = cleanBloodGroup(bloodGroup);
        organ = cleanOrgan(organ);
        if (age < 1 || age > 100) {
            throw new IllegalArgumentException("Recipient age must be between 1 and 100.");
        }
        if (urgency < 1 || urgency > 5) {
            throw new IllegalArgumentException("Urgency must be between 1 (low) and 5 (critical).");
        }

        for (Recipient r : recipientDAO.getAllRecipients()) {
            if (r.getName().equalsIgnoreCase(name)
                    && r.getBloodGroup().equals(bloodGroup)
                    && r.getOrganNeeded().equals(organ)) {
                throw new IllegalArgumentException(
                        "This recipient is already registered (recipient #" + r.getRecipientId() + ").");
            }
        }

        Recipient recipient = new Recipient(name, age, bloodGroup, organ, city, urgency);
        recipientDAO.addRecipient(recipient);
        return recipient;
    }

    // ---------- viewing ----------

    public List<Donor> getAvailableDonors() throws Exception {
        return donorDAO.getAvailableDonors();
    }

    public Donor getDonor(int donorId) throws Exception {
        return donorDAO.getDonorById(donorId);
    }

    // Pass null or blank for all organs
    public List<Recipient> getWaitingList(String organ) throws Exception {
        if (organ == null || organ.trim().isEmpty()) {
            return recipientDAO.getWaitingList(null);
        }
        return recipientDAO.getWaitingList(cleanOrgan(organ));
    }

    // ---------- matching ----------

    // Ranked list, best match first. Empty list means nobody is compatible.
    public List<Recipient> findBestMatches(int donorId) throws Exception {
        Donor donor = donorDAO.getDonorById(donorId);
        if (donor == null) {
            throw new IllegalArgumentException("No donor found with ID " + donorId + ".");
        }
        if (!"AVAILABLE".equals(donor.getStatus())) {
            throw new IllegalArgumentException("Donor #" + donorId + " is already matched.");
        }
        return matchDAO.findCompatibleRecipients(donorId);
    }

    // Returns the new match ID; the DAO re-checks compatibility and rolls back on any failure
    public int confirmMatch(int donorId, int recipientId) throws Exception {
        return matchDAO.confirmMatch(donorId, recipientId);
    }

    public List<String> getMatchHistory() throws Exception {
        return matchDAO.getMatchHistoryReport();
    }

    public static long daysWaiting(Recipient r) {
        return ChronoUnit.DAYS.between(r.getRegisteredDate(), LocalDate.now());
    }

    // ---------- reports ----------

    public Map<String, Integer> waitingCountByOrgan() throws Exception {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String organ : ORGANS) {
            counts.put(organ, 0);
        }
        for (Recipient r : recipientDAO.getWaitingList(null)) {
            counts.merge(r.getOrganNeeded(), 1, Integer::sum);
        }
        return counts;
    }

    public double averageWaitingDays() throws Exception {
        List<Recipient> waiting = recipientDAO.getWaitingList(null);
        if (waiting.isEmpty()) {
            return 0;
        }
        long total = 0;
        for (Recipient r : waiting) {
            total += daysWaiting(r);
        }
        return (double) total / waiting.size();
    }

    public Map<String, Integer> availableDonorsByBloodGroup() throws Exception {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String bg : BLOOD_GROUPS) {
            counts.put(bg, 0);
        }
        for (Donor d : donorDAO.getAvailableDonors()) {
            counts.merge(d.getBloodGroup(), 1, Integer::sum);
        }
        return counts;
    }

    public Map<String, Integer> matchesByOrgan() throws Exception {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (MatchRecord m : matchDAO.getAllMatches()) {
            Donor d = donorDAO.getDonorById(m.getDonorId());
            if (d != null) {
                counts.merge(d.getOrganType(), 1, Integer::sum);
            }
        }
        return counts;
    }
}
