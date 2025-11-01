package connections;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import exceptions.ErrorConexionMongoException;
import utils.ConfigLoader;

public class MongoPool {
    private static MongoPool instance;
    private MongoClient mongoClient;
    private String databaseName;

    private MongoPool() throws ErrorConexionMongoException {
        try {
            ConfigLoader config = ConfigLoader.getInstance();
            String url = config.getProperty("mongodb.url", "mongodb://127.0.0.1:27017");
            this.databaseName = config.getProperty("mongodb.database", "sensor_db");

            this.mongoClient = MongoClients.create(url);
        } catch (Exception e) {
            throw new ErrorConexionMongoException("Error al conectar a MongoDB", e);
        }
    }

    public static MongoPool getInstance() throws ErrorConexionMongoException {
        if (instance == null) {
            instance = new MongoPool();
        }
        return instance;
    }

    public MongoDatabase getDatabase() {
        return mongoClient.getDatabase(databaseName);
    }

    public void close() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }

    public void cerrar() {
        close();
    }
}
