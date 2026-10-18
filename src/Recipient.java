import java.time.LocalDate;

public class Recipient {
    private int recipientId;
    private String name;
    private int age;
    private String bloodGroup;
    private String organNeeded;
    private String city;
    private int urgency;          
    private String status;
    private LocalDate registeredDate;

    
    public Recipient(String name, int age, String bloodGroup, String organNeeded,
                     String city, int urgency) {
        this.name = name;
        this.age = age;
        this.bloodGroup = bloodGroup;
        this.organNeeded = organNeeded;
        this.city = city;
        this.urgency = urgency;
        this.status = "WAITING";
    }

    
    public Recipient(int recipientId, String name, int age, String bloodGroup, String organNeeded,
                     String city, int urgency, String status, LocalDate registeredDate) {
        this.recipientId = recipientId;
        this.name = name;
        this.age = age;
        this.bloodGroup = bloodGroup;
        this.organNeeded = organNeeded;
        this.city = city;
        this.urgency = urgency;
        this.status = status;
        this.registeredDate = registeredDate;
    }

    public int getRecipientId() { return recipientId; }
    public String getName() { return name; }
    public int getAge() { return age; }
    public String getBloodGroup() { return bloodGroup; }
    public String getOrganNeeded() { return organNeeded; }
    public String getCity() { return city; }
    public int getUrgency() { return urgency; }
    public String getStatus() { return status; }
    public LocalDate getRegisteredDate() { return registeredDate; }

    public void setRecipientId(int recipientId) { this.recipientId = recipientId; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return String.format("Recipient #%d | %s, %d | %s | needs %s | %s | urgency %d | %s",
                recipientId, name, age, bloodGroup, organNeeded, city, urgency, status);
    }
}
