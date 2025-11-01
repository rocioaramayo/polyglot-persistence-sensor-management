package repository;

import connections.RedisPool;
import exceptions.ErrorConexionRedisException;
import redis.clients.jedis.Jedis;
import org.json.JSONObject;

public class SesionRedisDAO {
    private static SesionRedisDAO instance;
    private static final int TTL_SESION = 1800; // 30 minutos

    private SesionRedisDAO() {}

    public static SesionRedisDAO getInstance() {
        if (instance == null) {
            instance = new SesionRedisDAO();
        }
        return instance;
    }

    public void guardarSesion(String token, String usuarioId, String rol) throws ErrorConexionRedisException {
        try (Jedis jedis = RedisPool.getInstance().getConnection()) {
            JSONObject sesion = new JSONObject();
            sesion.put("usuarioId", usuarioId);
            sesion.put("rol", rol);
            sesion.put("timestamp", System.currentTimeMillis());

            String clave = "sess:" + token;
            jedis.setex(clave, TTL_SESION, sesion.toString());
        } catch (Exception e) {
            throw new ErrorConexionRedisException("Error al guardar sesión", e);
        }
    }

    public JSONObject obtenerSesion(String token) throws ErrorConexionRedisException {
        try (Jedis jedis = RedisPool.getInstance().getConnection()) {
            String clave = "sess:" + token;
            String valor = jedis.get(clave);
            if (valor != null) {
                return new JSONObject(valor);
            }
        } catch (Exception e) {
            throw new ErrorConexionRedisException("Error al obtener sesión", e);
        }
        return null;
    }

    public void eliminarSesion(String token) throws ErrorConexionRedisException {
        try (Jedis jedis = RedisPool.getInstance().getConnection()) {
            String clave = "sess:" + token;
            jedis.del(clave);
        } catch (Exception e) {
            throw new ErrorConexionRedisException("Error al eliminar sesión", e);
        }
    }
}
