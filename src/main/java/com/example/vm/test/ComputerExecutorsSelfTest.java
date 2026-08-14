package com.example.vm.test;

import com.example.vm.concurrent.ComputerExecutors;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public final class ComputerExecutorsSelfTest {
    private ComputerExecutorsSelfTest() {
    }

    public static void main(String[] args) throws Exception {
        ExecutorService executor = ComputerExecutors.singleThread("computer-test-executor", 1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicReference<Thread> workerThread = new AtomicReference<>();

        executor.execute(() -> {
            workerThread.set(Thread.currentThread());
            await(release);
        });
        executor.execute(() -> {
        });
        assertRejected(() -> executor.execute(() -> {
        }));

        release.countDown();
        executor.shutdown();
        if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
            throw new AssertionError("Executor did not terminate");
        }

        Thread thread = workerThread.get();
        if (thread == null || !thread.isDaemon() || !thread.getName().equals("computer-test-executor")) {
            throw new AssertionError("Unexpected executor thread: " + thread);
        }
    }

    private static void await(CountDownLatch release) {
        try {
            release.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while waiting", exception);
        }
    }

    private static void assertRejected(Runnable runnable) {
        try {
            runnable.run();
        } catch (RejectedExecutionException expected) {
            return;
        }
        throw new AssertionError("Expected RejectedExecutionException");
    }
}
