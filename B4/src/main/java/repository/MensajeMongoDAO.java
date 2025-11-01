package repository;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import connections.MongoPool;
import exceptions.ErrorConexionMongoException;
import modelo.Mensaje;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class MensajeMongoDAO {
    private static MensajeMongoDAO instance;
    private final MongoCollection<Document> collection;

    private MensajeMongoDAO() throws ErrorConexionMongoException {
        MongoDatabase db = MongoPool.getInstance().getDatabase();
        this.collection = db.getCollection("mensajes");
    }

    public static MensajeMongoDAO getInstance() throws ErrorConexionMongoException {
        if (instance == null) {
            instance = new MensajeMongoDAO();
        }
        return instance;
    }

    public void crear(Mensaje mensaje) throws ErrorConexionMongoException {
        try {
            LocalDateTime fecha = mensaje.getFechaHora() != null ? mensaje.getFechaHora() : LocalDateTime.now();
            mensaje.setFechaHora(fecha);
            Boolean leido = mensaje.getLeido() != null ? mensaje.getLeido() : Boolean.FALSE;
            mensaje.setLeido(leido);
            String tipo = mensaje.getTipo() != null && !mensaje.getTipo().isBlank()
                    ? mensaje.getTipo().trim().toUpperCase()
                    : "PRIVADO";
            mensaje.setTipo(tipo);

            Document doc = new Document()
                    .append("remitenteId", mensaje.getRemitenteId())
                    .append("destinatarioId", mensaje.getDestinatarioId())
                    .append("contenido", mensaje.getContenido())
                    .append("tipo", tipo)
                    .append("fechaHora", Date.from(fecha.atZone(ZoneId.systemDefault()).toInstant()))
                    .append("leido", leido);

            collection.insertOne(doc);
            mensaje.setId(doc.getObjectId("_id").toString());
        } catch (Exception e) {
            throw new ErrorConexionMongoException("Error al crear mensaje", e);
        }
    }

    public void guardar(Mensaje mensaje) throws ErrorConexionMongoException {
        crear(mensaje);
    }

    public List<Mensaje> obtenerPorDestinatario(String destinatarioId) throws ErrorConexionMongoException {
        try {
            Bson filter = Filters.eq("destinatarioId", destinatarioId);
            return mapearMensajes(collection.find(filter).sort(Sorts.descending("fechaHora")));
        } catch (Exception e) {
            throw new ErrorConexionMongoException("Error al obtener mensajes por destinatario", e);
        }
    }

    public List<Mensaje> obtenerRecibidos(String usuarioId, List<String> grupoIds) throws ErrorConexionMongoException {
        try {
            List<Bson> condiciones = new ArrayList<>();
            condiciones.add(Filters.eq("destinatarioId", usuarioId));

            if (grupoIds != null && !grupoIds.isEmpty()) {
                Bson filtroGrupo = Filters.and(
                        Filters.eq("tipo", "GRUPAL"),
                        Filters.in("destinatarioId", grupoIds)
                );
                condiciones.add(filtroGrupo);
            }

            Bson query = condiciones.size() == 1 ? condiciones.get(0) : Filters.or(condiciones);
            FindIterable<Document> iterable = collection.find(query).sort(Sorts.descending("fechaHora"));
            return mapearMensajes(iterable);
        } catch (Exception e) {
            throw new ErrorConexionMongoException("Error al obtener mensajes recibidos", e);
        }
    }

    public List<Mensaje> obtenerPorRemitente(String remitenteId) throws ErrorConexionMongoException {
        try {
            Bson filter = Filters.eq("remitenteId", remitenteId);
            return mapearMensajes(collection.find(filter).sort(Sorts.descending("fechaHora")));
        } catch (Exception e) {
            throw new ErrorConexionMongoException("Error al obtener mensajes enviados", e);
        }
    }

    private List<Mensaje> mapearMensajes(Iterable<Document> documentos) {
        List<Mensaje> mensajes = new ArrayList<>();
        for (Document doc : documentos) {
            mensajes.add(mapearMensaje(doc));
        }
        return mensajes;
    }

    private Mensaje mapearMensaje(Document doc) {
        Mensaje mensaje = new Mensaje();
        mensaje.setId(doc.getObjectId("_id").toString());
        mensaje.setRemitenteId(doc.getString("remitenteId"));
        mensaje.setDestinatarioId(doc.getString("destinatarioId"));
        mensaje.setContenido(doc.getString("contenido"));
        mensaje.setTipo(doc.getString("tipo"));

        Object fechaObj = doc.get("fechaHora");
        if (fechaObj instanceof Date) {
            Date fecha = (Date) fechaObj;
            mensaje.setFechaHora(fecha.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
        } else if (fechaObj instanceof String) {
            try {
                mensaje.setFechaHora(LocalDateTime.parse((String) fechaObj));
            } catch (DateTimeParseException ignored) {
                // dejamos la fecha en null si el formato no es válido
            }
        }

        Boolean leido = doc.getBoolean("leido");
        mensaje.setLeido(leido != null ? leido : Boolean.FALSE);
        return mensaje;
    }
}
