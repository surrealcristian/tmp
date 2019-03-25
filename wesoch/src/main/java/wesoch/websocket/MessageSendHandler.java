package wesoch.websocket;

import javax.websocket.SendHandler;
import javax.websocket.SendResult;

public class MessageSendHandler implements SendHandler {
    @Override
    public void onResult(SendResult result) {
        if (!result.isOK()) {
            System.out.println("[ERROR] Could not send the message. Exception: " + result.getException());
        }
    }
}
