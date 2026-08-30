package com.example.vm.blockentity;

import com.example.vm.computer.ComputerHardwareConfiguration;
import com.example.vm.registry.ModBlockEntities;
import com.example.vm.vm.GuestOperatingSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.Optional;

public class ComputerBlockEntity extends BlockEntity {
    public static final String ITEM_TAG_COMPUTER = "MineVMComputer";
    private static final String TAG_COMPUTER_ID = "ComputerId";
    private static final String TAG_CPU_CORES = "CpuCores";
    private static final String TAG_MEMORY_MB = "MemoryMb";
    private static final String TAG_DISK_FILE = "DiskFile";
    private static final String TAG_INSERTED_DISC = "InsertedDisc";
    private static final String TAG_LINKED_MONITOR_POS = "LinkedMonitorPos";
    private static final String TAG_KEYBOARD_CONNECTED = "KeyboardConnected";
    private static final String TAG_MOUSE_CONNECTED = "MouseConnected";
    private static final String TAG_POWERED_ON = "PoweredOn";
    private static final String TAG_VM_DISPLAY_ACTIVE = "VmDisplayActive";
    private static final String TAG_ACTIVE_GUEST_OPERATING_SYSTEM = "ActiveGuestOperatingSystem";
    private static final String TAG_DISK_GUEST_OPERATING_SYSTEM = "DiskGuestOperatingSystem";

    private String computerId;
    private ComputerHardwareConfiguration hardwareConfiguration;
    private GuestOperatingSystem insertedDisc;
    @Nullable
    private GuestOperatingSystem activeGuestOperatingSystem;
    @Nullable
    private GuestOperatingSystem diskGuestOperatingSystem;
    private BlockPos linkedMonitorPos;
    private boolean keyboardConnected;
    private boolean mouseConnected;
    private boolean poweredOn;
    private boolean vmDisplayActive;
    private boolean powerTransitioning;

