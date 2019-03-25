package wesoch;

import com.google.gson.Gson;
import wesoch.domain.Room;
import wesoch.domain.User;
import wesoch.websocket.MessageSendHandler;

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

    private final Map<User, Set<Session>> userToSessions = Collections.synchronizedMap(new HashMap<>());
    private final Map<Session, User> sessionToUser = Collections.synchronizedMap(new HashMap<>());

    private final Map<Room, Set<User>> roomToUsers = Collections.synchronizedMap(new HashMap<>());
    private final Map<User, Room> userToRoom = Collections.synchronizedMap(new HashMap<>());

    private final MessageSendHandler messageSendHandler = new MessageSendHandler();

    public void onOpen(Session session) {
        addSession(session);
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
        User user = sessionToUser.get(session);

        if (user == null) {
            return;
        }


        sessionToUser.remove(session);


        Set<Session> userSessions = userToSessions.get(user);

        if (userSessions != null) {
            userSessions.remove(session);
        }


        Room room = userToRoom.get(user);

        if (room != null) {
            userToRoom.remove(user);
        }

        roomToUsers.remove(room);

        userNameToUser.remove(user.getName());

        sessionIdToSession.remove(session.getId());


        try {
            session.close();
        } catch (IOException e) {
            System.out.println("[ERROR] Could not close the session (Exception message: " + e.getMessage() + ")");
        }
    }

    private void addSession(Session session) {
        sessionIdToSession.putIfAbsent(session.getId(), session);
    }

    private User getUser(String userName) {
        User user = userNameToUser.get(userName);

        if (user == null) {
            user = new User(userName);
            userNameToUser.put(userName, user);
        }

        return user;
    }

    private void addSessionToUser(Session session, User user) {
        Set<Session> userSessions = userToSessions.computeIfAbsent(user, k -> Collections.synchronizedSet(new HashSet<>()));

        userSessions.add(session);

        sessionToUser.putIfAbsent(session, user);
    }

    private Room getRoom(String roomName) {
        Room room = roomNameToRoom.get(roomName);

        if (room == null) {
            room = new Room(roomName);
            roomNameToRoom.put(roomName, room);
        }

        return room;
    }

    private void addUserToRoom(User user, Room room) {
        roomToUsers.putIfAbsent(room, Collections.synchronizedSet(new HashSet<>()));

        roomToUsers.get(room).add(user);

        userToRoom.putIfAbsent(user, roomNameToRoom.get(room.getName()));
    }

    private void processInitCommand(Session session, Map<String, Object> data) {
        final String roomName = (String) data.get("room"); //TODO: ensure not null
        final String userName = (String) data.get("nickname"); //TODO: ensure not null

        Room room = getRoom(roomName);

        User user = getUser(userName);

        addSessionToUser(session, user);

        addUserToRoom(user, room);
    }

    private void processMessageCommand(Session session, String text) {
        User user = sessionToUser.get(session);

        Room room = userToRoom.get(user);

        for (final User u : roomToUsers.get(room)) {
            for (final Session s : userToSessions.get(u)) {
                Gson gson = new Gson();

                ServerToClientMessageData data = new ServerToClientMessageData(user.getName(), LocalDateTime.now(), text);

                Map<String, Object> command = new HashMap<>();
                command.put("type", "message");
                command.put("data", data);

                s.getAsyncRemote().sendText(gson.toJson(command), messageSendHandler);
            }
        }
    }
}
