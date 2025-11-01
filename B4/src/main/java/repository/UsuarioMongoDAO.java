package repository;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import connections.MongoPool;
import exceptions.ErrorConexionMongoException;
import modelo.Usuario;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class UsuarioMongoDAO {
    private final MongoCollection<Document> collection;

    public UsuarioMongoDAO() throws ErrorConexionMongoException {
        MongoDatabase db = MongoPool.getInstance().getDatabase();
        this.collection = db.getCollection("usuarios");
    }

    public String insertar(Usuario usuario) {
        Document doc = new Document()
                .append("nombre", usuario.getNombre())
                .append("apellido", usuario.getApellido())
                .append("email", usuario.getEmail())
                .append("password", usuario.getPasswordHash())
                .append("rol", usuario.getRol())
                .append("activo", usuario.getActivo())
                .append("fecha_registro", Date.from(usuario.getFechaRegistro().atZone(ZoneId.systemDefault()).toInstant()));
        
        collection.insertOne(doc);
        return doc.getObjectId("_id").toString();
    }

    public Usuario buscarPorEmail(String email) {
        Document doc = collection.find(Filters.eq("email", email)).first();
        if (doc == null) return null;

        Usuario usuario = new Usuario();
        usuario.setId(doc.getObjectId("_id").toString());
        usuario.setNombre(doc.getString("nombre"));
        usuario.setApellido(doc.getString("apellido"));
        usuario.setEmail(doc.getString("email"));
        usuario.setPasswordHash(doc.getString("password"));
        usuario.setRol(doc.getString("rol"));
        usuario.setActivo(doc.getBoolean("activo"));
        usuario.setFechaRegistro(doc.getDate("fecha_registro").toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDateTime());
        return usuario;
    }

    public Usuario buscarPorId(String id) {
        Document doc = collection.find(Filters.eq("_id", new ObjectId(id))).first();
        if (doc == null) return null;

        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNombre(doc.getString("nombre"));
        usuario.setApellido(doc.getString("apellido"));
        usuario.setEmail(doc.getString("email"));
        usuario.setPasswordHash(doc.getString("password"));
        usuario.setRol(doc.getString("rol"));
        usuario.setActivo(doc.getBoolean("activo"));
        usuario.setFechaRegistro(doc.getDate("fecha_registro").toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDateTime());
        return usuario;
    }

    public List<Usuario> listarTodos() {
        List<Usuario> usuarios = new ArrayList<>();
        for (Document doc : collection.find()) {
            Usuario usuario = new Usuario();
            usuario.setId(doc.getObjectId("_id").toString());
            usuario.setNombre(doc.getString("nombre"));
            usuario.setApellido(doc.getString("apellido"));
            usuario.setEmail(doc.getString("email"));
            usuario.setRol(doc.getString("rol"));
            usuario.setActivo(doc.getBoolean("activo"));
            usuarios.add(usuario);
        }
        return usuarios;
    }

    public List<Usuario> listarPorRol(String rol) {
        List<Usuario> usuarios = new ArrayList<>();
        for (Document doc : collection.find(Filters.eq("rol", rol))) {
            Usuario usuario = new Usuario();
            usuario.setId(doc.getObjectId("_id").toString());
            usuario.setNombre(doc.getString("nombre"));
            usuario.setApellido(doc.getString("apellido"));
            usuario.setEmail(doc.getString("email"));
            usuario.setRol(doc.getString("rol"));
            usuario.setActivo(doc.getBoolean("activo"));
            usuarios.add(usuario);
        }
        return usuarios;
    }

    public void actualizar(Usuario usuario) {
        Document update = new Document()
                .append("nombre", usuario.getNombre())
                .append("apellido", usuario.getApellido())
                .append("email", usuario.getEmail())
                .append("rol", usuario.getRol())
                .append("activo", usuario.getActivo());
        
        if (usuario.getPasswordHash() != null) {
            update.append("password", usuario.getPasswordHash());
        }
        
        collection.updateOne(
                Filters.eq("_id", new ObjectId(usuario.getId())),
                new Document("$set", update)
        );
    }

    public void eliminar(String id) {
        collection.deleteOne(Filters.eq("_id", new ObjectId(id)));
    }
}
