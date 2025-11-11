package services;

import exceptions.ErrorConexionMongoException;
import modelo.ReportePeriodico;
import repository.ReportePeriodicoMongoDAO;

import java.util.Collections;
import java.util.List;

public class ReportePeriodicoService {
    private static ReportePeriodicoService instance;

    private ReportePeriodicoService() {}

    public static ReportePeriodicoService getInstance() {
        if (instance == null) {
            instance = new ReportePeriodicoService();
        }
        return instance;
    }

    public List<ReportePeriodico> listarRecientes(int limite) throws ErrorConexionMongoException {
        ReportePeriodicoMongoDAO dao = new ReportePeriodicoMongoDAO();
        List<ReportePeriodico> reportes = dao.listarRecientes(limite);
        return reportes != null ? reportes : Collections.emptyList();
    }
}
