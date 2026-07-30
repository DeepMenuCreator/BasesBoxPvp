package com.github.yourname.basesplugin.manager;

import org.bukkit.Location;
import java.util.UUID;

public class Base {
    private final UUID owner;
    private int level;
    private final Location location;

    public Base(UUID owner, int level, Location location) {
        this.owner = owner;
        this.level = level;
        this.location = location.clone();
    }

    public UUID getOwner() {
        return owner;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public Location getLocation() {
        return location.clone();
    }
}
