package wesoch;

import com.google.gson.Gson;

import javax.websocket.Session;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

public class Chat {
    private final Map<String, Session> sessions = Collections.synchronizedMap(new HashMap<>());
    private final Map<String, Room> rooms = Collections.synchronizedMap(new HashMap<>());
    private final Map<String, User> users = Collections.synchronizedMap(new HashMap<>());

    private final Map<String, Set<Session>> userSessionMap = Collections.synchronizedMap(new HashMap<>());
    private final Map<String, User> sessionUserMap = Collections.synchronizedMap(new HashMap<>());

    private final Map<String, Set<User>> roomUsersMap = Collections.synchronizedMap(new HashMap<>());
    private final Map<String, Room> userRoomMap = Collections.synchronizedMap(new HashMap<>());

    public void onOpen(Session session) {
        synchronized (this.sessions) {
            this.sessions.put(session.getId(), session);
        }
    }

    public void onMessage(Session session, String text) {
        Gson gson = new Gson();

        Map<String, Object> command = gson.fromJson(text, HashMap.class);

        switch ((String) command.get("type")) {
            case "init":
                this.processInitCommand(session, (Map<String, Object>) command.get("data"));
                break;
            case "message":
                this.processMessageCommand(session, (String) command.get("data"));
                break;
            default:
                break;
        }
    }

    public void onClose(Session session) {
        User user = null;

        user = this.sessionUserMap.get(session.getId());

        if (user == null) {
            return;
        }

        this.sessionUserMap.remove(session.getId());

        Set<Session> userSessions = this.userSessionMap.get(user.getNickname());

        if (userSessions != null) {
            userSessions.remove(session);
        }

        Room room = null;

        room = this.userRoomMap.get(user.getNickname());

        if (room != null) {
            this.userRoomMap.remove(user.getNickname());
        }

        this.roomUsersMap.remove(room.getName());

        this.users.remove(user.getNickname());

        this.sessions.remove(session.getId());

        try {
            session.close();
        } catch (IOException e) {
            System.out.println("[ERROR] Could not close the session (Exception message: " + e.getMessage() + ")");
        }
    }

    private void processInitCommand(Session session, Map<String, Object> data) {
        final String roomName = (String) data.get("room");
        final String nickname = (String) data.get("nickname");

        User user = this.users.get(nickname);

        if (user == null) {
            user = new User(nickname);

            this.users.put(nickname, user);
        }

        Set<Session> userSessions = this.userSessionMap.get(nickname);

        if (userSessions == null) {
            Set<Session> sessions = Collections.synchronizedSet(new HashSet<>());
            sessions.add(session);
            this.userSessionMap.put(nickname, sessions);
        } else {
            userSessions.add(session);
        }

        if (this.sessionUserMap.get(session.getId()) == null) {
            this.sessionUserMap.put(session.getId(), this.users.get(nickname));
        }

        if (this.rooms.get(roomName) == null) {
            Room room = new Room();

            room.setName(roomName);

            this.rooms.put(roomName, room);
        }

        if (this.roomUsersMap.get(roomName) == null) {
            this.roomUsersMap.put(roomName, Collections.synchronizedSet(new HashSet<User>()));
        }

        this.roomUsersMap.get(roomName).add(this.users.get(nickname));

        if (this.userRoomMap.get(nickname) == null) {
            this.userRoomMap.put(nickname, this.rooms.get(roomName));
        }
    }

    private void processMessageCommand(Session session, String text) {
        User user;

        user = this.sessionUserMap.get(session.getId());

        Room room;

        room = this.userRoomMap.get(user.getNickname());

        for (final User u : this.roomUsersMap.get(room.getName())) {
            for (final Session s : this.userSessionMap.get(u.getNickname())) {
                Gson gson = new Gson();

                ServerToClientMessageData data = new ServerToClientMessageData(user.getNickname(), LocalDateTime.now(), text);

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
