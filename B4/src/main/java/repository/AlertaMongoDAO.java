package repository;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import connections.MongoPool;
import modelo.Alerta;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AlertaMongoDAO {
    private final MongoCollection<Document> collection;

    public AlertaMongoDAO() throws exceptions.ErrorConexionMongoException {
        MongoDatabase db = MongoPool.getInstance().getDatabase();
        this.collection = db.getCollection("alertas");
    }

    public String insertar(Alerta alerta) {
        Document doc = new Document()
                .append("tipo", alerta.getTipo())
                .append("fecha", Date.from(alerta.getFecha().atZone(ZoneId.systemDefault()).toInstant()))
                .append("descripcion", alerta.getDescripcion())
                .append("severidad", alerta.getSeveridad())
                .append("estado", alerta.getEstado());
        
        if (alerta.getSensorId() != null) {
            doc.append("sensor_id", alerta.getSensorId());
        }
        if (alerta.getUsuarioId() != null) {
            doc.append("usuario_id", alerta.getUsuarioId());
        }
        
        collection.insertOne(doc);
        return doc.getObjectId("_id").toString();
    }

    public List<Alerta> listarActivas() {
        List<Alerta> alertas = new ArrayList<>();
        for (Document doc : collection.find(Filters.eq("estado", "ACTIVA"))) {
            Alerta alerta = new Alerta();
            alerta.setId(doc.getObjectId("_id").toString());
            alerta.setTipo(doc.getString("tipo"));
            alerta.setDescripcion(doc.getString("descripcion"));
            alerta.setSeveridad(doc.getString("severidad"));
            alerta.setEstado(doc.getString("estado"));
            alerta.setFecha(doc.getDate("fecha").toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDateTime());
            alertas.add(alerta);
        }
        return alertas;
    }

    public List<Alerta> listarPorUsuario(String usuarioId) {
        List<Alerta> alertas = new ArrayList<>();
        for (Document doc : collection.find(Filters.eq("usuario_id", usuarioId))) {
            Alerta alerta = new Alerta();
            alerta.setId(doc.getObjectId("_id").toString());
            alerta.setTipo(doc.getString("tipo"));
            alerta.setDescripcion(doc.getString("descripcion"));
            alerta.setEstado(doc.getString("estado"));
            alertas.add(alerta);
        }
        return alertas;
    }

    public void resolver(String id, String resueltoPor) {
        Document update = new Document()
                .append("estado", "RESUELTA")
                .append("fecha_resolucion", new Date())
                .append("resuelto_por", resueltoPor);
        
        collection.updateOne(
                Filters.eq("_id", new ObjectId(id)),
                new Document("$set", update)
        );
    }
}
