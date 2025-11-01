package services;

import exceptions.ErrorConexionMongoException;
import modelo.Grupo;
import repository.GrupoMongoDAO;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class GrupoService {
    private static GrupoService instance;

    private GrupoService() {}

    public static GrupoService getInstance() {
        if (instance == null) {
            instance = new GrupoService();
        }
        return instance;
    }

    public Grupo crearGrupo(String nombre, String descripcion, List<String> miembros) throws ErrorConexionMongoException {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del grupo es obligatorio");
        }
        Grupo grupo = new Grupo(nombre.trim(), descripcion != null ? descripcion.trim() : null,
                miembros != null ? miembros.stream().filter(Objects::nonNull)
                        .map(String::trim).filter(s -> !s.isEmpty()).distinct().collect(Collectors.toList())
                        : Collections.emptyList());
        return GrupoMongoDAO.getInstance().crear(grupo);
    }

    public Grupo obtenerGrupo(String id) throws ErrorConexionMongoException {
        if (id == null || id.isBlank()) {
            return null;
        }
        return GrupoMongoDAO.getInstance().obtenerPorId(id);
    }

    public List<Grupo> listarGrupos() throws ErrorConexionMongoException {
        return GrupoMongoDAO.getInstance().listarTodos();
    }

    public List<Grupo> listarGruposDeUsuario(String usuarioId) throws ErrorConexionMongoException {
        if (usuarioId == null || usuarioId.isBlank()) {
            return Collections.emptyList();
        }
        return GrupoMongoDAO.getInstance().listarPorMiembro(usuarioId);
    }

    public List<String> obtenerIdsGruposDeUsuario(String usuarioId) throws ErrorConexionMongoException {
        return listarGruposDeUsuario(usuarioId).stream()
                .map(Grupo::getId)
                .collect(Collectors.toList());
    }
}
