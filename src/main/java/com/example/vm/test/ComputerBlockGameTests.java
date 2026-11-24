package com.example.vm.test;

import com.example.vm.MineVM;
import com.example.vm.blockentity.ComputerBlockEntity;
import com.example.vm.registry.ModBlocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;

@GameTestHolder(MineVM.MOD_ID)
@net.minecraftforge.gametest.PrefixGameTestTemplate(false)
public final class ComputerBlockGameTests {
    private ComputerBlockGameTests() {
    }

    @GameTest(template = "empty")
    public static void computerBlockCanBePlaced(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);

        helper.setBlock(pos, ModBlocks.COMPUTER_BLOCK.get());
        helper.assertBlockPresent(ModBlocks.COMPUTER_BLOCK.get(), pos);

        BlockEntity blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(pos));
        if (!(blockEntity instanceof ComputerBlockEntity computerBlockEntity)) {
            helper.fail("Expected computer block entity at " + pos);
            return;
        }

        if (computerBlockEntity.getComputerId().isBlank()) {
            helper.fail("Expected generated computer id");
            return;
        }

        if (computerBlockEntity.getHardwareConfiguration().cpuCores() != 4) {
            helper.fail("Expected default CPU core count");
            return;
        }

        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void physicalPeripheralRemovalInvalidatesInput(GameTestHelper helper) {
        BlockPos computerPos = new BlockPos(1, 1, 1);
        BlockPos keyboardPos = new BlockPos(2, 1, 1);
        BlockPos mousePos = new BlockPos(2, 1, 2);
        helper.setBlock(computerPos, ModBlocks.COMPUTER_BLOCK.get());
        helper.setBlock(keyboardPos, ModBlocks.KEYBOARD.get());
        helper.setBlock(mousePos, ModBlocks.MOUSE.get());
        var level = helper.getLevel();
        ComputerBlockEntity computer = (ComputerBlockEntity) level.getBlockEntity(helper.absolutePos(computerPos));
        computer.connectPeripheral(com.example.vm.item.ComputerPeripheralType.KEYBOARD,
                (com.example.vm.blockentity.ComputerPeripheralBlockEntity) level.getBlockEntity(helper.absolutePos(keyboardPos)));
        computer.connectPeripheral(com.example.vm.item.ComputerPeripheralType.MOUSE,
                (com.example.vm.blockentity.ComputerPeripheralBlockEntity) level.getBlockEntity(helper.absolutePos(mousePos)));
        helper.assertTrue(computer.hasDesktopInputDevices(), "Connected physical devices must authorize input");
        level.destroyBlock(helper.absolutePos(keyboardPos), true);
        helper.assertTrue(!computer.hasDesktopInputDevices(), "Destroyed keyboard must revoke input");
        helper.setBlock(keyboardPos, ModBlocks.KEYBOARD.get());
        helper.assertTrue(!computer.hasDesktopInputDevices(), "Replacing a keyboard at the same coordinate must not reconnect it");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void peripheralNbtAndOwnershipPersist(GameTestHelper helper) {
        BlockPos computerPos = new BlockPos(1, 1, 1);
        BlockPos keyboardPos = new BlockPos(2, 1, 1);
        helper.setBlock(computerPos, ModBlocks.COMPUTER_BLOCK.get());
        helper.setBlock(keyboardPos, ModBlocks.KEYBOARD.get());
        ComputerBlockEntity computer = (ComputerBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(computerPos));
        var keyboard = (com.example.vm.blockentity.ComputerPeripheralBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(keyboardPos));
        java.util.UUID owner = java.util.UUID.randomUUID();
        computer.claimOwner(owner);
        computer.connectPeripheral(com.example.vm.item.ComputerPeripheralType.KEYBOARD, keyboard);
        var savedComputer = computer.saveWithFullMetadata();
        var savedKeyboard = keyboard.saveWithFullMetadata();
        computer.load(savedComputer); keyboard.load(savedKeyboard);
        helper.assertTrue(computer.keyboardConnected(), "NBT reload must preserve reciprocal device identity");
        helper.assertTrue(computer.owner().filter(owner::equals).isPresent(), "NBT reload lost VM ownership");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void recipesCommandsAndFramePacketAreAvailable(GameTestHelper helper) {
        for (String item : new String[] {"desktop_computer", "laptop_computer", "computer_monitor", "keyboard", "mouse", "usb_cable", "hdmi_cable", "floppy_drive", "blank_floppy", "windows_95_boot_floppy", "ubuntu_server_disc", "ubuntu_desktop_disc", "freedos_disc", "windows_95_disc", "windows_98_disc", "windows_xp_disc", "windows_vista_disc", "windows_10_disc", "windows_11_disc"}) {
            helper.assertTrue(helper.getLevel().getRecipeManager().byKey(new net.minecraft.resources.ResourceLocation("minevm", item)).isPresent(), "Missing recipe: " + item);
        }
        var root = helper.getLevel().getServer().getCommands().getDispatcher().getRoot().getChild("minevm");
        helper.assertTrue(root != null && root.getChild("doctor") != null && root.getChild("disk") != null && root.getChild("access") != null,
                "MineVM commands were not registered on the dedicated server");
        net.minecraft.network.FriendlyByteBuf buffer = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        try {
            byte[] bytes = {1, 2, 3, 4};
            var packet = new com.example.vm.network.ScreenFramePacket("vm-test", 123, 456, "raw", 1, 1, bytes);
            com.example.vm.network.ScreenFramePacket.encode(packet, buffer);
            var decoded = com.example.vm.network.ScreenFramePacket.decode(buffer);
            helper.assertTrue(decoded.computerId().equals("vm-test") && decoded.epoch() == 123 && decoded.sequence() == 456
                    && java.util.Arrays.equals(decoded.payload(), bytes), "Screen packet roundtrip changed its fields");
        } finally { buffer.release(); }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void ownerAndTrustedControlAreSeparate(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.LAPTOP_COMPUTER.get());
        ComputerBlockEntity computer = (ComputerBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
        var owner = net.minecraftforge.common.util.FakePlayerFactory.get(helper.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "MineVmOwnerTest"));
        var visitor = net.minecraftforge.common.util.FakePlayerFactory.get(helper.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "MineVmGuestTest"));
        computer.claimOwner(owner.getUUID());
        helper.assertTrue(computer.canManage(owner) && !computer.canControl(visitor), "An unknown visitor must not control an owned VM");
        computer.setTrusted(visitor.getUUID(), true);
        computer.load(computer.saveWithFullMetadata());
        helper.assertTrue(computer.canControl(visitor) && !computer.canManage(visitor), "Trust must survive NBT reload without granting disk management");
        computer.setTrusted(visitor.getUUID(), false);
        helper.assertTrue(!computer.canControl(visitor), "Revocation must remove control permission");
        helper.succeed();
    }
}
