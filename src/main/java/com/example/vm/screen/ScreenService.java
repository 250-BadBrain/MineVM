package com.example.vm.screen;

import com.example.vm.concurrent.ComputerExecutors;
import com.example.vm.MineVM;
import com.example.vm.input.VirtualMachineInput;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public final class ScreenService implements AutoCloseable {
    private static final int CONNECT_RETRY_COUNT = 20;
    private static final long CONNECT_RETRY_DELAY_MS = 500L;

    private final Map<String, ScreenFrameBuffer> frameBuffers = new ConcurrentHashMap<>();
    private final Map<String, Future<?>> receivers = new ConcurrentHashMap<>();
    private final ScreenTransport vncFrameReceiver;
    private ExecutorService executor;
    private ExecutorService receiverExecutor;

    public ScreenService() {
        this(newScreenExecutor(), new VncFrameReceiver());
    }

    ScreenService(ExecutorService executor) {
        this(executor, new VncFrameReceiver());
    }

    public ScreenService(ExecutorService executor, ScreenTransport vncFrameReceiver) {
        this.executor = executor;
        this.receiverExecutor = newReceiverExecutor();
        this.vncFrameReceiver = vncFrameReceiver;
    }

    public CompletableFuture<Void> updateFrame(String computerId, ScreenFrame frame) {
        return CompletableFuture.runAsync(() -> applyFrame(requireComputerId(computerId), frame), executor());
    }

    public CompletableFuture<Optional<ScreenFrame>> getLatestFrame(String computerId) {
        return CompletableFuture.supplyAsync(() -> latestFrameSnapshot(computerId), executor());
    }

    public Optional<ScreenFrame> latestFrameSnapshot(String computerId) {
        ScreenFrameBuffer frameBuffer = frameBuffers.get(requireComputerId(computerId));
        return frameBuffer == null ? Optional.empty() : frameBuffer.snapshot();
    }

    public Optional<ScreenFrame> fullFrameSnapshot(String computerId) {
        ScreenFrameBuffer buffer = frameBuffers.get(requireComputerId(computerId));
        return buffer == null ? Optional.empty() : buffer.fullSnapshot();
    }

    public long frameRevision(String computerId) {
        ScreenFrameBuffer buffer = frameBuffers.get(requireComputerId(computerId));
        return buffer == null ? 0 : buffer.revision();
    }

    public String transportName() { return vncFrameReceiver.name(); }

    public boolean sendVncInput(String computerId, VirtualMachineInput input) {
        return vncFrameReceiver.sendInput(VncEndpoint.forComputer(requireComputerId(computerId)), input);
    }

    public synchronized void startVncReceiver(String computerId, VncEndpoint endpoint) {
        String id = requireComputerId(computerId);
        stopVncReceiver(id, true);
        receivers.put(id, receiverExecutor().submit(() -> receiveWithRetry(id, endpoint)));
    }

    public synchronized void refreshVncReceiver(String computerId) {
        String id = requireComputerId(computerId);
        VncEndpoint endpoint = VncEndpoint.forComputer(id);
        Future<?> receiver = receivers.get(id);
        if (receiver != null && !receiver.isDone() && vncFrameReceiver.hasActiveConnection(endpoint)) {
            return;
        }
        stopVncReceiver(id, false);
        receivers.put(id, receiverExecutor().submit(() -> receiveWithRetry(id, endpoint)));
    }

    public synchronized void stopVncReceiver(String computerId) {
        String id = requireComputerId(computerId);
        stopVncReceiver(id, true);
    }

    private void stopVncReceiver(String id, boolean removeFrameBuffer) {
        Future<?> receiver = receivers.remove(id);
        if (receiver != null) {
            receiver.cancel(true);
        }
        vncFrameReceiver.close(VncEndpoint.forComputer(id));
        if (removeFrameBuffer) {
            frameBuffers.remove(id);
        }
    }

    @Override
    public synchronized void close() {
        receivers.values().forEach(receiver -> receiver.cancel(true));
        receivers.clear();
        receiverExecutor.shutdownNow();
        executor.shutdownNow();
        vncFrameReceiver.close();
        frameBuffers.clear();
    }

    private synchronized ExecutorService receiverExecutor() {
        if (receiverExecutor.isShutdown() || receiverExecutor.isTerminated()) {
            receiverExecutor = newReceiverExecutor();
        }
        return receiverExecutor;
    }

    private void receiveWithRetry(String computerId, VncEndpoint endpoint) {
        for (int attempt = 1; attempt <= CONNECT_RETRY_COUNT && !Thread.currentThread().isInterrupted(); attempt++) {
            try {
                vncFrameReceiver.receive(endpoint, frame -> applyFrame(computerId, frame));
                return;
            } catch (IOException exception) {
                if (attempt == CONNECT_RETRY_COUNT) {
                    MineVM.LOGGER.warn("VNC receiver failed for {} at {}", computerId, endpoint.toDisplayAddress(), exception);
                    return;
                }
                sleepBeforeRetry();
            }
        }
    }

    private static void sleepBeforeRetry() {
        try {
            Thread.sleep(CONNECT_RETRY_DELAY_MS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private synchronized ExecutorService executor() {
        if (executor.isShutdown() || executor.isTerminated()) {
            executor = newScreenExecutor();
        }
        return executor;
    }

    private void applyFrame(String computerId, ScreenFrame frame) {
        long started = System.nanoTime();
        frameBuffers.computeIfAbsent(computerId, ignored -> new ScreenFrameBuffer()).apply(frame);
        FrameMetrics.forComputer(computerId).frame((long) frame.changedWidth() * frame.changedHeight(), System.nanoTime() - started);
    }

    private static String requireComputerId(String computerId) {
        if (computerId == null || computerId.isBlank()) {
            throw new IllegalArgumentException("computerId must not be blank");
        }
        return computerId;
    }

    private static ExecutorService newScreenExecutor() {
        return ComputerExecutors.singleThread("minevm-screen-receiver", 16);
    }

    private static ExecutorService newReceiverExecutor() {
        return ComputerExecutors.fixedThreads("minevm-vnc-receiver", 16, 16);
    }
}
