package wesoch.domain;

import java.util.concurrent.atomic.AtomicInteger;

public class User {
    private static final AtomicInteger ID_GENERATOR = new AtomicInteger(0);

    private final int id;
    private final String name;

    public User(String name) {
        this.id = ID_GENERATOR.incrementAndGet();
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
