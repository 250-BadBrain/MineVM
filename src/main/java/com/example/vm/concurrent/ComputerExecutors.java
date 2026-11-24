package com.example.vm.concurrent;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class ComputerExecutors {
    private ComputerExecutors() {
    }

    public static ExecutorService singleThread(String threadName, int queueCapacity) {
        return fixedThreads(threadName, 1, queueCapacity);
    }

    public static ExecutorService fixedThreads(String threadName, int threadCount, int queueCapacity) {
        if (threadName == null || threadName.isBlank()) {
            throw new IllegalArgumentException("threadName must not be blank");
        }
        if (queueCapacity < 1) {
            throw new IllegalArgumentException("queueCapacity must be positive");
        }
        if (threadCount < 1) {
            throw new IllegalArgumentException("threadCount must be positive");
        }

        ThreadFactory threadFactory = runnable -> {
            Thread thread = new Thread(runnable, threadName);
            thread.setDaemon(true);
            return thread;
        };

        return new ThreadPoolExecutor(
                threadCount,
                threadCount,
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queueCapacity),
                threadFactory,
                new ThreadPoolExecutor.AbortPolicy()
        );
    }
}
