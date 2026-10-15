package ru.axefu.ogava;

public class Window {
    static {
        System.loadLibrary("ogava");
    }
    private static Thread glfwThread;

    private final long id;
    public Window(int width, int height, String title) {
        id = create(width, height, title);
        if (id < 0) {
            throw new RuntimeException("Failed to create window: " + id);
        }
        if (glfwThread == null || glfwThread.isInterrupted()) {
            glfwThread = Thread.ofPlatform().start(Window::loop);
        }
    }

    public void close() {
        close(id);
    }

    private native long create(int width, int height, String title);
    private native void close(long id);
    private static native void loop();
}
