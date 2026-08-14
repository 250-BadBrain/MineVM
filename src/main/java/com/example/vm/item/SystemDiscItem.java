package com.example.vm.item;

import com.example.vm.vm.GuestOperatingSystem;
import net.minecraft.world.item.Item;

public class SystemDiscItem extends Item {
    private final SystemDiscDefinition definition;

    public SystemDiscItem(GuestOperatingSystem guestOperatingSystem, String displayNameKey, Properties properties) {
        this(new SystemDiscDefinition(guestOperatingSystem, displayNameKey), properties);
    }

    public SystemDiscItem(SystemDiscDefinition definition, Properties properties) {
        super(properties);
        if (definition == null) {
            throw new IllegalArgumentException("definition must not be null");
        }
        this.definition = definition;
    }

    public GuestOperatingSystem guestOperatingSystem() {
        return definition.guestOperatingSystem();
    }

    public String displayNameKey() {
        return definition.displayNameKey();
    }
}
