package repository;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import connections.MongoPool;
import exceptions.ErrorConexionMongoException;
import modelo.SolicitudProceso;
import org.bson.Document;
import java.util.UUID;
import org.bson.types.ObjectId;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SolicitudProcesoMongoDAO {
    private final MongoCollection<Document> collection;

    public SolicitudProcesoMongoDAO() throws ErrorConexionMongoException {
        MongoDatabase db = MongoPool.getInstance().getDatabase();
        this.collection = db.getCollection("solicitudes_proceso");
    }

    public String insertar(SolicitudProceso solicitud) throws ErrorConexionMongoException {
        String id = solicitud.getId() != null ? solicitud.getId() : UUID.randomUUID().toString();
        Document doc = new Document("_id", id)
                .append("usuario_id", solicitud.getUsuarioId())
                .append("proceso_id", solicitud.getProcesoId())
                .append("fecha_solicitud", Date.from(solicitud.getFechaSolicitud().atZone(ZoneId.systemDefault()).toInstant()))
                .append("estado", solicitud.getEstado())
                .append("parametros", solicitud.getParametros())
                .append("origen", solicitud.getOrigen());
        
        if (solicitud.getTecnicoAsignadoId() != null) {
            doc.append("tecnico_asignado_id", solicitud.getTecnicoAsignadoId());
        }
        
        if (solicitud.getResultado() != null) {
            doc.append("resultado", solicitud.getResultado());
        }
        if (solicitud.getObservaciones() != null) {
            doc.append("observaciones", solicitud.getObservaciones());
        }
        try {
            collection.insertOne(doc);
            return id;
        } catch (Exception e) {
            throw new ErrorConexionMongoException("Error al insertar solicitud en Mongo", e);
        }
    }

    public SolicitudProceso buscarPorId(String id) {
        Document doc = collection.find(Filters.eq("_id", id)).first();
        if (doc == null && ObjectId.isValid(id)) {
            doc = collection.find(Filters.eq("_id", new ObjectId(id))).first();
        }
        if (doc == null) return null;

        SolicitudProceso solicitud = new SolicitudProceso();
        solicitud.setId(id);
        solicitud.setUsuarioId(doc.getString("usuario_id"));
        solicitud.setProcesoId(doc.getString("proceso_id"));
        solicitud.setEstado(doc.getString("estado"));
        if (doc.containsKey("origen")) {
            solicitud.setOrigen(doc.getString("origen"));
        }
        if (doc.getDate("fecha_solicitud") != null) {
            solicitud.setFechaSolicitud(doc.getDate("fecha_solicitud").toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDateTime());
        }
        org.bson.Document paramsDoc = doc.get("parametros", org.bson.Document.class);
        if (paramsDoc != null) {
            // Document implements Map<String,Object> so we can pass it directly
            solicitud.setParametros(paramsDoc);
        }
        
        if (doc.containsKey("tecnico_asignado_id")) {
            solicitud.setTecnicoAsignadoId(doc.getString("tecnico_asignado_id"));
        }
        
        if (doc.containsKey("resultado")) {
            solicitud.setResultado(doc.getString("resultado"));
        }
        if (doc.containsKey("observaciones")) {
            solicitud.setObservaciones(doc.getString("observaciones"));
        }
        
        return solicitud;
    }

    public List<SolicitudProceso> listarPorUsuario(String usuarioId) {
        List<SolicitudProceso> solicitudes = new ArrayList<>();
        var filtro = Filters.eq("usuario_id", usuarioId);
        if (ObjectId.isValid(usuarioId)) {
            filtro = Filters.or(
                    Filters.eq("usuario_id", usuarioId),
                    Filters.eq("usuario_id", new ObjectId(usuarioId))
            );
        }
        for (Document doc : collection.find(filtro)) {
            SolicitudProceso solicitud = new SolicitudProceso();
            Object rawId = doc.get("_id");
            solicitud.setId(rawId != null ? rawId.toString() : null);
            solicitud.setUsuarioId(doc.getString("usuario_id"));
            solicitud.setProcesoId(doc.getString("proceso_id"));
            solicitud.setEstado(doc.getString("estado"));
            if (doc.getDate("fecha_solicitud") != null) {
                solicitud.setFechaSolicitud(doc.getDate("fecha_solicitud").toInstant()
                        .atZone(ZoneId.systemDefault()).toLocalDateTime());
            }
            org.bson.Document paramsDoc = doc.get("parametros", org.bson.Document.class);
            if (paramsDoc != null) {
                solicitud.setParametros(paramsDoc);
            }
            if (doc.containsKey("resultado")) {
                solicitud.setResultado(doc.getString("resultado"));
            }
            if (doc.containsKey("observaciones")) {
                solicitud.setObservaciones(doc.getString("observaciones"));
            }
            if (doc.containsKey("origen")) {
                solicitud.setOrigen(doc.getString("origen"));
            }
            
            solicitudes.add(solicitud);
        }
        return solicitudes;
    }

    public List<SolicitudProceso> listarPorTecnico(String tecnicoId) {
        List<SolicitudProceso> solicitudes = new ArrayList<>();
        for (Document doc : collection.find(Filters.eq("tecnico_asignado_id", tecnicoId))) {
            String origen = doc.getString("origen");
            if (origen != null && "AUTOMATICO".equalsIgnoreCase(origen)) {
                continue;
            }
            SolicitudProceso solicitud = new SolicitudProceso();
            Object rawId = doc.get("_id");
            solicitud.setId(rawId != null ? rawId.toString() : null);
            solicitud.setUsuarioId(doc.getString("usuario_id"));
            solicitud.setProcesoId(doc.getString("proceso_id"));
            solicitud.setEstado(doc.getString("estado"));
            solicitud.setTecnicoAsignadoId(doc.getString("tecnico_asignado_id"));
            if (doc.containsKey("resultado")) {
                solicitud.setResultado(doc.getString("resultado"));
            }
            if (doc.containsKey("observaciones")) {
                solicitud.setObservaciones(doc.getString("observaciones"));
            }
            if (doc.containsKey("origen")) {
                solicitud.setOrigen(doc.getString("origen"));
            }
            solicitudes.add(solicitud);
        }
        return solicitudes;
    }

    public List<SolicitudProceso> listarPendientes() {
        List<SolicitudProceso> solicitudes = new ArrayList<>();
        for (Document doc : collection.find(Filters.eq("estado", "PENDIENTE"))) {
            String origen = doc.getString("origen");
            if (origen != null && "AUTOMATICO".equalsIgnoreCase(origen)) {
                continue;
            }
            SolicitudProceso solicitud = new SolicitudProceso();
            Object rawId = doc.get("_id");
            solicitud.setId(rawId != null ? rawId.toString() : null);
            solicitud.setUsuarioId(doc.getString("usuario_id"));
            solicitud.setProcesoId(doc.getString("proceso_id"));
            solicitud.setEstado(doc.getString("estado"));
            if (doc.getDate("fecha_solicitud") != null) {
                solicitud.setFechaSolicitud(doc.getDate("fecha_solicitud").toInstant()
                        .atZone(ZoneId.systemDefault()).toLocalDateTime());
            }
            org.bson.Document paramsDoc = doc.get("parametros", org.bson.Document.class);
            if (paramsDoc != null) {
                solicitud.setParametros(paramsDoc);
            }
            if (doc.containsKey("resultado")) {
                solicitud.setResultado(doc.getString("resultado"));
            }
            if (doc.containsKey("observaciones")) {
                solicitud.setObservaciones(doc.getString("observaciones"));
            }
            if (doc.containsKey("origen")) {
                solicitud.setOrigen(doc.getString("origen"));
            }
            
            solicitudes.add(solicitud);
        }
        return solicitudes;
    }

    public void actualizarEstado(String id, String estado) {
        var filtroId = ObjectId.isValid(id) ?
                Filters.or(Filters.eq("_id", id), Filters.eq("_id", new ObjectId(id))) :
                Filters.eq("_id", id);
        collection.updateOne(
                filtroId,
                new Document("$set", new Document("estado", estado))
        );
    }

    public void asignarTecnico(String id, String tecnicoId) {
        var filtroId = ObjectId.isValid(id) ?
                Filters.or(Filters.eq("_id", id), Filters.eq("_id", new ObjectId(id))) :
                Filters.eq("_id", id);
        collection.updateOne(
                filtroId,
                new Document("$set", new Document("tecnico_asignado_id", tecnicoId))
        );
    }
    
    public void actualizarResultado(String id, String resultado, String observaciones) {
        Document updateDoc = new Document("resultado", resultado);
        if (observaciones != null) {
            updateDoc.append("observaciones", observaciones);
        }
        var filtroId = ObjectId.isValid(id) ?
                Filters.or(Filters.eq("_id", id), Filters.eq("_id", new ObjectId(id))) :
                Filters.eq("_id", id);
        collection.updateOne(
                filtroId,
                new Document("$set", updateDoc)
        );
    }
}
