package wesoch.websocket;

import wesoch.Chat;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint(value = "/chat")
public class ChatEndpoint {
    private static final Chat chat = new Chat();

    @OnOpen
    public void onOpen(Session session, EndpointConfig config) {
        System.out.println("[INFO] Current thread ID: " + Thread.currentThread().getId());
        System.out.println("[INFO] Connection opened)");

        chat.onOpen(session);
    }

    @OnMessage
    public void onMessage(Session session, String message) {
        System.out.println("[INFO] Current Thread ID: " + Thread.currentThread().getId());
        System.out.println("[INFO] Text message received: " + message);

        chat.onMessage(session, message);
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {
        System.out.println("[INFO] Current Thread ID: " + Thread.currentThread().getId());
        System.out.println("[INFO] Connection closed (CloseReason reason phrase: " + reason.getReasonPhrase() + ")");

        chat.onClose(session);
    }

    @OnError
    public void onError(Session session, Throwable error) {
        System.out.println("[INFO] Current Thread ID: " + Thread.currentThread().getId());
        System.out.println("[ERROR] Connection error (Error message: " + error.getMessage() + ". Error: " + error + ")");
    }
}
