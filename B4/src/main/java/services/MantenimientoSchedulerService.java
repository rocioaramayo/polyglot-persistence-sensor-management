package services;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class MantenimientoSchedulerService {
    private static MantenimientoSchedulerService instance;

    private final ScheduledExecutorService executor;
    private volatile boolean iniciado;

    private MantenimientoSchedulerService() {
        this.executor = Executors.newSingleThreadScheduledExecutor(new DaemonThreadFactory());
    }

    public static MantenimientoSchedulerService getInstance() {
        if (instance == null) {
            synchronized (MantenimientoSchedulerService.class) {
                if (instance == null) {
                    instance = new MantenimientoSchedulerService();
                }
            }
        }
        return instance;
    }

    public synchronized void iniciar() {
        if (iniciado) {
            return;
        }
        executor.scheduleAtFixedRate(() -> {
            try {
                MantenimientoTaskService.getInstance().generarTareasProgramadas();
            } catch (Exception e) {
                System.err.println("No se pudo generar tareas de mantenimiento: " + e.getMessage());
            }
        }, 60, 3600, TimeUnit.SECONDS);
        iniciado = true;
    }

    private static class DaemonThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = Executors.defaultThreadFactory().newThread(r);
            thread.setDaemon(true);
            thread.setName("MantenimientoScheduler-" + thread.getId());
            return thread;
        }
    }
}
