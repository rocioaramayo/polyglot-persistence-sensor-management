package repository;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import connections.MongoPool;
import modelo.Proceso;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;

public class ProcesoMongoDAO {
    private final MongoCollection<Document> collection;

    public ProcesoMongoDAO() throws exceptions.ErrorConexionMongoException {
        MongoDatabase db = MongoPool.getInstance().getDatabase();
        this.collection = db.getCollection("procesos");
    }

    public String insertar(Proceso proceso) {
        Document doc = new Document()
                .append("nombre", proceso.getNombre())
                .append("tipo", proceso.getTipo())
                .append("descripcion", proceso.getDescripcion())
                .append("costo", proceso.getCosto())
                .append("activo", proceso.getActivo() == null ? Boolean.TRUE : proceso.getActivo());
        
        collection.insertOne(doc);
        return doc.getObjectId("_id").toString();
    }

    public Proceso buscarPorId(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        Document doc = collection.find(Filters.eq("_id", new ObjectId(id))).first();
        return mapToProceso(doc);
    }

    public Proceso buscarPorNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return null;
        }
        Document doc = collection.find(Filters.eq("nombre", nombre)).first();
        return mapToProceso(doc);
    }

    public List<Proceso> listarActivos() {
        List<Proceso> procesos = new ArrayList<>();
        for (Document doc : collection.find(Filters.eq("activo", true))) {
            Proceso proceso = mapToProceso(doc);
            if (proceso != null) {
                procesos.add(proceso);
            }
        }
        return procesos;
    }

    public void actualizar(String id, Proceso proceso) {
        if (id == null || id.isBlank() || proceso == null) {
            return;
        }
        Document update = new Document()
                .append("nombre", proceso.getNombre())
                .append("tipo", proceso.getTipo())
                .append("descripcion", proceso.getDescripcion())
                .append("costo", proceso.getCosto());
        if (proceso.getActivo() != null) {
            update.append("activo", proceso.getActivo());
        }
        
        collection.updateOne(
                Filters.eq("_id", new ObjectId(id)),
                new Document("$set", update)
        );
    }

    private Proceso mapToProceso(Document doc) {
        if (doc == null) {
            return null;
        }
        Proceso proceso = new Proceso();
        ObjectId objectId = doc.getObjectId("_id");
        if (objectId != null) {
            proceso.setId(objectId.toHexString());
        }
        proceso.setNombre(doc.getString("nombre"));
        proceso.setTipo(doc.getString("tipo"));
        proceso.setDescripcion(doc.getString("descripcion"));
        if (doc.containsKey("costo")) {
            Object costoObj = doc.get("costo");
            if (costoObj instanceof Number) {
                proceso.setCosto(((Number) costoObj).doubleValue());
            }
        }
        if (doc.containsKey("activo")) {
            proceso.setActivo(doc.getBoolean("activo", true));
        }
        return proceso;
    }
}
