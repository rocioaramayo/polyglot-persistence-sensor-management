package application;

import java.awt.GraphicsEnvironment;
import java.util.Arrays;

public class Application {
    public static void main(String[] args) {
        boolean noGui = Arrays.asList(args).contains("--nogui");
        String display = System.getenv("DISPLAY");

        if (noGui || display == null || display.isEmpty() || GraphicsEnvironment.isHeadless()) {
            System.err.println("El modo sin interfaz gráfica ya no está soportado (DemoRunner eliminado). Ejecuta la aplicación en un entorno con GUI.");
            System.exit(1);
        }

        try {
            Class<?> swingUtils = Class.forName("javax.swing.SwingUtilities");
            java.lang.reflect.Method invokeLater = swingUtils.getMethod("invokeLater", Runnable.class);
            invokeLater.invoke(null, (Runnable) () -> {
                try {
                    Class<?> loginCls = Class.forName("ui.LoginFrame");
                    Object loginFrame = loginCls.getDeclaredConstructor().newInstance();
                    loginCls.getMethod("setVisible", boolean.class).invoke(loginFrame, true);
                } catch (Throwable t) {
                    throw new RuntimeException("No se pudo iniciar la UI", t);
                }
            });
        } catch (Throwable t) {
            System.err.println("No se pudo iniciar la interfaz gráfica: " + t.getMessage());
            t.printStackTrace();
            System.exit(2);
        }
    }
}
