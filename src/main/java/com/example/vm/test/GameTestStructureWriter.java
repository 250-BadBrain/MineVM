package com.example.vm.test;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import java.nio.file.Files;
import java.nio.file.Path;

public final class GameTestStructureWriter {
    public static void main(String[] args) throws Exception {
        Path directory = Path.of(args[0]);
        Files.createDirectories(directory);
        CompoundTag structure = new CompoundTag();
        structure.putInt("DataVersion", 3465);
        ListTag size = new ListTag();
        for (int index = 0; index < 3; index++) size.add(IntTag.valueOf(4));
        structure.put("size", size);
        ListTag palette = new ListTag();
        CompoundTag air = new CompoundTag(); air.putString("Name", "minecraft:air"); palette.add(air);
        structure.put("palette", palette);
        structure.put("blocks", new ListTag());
        structure.put("entities", new ListTag());
        NbtIo.writeCompressed(structure, directory.resolve("empty.nbt").toFile());
    }
}
