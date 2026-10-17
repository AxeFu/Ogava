package ru.axefu.ogava;

import java.io.Closeable;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CountDownLatch;

public class Window implements Closeable {
    private static final Object lock = new Object();
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
                throw new NullPointerException("glfwThread is null");
            }
        });
    }

    /**
     * Устанавливает входящий поток для glfw контекста и блокирует его, данный поток будет исполнять все invoke и invokeLater вызовы
     * @param runnable - вызов после назначения glfwThread = Thread.currentThread(), рекомендуется создать окно
     */
    public static void setGlfwThread(Runnable runnable) {
        if (glfwThread != null && glfwThread != Thread.currentThread()) {
            throw new IllegalStateException("glfwThread is already set, close all windows in program");
        }
        glfwThread = Thread.currentThread();
        runnable.run();
        setGlfwThread();
    }

    /**
     * Устанавливает входящий поток для glfw контекста и блокирует его, данный поток будет исполнять все invoke и invokeLater вызовы
     */
    public static void setGlfwThread() {
        if (glfwThread != null && glfwThread != Thread.currentThread()) {
            throw new IllegalStateException("glfwThread is already set, close all windows in program");
        }
        glfwThread = Thread.currentThread();
        while (Thread.currentThread() == glfwThread) {
            try {
                Window.applyInvoke();
                loop();
            } finally {
                synchronized (lock) {
                    if (runnables.isEmpty()) {
                        glfwThread = null;
                    }
                }
            }
        }
    }

    /**
     * Сообщает окну о желании закрыться
     */
    @Override
    public void close() {
        invokeLater(() -> close(id));
    }

    /**
     * Просит glfwThread отвлечься от работы с контекстом glfw
     * thread возвращается в окружение Java и выполняет <code>applyInvoke()</code>
     * Если glfwThread null, метод создаёт его и сразу вызывает <code>applyInvoke()</code>
     *
     * @param runnable функция, которая будет вызвана в glfwThread
     */
    public static void invokeLater(Runnable runnable) {
        synchronized (lock) {
            runnables.addLast(runnable);
            invokeLater();
            if (glfwThread == null) {
                Thread.ofPlatform().start(Window::setGlfwThread);
            }
        }
    }
    /**
     * <code>invokeLater()</code>, но с блокировкой вызывающего потока, синхронный метод
     * @param runnable - функция, ктоорая будет вызвана в glfwThread
     */
    public static void invoke(Runnable runnable) {
        if (Thread.currentThread() == glfwThread) {
            runnable.run();
            return;
        }
        CountDownLatch latch = new CountDownLatch(1);
        final Throwable[] throwable = {null};
        invokeLater(() -> {
            try {
                runnable.run();
            } catch (Throwable e) {
                throwable[0] = e;
            } finally {
                latch.countDown();
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (throwable[0] != null) {
            throw new RuntimeException(throwable[0]);
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

    private static final ConcurrentLinkedDeque<Runnable> runnables = new ConcurrentLinkedDeque<>();
    private static void applyInvoke() {
        Runnable task;
        while ((task = runnables.pollFirst()) != null) {
            task.run();
        }
    }

    /**
     * Сообщает glfwThread о том что основной поток хочет вызвать метод, происходит это изменением флага на true
     * насколько я помню операция присваивания флага атомарная
     */
    private static native void invokeLater();
}