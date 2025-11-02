package services;

import exceptions.ErrorConexionMongoException;
import modelo.Proceso;
import repository.ProcesoMongoDAO;

import java.util.Collections;
import java.util.List;

public class ProcesoService {
    private static ProcesoService instance;

    private ProcesoService() {}

    public static ProcesoService getInstance() {
        if (instance == null) {
            instance = new ProcesoService();
        }
        return instance;
    }

    public List<Proceso> listarActivos() throws ErrorConexionMongoException {
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
}
