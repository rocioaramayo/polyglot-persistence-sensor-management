package repository;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import connections.MongoPool;
import exceptions.ErrorConexionMongoException;
import modelo.MantenimientoConfig;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MantenimientoConfigMongoDAO {
    private final MongoCollection<Document> collection;

    public MantenimientoConfigMongoDAO() throws ErrorConexionMongoException {
        MongoDatabase db = MongoPool.getInstance().getDatabase();
        this.collection = db.getCollection("mantenimiento_config");
    }

    public void upsert(MantenimientoConfig config) {
        Document doc = new Document()
                .append("sensor_id", config.getSensorId())
                .append("frecuencia_dias", config.getFrecuenciaDias())
                .append("ultima_revision", toDate(config.getUltimaRevision()));
        collection.replaceOne(Filters.eq("sensor_id", config.getSensorId()), doc,
                new ReplaceOptions().upsert(true));
    }

    public MantenimientoConfig obtenerPorSensor(String sensorId) {
        Document doc = collection.find(Filters.eq("sensor_id", sensorId)).first();
        return mapear(doc);
    }

    public void actualizarUltimaRevision(String sensorId, LocalDateTime fecha) {
        collection.updateOne(Filters.eq("sensor_id", sensorId),
                new Document("$set", new Document("ultima_revision", toDate(fecha))));
    }

    public List<MantenimientoConfig> listarTodas() {
        List<MantenimientoConfig> configs = new ArrayList<>();
        for (Document doc : collection.find()) {
            MantenimientoConfig config = mapear(doc);
            if (config != null) {
                configs.add(config);
            }
        }
        return configs;
    }

    private MantenimientoConfig mapear(Document doc) {
        if (doc == null) {
            return null;
        }
        MantenimientoConfig config = new MantenimientoConfig();
        ObjectId id = doc.getObjectId("_id");
        if (id != null) {
            config.setId(id.toHexString());
        }
        config.setSensorId(doc.getString("sensor_id"));
        config.setFrecuenciaDias(doc.getInteger("frecuencia_dias"));
        config.setUltimaRevision(toLocalDateTime(doc.getDate("ultima_revision")));
        return config;
    }

    private Date toDate(LocalDateTime fecha) {
        if (fecha == null) {
            return null;
        }
        return Date.from(fecha.atZone(ZoneId.systemDefault()).toInstant());
    }

    private LocalDateTime toLocalDateTime(Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
}
