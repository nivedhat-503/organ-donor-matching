import java.time.LocalDate;

public class Donor {
    private int donorId;
    private String name;
    private int age;
    private String bloodGroup;
    private String organType;
    private String city;
    private String status;
    private LocalDate registeredDate;

    
    public Donor(String name, int age, String bloodGroup, String organType, String city) {
        this.name = name;
        this.age = age;
        this.bloodGroup = bloodGroup;
        this.organType = organType;
        this.city = city;
        this.status = "AVAILABLE";
    }

    
    public Donor(int donorId, String name, int age, String bloodGroup, String organType,
                 String city, String status, LocalDate registeredDate) {
        this.donorId = donorId;
        this.name = name;
        this.age = age;
        this.bloodGroup = bloodGroup;
        this.organType = organType;
        this.city = city;
        this.status = status;
        this.registeredDate = registeredDate;
    }

    public int getDonorId() { return donorId; }
    public String getName() { return name; }
    public int getAge() { return age; }
    public String getBloodGroup() { return bloodGroup; }
    public String getOrganType() { return organType; }
    public String getCity() { return city; }
    public String getStatus() { return status; }
    public LocalDate getRegisteredDate() { return registeredDate; }

    public void setDonorId(int donorId) { this.donorId = donorId; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return String.format("Donor #%d | %s, %d | %s | %s | %s | %s",
                donorId, name, age, bloodGroup, organType, city, status);
    }
}
