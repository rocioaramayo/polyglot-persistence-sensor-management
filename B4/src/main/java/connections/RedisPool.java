package connections;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import exceptions.ErrorConexionRedisException;
import utils.ConfigLoader;

public class RedisPool {
    private static RedisPool instance;
    private JedisPool jedisPool;

    private RedisPool() throws ErrorConexionRedisException {
        try {
            ConfigLoader config = ConfigLoader.getInstance();
            String host = config.getProperty("redis.host", "localhost");
            int port = Integer.parseInt(config.getProperty("redis.port", "6379"));
            String password = config.getProperty("redis.password", "");

            JedisPoolConfig poolConfig = new JedisPoolConfig();
            poolConfig.setMaxTotal(8);
            poolConfig.setMaxIdle(8);
            poolConfig.setMinIdle(0);

            if (password != null && !password.isEmpty()) {
                this.jedisPool = new JedisPool(poolConfig, host, port, 2000, password);
            } else {
                this.jedisPool = new JedisPool(poolConfig, host, port);
            }
        } catch (Exception e) {
            throw new ErrorConexionRedisException("Error al conectar a Redis", e);
        }
    }

    public static RedisPool getInstance() throws ErrorConexionRedisException {
        if (instance == null) {
            instance = new RedisPool();
        }
        return instance;
    }

    public Jedis getConnection() {
        return jedisPool.getResource();
    }

    public void close() {
        if (jedisPool != null && !jedisPool.isClosed()) {
            jedisPool.close();
        }
    }

    public void cerrar() {
        close();
    }
}
