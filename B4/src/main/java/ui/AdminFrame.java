package ui;

import exceptions.ErrorConexionMongoException;
import modelo.Alerta;
import modelo.Proceso;
import modelo.Usuario;
import repository.AlertaMongoDAO;
import repository.ProcesoMongoDAO;
import repository.UsuarioMongoDAO;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class AdminFrame extends JFrame {
    private final String token;

    public AdminFrame(String token) {
        this.token = token;
        setTitle("Panel de Administración");
        setSize(900, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Sensores", crearPanelSensores());
        tabs.addTab("Técnicos", crearPanelTecnicos());
        tabs.addTab("Procesos", crearPanelProcesos());
        tabs.addTab("Alertas", crearPanelAlertas());

        JPanel header = new JPanel(new BorderLayout());
        JButton cerrarBtn = new JButton("Cerrar");
        cerrarBtn.addActionListener(e -> {
            // Volver al login
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
        JComboBox<String> tipoCombo = new JComboBox<>(new String[]{"temperatura","humedad"});
        JTextField latField = new JTextField(8);
        JTextField lonField = new JTextField(8);
        JTextField ciudadField = new JTextField(12);
        JTextField paisField = new JTextField(12);

        gbc.gridx=0; gbc.gridy=0; alta.add(new JLabel("Nombre:"), gbc);
        gbc.gridx=1; alta.add(nombreField, gbc);
        gbc.gridx=2; alta.add(new JLabel("Tipo:"), gbc);
        gbc.gridx=3; alta.add(tipoCombo, gbc);

        gbc.gridx=0; gbc.gridy=1; alta.add(new JLabel("Latitud:"), gbc);
        gbc.gridx=1; alta.add(latField, gbc);
        gbc.gridx=2; alta.add(new JLabel("Longitud:"), gbc);
        gbc.gridx=3; alta.add(lonField, gbc);

        gbc.gridx=0; gbc.gridy=2; alta.add(new JLabel("Ciudad:"), gbc);
        gbc.gridx=1; alta.add(ciudadField, gbc);
        gbc.gridx=2; alta.add(new JLabel("Pais:"), gbc);
        gbc.gridx=3; alta.add(paisField, gbc);

        gbc.gridx=0; gbc.gridy=3; gbc.gridwidth=4; gbc.anchor = GridBagConstraints.EAST; gbc.fill = GridBagConstraints.NONE;
        JButton crearBtn = new JButton("Crear");
        alta.add(crearBtn, gbc);

        crearBtn.addActionListener(e -> {
            try {
                String nombre = nombreField.getText().trim();
                String tipo = (String) tipoCombo.getSelectedItem();
                double lat = Double.parseDouble(latField.getText().trim());
                double lon = Double.parseDouble(lonField.getText().trim());
                String ciudad = ciudadField.getText().trim();
                String pais = paisField.getText().trim();
                if (nombre.isEmpty() || ciudad.isEmpty() || pais.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Complete nombre/ciudad/Pais", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                services.SensorService.getInstance().crearSensor(nombre, tipo, lat, lon, ciudad, pais);
                // No es necesario setear flag: por defecto queda ACTIVA si no hay registro en Mongo
                JOptionPane.showMessageDialog(this, "Sensor creado (extracción ACTIVA por defecto)", "OK", JOptionPane.INFORMATION_MESSAGE);
                nombreField.setText(""); latField.setText(""); lonField.setText(""); ciudadField.setText(""); paisField.setText("");
            } catch (NumberFormatException nfe) {
                JOptionPane.showMessageDialog(this, "Lat/Long deben ser numéricos", "Error", JOptionPane.ERROR_MESSAGE);
            } catch (exceptions.ErrorConexionCassandraException ex) {
                JOptionPane.showMessageDialog(this, "Error creando sensor: "+ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        DefaultTableModel model = new DefaultTableModel(new Object[]{"ID","Nombre","Tipo","Ciudad","Pais","Estado"},0){
            @Override public boolean isCellEditable(int r,int c){return false;}
        };
        JTable table = new JTable(model);
        table.setAutoCreateRowSorter(true);

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

    private JPanel crearPanelProcesos() {
        JPanel panel = new JPanel(new BorderLayout(10,10));
        panel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        JPanel alta = new JPanel(new GridBagLayout());
        alta.setBorder(BorderFactory.createTitledBorder("Alta de Proceso"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5,5,5,5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField nombreField = new JTextField(18);
        JTextField tipoField = new JTextField(18);
        JTextField costoField = new JTextField(10);
        JTextField descField = new JTextField(24);

        gbc.gridx=0; gbc.gridy=0; alta.add(new JLabel("Nombre:"), gbc);
        gbc.gridx=1; alta.add(nombreField, gbc);
        gbc.gridx=0; gbc.gridy=1; alta.add(new JLabel("Tipo:"), gbc);
        gbc.gridx=1; alta.add(tipoField, gbc);
        gbc.gridx=0; gbc.gridy=2; alta.add(new JLabel("Costo:"), gbc);
        gbc.gridx=1; alta.add(costoField, gbc);
        gbc.gridx=0; gbc.gridy=3; alta.add(new JLabel("Descripción:"), gbc);
        gbc.gridx=1; alta.add(descField, gbc);
        gbc.gridx=0; gbc.gridy=4; gbc.gridwidth=2; gbc.anchor = GridBagConstraints.EAST; gbc.fill = GridBagConstraints.NONE;
        JButton crearBtn = new JButton("Crear Proceso");
        alta.add(crearBtn, gbc);

        DefaultTableModel model = new DefaultTableModel(new Object[]{"Nombre","Tipo","Costo","Activo"},0){
            @Override public boolean isCellEditable(int r,int c){return false;}
        };
        JTable table = new JTable(model);

        crearBtn.addActionListener(e -> {
            String nombre = nombreField.getText().trim();
            String tipo = tipoField.getText().trim();
            String desc = descField.getText().trim();
            Double costo;
            try { costo = Double.parseDouble(costoField.getText().trim()); } catch(Exception ex){ costo = 0.0; }
            if (nombre.isEmpty() || tipo.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Completa nombre y tipo", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            try {
                Proceso p = new Proceso(nombre, desc, tipo, costo);
                new ProcesoMongoDAO().insertar(p);
                JOptionPane.showMessageDialog(this, "Proceso creado", "Info", JOptionPane.INFORMATION_MESSAGE);
                nombreField.setText(""); tipoField.setText(""); costoField.setText(""); descField.setText("");
                cargarProcesos(model);
            } catch (ErrorConexionMongoException ex) {
                JOptionPane.showMessageDialog(this, "Error: "+ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        cargarProcesos(model);

        panel.add(alta, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private void cargarProcesos(DefaultTableModel model) {
        try {
            List<Proceso> procesos = new ProcesoMongoDAO().listarActivos();
            model.setRowCount(0);
            for (Proceso p : procesos) {
                model.addRow(new Object[]{p.getNombre(), p.getTipo(), p.getCosto(), p.getActivo()});
            }
        } catch (ErrorConexionMongoException e) {
            JOptionPane.showMessageDialog(this, "No se pudieron cargar procesos: "+e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
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
}
