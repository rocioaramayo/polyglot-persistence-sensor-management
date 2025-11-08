package ui;

import exceptions.ErrorConexionCassandraException;
import exceptions.ErrorConexionMongoException;
import modelo.Alerta;
import modelo.AlertRule;
import modelo.Sensor;
import modelo.Usuario;
import repository.AlertaMongoDAO;
import repository.UsuarioMongoDAO;
import services.AlertRuleService;
import services.SensorService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.AWTEventListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class AdminFrame extends JFrame {
    private final String token;
    private static final long INACTIVITY_LIMIT_MS = TimeUnit.MINUTES.toMillis(15);
    private static final String[] ALERT_TEMPLATES = {
            "Temperaturas extremas detectadas en su zona. Tome precauciones con la exposición prolongada.",
            "Incremento de humedad registrado. Verifique equipos sensibles y resguarde materiales.",
            "Mantenimiento programado de sensores. Pueden registrarse lecturas irregulares.",
            "Advertencia por tormentas en la región. Revise mediciones en tiempo real.",
            "Alertas personalizadas: ingrese detalles en el cuadro inferior."
    };
    private static final DateTimeFormatter ALERT_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter ALERT_DAY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private javax.swing.Timer inactivityTimer;
    private long lastInteractionMs;
    private AWTEventListener globalActivityListener;
    private boolean logoutTriggered;

    public AdminFrame(String token) {
        this.token = token;
        setTitle("Panel de Administración");
        setSize(900, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                detenerMonitoreos();
            }
        });
        lastInteractionMs = System.currentTimeMillis();

        inactivityTimer = new javax.swing.Timer(30_000, e -> verificarInactividad());
        inactivityTimer.start();

        globalActivityListener = event -> updateLastInteraction();
        Toolkit.getDefaultToolkit().addAWTEventListener(
                globalActivityListener,
                AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK | AWTEvent.KEY_EVENT_MASK
        );

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Sensores", crearPanelSensores());
        tabs.addTab("Técnicos", crearPanelTecnicos());
        tabs.addTab("Alertas", crearPanelAlertas());

        JPanel header = new JPanel(new BorderLayout());
        JButton cerrarBtn = new JButton("Cerrar");
        cerrarBtn.addActionListener(e -> {
            detenerMonitoreos();
            new LoginFrame().setVisible(true);
            dispose();
        });
        header.add(cerrarBtn, BorderLayout.EAST);

        JPanel container = new JPanel(new BorderLayout());
        container.add(header, BorderLayout.NORTH);
        container.add(tabs, BorderLayout.CENTER);
        add(container);
    }

    private JPanel crearPanelReglasAutomaticas() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Crear regla automática"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;

        JTextField nombreField = new JTextField(20);
        JTextArea descripcionArea = new JTextArea(3, 24);
        descripcionArea.setLineWrap(true);
        descripcionArea.setWrapStyleWord(true);

        JComboBox<String> ciudadCombo = new JComboBox<>();
        JComboBox<String> zonaCombo = new JComboBox<>();
        JComboBox<String> paisCombo = new JComboBox<>();
        JButton refrescarUbicacionesBtn = new JButton("Actualizar ubicaciones");
        refrescarUbicacionesBtn.addActionListener(e -> popularUbicaciones(ciudadCombo, zonaCombo, paisCombo));
        popularUbicaciones(ciudadCombo, zonaCombo, paisCombo);

        JTextField fechaDesdeField = new JTextField(10);
        JTextField fechaHastaField = new JTextField(10);
        JTextField tempMinField = new JTextField(6);
        JTextField tempMaxField = new JTextField(6);
        JTextField humMinField = new JTextField(6);
        JTextField humMaxField = new JTextField(6);
        JSpinner ventanaSpinner = new JSpinner(new SpinnerNumberModel(60, 5, 1440, 5));
        JComboBox<String> severidadCombo = new JComboBox<>(new String[]{"BAJA", "MEDIA", "ALTA", "CRITICA"});

        gbc.gridx = 0; gbc.gridy = 0; form.add(new JLabel("Nombre:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 3; form.add(nombreField, gbc);
        gbc.gridwidth = 1;

        gbc.gridx = 0; gbc.gridy = 1; form.add(new JLabel("Descripción:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 3;
        form.add(new JScrollPane(descripcionArea), gbc);
        gbc.gridwidth = 1;

        gbc.gridx = 0; gbc.gridy = 2; form.add(new JLabel("Ciudad:"), gbc);
        gbc.gridx = 1; form.add(ciudadCombo, gbc);
        gbc.gridx = 2; form.add(new JLabel("Zona:"), gbc);
        gbc.gridx = 3; form.add(zonaCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 3; form.add(new JLabel("País:"), gbc);
        gbc.gridx = 1; form.add(paisCombo, gbc);
        gbc.gridx = 2; gbc.gridwidth = 2; form.add(refrescarUbicacionesBtn, gbc);
        gbc.gridwidth = 1;

        gbc.gridx = 0; gbc.gridy = 4; form.add(new JLabel("Fecha desde (AAAA-MM-DD):"), gbc);
        gbc.gridx = 1; form.add(fechaDesdeField, gbc);
        gbc.gridx = 2; form.add(new JLabel("Fecha hasta:"), gbc);
        gbc.gridx = 3; form.add(fechaHastaField, gbc);

        gbc.gridx = 0; gbc.gridy = 5; form.add(new JLabel("Temp. mín (°C):"), gbc);
        gbc.gridx = 1; form.add(tempMinField, gbc);
        gbc.gridx = 2; form.add(new JLabel("Temp. máx (°C):"), gbc);
        gbc.gridx = 3; form.add(tempMaxField, gbc);

        gbc.gridx = 0; gbc.gridy = 6; form.add(new JLabel("Humedad mín (%):"), gbc);
        gbc.gridx = 1; form.add(humMinField, gbc);
        gbc.gridx = 2; form.add(new JLabel("Humedad máx (%):"), gbc);
        gbc.gridx = 3; form.add(humMaxField, gbc);

        gbc.gridx = 0; gbc.gridy = 7; form.add(new JLabel("Ventana (minutos):"), gbc);
        gbc.gridx = 1; form.add(ventanaSpinner, gbc);
        gbc.gridx = 2; form.add(new JLabel("Severidad:"), gbc);
        gbc.gridx = 3; form.add(severidadCombo, gbc);

        JButton crearBtn = new JButton("Crear regla");
        gbc.gridx = 3; gbc.gridy = 8; gbc.anchor = GridBagConstraints.EAST;
        form.add(crearBtn, gbc);

        DefaultTableModel model = new DefaultTableModel(
                new Object[]{"ID", "Nombre", "Ubicación", "Condiciones", "Ventana", "Estado", "Última alerta"},
                0
        ) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable tabla = new JTable(model);
        tabla.getColumnModel().getColumn(0).setMinWidth(0);
        tabla.getColumnModel().getColumn(0).setMaxWidth(0);
        JScrollPane tablaScroll = new JScrollPane(tabla);

        JButton refrescarBtn = new JButton("Refrescar");
        JButton toggleBtn = new JButton("Activar/Desactivar");

        crearBtn.addActionListener(e -> {
            try {
                AlertRule rule = new AlertRule();
                rule.setNombre(nombreField.getText().trim());
                rule.setDescripcion(descripcionArea.getText().trim());
                rule.setCiudad(opcionTexto(ciudadCombo));
                rule.setZona(opcionTexto(zonaCombo));
                rule.setPais(opcionTexto(paisCombo));
                rule.setFechaDesde(parseFecha(fechaDesdeField.getText().trim()));
                rule.setFechaHasta(parseFecha(fechaHastaField.getText().trim()));
                rule.setTemperaturaMin(parseDouble(tempMinField.getText().trim()));
                rule.setTemperaturaMax(parseDouble(tempMaxField.getText().trim()));
                rule.setHumedadMin(parseDouble(humMinField.getText().trim()));
                rule.setHumedadMax(parseDouble(humMaxField.getText().trim()));
                rule.setVentanaMinutos((Integer) ventanaSpinner.getValue());
                rule.setSeveridad(severidadCombo.getSelectedItem().toString());

                AlertRuleService.getInstance().crear(rule);
                JOptionPane.showMessageDialog(this, "Regla creada correctamente.", "Info", JOptionPane.INFORMATION_MESSAGE);
                nombreField.setText("");
                descripcionArea.setText("");
                fechaDesdeField.setText("");
                fechaHastaField.setText("");
                tempMinField.setText("");
                tempMaxField.setText("");
                humMinField.setText("");
                humMaxField.setText("");
                ventanaSpinner.setValue(60);
                severidadCombo.setSelectedItem("MEDIA");
                cargarReglas(model);
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Validación", JOptionPane.WARNING_MESSAGE);
            } catch (ErrorConexionMongoException ex) {
                JOptionPane.showMessageDialog(this, "No se pudo crear la regla: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Fallo inesperado al crear la regla: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        refrescarBtn.addActionListener(e -> cargarReglas(model));
        toggleBtn.addActionListener(e -> {
            int row = tabla.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(this, "Seleccione una regla para cambiar su estado.", "Aviso", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int modelRow = tabla.convertRowIndexToModel(row);
            String id = (String) model.getValueAt(modelRow, 0);
            String estado = (String) model.getValueAt(modelRow, 5);
            boolean activo = !"ACTIVA".equalsIgnoreCase(estado);
            try {
                AlertRuleService.getInstance().actualizarEstado(id, activo);
                cargarReglas(model);
            } catch (ErrorConexionMongoException ex) {
                JOptionPane.showMessageDialog(this, "No se pudo actualizar el estado: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        cargarReglas(model);

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        acciones.add(refrescarBtn);
        acciones.add(toggleBtn);

        panel.add(form, BorderLayout.NORTH);
        panel.add(tablaScroll, BorderLayout.CENTER);
        panel.add(acciones, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel crearPanelSensores() {
        JPanel panel = new JPanel(new BorderLayout(10,10));
        panel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        // Alta de sensor
        JPanel alta = new JPanel(new GridBagLayout());
        alta.setBorder(BorderFactory.createTitledBorder("Crear Sensor"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5,5,5,5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField nombreField = new JTextField(16);
        JTextField codigoField = new JTextField(12);
        JComboBox<String> tipoCombo = new JComboBox<>(new String[]{"temperatura","humedad"});
        JTextField latField = new JTextField(8);
        JTextField lonField = new JTextField(8);
        JTextField ciudadField = new JTextField(12);
        JTextField zonaField = new JTextField(12);
        JTextField paisField = new JTextField(12);
        JComboBox<String> estadoCombo = new JComboBox<>(new String[]{"ACTIVO","INACTIVO","MANTENIMIENTO"});
        JTextField fechaInstField = new JTextField(12);
        JTextArea observacionesArea = new JTextArea(3, 24);
        observacionesArea.setLineWrap(true);
        observacionesArea.setWrapStyleWord(true);

        gbc.gridx=0; gbc.gridy=0; alta.add(new JLabel("Nombre:"), gbc);
        gbc.gridx=1; alta.add(nombreField, gbc);
        gbc.gridx=2; alta.add(new JLabel("Código:"), gbc);
        gbc.gridx=3; alta.add(codigoField, gbc);

        gbc.gridx=0; gbc.gridy=1; alta.add(new JLabel("Tipo:"), gbc);
        gbc.gridx=1; alta.add(tipoCombo, gbc);
        gbc.gridx=2; alta.add(new JLabel("Estado:"), gbc);
        gbc.gridx=3; alta.add(estadoCombo, gbc);

        gbc.gridx=0; gbc.gridy=2; alta.add(new JLabel("Latitud:"), gbc);
        gbc.gridx=1; alta.add(latField, gbc);
        gbc.gridx=2; alta.add(new JLabel("Longitud:"), gbc);
        gbc.gridx=3; alta.add(lonField, gbc);

        gbc.gridx=0; gbc.gridy=3; alta.add(new JLabel("Ciudad:"), gbc);
        gbc.gridx=1; alta.add(ciudadField, gbc);
        gbc.gridx=2; alta.add(new JLabel("Zona:"), gbc);
        gbc.gridx=3; alta.add(zonaField, gbc);

        gbc.gridx=0; gbc.gridy=4; alta.add(new JLabel("Pais:"), gbc);
        gbc.gridx=1; alta.add(paisField, gbc);
        gbc.gridx=2; alta.add(new JLabel("Fecha instalación (AAAA-MM-DD):"), gbc);
        gbc.gridx=3; alta.add(fechaInstField, gbc);

        gbc.gridx=0; gbc.gridy=5; alta.add(new JLabel("Observaciones:"), gbc);
        gbc.gridx=1; gbc.gridwidth=3;
        gbc.fill = GridBagConstraints.BOTH;
        alta.add(new JScrollPane(observacionesArea), gbc);
        gbc.gridwidth=1;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx=0; gbc.gridy=6; gbc.gridwidth=4; gbc.anchor = GridBagConstraints.EAST; gbc.fill = GridBagConstraints.NONE;
        JButton crearBtn = new JButton("Crear");
        alta.add(crearBtn, gbc);

        // Declarar la tabla y el modelo antes del listener para refrescar tras crear
        DefaultTableModel model = new DefaultTableModel(new Object[]{"ID","Nombre","Tipo","Ciudad","Pais","Estado"},0){
            @Override public boolean isCellEditable(int r,int c){return false;}
        };
        JTable table = new JTable(model);
        table.setAutoCreateRowSorter(true);

        crearBtn.addActionListener(e -> {
            try {
                String nombre = nombreField.getText().trim();
                String tipo = (String) tipoCombo.getSelectedItem();
                double lat = Double.parseDouble(latField.getText().trim());
                double lon = Double.parseDouble(lonField.getText().trim());
                String ciudad = ciudadField.getText().trim();
                String zona = zonaField.getText().trim();
                String pais = paisField.getText().trim();
                String codigo = codigoField.getText().trim();
                String estado = (String) estadoCombo.getSelectedItem();
                String fechaInstStr = fechaInstField.getText().trim();
                String observaciones = observacionesArea.getText().trim();
                if (nombre.isEmpty() || ciudad.isEmpty() || pais.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Complete nombre/ciudad/pais", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                java.time.LocalDate fechaInstalacion = null;
                if (!fechaInstStr.isEmpty()) {
                    try {
                        fechaInstalacion = java.time.LocalDate.parse(fechaInstStr);
                    } catch (java.time.format.DateTimeParseException dte) {
                        JOptionPane.showMessageDialog(this, "Fecha con formato inválido (use AAAA-MM-DD)", "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }
                services.SensorService.getInstance().crearSensor(
                        nombre,
                        codigo.isEmpty() ? null : codigo,
                        tipo,
                        lat,
                        lon,
                        ciudad,
                        zona.isEmpty() ? null : zona,
                        pais,
                        estado,
                        fechaInstalacion,
                        observaciones.isEmpty() ? null : observaciones
                );
                // No es necesario setear flag: por defecto queda ACTIVA si no hay registro en Mongo
                JOptionPane.showMessageDialog(this, "Sensor creado (extracción ACTIVA por defecto)", "OK", JOptionPane.INFORMATION_MESSAGE);
                nombreField.setText("");
                codigoField.setText("");
                latField.setText("");
                lonField.setText("");
                ciudadField.setText("");
                zonaField.setText("");
                paisField.setText("");
                estadoCombo.setSelectedIndex(0);
                fechaInstField.setText("");
                observacionesArea.setText("");
                cargarSensores(model);
            } catch (NumberFormatException nfe) {
                JOptionPane.showMessageDialog(this, "Lat/Long deben ser numéricos", "Error", JOptionPane.ERROR_MESSAGE);
            } catch (exceptions.ErrorConexionCassandraException ex) {
                JOptionPane.showMessageDialog(this, "Error creando sensor: "+ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton refrescar = new JButton("Refrescar");
        JButton toggle = new JButton("Habilitar/Deshabilitar extracción");
        
        acciones.add(refrescar);
        acciones.add(toggle);
        
        // Generación automática global se inicia al hacer login; no necesitamos controles aquí

        refrescar.addActionListener(e -> { cargarSensores(model); });
        toggle.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { JOptionPane.showMessageDialog(this, "Selecciona un sensor", "Aviso", JOptionPane.WARNING_MESSAGE); return; }
            int modelRow = table.convertRowIndexToModel(row);
            String id = (String) model.getValueAt(modelRow, 0);
            String estado = (String) model.getValueAt(modelRow, 5);
            boolean activa = "ACTIVO".equalsIgnoreCase(estado);
            try {
                repository.SensorCassandraDAO.getInstance().actualizarEstado(id, activa ? "INACTIVO" : "ACTIVO");
                cargarSensores(model);
            } catch (exceptions.ErrorConexionCassandraException ex) {
                JOptionPane.showMessageDialog(this, "Error actualizando estado: "+ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        

        // Controles de auto-generación eliminados (corre global y continuo)

        cargarSensores(model);
        panel.add(alta, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(acciones, BorderLayout.SOUTH);
        return panel;
    }

    private void cargarSensores(DefaultTableModel model) {
        try {
            java.util.List<modelo.Sensor> sensores = repository.SensorCassandraDAO.getInstance().listarTodos();
            model.setRowCount(0);
            for (modelo.Sensor s : sensores) {
                String est = s.getEstado() != null ? s.getEstado().toUpperCase() : "ACTIVO";
                model.addRow(new Object[]{ s.getId(), s.getNombre(), s.getTipo(), s.getCiudad(), s.getPais(), est });
            }
        } catch (exceptions.ErrorConexionCassandraException e) {
            JOptionPane.showMessageDialog(this, "Error cargando sensores: "+e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JPanel crearPanelTecnicos() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        JPanel alta = new JPanel(new GridBagLayout());
        alta.setBorder(BorderFactory.createTitledBorder("Alta de Técnico"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5,5,5,5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField nombreField = new JTextField(18);
        JTextField apellidoField = new JTextField(18);
        JTextField emailField = new JTextField(18);
        JPasswordField passField = new JPasswordField(18);
        JTextField telefonoField = new JTextField(18);
        JTextField direccionField = new JTextField(18);

        gbc.gridx=0; gbc.gridy=0; alta.add(new JLabel("Nombre:"), gbc);
        gbc.gridx=1; alta.add(nombreField, gbc);
        gbc.gridx=0; gbc.gridy=1; alta.add(new JLabel("Apellido:"), gbc);
        gbc.gridx=1; alta.add(apellidoField, gbc);
        gbc.gridx=0; gbc.gridy=2; alta.add(new JLabel("Email:"), gbc);
        gbc.gridx=1; alta.add(emailField, gbc);
        gbc.gridx=0; gbc.gridy=3; alta.add(new JLabel("Contraseña:"), gbc);
        gbc.gridx=1; alta.add(passField, gbc);
        gbc.gridx=0; gbc.gridy=4; alta.add(new JLabel("Teléfono:"), gbc);
        gbc.gridx=1; alta.add(telefonoField, gbc);
        gbc.gridx=0; gbc.gridy=5; alta.add(new JLabel("Dirección:"), gbc);
        gbc.gridx=1; alta.add(direccionField, gbc);
        gbc.gridx=0; gbc.gridy=6; gbc.gridwidth=2; gbc.anchor = GridBagConstraints.EAST; gbc.fill = GridBagConstraints.NONE;
        JButton crearBtn = new JButton("Crear Técnico");
        alta.add(crearBtn, gbc);

        DefaultTableModel model = new DefaultTableModel(new Object[]{"ID","Nombre","Email","Activo"}, 0) {
            @Override public boolean isCellEditable(int r,int c){ return false; }
        };
        JTable table = new JTable(model);
        table.setFillsViewportHeight(true);
        JScrollPane scroll = new JScrollPane(table);

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton refrescarBtn = new JButton("Refrescar");
        JButton toggleBtn = new JButton("Activar/Desactivar");
        acciones.add(refrescarBtn);
        acciones.add(toggleBtn);

        crearBtn.addActionListener(e -> {
            String nombre = nombreField.getText().trim();
            String apellido = apellidoField.getText().trim();
            String email = emailField.getText().trim();
            String pass = new String(passField.getPassword());
            String tel = telefonoField.getText().trim();
            String dir = direccionField.getText().trim();
            if (nombre.isEmpty() || apellido.isEmpty() || email.isEmpty() || pass.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Completa nombre, apellido, email y contraseña", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            try {
                services.AuthService.getInstance().registrar(nombre, apellido, email, pass, "TECNICO", tel, dir);
                JOptionPane.showMessageDialog(this, "Técnico creado", "Info", JOptionPane.INFORMATION_MESSAGE);
                nombreField.setText(""); apellidoField.setText(""); emailField.setText(""); passField.setText(""); telefonoField.setText(""); direccionField.setText("");
                cargarTecnicos(model);
            } catch (ErrorConexionMongoException ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        refrescarBtn.addActionListener(e -> cargarTecnicos(model));
        toggleBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { JOptionPane.showMessageDialog(this, "Selecciona un técnico", "Aviso", JOptionPane.WARNING_MESSAGE); return; }
            String id = (String) model.getValueAt(row, 0);
            try {
                UsuarioMongoDAO dao = new UsuarioMongoDAO();
                Usuario u = dao.buscarPorId(id);
                if (u == null) { JOptionPane.showMessageDialog(this, "No encontrado", "Error", JOptionPane.ERROR_MESSAGE); return; }
                u.setActivo(u.getActivo()==null ? Boolean.FALSE : !u.getActivo());
                dao.actualizar(u);
                cargarTecnicos(model);
            } catch (ErrorConexionMongoException ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        cargarTecnicos(model);

        panel.add(alta, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        panel.add(acciones, BorderLayout.SOUTH);
        return panel;
    }

    private void cargarTecnicos(DefaultTableModel model) {
        try {
            UsuarioMongoDAO dao = new UsuarioMongoDAO();
            List<Usuario> tecnicos = dao.listarPorRol("TECNICO");
            model.setRowCount(0);
            for (Usuario u : tecnicos) {
                String nombre = (u.getNombre()!=null?u.getNombre():"") + " " + (u.getApellido()!=null?u.getApellido():"");
                model.addRow(new Object[]{u.getId(), nombre.trim(), u.getEmail(), u.getActivo()});
            }
        } catch (ErrorConexionMongoException e) {
            JOptionPane.showMessageDialog(this, "No se pudo cargar técnicos: "+e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JPanel crearPanelAlertas() {
        JPanel panel = new JPanel(new BorderLayout());
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Manual", crearPanelAlertasManual());
        tabs.addTab("Automáticas", crearPanelReglasAutomaticas());
        panel.add(tabs, BorderLayout.CENTER);
        return panel;
    }

    private JPanel crearPanelAlertasManual() {
        JPanel panel = new JPanel(new BorderLayout(10,10));
        panel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        JPanel alta = new JPanel(new GridBagLayout());
        alta.setBorder(BorderFactory.createTitledBorder("Enviar alerta masiva"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5,5,5,5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;

        JLabel info = new JLabel("Las alertas creadas aquí se envían automáticamente a todos los usuarios.");
        info.setFont(info.getFont().deriveFont(Font.ITALIC, 11f));
        info.setForeground(new Color(0x2f6b2f));

        JComboBox<String> tipoCombo = new JComboBox<>(new String[]{"SENSOR","CLIMATICA","SISTEMA"});
        JComboBox<String> sevCombo = new JComboBox<>(new String[]{"BAJA","MEDIA","ALTA","CRITICA"});
        JComboBox<SensorItem> sensorCombo = new JComboBox<>(construirModeloSensores());
        JButton recargarSensoresBtn = new JButton("Actualizar lista");
        recargarSensoresBtn.addActionListener(e -> sensorCombo.setModel(construirModeloSensores()));

        JComboBox<String> plantillaCombo = new JComboBox<>(ALERT_TEMPLATES);
        plantillaCombo.setSelectedIndex(0);
        JTextArea descArea = new JTextArea(3, 28);
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        descArea.setText(ALERT_TEMPLATES[0]);
        plantillaCombo.addActionListener(e -> {
            Object sel = plantillaCombo.getSelectedItem();
            if (sel != null) {
                descArea.setText(sel.toString());
            }
        });

        gbc.gridx=0; gbc.gridy=0; gbc.gridwidth=4; alta.add(info, gbc);
        gbc.gridwidth=1;

        gbc.gridx=0; gbc.gridy=1; alta.add(new JLabel("Tipo:"), gbc);
        gbc.gridx=1; alta.add(tipoCombo, gbc);
        gbc.gridx=2; alta.add(new JLabel("Severidad:"), gbc);
        gbc.gridx=3; alta.add(sevCombo, gbc);

        gbc.gridx=0; gbc.gridy=2; alta.add(new JLabel("Sensor asociado:"), gbc);
        gbc.gridx=1; gbc.gridwidth=2; alta.add(sensorCombo, gbc);
        gbc.gridx=3; gbc.gridwidth=1; alta.add(recargarSensoresBtn, gbc);

        gbc.gridx=0; gbc.gridy=3; alta.add(new JLabel("Plantilla rápida:"), gbc);
        gbc.gridx=1; gbc.gridwidth=3; alta.add(plantillaCombo, gbc);
        gbc.gridwidth=1;

        gbc.gridx=0; gbc.gridy=4; gbc.anchor = GridBagConstraints.NORTHWEST;
        alta.add(new JLabel("Mensaje a enviar:"), gbc);
        gbc.gridx=1; gbc.gridy=4; gbc.gridwidth=3; gbc.fill = GridBagConstraints.BOTH;
        JScrollPane descScroll = new JScrollPane(descArea);
        alta.add(descScroll, gbc);
        gbc.gridwidth=1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.EAST;

        gbc.gridx=3; gbc.gridy=5;
        JButton crearBtn = new JButton("Enviar alerta");
        alta.add(crearBtn, gbc);

        DefaultTableModel model = new DefaultTableModel(new Object[]{"Fecha","Tipo","Severidad","Sensor","Estado"},0){
            @Override public boolean isCellEditable(int r,int c){return false;}
        };
        JTable table = new JTable(model);
        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(true);

        JButton refrescarBtn = new JButton("Refrescar");

        crearBtn.addActionListener(e -> {
            try {
                String descripcion = descArea.getText().trim();
                if (descripcion.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Ingrese un mensaje para la alerta.", "Validación", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                Alerta alerta = new Alerta(
                        tipoCombo.getSelectedItem().toString(),
                        descripcion,
                        sevCombo.getSelectedItem().toString()
                );

                SensorItem sensorSel = (SensorItem) sensorCombo.getSelectedItem();
                if (sensorSel != null && sensorSel.tieneId()) {
                    alerta.setSensorId(sensorSel.id());
                }

                new AlertaMongoDAO().insertar(alerta);
                JOptionPane.showMessageDialog(this, "Alerta enviada a todos los usuarios.", "Info", JOptionPane.INFORMATION_MESSAGE);

                sensorCombo.setSelectedIndex(0);
                plantillaCombo.setSelectedIndex(0);
                descArea.setText(ALERT_TEMPLATES[0]);
                cargarAlertas(model);
            } catch (ErrorConexionMongoException ex) {
                JOptionPane.showMessageDialog(this, "Error: "+ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "No se pudo crear la alerta: " + ex.getMessage(),
                        "Error inesperado", JOptionPane.ERROR_MESSAGE);
            }
        });
        refrescarBtn.addActionListener(e -> cargarAlertas(model));
        cargarAlertas(model);

        JPanel center = new JPanel(new BorderLayout());
        center.add(new JScrollPane(table), BorderLayout.CENTER);
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.add(refrescarBtn);
        center.add(south, BorderLayout.SOUTH);

        panel.add(alta, BorderLayout.NORTH);
        panel.add(center, BorderLayout.CENTER);
        return panel;
    }

    private void cargarAlertas(DefaultTableModel model) {
        try {
            List<Alerta> alertas = new AlertaMongoDAO().listarActivas();
            model.setRowCount(0);
            for (Alerta a : alertas) {
                String sensor = a.getSensorId() != null ? a.getSensorId() : "-";
                model.addRow(new Object[]{
                        a.getFecha() != null ? a.getFecha() : "",
                        a.getTipo(),
                        a.getSeveridad(),
                        sensor,
                        a.getEstado()
                });
            }
        } catch (ErrorConexionMongoException e) {
            JOptionPane.showMessageDialog(this, "No se pudieron cargar alertas: "+e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cargarReglas(DefaultTableModel model) {
        try {
            List<AlertRule> reglas = AlertRuleService.getInstance().listarTodas();
            model.setRowCount(0);
            for (AlertRule regla : reglas) {
                model.addRow(new Object[]{
                        regla.getId(),
                        regla.getNombre(),
                        formatearUbicacion(regla),
                        describirCondiciones(regla),
                        regla.getVentanaMinutos() != null ? regla.getVentanaMinutos() + " min" : "-",
                        Boolean.TRUE.equals(regla.getActivo()) ? "ACTIVA" : "INACTIVA",
                        regla.getUltimaAlerta() != null ? ALERT_DATE_FORMAT.format(regla.getUltimaAlerta()) : "-"
                });
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "No se pudieron cargar las reglas: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void popularUbicaciones(JComboBox<String> ciudadCombo,
                                    JComboBox<String> zonaCombo,
                                    JComboBox<String> paisCombo) {
        try {
            List<Sensor> sensores = SensorService.getInstance().listarTodos();
            Set<String> ciudades = valoresUnicos(sensores, Sensor::getCiudad);
            Set<String> zonas = valoresUnicos(sensores, Sensor::getZona);
            Set<String> paises = valoresUnicos(sensores, Sensor::getPais);
            actualizarCombo(ciudadCombo, ciudades);
            actualizarCombo(zonaCombo, zonas);
            actualizarCombo(paisCombo, paises);
        } catch (ErrorConexionCassandraException e) {
            JOptionPane.showMessageDialog(this, "No se pudieron actualizar las ubicaciones: " + e.getMessage(),
                    "Aviso", JOptionPane.WARNING_MESSAGE);
        }
    }

    private Set<String> valoresUnicos(List<Sensor> sensores, Function<Sensor, String> extractor) {
        Set<String> valores = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (Sensor sensor : sensores) {
            String valor = extractor.apply(sensor);
            if (valor != null && !valor.isBlank()) {
                valores.add(valor.trim());
            }
        }
        return valores;
    }

    private void actualizarCombo(JComboBox<String> combo, Set<String> valores) {
        DefaultComboBoxModel<String> modelo = new DefaultComboBoxModel<>();
        modelo.addElement("(Todos)");
        for (String valor : valores) {
            modelo.addElement(valor);
        }
        combo.setModel(modelo);
    }

    private String opcionTexto(JComboBox<String> combo) {
        Object obj = combo.getSelectedItem();
        if (obj == null) {
            return null;
        }
        String valor = obj.toString();
        return "(Todos)".equalsIgnoreCase(valor) ? null : valor;
    }

    private LocalDateTime parseFecha(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(valor.trim()).atStartOfDay();
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Formato de fecha inválido. Use AAAA-MM-DD.");
        }
    }

    private Double parseDouble(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(valor.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Los campos numéricos deben contener valores válidos.");
        }
    }

    private String formatearUbicacion(AlertRule regla) {
        List<String> partes = new ArrayList<>();
        if (regla.getCiudad() != null && !regla.getCiudad().isBlank()) {
            partes.add(regla.getCiudad());
        }
        if (regla.getZona() != null && !regla.getZona().isBlank()) {
            partes.add(regla.getZona());
        }
        if (regla.getPais() != null && !regla.getPais().isBlank()) {
            partes.add(regla.getPais());
        }
        return partes.isEmpty() ? "Global" : String.join(" / ", partes);
    }

    private String describirCondiciones(AlertRule regla) {
        List<String> condiciones = new ArrayList<>();
        if (regla.getTemperaturaMin() != null || regla.getTemperaturaMax() != null) {
            condiciones.add("Temp " + construirRango(regla.getTemperaturaMin(), regla.getTemperaturaMax(), "°C"));
        }
        if (regla.getHumedadMin() != null || regla.getHumedadMax() != null) {
            condiciones.add("Hum " + construirRango(regla.getHumedadMin(), regla.getHumedadMax(), "%"));
        }
        return condiciones.isEmpty() ? "-" : String.join(" | ", condiciones);
    }

    private String construirRango(Double min, Double max, String unidad) {
        StringBuilder sb = new StringBuilder();
        if (min != null) {
            sb.append(">= ").append(min);
        }
        if (max != null) {
            if (min != null) sb.append(" ");
            sb.append("<= ").append(max);
        }
        if (unidad != null) {
            sb.append(" ").append(unidad);
        }
        return sb.toString().trim();
    }

    private void updateLastInteraction() {
        lastInteractionMs = System.currentTimeMillis();
    }

    private void verificarInactividad() {
        if (!logoutTriggered && System.currentTimeMillis() - lastInteractionMs >= INACTIVITY_LIMIT_MS) {
            cerrarSesionPorInactividad();
        }
    }

    private void cerrarSesionPorInactividad() {
        if (logoutTriggered) {
            return;
        }
        logoutTriggered = true;
        detenerMonitoreos();
        JOptionPane.showMessageDialog(this, "Sesión cerrada por inactividad (15 minutos).", "Sesión finalizada", JOptionPane.WARNING_MESSAGE);
        new LoginFrame().setVisible(true);
        dispose();
    }

    private void detenerMonitoreos() {
        if (inactivityTimer != null) {
            inactivityTimer.stop();
        }
        if (globalActivityListener != null) {
            Toolkit.getDefaultToolkit().removeAWTEventListener(globalActivityListener);
            globalActivityListener = null;
        }
    }

    private DefaultComboBoxModel<SensorItem> construirModeloSensores() {
        Vector<SensorItem> items = new Vector<>();
        items.add(SensorItem.sinSensor());
        try {
            List<Sensor> sensores = SensorService.getInstance().listarTodos();
            for (Sensor sensor : sensores) {
                if (sensor.getId() == null) {
                    continue;
                }
                String nombre = sensor.getNombre() != null && !sensor.getNombre().isBlank()
                        ? sensor.getNombre().trim()
                        : sensor.getId();
                String ciudad = sensor.getCiudad();
                String etiqueta = ciudad != null && !ciudad.isBlank()
                        ? nombre + " (" + ciudad + ")"
                        : nombre;
                items.add(new SensorItem(sensor.getId(), etiqueta));
            }
        } catch (ErrorConexionCassandraException e) {
            System.err.println("No se pudo cargar la lista de sensores para alertas: " + e.getMessage());
        }
        return new DefaultComboBoxModel<>(items);
    }

    private static class SensorItem {
        private final String id;
        private final String label;

        private SensorItem(String id, String label) {
            this.id = id;
            this.label = label;
        }

        private static SensorItem sinSensor() {
            return new SensorItem(null, "Sin sensor específico");
        }

        private boolean tieneId() {
            return id != null && !id.isBlank();
        }

        private String id() {
            return id;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
