package services;

import exceptions.ErrorConexionMongoException;
import modelo.Usuario;
import repository.UsuarioMongoDAO;

import java.util.List;
import java.util.stream.Collectors;

public class UsuarioService {
    private static UsuarioService instance;

    private UsuarioService() {}

    public static UsuarioService getInstance() {
        if (instance == null) {
            instance = new UsuarioService();
        }
        return instance;
    }

    public List<Usuario> listarUsuariosActivos() throws ErrorConexionMongoException {
        UsuarioMongoDAO dao = new UsuarioMongoDAO();
        return dao.listarTodos().stream()
                .filter(u -> u.getActivo() == null || Boolean.TRUE.equals(u.getActivo()))
                .collect(Collectors.toList());
    }

    public Usuario obtenerPorId(String usuarioId) throws ErrorConexionMongoException {
        if (usuarioId == null || usuarioId.isBlank()) {
            return null;
        }
        UsuarioMongoDAO dao = new UsuarioMongoDAO();
        return dao.buscarPorId(usuarioId);
    }
}
