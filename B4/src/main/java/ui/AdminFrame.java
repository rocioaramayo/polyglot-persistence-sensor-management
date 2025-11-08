package ui;

import exceptions.ErrorConexionMongoException;
import modelo.Alerta;
import modelo.Usuario;
import repository.AlertaMongoDAO;
import repository.UsuarioMongoDAO;
import ui.components.CassandraTerminalPanel;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.AWTEventListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class AdminFrame extends JFrame {
    private final String token;
    private static final long INACTIVITY_LIMIT_MS = TimeUnit.MINUTES.toMillis(15);
    private javax.swing.Timer inactivityTimer;
    private long lastInteractionMs;
    private AWTEventListener globalActivityListener;
    private boolean logoutTriggered;
    private CassandraTerminalPanel serviciosPanel;

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
        serviciosPanel = new CassandraTerminalPanel();
        tabs.addTab("Servicios", serviciosPanel);
        tabs.addChangeListener(e -> {
            updateLastInteraction();
            if (tabs.getSelectedComponent() == serviciosPanel) {
                serviciosPanel.startSessionTracking();
            } else {
                serviciosPanel.pauseSessionTracking();
            }
        });

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
        JPanel panel = new JPanel(new BorderLayout(10,10));
        panel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        JPanel alta = new JPanel(new GridBagLayout());
        alta.setBorder(BorderFactory.createTitledBorder("Crear Alerta"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5,5,5,5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JComboBox<String> tipoCombo = new JComboBox<>(new String[]{"SENSOR","CLIMATICA","SISTEMA"});
        JComboBox<String> sevCombo = new JComboBox<>(new String[]{"BAJA","MEDIA","ALTA","CRITICA"});
        JTextField sensorField = new JTextField(18);
        JTextField usuarioField = new JTextField(18);
        JTextField descField = new JTextField(28);

        gbc.gridx=0; gbc.gridy=0; alta.add(new JLabel("Tipo:"), gbc);
        gbc.gridx=1; alta.add(tipoCombo, gbc);
        gbc.gridx=0; gbc.gridy=1; alta.add(new JLabel("Severidad:"), gbc);
        gbc.gridx=1; alta.add(sevCombo, gbc);
        gbc.gridx=0; gbc.gridy=2; alta.add(new JLabel("SensorId (opcional):"), gbc);
        gbc.gridx=1; alta.add(sensorField, gbc);
        gbc.gridx=0; gbc.gridy=3; alta.add(new JLabel("UsuarioId (opcional):"), gbc);
        gbc.gridx=1; alta.add(usuarioField, gbc);
        gbc.gridx=0; gbc.gridy=4; alta.add(new JLabel("Descripción:"), gbc);
        gbc.gridx=1; alta.add(descField, gbc);
        gbc.gridx=0; gbc.gridy=5; gbc.gridwidth=2; gbc.anchor = GridBagConstraints.EAST; gbc.fill = GridBagConstraints.NONE;
        JButton crearBtn = new JButton("Crear Alerta");
        alta.add(crearBtn, gbc);

        DefaultTableModel model = new DefaultTableModel(new Object[]{"ID","Tipo","Severidad","Estado","Fecha"},0){
            @Override public boolean isCellEditable(int r,int c){return false;}
        };
        JTable table = new JTable(model);
        JButton refrescarBtn = new JButton("Refrescar");

        crearBtn.addActionListener(e -> {
            try {
                Alerta a = new Alerta(tipoCombo.getSelectedItem().toString(), descField.getText(), sevCombo.getSelectedItem().toString());
                String sId = sensorField.getText().trim(); if (!sId.isEmpty()) a.setSensorId(sId);
                String uId = usuarioField.getText().trim(); if (!uId.isEmpty()) a.setUsuarioId(uId);
                new AlertaMongoDAO().insertar(a);
                JOptionPane.showMessageDialog(this, "Alerta creada", "Info", JOptionPane.INFORMATION_MESSAGE);
                descField.setText(""); sensorField.setText(""); usuarioField.setText("");
                cargarAlertas(model);
            } catch (ErrorConexionMongoException ex) {
                JOptionPane.showMessageDialog(this, "Error: "+ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
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
                model.addRow(new Object[]{a.getId(), a.getTipo(), a.getSeveridad(), a.getEstado(), a.getFecha()});
            }
        } catch (ErrorConexionMongoException e) {
            JOptionPane.showMessageDialog(this, "No se pudieron cargar alertas: "+e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
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
        if (serviciosPanel != null) {
            serviciosPanel.pauseSessionTracking();
        }
        if (inactivityTimer != null) {
            inactivityTimer.stop();
        }
        if (globalActivityListener != null) {
            Toolkit.getDefaultToolkit().removeAWTEventListener(globalActivityListener);
            globalActivityListener = null;
        }
    }
}
