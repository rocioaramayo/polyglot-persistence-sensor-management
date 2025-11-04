package application;

import javax.swing.SwingUtilities;

import services.ProcesoService;

public class Application {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                try {
                    ProcesoService.getInstance().inicializarProcesosPorDefecto();
                } catch (Exception initEx) {
                    System.err.println("Advertencia: no se pudieron precargar los procesos por defecto: " + initEx.getMessage());
                }

                Class<?> loginCls = Class.forName("ui.LoginFrame");
                Object loginFrame = loginCls.getDeclaredConstructor().newInstance();
                loginCls.getMethod("setVisible", boolean.class).invoke(loginFrame, true);

                // Marca de arranque para confirmar que este JAR es el que corre
                System.err.println("APP BOOT OK :: " + System.currentTimeMillis());
            } catch (Throwable t) {
                System.err.println("No se pudo inicializar la interfaz gráfica: " + t.getMessage());
                t.printStackTrace();
            }
        });
    }
}
