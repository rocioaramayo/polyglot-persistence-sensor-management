package repository;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Sorts;
import connections.MongoPool;
import exceptions.ErrorConexionMongoException;
import modelo.ControlSensor;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ControlSensorMongoDAO {
    private final MongoCollection<Document> collection;

    public ControlSensorMongoDAO() throws ErrorConexionMongoException {
        MongoDatabase db = MongoPool.getInstance().getDatabase();
        this.collection = db.getCollection("control_sensores");
    }

    public String insertar(ControlSensor control) {
        Document doc = new Document()
                .append("sensor_id", control.getSensorId())
                .append("tecnico_id", control.getTecnicoId())
                .append("fecha_revision", toDate(control.getFechaRevision()))
                .append("estado_sensor", control.getEstadoSensor())
                .append("observaciones", control.getObservaciones())
                .append("acciones_realizadas", control.getAccionesRealizadas())
                .append("proxima_revision", toDate(control.getProximaRevision()));
        collection.insertOne(doc);
        ObjectId id = doc.getObjectId("_id");
        if (id != null) {
            control.setId(id.toHexString());
        }
        return control.getId();
    }

    public List<ControlSensor> listarRecientes(int limit) {
        List<ControlSensor> controles = new ArrayList<>();
        for (Document doc : collection.find().sort(Sorts.descending("fecha_revision")).limit(limit)) {
            ControlSensor control = mapear(doc);
            if (control != null) controles.add(control);
        }
        return controles;
    }

    public ControlSensor obtenerUltimoPorSensor(String sensorId) {
        Document doc = collection.find(new Document("sensor_id", sensorId))
                .sort(Sorts.descending("fecha_revision"))
                .limit(1)
                .first();
        return mapear(doc);
    }

    private ControlSensor mapear(Document doc) {
        if (doc == null) {
            return null;
        }
        ControlSensor control = new ControlSensor();
        ObjectId id = doc.getObjectId("_id");
        if (id != null) {
            control.setId(id.toHexString());
        }
        control.setSensorId(doc.getString("sensor_id"));
        control.setTecnicoId(doc.getInteger("tecnico_id"));
        control.setFechaRevision(toLocalDateTime(doc.getDate("fecha_revision")));
        control.setEstadoSensor(doc.getString("estado_sensor"));
        control.setObservaciones(doc.getString("observaciones"));
        control.setAccionesRealizadas(doc.getList("acciones_realizadas", String.class));
        control.setProximaRevision(toLocalDateTime(doc.getDate("proxima_revision")));
        return control;
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
