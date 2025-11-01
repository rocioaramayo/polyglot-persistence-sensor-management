package simulator;

import connections.*;
import exceptions.ErrorConexionMongoException;
import modelo.*;
import repository.*;
import services.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Ejecuta demostraciones del sistema completo
 * Prueba todas las bases de datos y funcionalidades
 */
public class DemoRunner {
    private final repository.UsuarioMongoDAO usuarioRepo;
    private final FacturaMySQLRepository facturaRepo;
    private final CuentaMySQLRepository cuentaRepo;
    private final MovimientoMySQLRepository movimientoRepo;
    private final SensorCassandraDAO sensorDAO;
    private final MedicionCassandraDAO medicionDAO;
    private final SesionRedisDAO sesionDAO;
    private final MensajeMongoDAO mensajeDAO;
    private final AuthService authService;
    private final SensorService sensorService;
    private final MedicionService medicionService;
    private final FacturaService facturaService;
    private final MensajeService mensajeService;
    private final SensorSimulator simulator;

    public DemoRunner() {
        repository.UsuarioMongoDAO tempUsuarioRepo = null;
        try {
            tempUsuarioRepo = new repository.UsuarioMongoDAO();
        } catch (Exception e) {
            System.err.println("Aviso: no se pudo inicializar UsuarioMongoDAO: " + e.getMessage());
        }
        this.usuarioRepo = tempUsuarioRepo;
        this.facturaRepo = FacturaMySQLRepository.getInstance();
        this.cuentaRepo = CuentaMySQLRepository.getInstance();
        this.movimientoRepo = MovimientoMySQLRepository.getInstance();
        this.sensorDAO = SensorCassandraDAO.getInstance();
        this.medicionDAO = MedicionCassandraDAO.getInstance();
        this.sesionDAO = SesionRedisDAO.getInstance();
        MensajeMongoDAO tempMensajeDao = null;
        try {
            tempMensajeDao = MensajeMongoDAO.getInstance();
        } catch (ErrorConexionMongoException e) {
            System.err.println("Aviso: no se pudo inicializar MensajeMongoDAO: " + e.getMessage());
        }
        this.mensajeDAO = tempMensajeDao;
        this.authService = AuthService.getInstance();
        this.sensorService = SensorService.getInstance();
        this.medicionService = MedicionService.getInstance();
        this.facturaService = FacturaService.getInstance();
        this.mensajeService = MensajeService.getInstance();
        this.simulator = new SensorSimulator(medicionDAO);
    }

    /**
     * Ejecuta la demostración completa
     */
    public void ejecutarDemo() {
        System.out.println("\n========== DEMO PERSISTENCIA POLIGLOTA ==========\n");

        try {
            // 1. Crear usuarios de prueba
            System.out.println("1. Creando usuarios de prueba...");
            crearUsuariosPrueba();

            // 2. Autenticar usuario
            System.out.println("\n2. Autenticando usuario...");
            Usuario usuarioAutenticado = autenticarUsuario();

            // 3. Inicializar sensores
            System.out.println("\n3. Inicializando sensores...");
            simulator.inicializarSensores();

            // 4. Iniciar simulación
            System.out.println("\n4. Iniciando simulación de sensores...");
            simulator.iniciarSimulacion();

            // 5. Esperar a que se generen mediciones
            System.out.println("\n5. Generando mediciones (esperando 15 segundos)...");
            Thread.sleep(15000);

            // 6. Consultar mediciones
            System.out.println("\n6. Consultando mediciones...");
            consultarMediciones();

            // 7. Crear factura
            System.out.println("\n7. Creando factura...");
            crearFactura(usuarioAutenticado);

            // 8. Enviar mensaje
            System.out.println("\n8. Enviando mensaje...");
            enviarMensaje(usuarioAutenticado);

            // 9. Consultar cuenta corriente
            System.out.println("\n9. Consultando cuenta corriente...");
            consultarCuenta(usuarioAutenticado);

        } catch (Exception e) {
            System.err.println("Error en demo: " + e.getMessage());
            e.printStackTrace();
        } finally {
            simulator.detenerSimulacion();
            cerrarConexiones();
        }

        System.out.println("\n========== FIN DEMO ==========\n");
    }

