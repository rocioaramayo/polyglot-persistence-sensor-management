package services;

import exceptions.ErrorConexionMongoException;
import modelo.AlertRule;
import repository.AlertRuleMongoDAO;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

public class AlertRuleService {
    private static AlertRuleService instance;

    private AlertRuleService() {}

    public static AlertRuleService getInstance() {
        if (instance == null) {
            instance = new AlertRuleService();
        }
        return instance;
    }

    public String crear(AlertRule rule) throws ErrorConexionMongoException {
        validar(rule);
        AlertRuleMongoDAO dao = new AlertRuleMongoDAO();
        rule.setFechaCreacion(LocalDateTime.now());
        if (rule.getVentanaMinutos() == null || rule.getVentanaMinutos() <= 0) {
            rule.setVentanaMinutos(60);
        }
        if (rule.getSeveridad() == null || rule.getSeveridad().isBlank()) {
            rule.setSeveridad("MEDIA");
        }
        return dao.insertar(rule);
    }

    public List<AlertRule> listarTodas() {
        try {
            return new AlertRuleMongoDAO().listarTodas();
        } catch (ErrorConexionMongoException e) {
            System.err.println("No se pudieron obtener las reglas de alerta: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<AlertRule> listarActivas() throws ErrorConexionMongoException {
        return new AlertRuleMongoDAO().listarActivas();
    }

    public void actualizarEstado(String id, boolean activo) throws ErrorConexionMongoException {
        new AlertRuleMongoDAO().actualizarEstado(id, activo);
    }

    public void registrarSeguimiento(String id, LocalDateTime evaluacion, LocalDateTime ultimaAlerta)
            throws ErrorConexionMongoException {
        new AlertRuleMongoDAO().registrarSeguimiento(id, evaluacion, ultimaAlerta);
    }

    private void validar(AlertRule rule) {
        if (rule == null) {
            throw new IllegalArgumentException("La regla no puede ser nula");
        }
        if (rule.getNombre() == null || rule.getNombre().isBlank()) {
            throw new IllegalArgumentException("Debe indicar un nombre para la regla");
        }
        if (rule.getVentanaMinutos() != null && rule.getVentanaMinutos() < 1) {
            throw new IllegalArgumentException("La ventana de minutos debe ser mayor a cero");
        }
        if (rule.getSeveridad() != null && !rule.getSeveridad().isBlank()) {
            String upper = rule.getSeveridad().toUpperCase();
            if (!upper.equals("BAJA") && !upper.equals("MEDIA") && !upper.equals("ALTA") && !upper.equals("CRITICA")) {
                throw new IllegalArgumentException("La severidad indicada no es válida");
            }
        }
        if (rule.getTemperaturaMin() != null && rule.getTemperaturaMax() != null
                && rule.getTemperaturaMin() > rule.getTemperaturaMax()) {
            throw new IllegalArgumentException("El rango de temperatura es inválido");
        }
        if (rule.getHumedadMin() != null && rule.getHumedadMax() != null
                && rule.getHumedadMin() > rule.getHumedadMax()) {
            throw new IllegalArgumentException("El rango de humedad es inválido");
        }
        if (rule.getFechaDesde() != null && rule.getFechaHasta() != null
                && rule.getFechaDesde().isAfter(rule.getFechaHasta())) {
            throw new IllegalArgumentException("El rango de fechas es inválido");
        }
    }
}
