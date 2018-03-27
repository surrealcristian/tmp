package wesoch;

import com.google.gson.Gson;

import javax.websocket.Session;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class Chat {
    private final Map<String, Session> sessions = new HashMap<>();
    private final Map<String, Room> rooms = new HashMap<>();
    private final Map<String, User> users = new HashMap<>();

    private final Map<String, Session> userSessionMap = new HashMap<>();
    private final Map<String, User> sessionUserMap = new HashMap<>();
    private final Map<String, List<User>> roomUsersMap = new HashMap<>();
    private final Map<String, Room> userRoomMap = new HashMap<>();

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

        synchronized (this.sessionUserMap) {
            user = this.sessionUserMap.get(session.getId());

            if (user == null) {
                return;
            }

            this.sessionUserMap.remove(session.getId());
        }

        synchronized (this.userSessionMap) {
            this.userSessionMap.remove(user.getNickname());
        }

        Room room = null;

        synchronized (this.userRoomMap) {
            room = this.userRoomMap.get(user.getNickname());

            if (room != null) {
                this.userRoomMap.remove(user.getNickname());
            }
        }

        synchronized (this.roomUsersMap) {
            this.roomUsersMap.remove(room.getName());
        }

        synchronized (this.users) {
                this.users.remove(user.getNickname());
        };

        synchronized (this.sessions) {
            this.sessions.remove(session.getId());
        }

        try {
            session.close();
        } catch (IOException e) {
            System.out.println("[ERROR] Could not close the session (Exception message: " + e.getMessage() + ")");
        }
    }

    private void processInitCommand(Session session, Map<String, Object> data) {
        final String roomName = (String) data.get("room");
        final String nickname = (String) data.get("nickname");

        synchronized (this.users) {
            if (this.users.get(nickname) != null) {
                try {
                    session.close();
                } catch (IOException e) {
                    System.out.println("[ERROR] Could not close the session (Exception message: " + e.getMessage() + ")");
                }

                return;
            }

            User user = new User(nickname);

            this.users.put(nickname, user);
        }

        synchronized (this.userSessionMap) {
            this.userSessionMap.put(nickname, session);
        }

        synchronized (this.sessionUserMap) {
            if (this.sessionUserMap.get(session.getId()) == null) {
                this.sessionUserMap.put(session.getId(), this.users.get(nickname));
            }
        }

        synchronized (this.rooms) {
            if (this.rooms.get(roomName) == null) {
                Room room = new Room();

                room.setName(roomName);

                this.rooms.put(roomName, room);
            }
        }

        synchronized (this.roomUsersMap) {
            if (this.roomUsersMap.get(roomName) == null) {
                this.roomUsersMap.put(roomName, new Vector<>());
            }

            this.roomUsersMap.get(roomName).add(this.users.get(nickname));
        }

        synchronized (this.userRoomMap) {
            if (this.userRoomMap.get(nickname) == null) {
                this.userRoomMap.put(nickname, this.rooms.get(roomName));
            }
        }
    }

    private void processMessageCommand(Session session, String text) {
        User user;

        synchronized (this.sessionUserMap) {
            user = this.sessionUserMap.get(session.getId());
        }

        Room room;

        synchronized (this.userRoomMap) {
            room = this.userRoomMap.get(user.getNickname());
        }

        for (final User u : this.roomUsersMap.get(room.getName())) {
            synchronized (u) {
                Session s = this.userSessionMap.get(u.getNickname());

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
