package ru.axefu.ogava;

public abstract class Application {
    private static final boolean macOS = System.getProperty("os.name").toLowerCase().startsWith("mac");

    public abstract void start() throws Exception;

    private static void start(Class<?> caller) {
        try {
            ((Application)caller.getConstructor().newInstance()).start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected static void launch(String[] args) {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        try {
            Class<?> caller = Class.forName(stackTrace[2].getClassName());
            if (macOS) {//Согласно интернету GLFW контекст на маках должен обрабатываться в main Thread потоке
                Thread programThread;
                Window.setGlfwThread(programThread = new Thread(() -> Application.start(caller)));
                if (!programThread.isInterrupted()) {
                    Window.setGlfwThread();
                }
            } else {
                Application.start(caller);
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

}
