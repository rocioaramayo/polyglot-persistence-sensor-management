package services;

import exceptions.ErrorConexionMongoException;
import modelo.Grupo;
import modelo.Mensaje;
import repository.MensajeMongoDAO;

import java.util.Collections;
import java.util.List;

public class MensajeService {
    private static MensajeService instance;

    private MensajeService() {}

    public static MensajeService getInstance() {
        if (instance == null) {
            instance = new MensajeService();
        }
        return instance;
    }

    public void enviarMensaje(String remitenteId, String destinatarioId, String contenido, String tipo)
            throws ErrorConexionMongoException {
        if (remitenteId == null || remitenteId.isBlank()) {
            throw new IllegalArgumentException("Remitente inválido");
        }
        if (destinatarioId == null || destinatarioId.isBlank()) {
            throw new IllegalArgumentException("Destinatario inválido");
        }
        if (contenido == null || contenido.isBlank()) {
            throw new IllegalArgumentException("El contenido del mensaje está vacío");
        }

        String tipoNormalizado = (tipo == null || tipo.isBlank()) ? "PRIVADO" : tipo.trim().toUpperCase();

        if ("GRUPAL".equals(tipoNormalizado)) {
            try {
                Grupo grupo = GrupoService.getInstance().obtenerGrupo(destinatarioId);
                if (grupo == null) {
                    throw new IllegalArgumentException("El grupo seleccionado no existe");
                }
                List<String> miembros = grupo.getMiembros() != null ? grupo.getMiembros() : Collections.emptyList();
                if (!miembros.contains(remitenteId)) {
                    throw new IllegalArgumentException("El remitente no pertenece al grupo");
                }
            } catch (ErrorConexionMongoException e) {
                throw e;
            }
        }

        Mensaje mensaje = new Mensaje(remitenteId, destinatarioId, contenido.trim(), tipoNormalizado);
        MensajeMongoDAO.getInstance().crear(mensaje);
    }

    public List<Mensaje> obtenerMensajesRecibidos(String usuarioId) throws ErrorConexionMongoException {
        List<String> grupos = GrupoService.getInstance().obtenerIdsGruposDeUsuario(usuarioId);
        return MensajeMongoDAO.getInstance().obtenerRecibidos(usuarioId, grupos);
    }

    public List<Mensaje> obtenerMensajesEnviados(String usuarioId) throws ErrorConexionMongoException {
        return MensajeMongoDAO.getInstance().obtenerPorRemitente(usuarioId);
    }
}
