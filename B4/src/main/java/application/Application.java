package application;

import simulator.DemoRunner;
import java.util.Arrays;
import java.awt.GraphicsEnvironment;

public class Application {
    public static void main(String[] args) {
        // If running in an environment without X (no DISPLAY) or --nogui passed,
        // avoid initializing Swing (which loads native AWT libraries) and run
        // the headless demo runner instead.
        boolean noGui = Arrays.asList(args).contains("--nogui");
        String display = System.getenv("DISPLAY");

        if (noGui || display == null || display.isEmpty()) {
            System.out.println("No DISPLAY found or --nogui specified: running headless demo...");
            try {
                DemoRunner demo = new DemoRunner();
                demo.ejecutarDemo();
            } catch (Exception e) {
                System.err.println("Error running demo in headless mode: " + e.getMessage());
                e.printStackTrace();
                System.exit(2);
            }
            return;
        }

        // If not headless, load Swing classes dynamically and start the login UI.
        try {
            if (!GraphicsEnvironment.isHeadless()) {
                // Load SwingUtilities reflectively to avoid loading AWT native libs at class load time
                Class<?> swingUtils = Class.forName("javax.swing.SwingUtilities");
                java.lang.reflect.Method invokeLater = swingUtils.getMethod("invokeLater", Runnable.class);
                invokeLater.invoke(null, (Runnable) () -> {
                    try {
                        // Load LoginFrame now (UI classes will be linked only here)
                        Class<?> loginCls = Class.forName("ui.LoginFrame");
                        Object loginFrame = loginCls.getDeclaredConstructor().newInstance();
                        loginCls.getMethod("setVisible", boolean.class).invoke(loginFrame, true);
                    } catch (Throwable t) {
                        throw new RuntimeException(t);
                    }
                });
            } else {
                throw new RuntimeException("Environment is headless");
            }
        } catch (Throwable t) {
            // Fallback: run headless demo when UI cannot be started
            System.err.println("Fallo al iniciar UI o entorno headless. Ejecutando demo en modo headless: " + t.getMessage());
            t.printStackTrace();
            try {
                DemoRunner demo = new DemoRunner();
                demo.ejecutarDemo();
            } catch (Exception e) {
                System.err.println("Error running demo fallback: " + e.getMessage());
                e.printStackTrace();
                System.exit(3);
            }
        }
    }
}
