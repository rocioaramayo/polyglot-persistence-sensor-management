package repository;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import connections.MongoPool;
import exceptions.ErrorConexionMongoException;
import modelo.MantenimientoTask;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MantenimientoTaskMongoDAO {
    private final MongoCollection<Document> collection;

    public MantenimientoTaskMongoDAO() throws ErrorConexionMongoException {
        MongoDatabase db = MongoPool.getInstance().getDatabase();
        this.collection = db.getCollection("mantenimiento_tasks");
    }

    public String insertar(MantenimientoTask task) {
        Document doc = new Document()
                .append("sensor_id", task.getSensorId())
                .append("sensor_nombre", task.getSensorNombre())
                .append("tipo", task.getTipo())
                .append("estado", task.getEstado())
                .append("fecha_programada", toDate(task.getFechaProgramada()))
                .append("fecha_creacion", toDate(task.getFechaCreacion()))
                .append("tecnico_id", task.getTecnicoId())
                .append("tecnico_nombre", task.getTecnicoNombre())
                .append("motivo", task.getMotivo())
                .append("observaciones", task.getObservaciones())
                .append("fecha_completada", toDate(task.getFechaCompletada()));
        collection.insertOne(doc);
        ObjectId id = doc.getObjectId("_id");
        if (id != null) {
            task.setId(id.toHexString());
        }
        return task.getId();
    }

    public void actualizar(MantenimientoTask task) {
        Document doc = new Document()
                .append("sensor_id", task.getSensorId())
                .append("sensor_nombre", task.getSensorNombre())
                .append("tipo", task.getTipo())
                .append("estado", task.getEstado())
                .append("fecha_programada", toDate(task.getFechaProgramada()))
                .append("fecha_creacion", toDate(task.getFechaCreacion()))
                .append("tecnico_id", task.getTecnicoId())
                .append("tecnico_nombre", task.getTecnicoNombre())
                .append("motivo", task.getMotivo())
                .append("observaciones", task.getObservaciones())
                .append("fecha_completada", toDate(task.getFechaCompletada()));
        collection.replaceOne(Filters.eq("_id", new ObjectId(task.getId())), doc);
    }

    public MantenimientoTask buscarPorId(String id) {
        Document doc = collection.find(Filters.eq("_id", new ObjectId(id))).first();
        return mapear(doc);
    }

    public List<MantenimientoTask> listarPendientes() {
        List<MantenimientoTask> tareas = new ArrayList<>();
        for (Document doc : collection.find(Filters.ne("estado", "COMPLETADA"))
                .sort(Sorts.ascending("fecha_programada"))) {
            MantenimientoTask task = mapear(doc);
            if (task != null) tareas.add(task);
        }
        return tareas;
    }

    public List<MantenimientoTask> listarDisponibles() {
        List<MantenimientoTask> tareas = new ArrayList<>();
        for (Document doc : collection.find(Filters.and(
                Filters.eq("estado", "PENDIENTE"),
                Filters.or(Filters.exists("tecnico_id", false), Filters.eq("tecnico_id", null))
        )).sort(Sorts.ascending("fecha_programada"))) {
            MantenimientoTask task = mapear(doc);
            if (task != null) tareas.add(task);
        }
        return tareas;
    }

    public List<MantenimientoTask> listarPorTecnico(String tecnicoId) {
        List<MantenimientoTask> tareas = new ArrayList<>();
        for (Document doc : collection.find(Filters.eq("tecnico_id", tecnicoId))
                .sort(Sorts.descending("fecha_programada"))) {
            MantenimientoTask task = mapear(doc);
            if (task != null) tareas.add(task);
        }
        return tareas;
    }

    public List<MantenimientoTask> listarCompletadas(int limit) {
        List<MantenimientoTask> tareas = new ArrayList<>();
        for (Document doc : collection.find(Filters.eq("estado", "COMPLETADA"))
                .sort(Sorts.descending("fecha_completada")).limit(limit)) {
            MantenimientoTask task = mapear(doc);
            if (task != null) tareas.add(task);
        }
        return tareas;
    }

    public boolean existePendiente(String sensorId, String tipo) {
        return collection.find(Filters.and(
                Filters.eq("sensor_id", sensorId),
                Filters.eq("tipo", tipo),
                Filters.ne("estado", "COMPLETADA")
        )).limit(1).first() != null;
    }

    public MantenimientoTask obtenerUltimaCompletadaPorSensor(String sensorId) {
        if (sensorId == null) {
            return null;
        }
        Document doc = collection.find(Filters.and(
                Filters.eq("sensor_id", sensorId),
                Filters.eq("estado", "COMPLETADA")
        )).sort(Sorts.descending("fecha_completada", "fecha_programada"))
                .first();
        return mapear(doc);
    }

    private MantenimientoTask mapear(Document doc) {
        if (doc == null) {
            return null;
        }
        MantenimientoTask task = new MantenimientoTask();
        ObjectId id = doc.getObjectId("_id");
        if (id != null) {
            task.setId(id.toHexString());
        }
        task.setSensorId(doc.getString("sensor_id"));
        task.setSensorNombre(doc.getString("sensor_nombre"));
        task.setTipo(doc.getString("tipo"));
        task.setEstado(doc.getString("estado"));
        task.setFechaProgramada(toLocalDateTime(doc.getDate("fecha_programada")));
        task.setFechaCreacion(toLocalDateTime(doc.getDate("fecha_creacion")));
        task.setFechaCompletada(toLocalDateTime(doc.getDate("fecha_completada")));
        task.setTecnicoId(doc.getString("tecnico_id"));
        task.setTecnicoNombre(doc.getString("tecnico_nombre"));
        task.setMotivo(doc.getString("motivo"));
        task.setObservaciones(doc.getString("observaciones"));
        return task;
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
