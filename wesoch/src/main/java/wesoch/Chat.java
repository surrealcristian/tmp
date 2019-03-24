package wesoch;

import com.google.gson.Gson;
import wesoch.domain.Room;
import wesoch.domain.User;

import javax.websocket.Session;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

public class Chat {
    private final Map<String, Integer> roomNameToRoomId = Collections.synchronizedMap(new HashMap<>());
    private final Map<String, Integer> userNameToUserId = Collections.synchronizedMap(new HashMap<>());

    private final Map<String, Session> sessionIdToSession = Collections.synchronizedMap(new HashMap<>());
    private final Map<String, Room> roomNameToRoom = Collections.synchronizedMap(new HashMap<>());
    private final Map<String, User> userNameToUser = Collections.synchronizedMap(new HashMap<>());

    private final Map<String, Set<Session>> userNameToSessions = Collections.synchronizedMap(new HashMap<>());
    private final Map<String, User> sessionIdToUser = Collections.synchronizedMap(new HashMap<>());

    private final Map<String, Set<User>> roomNameToUsers = Collections.synchronizedMap(new HashMap<>());
    private final Map<String, Room> userNameToRoom = Collections.synchronizedMap(new HashMap<>());

    public void onOpen(Session session) {
        sessionIdToSession.putIfAbsent(session.getId(), session);
    }

    public void onMessage(Session session, String text) {
        Gson gson = new Gson();

        Map<String, Object> command = gson.fromJson(text, HashMap.class);

        switch ((String) command.get("type")) {
            case "init":
                processInitCommand(session, (Map<String, Object>) command.get("data"));
                break;
            case "message":
                processMessageCommand(session, (String) command.get("data"));
                break;
            default:
                break;
        }
    }

    public void onClose(Session session) {
        User user = sessionIdToUser.get(session.getId());

        if (user == null) {
            return;
        }


        sessionIdToUser.remove(session.getId());


        Set<Session> userSessions = userNameToSessions.get(user.getName());

        if (userSessions != null) {
            userSessions.remove(session);
        }


        Room room = userNameToRoom.get(user.getName());

        if (room != null) {
            userNameToRoom.remove(user.getName());
        }


        roomNameToUsers.remove(room.getName());

        userNameToUser.remove(user.getName());

        sessionIdToSession.remove(session.getId());


        try {
            session.close();
        } catch (IOException e) {
            System.out.println("[ERROR] Could not close the session (Exception message: " + e.getMessage() + ")");
        }
    }

    private void processInitCommand(Session session, Map<String, Object> data) {
        final String roomName = (String) data.get("room");
        final String nickname = (String) data.get("nickname");

        User user = userNameToUser.putIfAbsent(nickname, new User(nickname));

        Set<Session> userSessions = userNameToSessions.putIfAbsent(nickname, Collections.synchronizedSet(new HashSet<>()));

        userSessions.add(session);

        sessionIdToUser.putIfAbsent(session.getId(), user);

        roomNameToRoom.putIfAbsent(roomName, new Room(roomName));

        roomNameToUsers.putIfAbsent(roomName, Collections.synchronizedSet(new HashSet<>()));

        roomNameToUsers.get(roomName).add(user);

        userNameToRoom.putIfAbsent(nickname, roomNameToRoom.get(roomName));
    }

    private void processMessageCommand(Session session, String text) {
        User user = sessionIdToUser.get(session.getId());

        Room room = userNameToRoom.get(user.getName());

        for (final User u : roomNameToUsers.get(room.getName())) {
            for (final Session s : userNameToSessions.get(u.getName())) {
                Gson gson = new Gson();

                ServerToClientMessageData data = new ServerToClientMessageData(user.getName(), LocalDateTime.now(), text);

                Map<String, Object> command = new HashMap<>();
                command.put("type", "message");
                command.put("data", data);

                try {
                    s.getBasicRemote().sendText(gson.toJson(command));
                } catch (IOException e) {
                    System.out.println("[ERROR] Could not send the message (" + text + ")");
                }
            }
        }
    }
}
