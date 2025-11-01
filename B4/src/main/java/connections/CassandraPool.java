package connections;

import com.datastax.oss.driver.api.core.CqlSession;
import exceptions.ErrorConexionCassandraException;
import utils.ConfigLoader;
import java.net.InetSocketAddress;

public class CassandraPool {
    private static CassandraPool instance;
    private CqlSession session;

    private CassandraPool() throws ErrorConexionCassandraException {
        try {
            ConfigLoader config = ConfigLoader.getInstance();
            String host = config.getProperty("cassandra.host", "127.0.0.1");
            int port = Integer.parseInt(config.getProperty("cassandra.port", "9042"));
            String keyspace = config.getProperty("cassandra.keyspace", "sensor_keyspace");

            this.session = CqlSession.builder()
                    .addContactPoint(new InetSocketAddress(host, port))
                    .withLocalDatacenter(config.getProperty("cassandra.datacenter", "datacenter1"))
                    .withKeyspace(keyspace)
                    .build();
        } catch (Exception e) {
            throw new ErrorConexionCassandraException("Error al conectar a Cassandra", e);
        }
    }

    public static CassandraPool getInstance() throws ErrorConexionCassandraException {
        if (instance == null) {
            instance = new CassandraPool();
        }
        return instance;
    }

    public CqlSession getSession() {
        return session;
    }

    public void close() {
        if (session != null && !session.isClosed()) {
            session.close();
        }
    }

    public void cerrar() {
        close();
    }
}
