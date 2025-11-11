package repository;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.Sorts;
import connections.MongoPool;
import exceptions.ErrorConexionMongoException;
import modelo.ConsultaPeriodica;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ConsultaPeriodicaMongoDAO {
    private final MongoCollection<Document> collection;

    public ConsultaPeriodicaMongoDAO() throws ErrorConexionMongoException {
        MongoDatabase db = MongoPool.getInstance().getDatabase();
        this.collection = db.getCollection("consultas_periodicas");
    }

    public void guardar(ConsultaPeriodica consulta) {
        String id = consulta.getId() != null ? consulta.getId() : UUID.randomUUID().toString();
        consulta.setId(id);
        Document doc = toDocument(consulta);
        collection.replaceOne(Filters.eq("_id", id), doc, new ReplaceOptions().upsert(true));
    }

    public ConsultaPeriodica buscarPorSolicitud(String solicitudId) {
        Document doc = collection.find(Filters.eq("solicitud_base_id", solicitudId)).first();
        return doc != null ? toModel(doc) : null;
    }

    public List<ConsultaPeriodica> listarActivasHasta(LocalDateTime limite) {
        List<ConsultaPeriodica> consultas = new ArrayList<>();
        LocalDateTime limiteEfectivo = limite != null ? limite : LocalDateTime.now();
        java.util.Date limiteDate = toDate(limiteEfectivo);
        ArrayList<Bson> vencimientos = new ArrayList<>();
        vencimientos.add(Filters.eq("proxima_ejecucion", null));
        vencimientos.add(Filters.exists("proxima_ejecucion", false));
        if (limiteDate != null) {
            vencimientos.add(Filters.lte("proxima_ejecucion", limiteDate));
        }
        Bson filtro = Filters.and(
                Filters.eq("activo", true),
                Filters.or(vencimientos.toArray(new Bson[0]))
        );
        try (MongoCursor<Document> cursor = collection.find(filtro)
                .sort(Sorts.ascending("proxima_ejecucion"))
                .iterator()) {
            while (cursor.hasNext()) {
                consultas.add(toModel(cursor.next()));
            }
        }
        return consultas;
    }

    public List<ConsultaPeriodica> listarPorTecnico(String tecnicoId) {
        if (tecnicoId == null || tecnicoId.isBlank()) {
            return List.of();
        }
        Bson filtro = Filters.and(
                Filters.eq("tecnico_id", tecnicoId),
                Filters.eq("activo", true)
        );
        return listarPorFiltro(filtro);
    }

    public List<ConsultaPeriodica> listarTodas() {
        return listarPorFiltro(null);
    }

    public void actualizarProgramacion(String id, LocalDateTime proxima, LocalDateTime ultima) {
        Document update = new Document();
        if (proxima != null) {
            update.append("proxima_ejecucion", toDate(proxima));
        }
        if (ultima != null) {
            update.append("ultima_ejecucion", toDate(ultima));
        }
        collection.updateOne(filtroPorId(id), new Document("$set", update));
    }

    public void desactivar(String id, String motivo) {
        Document update = new Document("activo", false);
        if (motivo != null) {
            update.append("motivo_baja", motivo);
        }
        collection.updateOne(filtroPorId(id), new Document("$set", update));
    }

    private Document filtroPorId(String id) {
        if (ObjectId.isValid(id)) {
            return new Document("$or", List.of(
                    new Document("_id", id),
                    new Document("_id", new ObjectId(id))
            ));
        }
        return new Document("_id", id);
    }

    private Document toDocument(ConsultaPeriodica consulta) {
        Document doc = new Document("_id", consulta.getId())
                .append("solicitud_base_id", consulta.getSolicitudBaseId())
                .append("proceso_id", consulta.getProcesoId())
                .append("tipo_proceso", consulta.getTipoProceso())
                .append("usuario_id", consulta.getUsuarioId())
                .append("tecnico_id", consulta.getTecnicoId())
                .append("nombre", consulta.getNombre())
                .append("descripcion", consulta.getDescripcion())
                .append("parametros", consulta.getParametros())
                .append("periodicidad", consulta.getPeriodicidad())
                .append("intervalo_minutos", consulta.getIntervaloMinutos())
                .append("proxima_ejecucion", toDate(consulta.getProximaEjecucion()))
                .append("activo", consulta.isActivo());
        if (consulta.getUltimaEjecucion() != null) {
            doc.append("ultima_ejecucion", toDate(consulta.getUltimaEjecucion()));
        }
        return doc;
    }

    private ConsultaPeriodica toModel(Document doc) {
        ConsultaPeriodica consulta = new ConsultaPeriodica();
        Object rawId = doc.get("_id");
        consulta.setId(rawId != null ? rawId.toString() : null);
        consulta.setSolicitudBaseId(doc.getString("solicitud_base_id"));
        consulta.setProcesoId(doc.getString("proceso_id"));
        consulta.setTipoProceso(doc.getString("tipo_proceso"));
        consulta.setUsuarioId(doc.getString("usuario_id"));
        consulta.setTecnicoId(doc.getString("tecnico_id"));
        consulta.setNombre(doc.getString("nombre"));
        consulta.setDescripcion(doc.getString("descripcion"));
        consulta.setPeriodicidad(doc.getString("periodicidad"));
        consulta.setIntervaloMinutos(doc.getLong("intervalo_minutos"));
        consulta.setActivo(doc.getBoolean("activo", true));
        var params = doc.get("parametros", Document.class);
        if (params != null) {
            consulta.setParametros(params);
        }
        var proxima = doc.getDate("proxima_ejecucion");
        if (proxima != null) {
            consulta.setProximaEjecucion(LocalDateTime.ofInstant(proxima.toInstant(), ZoneId.systemDefault()));
        }
        var ultima = doc.getDate("ultima_ejecucion");
        if (ultima != null) {
            consulta.setUltimaEjecucion(LocalDateTime.ofInstant(ultima.toInstant(), ZoneId.systemDefault()));
        }
        return consulta;
    }

    private java.util.Date toDate(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return java.util.Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    private List<ConsultaPeriodica> listarPorFiltro(Bson filtro) {
        List<ConsultaPeriodica> consultas = new ArrayList<>();
        var iterable = filtro != null ? collection.find(filtro) : collection.find();
        try (MongoCursor<Document> cursor = iterable
                .sort(Sorts.ascending("proxima_ejecucion"))
                .iterator()) {
            while (cursor.hasNext()) {
                consultas.add(toModel(cursor.next()));
            }
        }
        return consultas;
    }
}
