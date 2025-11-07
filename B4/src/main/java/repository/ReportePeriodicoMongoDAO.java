package repository;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import connections.MongoPool;
import exceptions.ErrorConexionMongoException;
import modelo.ReportePeriodico;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class ReportePeriodicoMongoDAO {
    private final MongoCollection<Document> collection;

    public ReportePeriodicoMongoDAO() throws ErrorConexionMongoException {
        MongoDatabase db = MongoPool.getInstance().getDatabase();
        this.collection = db.getCollection("reportes_periodicos");
    }

    public void guardar(ReportePeriodico reporte) {
        Document doc = new Document();
        if (reporte.getId() != null) {
            doc.append("_id", new ObjectId(reporte.getId()));
        }
        doc.append("nombre", reporte.getNombre());
        doc.append("proceso_id", reporte.getProcesoId());
        doc.append("tipo_proceso", reporte.getTipoProceso());
        doc.append("parametros", reporte.getParametros());
        doc.append("solicitud_id", reporte.getSolicitudId());
        doc.append("programacion_id", reporte.getProgramacionId());
        doc.append("resultado", reporte.getResultado());
        doc.append("observaciones", reporte.getObservaciones());
        doc.append("estado", reporte.getEstado());
        doc.append("mensaje_error", reporte.getMensajeError());
        if (reporte.getFechaEjecucion() != null) {
            Date fecha = Date.from(reporte.getFechaEjecucion().atZone(ZoneId.systemDefault()).toInstant());
            doc.append("fecha_ejecucion", fecha);
        }
        collection.insertOne(doc);
    }

    public List<ReportePeriodico> listarRecientes(int limite) {
        List<ReportePeriodico> reportes = new ArrayList<>();
        for (Document doc : collection.find().sort(new Document("fecha_ejecucion", -1)).limit(limite)) {
            reportes.add(map(doc));
        }
        return reportes;
    }

    public ReportePeriodico buscarPorId(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        Document doc = collection.find(Filters.eq("_id", new ObjectId(id))).first();
        return map(doc);
    }

    private ReportePeriodico map(Document doc) {
        if (doc == null) {
            return null;
        }
        ReportePeriodico reporte = new ReportePeriodico();
        ObjectId objectId = doc.getObjectId("_id");
        if (objectId != null) {
            reporte.setId(objectId.toHexString());
        }
        reporte.setNombre(doc.getString("nombre"));
        reporte.setProcesoId(doc.getString("proceso_id"));
        reporte.setTipoProceso(doc.getString("tipo_proceso"));
        reporte.setSolicitudId(doc.getString("solicitud_id"));
        reporte.setProgramacionId(doc.getString("programacion_id"));
        Document paramsDoc = doc.get("parametros", Document.class);
        if (paramsDoc != null) {
            reporte.setParametros(paramsDoc);
        }
        reporte.setResultado(doc.getString("resultado"));
        reporte.setObservaciones(doc.getString("observaciones"));
        reporte.setEstado(doc.getString("estado"));
        reporte.setMensajeError(doc.getString("mensaje_error"));
        Date fecha = doc.getDate("fecha_ejecucion");
        if (fecha != null) {
            reporte.setFechaEjecucion(fecha.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
        }
        return reporte;
    }
}
