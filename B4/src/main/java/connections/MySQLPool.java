package connections;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import exceptions.ErrorConexionMySQLException;
import utils.ConfigLoader;

public class MySQLPool {
    private static MySQLPool instance;
    private String url;
    private String user;
    private String password;

    private MySQLPool() throws ErrorConexionMySQLException {
        try {
            ConfigLoader config = ConfigLoader.getInstance();
            String host = config.getProperty("mysql.host", "localhost");
            String port = config.getProperty("mysql.port", "3306");
            String database = config.getProperty("mysql.database", "sensor_db");
            this.user = config.getProperty("mysql.user", "root");
            this.password = config.getProperty("mysql.password", "root");

            this.url = "jdbc:mysql://" + host + ":" + port + "/" + database 
                    + "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";

            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new ErrorConexionMySQLException("Driver MySQL no encontrado", e);
        }
    }

    public static MySQLPool getInstance() throws ErrorConexionMySQLException {
        if (instance == null) {
            instance = new MySQLPool();
        }
        return instance;
    }

    public Connection getConnection() throws ErrorConexionMySQLException {
        try {
            return DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            throw new ErrorConexionMySQLException("Error al conectar a MySQL", e);
        }
    }

    public void cerrar() {
        // Connection pool cleanup - individual connections are closed by try-with-resources
        System.out.println("MySQL Pool cerrado");
    }
}
