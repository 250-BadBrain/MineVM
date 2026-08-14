package com.example.vm.screen;

import com.example.vm.concurrent.ComputerExecutors;
import com.example.vm.ComputerInMinecraft;

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
    private final VncFrameReceiver vncFrameReceiver;
    private ExecutorService executor;
    private ExecutorService receiverExecutor;

    public ScreenService() {
        this(newScreenExecutor(), new VncFrameReceiver());
    }

    ScreenService(ExecutorService executor) {
        this(executor, new VncFrameReceiver());
    }

    ScreenService(ExecutorService executor, VncFrameReceiver vncFrameReceiver) {
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

    public synchronized void startVncReceiver(String computerId, VncEndpoint endpoint) {
        String id = requireComputerId(computerId);
        stopVncReceiver(id);
        receivers.put(id, receiverExecutor().submit(() -> receiveWithRetry(id, endpoint)));
    }

    public synchronized void stopVncReceiver(String computerId) {
        String id = requireComputerId(computerId);
        Future<?> receiver = receivers.remove(id);
        if (receiver != null) {
            receiver.cancel(true);
        }
        frameBuffers.remove(id);
    }

    @Override
    public synchronized void close() {
        receivers.values().forEach(receiver -> receiver.cancel(true));
        receivers.clear();
        receiverExecutor.shutdownNow();
        executor.shutdownNow();
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
                    ComputerInMinecraft.LOGGER.warn("VNC receiver failed for {} at {}", computerId, endpoint.toDisplayAddress(), exception);
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
        frameBuffers.computeIfAbsent(computerId, ignored -> new ScreenFrameBuffer()).apply(frame);
    }

    private static String requireComputerId(String computerId) {
        if (computerId == null || computerId.isBlank()) {
            throw new IllegalArgumentException("computerId must not be blank");
        }
        return computerId;
    }

    private static ExecutorService newScreenExecutor() {
        return ComputerExecutors.singleThread("computer-in-minecraft-screen-receiver", 16);
    }

    private static ExecutorService newReceiverExecutor() {
        return ComputerExecutors.singleThread("computer-in-minecraft-vnc-receiver", 16);
    }
}