    private void crearUsuariosPrueba() {
    Usuario usuario1 = new Usuario();
    usuario1.setId("1");
        usuario1.setNombre("Juan Pérez");
        usuario1.setEmail("juan@example.com");
        usuario1.setPasswordHash("password123");
        usuario1.setFechaRegistro(LocalDateTime.now());

    Usuario usuario2 = new Usuario();
    usuario2.setId("2");
        usuario2.setNombre("María García");
        usuario2.setEmail("maria@example.com");
        usuario2.setPasswordHash("password456");
        usuario2.setFechaRegistro(LocalDateTime.now());

        try {
            // Use AuthService.registrar so passwords are hashed consistently
            AuthService auth = AuthService.getInstance();
            try {
                auth.registrar(usuario1.getNombre(), usuario1.getEmail(), "password123", "USUARIO");
                auth.registrar(usuario2.getNombre(), usuario2.getEmail(), "password456", "USUARIO");
                // fetch created users to obtain their ids
                repository.UsuarioMongoDAO um = new repository.UsuarioMongoDAO();
                var u1 = um.buscarPorEmail(usuario1.getEmail());
                var u2 = um.buscarPorEmail(usuario2.getEmail());
                if (u1 != null) usuario1.setId(u1.getId());
                if (u2 != null) usuario2.setId(u2.getId());
                System.out.println("✓ Usuarios creados en Mongo exitosamente");
            } catch (Exception ie) {
                System.out.println("✓ Usuarios ya existen o error en registro: " + ie.getMessage());
            }
        } catch (Exception e) {
            System.out.println("✓ Error creando usuarios en demo: " + e.getMessage());
        }
    }

    private Usuario autenticarUsuario() throws Exception {
        String token = authService.login("juan@example.com", "password123");
        if (token != null) {
            Usuario usuario = authService.validarToken(token);
            if (usuario != null) {
                System.out.println("✓ Usuario autenticado: " + usuario.getNombre());
                return usuario;
            }
        }
        // Fallback: si no pudimos obtener token/validar (por Redis no disponible),
        // intentar leer directamente el usuario desde Mongo para continuar la demo.
        try {
            repository.UsuarioMongoDAO um = new repository.UsuarioMongoDAO();
            Usuario u = um.buscarPorEmail("juan@example.com");
            if (u != null) {
                System.out.println("✓ Usuario obtenido desde Mongo (fallback): " + u.getNombre());
                return u;
            }
        } catch (Exception e) {
            // Ignorar y fallar abajo
        }
        throw new Exception("Autenticación fallida");
    }

    private void consultarMediciones() {
        try {
            List<Medicion> mediciones = medicionDAO.obtenerUltimas(10);
            System.out.println("✓ Mediciones obtenidas: " + mediciones.size());
            for (Medicion m : mediciones) {
                System.out.println("  - Sensor: " + m.getSensorId() + 
                                 ", Temp: " + String.format("%.2f", m.getTemperatura()) + 
                                 "°C, Humedad: " + String.format("%.2f", m.getHumedad()) + "%");
            }
        } catch (Exception e) {
            System.out.println("✗ Error consultando mediciones: " + e.getMessage());
        }
    }

    private void crearFactura(Usuario usuario) {
        try {
            Factura factura = new Factura();
            factura.setId(1);
            try {
                factura.setUsuarioId(Integer.parseInt(usuario.getId()));
            } catch (NumberFormatException ex) {
                factura.setUsuarioId(null);
            }
            factura.setMonto(150.50);
            factura.setFecha(LocalDateTime.now());
            factura.setEstado("PENDIENTE");

            facturaRepo.guardar(factura);
            System.out.println("✓ Factura creada: " + factura.getId() + " - $" + factura.getMonto());
        } catch (Exception e) {
            System.out.println("✗ Error creando factura: " + e.getMessage());
        }
    }

    private void enviarMensaje(Usuario usuario) {
        try {
            Mensaje mensaje = new Mensaje();
            mensaje.setId(UUID.randomUUID().toString());
            // user IDs are strings (Mongo). Store them as-is in mensajes (MongoDB)
            mensaje.setRemitente(usuario.getId());
            mensaje.setDestinatario("2");
            mensaje.setContenido("Hola, este es un mensaje de prueba desde la demo");
            mensaje.setFecha(LocalDateTime.now());
            mensaje.setLeido(false);

            mensajeDAO.guardar(mensaje);
            System.out.println("✓ Mensaje enviado: " + mensaje.getId());
        } catch (Exception e) {
            System.out.println("✗ Error enviando mensaje: " + e.getMessage());
        }
    }

    private void consultarCuenta(Usuario usuario) {
        try {
            CuentaCorriente cuenta = null;
            try {
                cuenta = cuentaRepo.obtenerPorUsuario(Integer.parseInt(usuario.getId()));
            } catch (NumberFormatException ex) {
                // no numeric id
                cuenta = null;
            }
            if (cuenta != null) {
                System.out.println("✓ Cuenta corriente obtenida");
                System.out.println("  - Saldo: $" + cuenta.getSaldo());
                System.out.println("  - Límite: $" + cuenta.getLimite());
            }
        } catch (Exception e) {
            System.out.println("✗ Error consultando cuenta: " + e.getMessage());
        }
    }

    private void cerrarConexiones() {
        try {
            MySQLPool.getInstance().cerrar();
            CassandraPool.getInstance().cerrar();
            MongoPool.getInstance().cerrar();
            RedisPool.getInstance().cerrar();
            System.out.println("\n✓ Conexiones cerradas");
        } catch (Exception e) {
            System.err.println("Error cerrando conexiones: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        DemoRunner demo = new DemoRunner();
        demo.ejecutarDemo();
    }
}
