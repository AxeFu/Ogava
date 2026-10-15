package ru.axefu.ogava;

import java.io.Closeable;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;

public class Window implements Closeable {
    static {
        System.loadLibrary("ogava");
    }
    private static Thread glfwThread;

    private long id;
    public Window(int width, int height, String title) {
        invoke(() -> {
            id = create(width, height, title);
            if (id < 0) {
                throw new RuntimeException("Failed to create window: " + id);
            }
            if (glfwThread == null) {
                glfwThread = Thread.currentThread();
                loop();
            }
        });
    }

    @Override
    public void close() {
        invoke(() -> close(id));
    }

    private static Queue<Runnable> buffer2 = new LinkedList<>();
    private static Queue<Runnable> buffer = new LinkedList<>();
    private static void applyInvoke() {
        Queue<Runnable> runnables = buffer;
        buffer = buffer2;
        buffer2 = runnables;
        for (Runnable runnable : runnables) {
            runnable.run();
        }
        runnables.clear();
    }
    private static native void invokeLater();
    private static void invokeLater(Runnable runnable) {
        buffer.add(runnable);
        if (glfwThread == null) {
            glfwThread = Thread.ofPlatform().start(() -> {
                Window.applyInvoke();
                loop();
            });
            return;
        }
        invokeLater();
    }
    private static void invoke(Runnable runnable) {
        CountDownLatch latch = new CountDownLatch(1);
        invokeLater(() -> {
            runnable.run();
            invokeLater(latch::countDown);
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private native long create(int width, int height, String title);
    private native void close(long id);
    private static native void loop();
}
