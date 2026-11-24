package com.example.vm.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public class BlankFloppyItem extends Item {
    public static final String MEDIA_ID_TAG = "MineVMFloppyId";

    public BlankFloppyItem(Properties properties) {
        super(properties);
    }

    public static String ensureMediaId(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        String mediaId = tag.getString(MEDIA_ID_TAG);
        try {
            UUID.fromString(mediaId);
            return mediaId;
        } catch (IllegalArgumentException ignored) {
            String generated = UUID.randomUUID().toString();
            tag.putString(MEDIA_ID_TAG, generated);
            return generated;
        }
    }

    public static void setMediaId(ItemStack stack, String mediaId) {
        UUID.fromString(mediaId);
        stack.getOrCreateTag().putString(MEDIA_ID_TAG, mediaId);
    }
}
