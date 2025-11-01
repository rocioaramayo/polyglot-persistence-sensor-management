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
                .append("activo", true);
        
        collection.insertOne(doc);
        return doc.getObjectId("_id").toString();
    }

    public Proceso buscarPorId(String id) {
        Document doc = collection.find(Filters.eq("_id", new ObjectId(id))).first();
        if (doc == null) return null;

        Proceso proceso = new Proceso();
        proceso.setNombre(doc.getString("nombre"));
        proceso.setTipo(doc.getString("tipo"));
        proceso.setDescripcion(doc.getString("descripcion"));
        proceso.setCosto(doc.getDouble("costo"));
        if (doc.containsKey("activo")) {
            proceso.setActivo(doc.getBoolean("activo", true));
        }
        return proceso;
    }

    public List<Proceso> listarActivos() {
        List<Proceso> procesos = new ArrayList<>();
        for (Document doc : collection.find(Filters.eq("activo", true))) {
            Proceso proceso = new Proceso();
            proceso.setNombre(doc.getString("nombre"));
            proceso.setTipo(doc.getString("tipo"));
            proceso.setDescripcion(doc.getString("descripcion"));
            proceso.setCosto(doc.getDouble("costo"));
            if (doc.containsKey("activo")) {
                proceso.setActivo(doc.getBoolean("activo", true));
            }
            procesos.add(proceso);
        }
        return procesos;
    }

    public void actualizar(String id, Proceso proceso) {
        Document update = new Document()
                .append("nombre", proceso.getNombre())
                .append("tipo", proceso.getTipo())
                .append("descripcion", proceso.getDescripcion())
                .append("costo", proceso.getCosto());
        
        collection.updateOne(
                Filters.eq("_id", new ObjectId(id)),
                new Document("$set", update)
        );
    }
}
