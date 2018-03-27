package wesoch;

import java.time.LocalDateTime;

public class ServerToClientMessageData {
    private String user;
    private LocalDateTime dateTime;
    private String text;

    public ServerToClientMessageData(String author, LocalDateTime dateTime, String text) {
        this.user = author;
        this.dateTime = dateTime;
        this.text = text;
    }

    public String getUser() {
        return user;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public String getText() {
        return text;
    }
}
