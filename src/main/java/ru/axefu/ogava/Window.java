package ru.axefu.ogava;

import java.io.Closeable;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CountDownLatch;

public class Window implements Closeable {
    public static final Object lock = new Object();
    private static volatile Thread glfwThread;
    static {
        System.loadLibrary("ogava");
    }

    /**
     * Указатель GLFWwindow в памяти
     */
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

    /**
     * Сообщает окну о желании закрыться
     */
    @Override
    public void close() {
        invoke(() -> close(id));
    }

    private static ConcurrentLinkedDeque<Runnable> buffer2 = new ConcurrentLinkedDeque<>();
    private static ConcurrentLinkedDeque<Runnable> buffer = new ConcurrentLinkedDeque<>();
    private static void applyInvoke() {
        ConcurrentLinkedDeque<Runnable> runnables = buffer;
        buffer = buffer2;
        buffer2 = runnables;
        for (Runnable runnable : runnables) {
            runnable.run();
        }
        runnables.clear();
    }

    /**
     * Просит glfwThread отвлечься от работы с контекстом glfw
     * thread возвращается в окружение Java и выполняет <code>applyInvoke()</code>
     * Если glfwThread null, метод создаёт его и сразу вызывает <code>applyInvoke()</code>
     *
     * @param runnable функция, которая будет вызвана в glfwThread
     */
    public static void invokeLater(Runnable runnable) {
        buffer.addLast(runnable);
        if (glfwThread == null) {
            synchronized (lock) {
                if (glfwThread == null) {
                glfwThread = Thread.ofPlatform().start(() -> {
                    Window.applyInvoke();
                    loop();
                });
                return;
                }
            }
        }
        invokeLater();
    }

    /**
     * <code>invokeLater()</code>, но с блокировкой вызывающего потока, синхронный метод
     * @param runnable - функция, ктоорая будет вызвана в glfwThread
     */
    public static void invoke(Runnable runnable) {
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

    /**
     * Инициализация glfw контекста и создание окна
     * @param width ширина viewport в пикселях
     * @param height высота viewport в пикселях
     * @param title заголовок
     * @return указатель GLFWwindow
     */
    private native long create(int width, int height, String title);

    /**
     * Вызывает glfwSetWindowShouldClose(window);
     * @param id указатель GLFWwindow
     */
    private native void close(long id);

    /**
     * Жизненный цикл glfw, вызывается тем кто создал первое окно и не возвращается
     * до тех пор, пока не будет закрыто последнее окно.
     * Перед своим завершением метод назначает <code>glfwThread = null</code>
     */
    private static native void loop();

    /**
     * Сообщает glfwThread о том что основной поток хочет вызвать метод, происходит это изменением флага на true
     * насколько я помню операция присваивания флага атомарная
     */
    private static native void invokeLater();
}
