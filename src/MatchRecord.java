import java.time.LocalDateTime;

public class MatchRecord {
    private int matchId;
    private int donorId;
    private int recipientId;
    private LocalDateTime matchDate;

    public MatchRecord(int matchId, int donorId, int recipientId, LocalDateTime matchDate) {
        this.matchId = matchId;
        this.donorId = donorId;
        this.recipientId = recipientId;
        this.matchDate = matchDate;
    }

    public int getMatchId() { return matchId; }
    public int getDonorId() { return donorId; }
    public int getRecipientId() { return recipientId; }
    public LocalDateTime getMatchDate() { return matchDate; }

    @Override
    public String toString() {
        return String.format("Match #%d | donor %d -> recipient %d | %s",
                matchId, donorId, recipientId, matchDate);
    }
}
