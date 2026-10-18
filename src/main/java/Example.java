import ru.axefu.ogava.Application;
import ru.axefu.ogava.Window;

public class Example extends Application {static void main(String[] args) {launch(args);}
    @Override
    public void start() throws InterruptedException {
        Window window = new Window(600, 400, "Test");
        Window window2 = new Window(600, 400, "Test2");
        Thread.ofPlatform().start(() -> {
            try (Window window3 = new Window(600, 400, "Test3")) {
                System.out.println(window3);
            }
        });
        window.close();
        window2.close();
        Thread.sleep(1000);
        Window window4 = new Window(600, 400, "Test4");
        Window window5 = new Window(600, 400, "Test5");
        Window window6 = new Window(600, 400, "Test6");
    }
}