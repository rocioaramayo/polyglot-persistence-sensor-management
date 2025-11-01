package repository;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import connections.MongoPool;
import exceptions.ErrorConexionMongoException;
import modelo.Grupo;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class GrupoMongoDAO {
    private static GrupoMongoDAO instance;
    private final MongoCollection<Document> collection;

    private GrupoMongoDAO() throws ErrorConexionMongoException {
        MongoDatabase db = MongoPool.getInstance().getDatabase();
        this.collection = db.getCollection("grupos");
    }

    public static GrupoMongoDAO getInstance() throws ErrorConexionMongoException {
        if (instance == null) {
            instance = new GrupoMongoDAO();
        }
        return instance;
    }

    public Grupo crear(Grupo grupo) throws ErrorConexionMongoException {
        try {
            if (grupo.getMiembros() == null) {
                grupo.setMiembros(new ArrayList<>());
            }
            if (grupo.getAdministradores() == null) {
                grupo.setAdministradores(new ArrayList<>());
            }
            if (grupo.getFechaCreacion() == null) {
                grupo.setFechaCreacion(LocalDateTime.now());
            }
            if (grupo.getActivo() == null) {
                grupo.setActivo(true);
            }

            Document doc = new Document()
                    .append("nombre", grupo.getNombre())
                    .append("descripcion", grupo.getDescripcion())
                    .append("miembros", grupo.getMiembros())
                    .append("administradores", grupo.getAdministradores())
                    .append("fecha_creacion", Date.from(grupo.getFechaCreacion()
                            .atZone(ZoneId.systemDefault()).toInstant()))
                    .append("activo", grupo.getActivo());

            collection.insertOne(doc);
            grupo.setId(doc.getObjectId("_id").toString());
            return grupo;
        } catch (Exception e) {
            throw new ErrorConexionMongoException("Error al crear el grupo", e);
        }
    }

    public Grupo obtenerPorId(String id) throws ErrorConexionMongoException {
        try {
            Document doc = collection.find(Filters.eq("_id", new ObjectId(id))).first();
            return doc != null ? mapearGrupo(doc) : null;
        } catch (Exception e) {
            throw new ErrorConexionMongoException("Error al obtener grupo " + id, e);
        }
    }

    public List<Grupo> listarTodos() throws ErrorConexionMongoException {
        try {
            List<Grupo> grupos = new ArrayList<>();
            for (Document doc : collection.find()) {
                grupos.add(mapearGrupo(doc));
            }
            return grupos;
        } catch (Exception e) {
            throw new ErrorConexionMongoException("Error al listar grupos", e);
        }
    }

    public List<Grupo> listarPorMiembro(String usuarioId) throws ErrorConexionMongoException {
        try {
            List<Grupo> grupos = new ArrayList<>();
            for (Document doc : collection.find(Filters.in("miembros", usuarioId))) {
                grupos.add(mapearGrupo(doc));
            }
            return grupos;
        } catch (Exception e) {
            throw new ErrorConexionMongoException("Error al listar grupos del usuario", e);
        }
    }

    private Grupo mapearGrupo(Document doc) {
        Grupo grupo = new Grupo();
        grupo.setId(doc.getObjectId("_id").toString());
        grupo.setNombre(doc.getString("nombre"));
        grupo.setDescripcion(doc.getString("descripcion"));
        grupo.setMiembros((List<String>) doc.getList("miembros", String.class, new ArrayList<>()));
        grupo.setAdministradores((List<String>) doc.getList("administradores", String.class, new ArrayList<>()));

        Date fecha = doc.getDate("fecha_creacion");
        if (fecha != null) {
            grupo.setFechaCreacion(fecha.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
        }
        grupo.setActivo(doc.getBoolean("activo", Boolean.TRUE));
        return grupo;
    }
}
