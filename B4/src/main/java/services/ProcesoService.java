package services;

import exceptions.ErrorConexionMongoException;
import modelo.Proceso;
import repository.ProcesoMongoDAO;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ProcesoService {
    private static ProcesoService instance;

    private static final List<ProcesoDefinicion> PROCESOS_PREDETERMINADOS = Arrays.asList(
            new ProcesoDefinicion(
                    "Informe de extremos climáticos",
                    "Informe de humedad y temperaturas máximas y mínimas por ciudades, zonas y países en rangos anualizados o mensualizados.",
                    "INFORME_MAX_MIN",
                    250.0
            ),
            new ProcesoDefinicion(
                    "Informe de promedios climáticos",
                    "Informe de humedad y temperaturas promedio por ciudades, zonas y países en rangos anualizados o mensualizados.",
                    "INFORME_PROMEDIOS",
                    220.0
            ),
            new ProcesoDefinicion(
                    "Consultas en línea",
                    "Servicios de consultas en línea sobre información de sensores por ciudad, zona o país dentro de un rango de fechas.",
                    "CONSULTA_ONLINE",
                    150.0
            ),
            new ProcesoDefinicion(
                    "Procesos periódicos programados",
                    "Procesos periódicos de consultas sobre humedad y temperaturas por ciudades, zonas y países de forma anualizada o mensualizada.",
                    "PROCESO_PERIODICO",
                    200.0
            )
    );
    private static final List<String> PROCESOS_OBSOLETOS = Collections.singletonList("Alertas de condiciones críticas");

    private ProcesoService() {}

    public static ProcesoService getInstance() {
        if (instance == null) {
            instance = new ProcesoService();
        }
        return instance;
    }

    public List<Proceso> listarActivos() throws ErrorConexionMongoException {
        inicializarProcesosPorDefecto();
        ProcesoMongoDAO dao = new ProcesoMongoDAO();
        List<Proceso> procesos = dao.listarActivos();
        return procesos != null ? procesos : Collections.emptyList();
    }

    public Proceso obtenerPorId(String id) throws ErrorConexionMongoException {
        if (id == null || id.isBlank()) {
            return null;
        }
        ProcesoMongoDAO dao = new ProcesoMongoDAO();
        return dao.buscarPorId(id);
    }

    public void inicializarProcesosPorDefecto() {
        try {
            ProcesoMongoDAO dao = new ProcesoMongoDAO();
            for (ProcesoDefinicion definicion : PROCESOS_PREDETERMINADOS) {
                Proceso deseado = definicion.crearInstancia();
                Proceso existente = dao.buscarPorNombre(definicion.nombre);
                if (existente == null) {
                    dao.insertar(deseado);
                } else if (requiereActualizacion(existente, deseado)) {
                    deseado.setActivo(true);
                    dao.actualizar(existente.getId(), deseado);
                }
            }
            desactivarProcesosObsoletos(dao);
        } catch (ErrorConexionMongoException e) {
            System.err.println("No se pudieron inicializar los procesos predeterminados: " + e.getMessage());
        }
    }

    private boolean requiereActualizacion(Proceso existente, Proceso deseado) {
        if (existente == null) {
            return true;
        }
        if (!Objects.equals(existente.getDescripcion(), deseado.getDescripcion())) {
            return true;
        }
        if (!Objects.equals(existente.getTipo(), deseado.getTipo())) {
            return true;
        }
        if (!Objects.equals(existente.getCosto(), deseado.getCosto())) {
            return true;
        }
        if (existente.getActivo() == null || !existente.getActivo()) {
            return true;
        }
        return false;
    }

    private void desactivarProcesosObsoletos(ProcesoMongoDAO dao) throws ErrorConexionMongoException {
        for (String nombre : PROCESOS_OBSOLETOS) {
            Proceso existente = dao.buscarPorNombre(nombre);
            if (existente != null && (existente.getActivo() == null || existente.getActivo())) {
                Proceso actualizado = new Proceso();
                actualizado.setNombre(existente.getNombre());
                actualizado.setDescripcion(existente.getDescripcion());
                actualizado.setTipo(existente.getTipo());
                actualizado.setCosto(existente.getCosto());
                actualizado.setActivo(false);
                dao.actualizar(existente.getId(), actualizado);
            }
        }
    }

    private static class ProcesoDefinicion {
        private final String nombre;
        private final String descripcion;
        private final String tipo;
        private final double costo;

        private ProcesoDefinicion(String nombre, String descripcion, String tipo, double costo) {
            this.nombre = nombre;
            this.descripcion = descripcion;
            this.tipo = tipo;
            this.costo = costo;
        }

        private Proceso crearInstancia() {
            Proceso proceso = new Proceso(nombre, descripcion, tipo, costo);
            proceso.setActivo(true);
            return proceso;
        }
    }
}
