package repository;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import connections.MongoPool;
import exceptions.ErrorConexionMongoException;
import modelo.AlertRule;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AlertRuleMongoDAO {
    private final MongoCollection<Document> collection;

    public AlertRuleMongoDAO() throws ErrorConexionMongoException {
        MongoDatabase db = MongoPool.getInstance().getDatabase();
        ensureCollection(db, "alert_rules");
        this.collection = db.getCollection("alert_rules");
    }

    private void ensureCollection(MongoDatabase db, String name) {
        boolean exists = false;
        for (String collName : db.listCollectionNames()) {
            if (collName.equalsIgnoreCase(name)) {
                exists = true;
                break;
            }
        }
        if (!exists) {
            try {
                db.createCollection(name);
            } catch (Exception ignored) {
                // Si otro proceso la creó en paralelo o no tenemos permisos, continuamos
            }
        }
    }

    public String insertar(AlertRule rule) {
        Document doc = new Document()
                .append("nombre", rule.getNombre())
                .append("severidad", rule.getSeveridad())
                .append("ventana_minutos", rule.getVentanaMinutos())
                .append("activo", rule.getActivo() == null || rule.getActivo());
        appendIfNotBlank(doc, "descripcion", rule.getDescripcion());
        appendIfNotBlank(doc, "ciudad", rule.getCiudad());
        appendIfNotBlank(doc, "zona", rule.getZona());
        appendIfNotBlank(doc, "pais", rule.getPais());
        appendIfNotNull(doc, "temperatura_min", rule.getTemperaturaMin());
        appendIfNotNull(doc, "temperatura_max", rule.getTemperaturaMax());
        appendIfNotNull(doc, "humedad_min", rule.getHumedadMin());
        appendIfNotNull(doc, "humedad_max", rule.getHumedadMax());
        appendIfDate(doc, "fecha_desde", rule.getFechaDesde());
        appendIfDate(doc, "fecha_hasta", rule.getFechaHasta());
        appendIfDate(doc, "fecha_creacion", rule.getFechaCreacion());
        appendIfDate(doc, "ultima_evaluacion", rule.getUltimaEvaluacion());
        appendIfDate(doc, "ultima_alerta", rule.getUltimaAlerta());
        collection.insertOne(doc);
        return doc.getObjectId("_id").toString();
    }

    public List<AlertRule> listarTodas() {
        List<AlertRule> reglas = new ArrayList<>();
        for (Document doc : collection.find()) {
            reglas.add(mapear(doc));
        }
        return reglas;
    }

    public List<AlertRule> listarActivas() {
        List<AlertRule> reglas = new ArrayList<>();
        for (Document doc : collection.find(Filters.eq("activo", true))) {
            reglas.add(mapear(doc));
        }
        return reglas;
    }

    public void actualizarEstado(String id, boolean activo) {
        collection.updateOne(
                Filters.eq("_id", new ObjectId(id)),
                new Document("$set", new Document("activo", activo))
        );
    }

    public void registrarSeguimiento(String id, LocalDateTime evaluacion, LocalDateTime ultimaAlerta) {
        Document set = new Document();
        if (evaluacion != null) {
            set.append("ultima_evaluacion", toDate(evaluacion));
        }
        if (ultimaAlerta != null) {
            set.append("ultima_alerta", toDate(ultimaAlerta));
        }
        if (!set.isEmpty()) {
            collection.updateOne(
                    Filters.eq("_id", new ObjectId(id)),
                    new Document("$set", set)
            );
        }
    }

    private AlertRule mapear(Document doc) {
        if (doc == null) {
            return null;
        }
        AlertRule rule = new AlertRule();
        ObjectId id = doc.getObjectId("_id");
        if (id != null) {
            rule.setId(id.toString());
        }
        rule.setNombre(doc.getString("nombre"));
        rule.setDescripcion(doc.getString("descripcion"));
        rule.setCiudad(doc.getString("ciudad"));
        rule.setZona(doc.getString("zona"));
        rule.setPais(doc.getString("pais"));
        rule.setSeveridad(doc.getString("severidad"));
        rule.setTemperaturaMin(getDouble(doc, "temperatura_min"));
        rule.setTemperaturaMax(getDouble(doc, "temperatura_max"));
        rule.setHumedadMin(getDouble(doc, "humedad_min"));
        rule.setHumedadMax(getDouble(doc, "humedad_max"));
        rule.setFechaDesde(toLocalDateTime(doc.getDate("fecha_desde")));
        rule.setFechaHasta(toLocalDateTime(doc.getDate("fecha_hasta")));
        rule.setVentanaMinutos(getInteger(doc, "ventana_minutos"));
        rule.setActivo(doc.getBoolean("activo", true));
        rule.setFechaCreacion(toLocalDateTime(doc.getDate("fecha_creacion")));
        rule.setUltimaEvaluacion(toLocalDateTime(doc.getDate("ultima_evaluacion")));
        rule.setUltimaAlerta(toLocalDateTime(doc.getDate("ultima_alerta")));
        return rule;
    }

    private Date toDate(LocalDateTime value) {
        if (value == null) {
            return null;
        }
        return Date.from(value.atZone(ZoneId.systemDefault()).toInstant());
    }
    private void appendIfNotBlank(Document doc, String key, String value) {
        if (value != null && !value.isBlank()) {
            doc.append(key, value);
        }
    }

    private LocalDateTime toLocalDateTime(Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    private Double getDouble(Document doc, String field) {
        Object value = doc.get(field);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return null;
    }

    private void appendIfNotNull(Document doc, String key, Double value) {
        if (value != null) {
            doc.append(key, value);
        }
    }

    private void appendIfDate(Document doc, String key, LocalDateTime value) {
        if (value != null) {
            doc.append(key, toDate(value));
        }
    }

    private Integer getInteger(Document doc, String field) {
        Object value = doc.get(field);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return null;
    }
}