    public ComputerBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.COMPUTER_BLOCK_ENTITY.get(), pos, blockState);
        this.computerId = createComputerId();
        this.hardwareConfiguration = ComputerHardwareConfiguration.defaultConfiguration(this.computerId);
    }

    public String getComputerId() {
        return computerId;
    }

    public ComputerHardwareConfiguration getHardwareConfiguration() {
        return hardwareConfiguration;
    }

    public void loadFromItemStack(net.minecraft.world.item.ItemStack stack) {
        if (!stack.hasTag()) {
            return;
        }
        CompoundTag computerTag = stack.getTag().getCompound(ITEM_TAG_COMPUTER);
        if (computerTag.isEmpty()) {
            return;
        }
        loadIdentity(computerTag);
        markUpdated();
    }

    public net.minecraft.world.item.ItemStack saveToItemStack(net.minecraft.world.level.ItemLike item) {
        net.minecraft.world.item.ItemStack stack = new net.minecraft.world.item.ItemStack(item);
        stack.getOrCreateTag().put(ITEM_TAG_COMPUTER, saveIdentity());
        return stack;
    }

    public Optional<GuestOperatingSystem> insertedDisc() {
        return Optional.ofNullable(insertedDisc);
    }

    @Nullable
    public GuestOperatingSystem activeGuestOperatingSystem() {
        return activeGuestOperatingSystem;
    }

    public Optional<GuestOperatingSystem> selectedGuestOperatingSystem() {
        return Optional.ofNullable(activeGuestOperatingSystem)
                .or(() -> Optional.ofNullable(diskGuestOperatingSystem));
    }

    public void setActiveGuestOperatingSystem(GuestOperatingSystem guestOperatingSystem) {
        if (activeGuestOperatingSystem == guestOperatingSystem) {
            return;
        }
        activeGuestOperatingSystem = guestOperatingSystem;
        markUpdated();
    }

    public void rememberDiskGuestOperatingSystem(GuestOperatingSystem guestOperatingSystem) {
        if (diskGuestOperatingSystem == guestOperatingSystem) {
            return;
        }
        diskGuestOperatingSystem = guestOperatingSystem;
        markUpdated();
    }

    @Nullable
    public GuestOperatingSystem displayGuestOperatingSystem() {
        if (activeGuestOperatingSystem != null) {
            return activeGuestOperatingSystem;
        }
        if (insertedDisc != null) {
            return insertedDisc;
        }
        return diskGuestOperatingSystem;
    }

    public Optional<BlockPos> linkedMonitorPos() {
        return Optional.ofNullable(linkedMonitorPos);
    }

    public void linkMonitor(BlockPos monitorPos) {
        linkedMonitorPos = monitorPos;
        markUpdated();
    }

    public boolean keyboardConnected() {
        return keyboardConnected;
    }

    public boolean mouseConnected() {
        return mouseConnected;
    }

    public void connectKeyboard() {
        keyboardConnected = true;
        markUpdated();
    }

    public void connectMouse() {
        mouseConnected = true;
        markUpdated();
    }

    public boolean hasDesktopInputDevices() {
        return keyboardConnected && mouseConnected;
    }

    public boolean poweredOn() {
        return poweredOn;
    }

    public boolean powerTransitioning() {
        return powerTransitioning;
    }

    public void setPowerTransitioning(boolean powerTransitioning) {
        if (this.powerTransitioning == powerTransitioning) {
            return;
        }
        this.powerTransitioning = powerTransitioning;
        markUpdated();
    }

    public void togglePower() {
        poweredOn = !poweredOn;
        markUpdated();
    }

    public void setPoweredOn(boolean poweredOn) {
        if (this.poweredOn == poweredOn) {
            return;
        }
        this.poweredOn = poweredOn;
        if (!poweredOn) {
            vmDisplayActive = false;
        }
        markUpdated();
    }

    public boolean vmDisplayActive() {
        return vmDisplayActive;
    }

    public void setVmDisplayActive(boolean vmDisplayActive) {
        if (this.vmDisplayActive == vmDisplayActive) {
            return;
        }
        this.vmDisplayActive = vmDisplayActive;
        if (vmDisplayActive) {
            poweredOn = true;
        }
        markUpdated();
    }

    public boolean insertDisc(GuestOperatingSystem guestOperatingSystem) {
        if (guestOperatingSystem == null || insertedDisc != null) {
            return false;
        }

        insertedDisc = guestOperatingSystem;
        markUpdated();
        return true;
    }

    public Optional<GuestOperatingSystem> ejectDisc() {
        Optional<GuestOperatingSystem> disc = Optional.ofNullable(insertedDisc);
        if (disc.isPresent()) {
            insertedDisc = null;
            markUpdated();
        }
        return disc;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString(TAG_COMPUTER_ID, computerId);
        tag.putInt(TAG_CPU_CORES, hardwareConfiguration.cpuCores());
        tag.putInt(TAG_MEMORY_MB, hardwareConfiguration.memoryMb());
        tag.putString(TAG_DISK_FILE, hardwareConfiguration.diskFile());
        if (insertedDisc != null) {
            tag.putString(TAG_INSERTED_DISC, insertedDisc.name());
        }
        if (activeGuestOperatingSystem != null) {
            tag.putString(TAG_ACTIVE_GUEST_OPERATING_SYSTEM, activeGuestOperatingSystem.name());
        }
        if (diskGuestOperatingSystem != null) {
            tag.putString(TAG_DISK_GUEST_OPERATING_SYSTEM, diskGuestOperatingSystem.name());
        }
        if (linkedMonitorPos != null) {
            tag.putLong(TAG_LINKED_MONITOR_POS, linkedMonitorPos.asLong());
        }
        tag.putBoolean(TAG_KEYBOARD_CONNECTED, keyboardConnected);
        tag.putBoolean(TAG_MOUSE_CONNECTED, mouseConnected);
        tag.putBoolean(TAG_POWERED_ON, poweredOn);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);

        loadIdentity(tag);
        linkedMonitorPos = tag.contains(TAG_LINKED_MONITOR_POS) ? BlockPos.of(tag.getLong(TAG_LINKED_MONITOR_POS)) : null;
        keyboardConnected = tag.getBoolean(TAG_KEYBOARD_CONNECTED);
        mouseConnected = tag.getBoolean(TAG_MOUSE_CONNECTED);
        poweredOn = tag.getBoolean(TAG_POWERED_ON);
        vmDisplayActive = tag.getBoolean(TAG_VM_DISPLAY_ACTIVE);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        tag.putBoolean(TAG_VM_DISPLAY_ACTIVE, vmDisplayActive);
        return tag;
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(3.0D);
    }

    private void markUpdated() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    private CompoundTag saveIdentity() {
        CompoundTag tag = new CompoundTag();
        tag.putString(TAG_COMPUTER_ID, computerId);
        tag.putInt(TAG_CPU_CORES, hardwareConfiguration.cpuCores());
        tag.putInt(TAG_MEMORY_MB, hardwareConfiguration.memoryMb());
        tag.putString(TAG_DISK_FILE, hardwareConfiguration.diskFile());
        if (insertedDisc != null) {
            tag.putString(TAG_INSERTED_DISC, insertedDisc.name());
        }
        if (activeGuestOperatingSystem != null) {
            tag.putString(TAG_ACTIVE_GUEST_OPERATING_SYSTEM, activeGuestOperatingSystem.name());
        }
        if (diskGuestOperatingSystem != null) {
            tag.putString(TAG_DISK_GUEST_OPERATING_SYSTEM, diskGuestOperatingSystem.name());
        }
        return tag;
    }

    private void loadIdentity(CompoundTag tag) {
        if (tag.contains(TAG_COMPUTER_ID)) {
            computerId = tag.getString(TAG_COMPUTER_ID);
        }
        if (computerId == null || computerId.isBlank()) {
            computerId = createComputerId();
        }

        int cpuCores = tag.contains(TAG_CPU_CORES) ? tag.getInt(TAG_CPU_CORES) : ComputerHardwareConfiguration.DEFAULT_CPU_CORES;
        int memoryMb = tag.contains(TAG_MEMORY_MB) ? tag.getInt(TAG_MEMORY_MB) : ComputerHardwareConfiguration.DEFAULT_MEMORY_MB;
        String diskFile = tag.contains(TAG_DISK_FILE) ? tag.getString(TAG_DISK_FILE) : computerId + ".qcow2";
        hardwareConfiguration = new ComputerHardwareConfiguration(cpuCores, memoryMb, diskFile);
        insertedDisc = tag.contains(TAG_INSERTED_DISC) ? GuestOperatingSystem.parse(tag.getString(TAG_INSERTED_DISC)) : null;
        activeGuestOperatingSystem = tag.contains(TAG_ACTIVE_GUEST_OPERATING_SYSTEM) ? GuestOperatingSystem.parse(tag.getString(TAG_ACTIVE_GUEST_OPERATING_SYSTEM)) : null;
        diskGuestOperatingSystem = tag.contains(TAG_DISK_GUEST_OPERATING_SYSTEM) ? GuestOperatingSystem.parse(tag.getString(TAG_DISK_GUEST_OPERATING_SYSTEM)) : null;
    }

    private static String createComputerId() {
        return "computer-" + UUID.randomUUID();
    }
}
