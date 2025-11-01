package ui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import services.*;
import modelo.*;
import exceptions.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DashboardFrame extends JFrame {
    private String token;
    private Usuario usuarioActual;
    private JTabbedPane tabbedPane;
    private DefaultTableModel mensajesRecibidosModel;
    private DefaultTableModel mensajesEnviadosModel;
    private JTable mensajesRecibidosTable;
    private JTable mensajesEnviadosTable;
    private DefaultListModel<String> gruposListModel;
    private JComboBox<DestinatarioItem> usuariosCombo;
    private JComboBox<DestinatarioItem> gruposCombo;
    private CardLayout destinatarioCardLayout;
    private JPanel destinatarioCardPanel;
    private final Map<String, String> usuariosCache = new HashMap<>();
    private final Map<String, String> gruposCache = new HashMap<>();
    private static final DateTimeFormatter MENSAJE_FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public DashboardFrame(String token) {
        this.token = token;
        setTitle("Polyglot Persistence - Dashboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);

        try {
            usuarioActual = AuthService.getInstance().validarToken(token);
            if (usuarioActual == null) {
                JOptionPane.showMessageDialog(this, "Invalid token", "Error", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Sensores", crearPanelSensores());
        tabbedPane.addTab("Procesos", crearPanelProcesos());
    tabbedPane.addTab("Solicitudes", crearPanelSolicitudes());
    tabbedPane.addTab("Alertas", crearPanelAlertas());
        tabbedPane.addTab("Mensajería", crearPanelMensajeria());
        tabbedPane.addTab("Cuenta", crearPanelCuenta());

        if (usuarioActual.getRol() != null && usuarioActual.getRol().equalsIgnoreCase("ADMINISTRADOR")) {
            tabbedPane.addTab("Admin", crearPanelAdmin());
        }

        JPanel header = crearHeader();
        JPanel container = new JPanel(new BorderLayout());
        container.add(header, BorderLayout.NORTH);
        container.add(tabbedPane, BorderLayout.CENTER);
        add(container);
    }

    private JPanel crearPanelSensores() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel formPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        formPanel.setBorder(BorderFactory.createTitledBorder("Crear Sensor"));

        JTextField nombreField = new JTextField();
        JComboBox<String> tipoCombo = new JComboBox<>(new String[]{"temperatura", "humedad"});
        JTextField latitudField = new JTextField();
        JTextField longitudField = new JTextField();
        JTextField ciudadField = new JTextField();
        JTextField paisField = new JTextField();

        formPanel.add(new JLabel("Nombre:"));
        formPanel.add(nombreField);
        formPanel.add(new JLabel("Tipo:"));
        formPanel.add(tipoCombo);
        formPanel.add(new JLabel("Latitud:"));
        formPanel.add(latitudField);
        formPanel.add(new JLabel("Longitud:"));
        formPanel.add(longitudField);
        formPanel.add(new JLabel("Ciudad:"));
        formPanel.add(ciudadField);
        formPanel.add(new JLabel("País:"));
        formPanel.add(paisField);

        JButton crearButton = new JButton("Crear Sensor");
        crearButton.addActionListener(e -> {
            try {
                SensorService.getInstance().crearSensor(
                    nombreField.getText(),
                    (String) tipoCombo.getSelectedItem(),
                    Double.parseDouble(latitudField.getText()),
                    Double.parseDouble(longitudField.getText()),
                    ciudadField.getText(),
                    paisField.getText()
                );
                JOptionPane.showMessageDialog(this, "Sensor creado exitosamente", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                nombreField.setText("");
                latitudField.setText("");
                longitudField.setText("");
                ciudadField.setText("");
                paisField.setText("");
            } catch (ErrorConexionCassandraException ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(crearButton);

        panel.add(formPanel, BorderLayout.NORTH);
        panel.add(buttonPanel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel crearPanelProcesos() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel label = new JLabel("Gestión de Procesos");
        label.setFont(new Font("Arial", Font.BOLD, 14));
        panel.add(label, BorderLayout.NORTH);

        JTextArea textArea = new JTextArea("Procesos disponibles:\n1. PROMEDIO_MENSUAL\n2. MAX_MIN\n3. ALERTAS");
        textArea.setEditable(false);
        panel.add(new JScrollPane(textArea), BorderLayout.CENTER);

        return panel;
    }

    private JPanel crearPanelMensajeria() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel envioPanel = construirPanelEnvioMensajes();
        JTabbedPane tabs = construirMensajeriaTabs();

        panel.add(envioPanel, BorderLayout.NORTH);
        panel.add(tabs, BorderLayout.CENTER);

        cargarCatalogosMensajeria();
        cargarMensajes();

        return panel;
    }

    private JPanel construirPanelEnvioMensajes() {
        JPanel envioPanel = new JPanel(new BorderLayout());
        envioPanel.setBorder(BorderFactory.createTitledBorder("Enviar mensaje"));

        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JComboBox<String> tipoCombo = new JComboBox<>(new String[]{"PRIVADO", "GRUPAL"});
        usuariosCombo = new JComboBox<>();
        gruposCombo = new JComboBox<>();

        DefaultListCellRenderer renderer = new DefaultListCellRenderer();
        renderer.setHorizontalAlignment(SwingConstants.LEFT);
        usuariosCombo.setRenderer(renderer);
        gruposCombo.setRenderer(renderer);

        destinatarioCardLayout = new CardLayout();
        destinatarioCardPanel = new JPanel(destinatarioCardLayout);
        destinatarioCardPanel.add(usuariosCombo, "PRIVADO");
        destinatarioCardPanel.add(gruposCombo, "GRUPAL");

        JTextArea contenidoArea = new JTextArea(4, 25);
        contenidoArea.setLineWrap(true);
        contenidoArea.setWrapStyleWord(true);

        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(new JLabel("Tipo:"), gbc);
        gbc.gridx = 1;
        formPanel.add(tipoCombo, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        formPanel.add(new JLabel("Destinatario:"), gbc);
        gbc.gridx = 1;
        formPanel.add(destinatarioCardPanel, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        formPanel.add(new JLabel("Contenido:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        formPanel.add(new JScrollPane(contenidoArea), gbc);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton actualizarButton = new JButton("Actualizar");
        JButton enviarButton = new JButton("Enviar");
        botones.add(actualizarButton);
        botones.add(enviarButton);

        envioPanel.add(formPanel, BorderLayout.CENTER);
        envioPanel.add(botones, BorderLayout.SOUTH);

        tipoCombo.addActionListener(e -> {
            String seleccion = ((String) tipoCombo.getSelectedItem()).toUpperCase();
            destinatarioCardLayout.show(destinatarioCardPanel, seleccion);
        });
        destinatarioCardLayout.show(destinatarioCardPanel, "PRIVADO");

        actualizarButton.addActionListener(e -> {
            cargarCatalogosMensajeria();
            cargarMensajes();
        });

        enviarButton.addActionListener(e -> {
            String tipoSeleccionado = ((String) tipoCombo.getSelectedItem()).toUpperCase();
            DestinatarioItem destinatario = ("GRUPAL".equals(tipoSeleccionado))
                    ? (DestinatarioItem) gruposCombo.getSelectedItem()
                    : (DestinatarioItem) usuariosCombo.getSelectedItem();

            if (destinatario == null) {
                JOptionPane.showMessageDialog(this, "Seleccione un destinatario", "Aviso", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String contenido = contenidoArea.getText();
            try {
                MensajeService.getInstance().enviarMensaje(
                        usuarioActual.getId(),
                        destinatario.getId(),
                        contenido,
                        tipoSeleccionado
                );
                JOptionPane.showMessageDialog(this, "Mensaje enviado correctamente", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                contenidoArea.setText("");
                cargarMensajes();
            } catch (IllegalArgumentException iae) {
                JOptionPane.showMessageDialog(this, iae.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            } catch (ErrorConexionMongoException ex) {
                JOptionPane.showMessageDialog(this, "Error enviando mensaje: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        return envioPanel;
    }

    private JTabbedPane construirMensajeriaTabs() {
        JTabbedPane tabs = new JTabbedPane();

        mensajesRecibidosModel = crearModeloMensajes();
        mensajesRecibidosTable = new JTable(mensajesRecibidosModel);
        mensajesRecibidosTable.setFillsViewportHeight(true);
        mensajesRecibidosTable.setAutoCreateRowSorter(true);

        mensajesEnviadosModel = crearModeloMensajes();
        mensajesEnviadosTable = new JTable(mensajesEnviadosModel);
        mensajesEnviadosTable.setFillsViewportHeight(true);
        mensajesEnviadosTable.setAutoCreateRowSorter(true);

        tabs.addTab("Recibidos", new JScrollPane(mensajesRecibidosTable));
        tabs.addTab("Enviados", new JScrollPane(mensajesEnviadosTable));
        tabs.addTab("Grupos", construirTabGrupos());

        return tabs;
    }

    private JPanel construirTabGrupos() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        gruposListModel = new DefaultListModel<>();
        JList<String> gruposList = new JList<>(gruposListModel);
        gruposList.setVisibleRowCount(8);
        panel.add(new JScrollPane(gruposList), BorderLayout.CENTER);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Crear nuevo grupo"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField nombreField = new JTextField(24);
        JTextField descripcionField = new JTextField(24);
        JTextField miembrosField = new JTextField(24);

        gbc.gridx = 0;
        gbc.gridy = 0;
        form.add(new JLabel("Nombre:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        form.add(nombreField, gbc);
        gbc.weightx = 0.0;

        gbc.gridx = 0;
        gbc.gridy = 1;
        form.add(new JLabel("Descripción:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        form.add(descripcionField, gbc);
        gbc.weightx = 0.0;

        gbc.gridx = 0;
        gbc.gridy = 2;
        form.add(new JLabel("Miembros (IDs separados por coma):"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        form.add(miembrosField, gbc);
        gbc.weightx = 0.0;

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.EAST;
        JButton crearGrupoButton = new JButton("Crear grupo");
        form.add(crearGrupoButton, gbc);

        crearGrupoButton.addActionListener(e -> {
            List<String> miembros = parsearMiembros(miembrosField.getText());
            if (!miembros.contains(usuarioActual.getId())) {
                miembros.add(usuarioActual.getId());
            }
            try {
                GrupoService.getInstance().crearGrupo(
                        nombreField.getText(),
                        descripcionField.getText(),
                        miembros
                );
                JOptionPane.showMessageDialog(this, "Grupo creado correctamente", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                nombreField.setText("");
                descripcionField.setText("");
                miembrosField.setText("");
                cargarCatalogosMensajeria();
            } catch (IllegalArgumentException iae) {
                JOptionPane.showMessageDialog(this, iae.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            } catch (ErrorConexionMongoException ex) {
                JOptionPane.showMessageDialog(this, "Error creando grupo: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        panel.add(form, BorderLayout.SOUTH);
        return panel;
    }

    private DefaultTableModel crearModeloMensajes() {
        return new DefaultTableModel(new Object[]{"Fecha", "Tipo", "Remitente", "Destinatario", "Contenido"}, 0) {
            private static final long serialVersionUID = 1L;
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private void cargarCatalogosMensajeria() {
        try {
            usuariosCache.clear();
            gruposCache.clear();

            if (usuarioActual != null) {
                String nombre = usuarioActual.getNombreCompleto() != null && !usuarioActual.getNombreCompleto().isBlank()
                        ? usuarioActual.getNombreCompleto()
                        : usuarioActual.getEmail();
                usuariosCache.put(usuarioActual.getId(), nombre != null && !nombre.isBlank() ? nombre : "Yo");
            }

            List<Usuario> usuarios = UsuarioService.getInstance().listarUsuariosActivos();
            DefaultComboBoxModel<DestinatarioItem> usuariosModel = new DefaultComboBoxModel<>();
            for (Usuario u : usuarios) {
                String label = (u.getNombreCompleto() != null && !u.getNombreCompleto().isBlank())
                        ? u.getNombreCompleto()
                        : u.getEmail();
                if (label == null || label.isBlank()) {
                    label = "Usuario " + u.getId();
                }
                usuariosCache.put(u.getId(), label);
                if (!u.getId().equals(usuarioActual.getId())) {
                    usuariosModel.addElement(new DestinatarioItem(u.getId(), label));
                }
            }
            usuariosCombo.setModel(usuariosModel);
            if (usuariosModel.getSize() > 0) {
                usuariosCombo.setSelectedIndex(0);
            }

            List<Grupo> grupos = GrupoService.getInstance().listarGruposDeUsuario(usuarioActual.getId());
            DefaultComboBoxModel<DestinatarioItem> gruposModel = new DefaultComboBoxModel<>();
            gruposListModel.clear();
            for (Grupo g : grupos) {
                String label = g.getNombre() != null && !g.getNombre().isBlank()
                        ? g.getNombre()
                        : "Grupo " + g.getId();
                gruposCache.put(g.getId(), label);
                gruposModel.addElement(new DestinatarioItem(g.getId(), label));
                gruposListModel.addElement(formatearDescripcionGrupo(g));
            }
            gruposCombo.setModel(gruposModel);
            if (gruposModel.getSize() > 0) {
                gruposCombo.setSelectedIndex(0);
            }
        } catch (ErrorConexionMongoException ex) {
            JOptionPane.showMessageDialog(this, "No se pudieron cargar los catálogos de mensajería: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cargarMensajes() {
        if (usuarioActual == null) {
            return;
        }
        try {
            mensajesRecibidosModel.setRowCount(0);
            mensajesEnviadosModel.setRowCount(0);

            List<Mensaje> recibidos = MensajeService.getInstance().obtenerMensajesRecibidos(usuarioActual.getId());
            for (Mensaje mensaje : recibidos) {
                mensajesRecibidosModel.addRow(crearFilaMensaje(mensaje));
            }

            List<Mensaje> enviados = MensajeService.getInstance().obtenerMensajesEnviados(usuarioActual.getId());
            for (Mensaje mensaje : enviados) {
                mensajesEnviadosModel.addRow(crearFilaMensaje(mensaje));
            }
        } catch (ErrorConexionMongoException ex) {
            JOptionPane.showMessageDialog(this, "No se pudieron cargar los mensajes: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private Object[] crearFilaMensaje(Mensaje mensaje) {
        String fecha = mensaje.getFechaHora() != null ? MENSAJE_FORMATO.format(mensaje.getFechaHora()) : "-";
        String tipo = mensaje.getTipo() != null ? mensaje.getTipo().toUpperCase() : "-";
        String remitente = obtenerNombreUsuario(mensaje.getRemitenteId());
        String destinatario = "GRUPAL".equalsIgnoreCase(mensaje.getTipo())
                ? obtenerNombreGrupo(mensaje.getDestinatarioId())
                : obtenerNombreUsuario(mensaje.getDestinatarioId());
        String contenido = mensaje.getContenido() != null ? mensaje.getContenido() : "";
        return new Object[]{fecha, tipo, remitente, destinatario, contenido};
    }

    private String obtenerNombreUsuario(String usuarioId) {
        if (usuarioId == null || usuarioId.isBlank()) {
            return "-";
        }
        if (usuarioId.equals(usuarioActual.getId())) {
            String nombre = usuariosCache.get(usuarioId);
            return nombre != null ? nombre : "Yo";
        }
        if (usuariosCache.containsKey(usuarioId)) {
            return usuariosCache.get(usuarioId);
        }
        try {
            Usuario usuario = UsuarioService.getInstance().obtenerPorId(usuarioId);
            if (usuario != null) {
                String label = (usuario.getNombreCompleto() != null && !usuario.getNombreCompleto().isBlank())
                        ? usuario.getNombreCompleto()
                        : usuario.getEmail();
                if (label == null || label.isBlank()) {
                    label = "Usuario " + usuarioId;
                }
                usuariosCache.put(usuarioId, label);
                return label;
            }
        } catch (ErrorConexionMongoException ignored) {
        }
        return "Usuario " + usuarioId;
    }

    private String obtenerNombreGrupo(String grupoId) {
        if (grupoId == null || grupoId.isBlank()) {
            return "-";
        }
        if (gruposCache.containsKey(grupoId)) {
            return gruposCache.get(grupoId);
        }
        try {
            Grupo grupo = GrupoService.getInstance().obtenerGrupo(grupoId);
            if (grupo != null) {
                String nombre = grupo.getNombre() != null && !grupo.getNombre().isBlank()
                        ? grupo.getNombre()
                        : "Grupo " + grupoId;
                gruposCache.put(grupoId, nombre);
                return nombre;
            }
        } catch (ErrorConexionMongoException ignored) {
        }
        return "Grupo " + grupoId;
    }

    private String formatearDescripcionGrupo(Grupo grupo) {
        String nombre = grupo.getNombre() != null && !grupo.getNombre().isBlank()
                ? grupo.getNombre()
                : "Grupo " + grupo.getId();
        int cantidad = grupo.getMiembros() != null ? grupo.getMiembros().size() : 0;
        String descripcion = grupo.getDescripcion() != null && !grupo.getDescripcion().isBlank()
                ? " - " + grupo.getDescripcion()
                : "";
        return nombre + " (" + cantidad + " miembros)" + descripcion;
    }

    private List<String> parsearMiembros(String texto) {
        if (texto == null || texto.isBlank()) {
            return new ArrayList<>();
        }
        return Arrays.stream(texto.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private static class DestinatarioItem {
        private final String id;
        private final String label;

        DestinatarioItem(String id, String label) {
            this.id = id;
            this.label = label;
        }

        String getId() {
            return id;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private JPanel crearPanelCuenta() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel label = new JLabel("Cuenta Corriente");
        label.setFont(new Font("Arial", Font.BOLD, 14));
        panel.add(label, BorderLayout.NORTH);

        JTextArea textArea = new JTextArea();
        textArea.setEditable(false);

        try {
            CuentaCorriente cuenta = null;
            try {
                cuenta = repository.CuentaMySQLRepository.getInstance()
                        .obtenerPorUsuario(Integer.parseInt(usuarioActual.getId()));
            } catch (NumberFormatException nfe) {
                // id no numérico -> no hay cuenta en MySQL
                cuenta = null;
            }
            if (cuenta != null) {
                textArea.setText("Saldo: $" + cuenta.getSaldo() + "\n\nMovimientos:\n");
                List<Movimiento> movimientos = repository.MovimientoMySQLRepository.getInstance()
                        .obtenerPorCuenta(cuenta.getId());
                for (Movimiento m : movimientos) {
                    textArea.append(m.getTipo() + ": $" + m.getMonto() + " - " + m.getDescripcion() + "\n");
                }
            }
        } catch (ErrorConexionMySQLException ex) {
            textArea.setText("Error: " + ex.getMessage());
        }

        panel.add(new JScrollPane(textArea), BorderLayout.CENTER);

        return panel;
    }

    private JPanel crearPanelSolicitudes() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel form = new JPanel(new GridLayout(4, 2, 10, 10));
        form.setBorder(BorderFactory.createTitledBorder("Crear Solicitud de Proceso"));

        JTextField procesoIdField = new JTextField();
        JTextField parametrosField = new JTextField();
        // parametros simple: key1=val1;key2=val2

        form.add(new JLabel("Proceso ID:"));
        form.add(procesoIdField);
        form.add(new JLabel("Parámetros (key=val;...):"));
        form.add(parametrosField);

        JButton crearBtn = new JButton("Crear Solicitud");
        crearBtn.addActionListener(e -> {
            String procesoId = procesoIdField.getText().trim();
            String paramsText = parametrosField.getText().trim();
            if (procesoId.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Ingrese el ID del proceso", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            java.util.Map<String, Object> params = new java.util.HashMap<>();
            if (!paramsText.isEmpty()) {
                String[] pairs = paramsText.split(";");
                for (String p : pairs) {
                    String[] kv = p.split("=", 2);
                    if (kv.length == 2) params.put(kv[0].trim(), kv[1].trim());
                }
            }

            try {
                String solicitudId = services.SolicitudProcesoService.getInstance()
                        .crearSolicitud(usuarioActual.getId(), procesoId, params);
                JOptionPane.showMessageDialog(this, "Solicitud creada. ID: " + solicitudId, "Éxito", JOptionPane.INFORMATION_MESSAGE);
                procesoIdField.setText("");
                parametrosField.setText("");
            } catch (exceptions.ErrorConexionMongoException ex) {
                JOptionPane.showMessageDialog(this, "Error creando solicitud: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        JPanel btnPanel = new JPanel();
        btnPanel.add(crearBtn);

        panel.add(form, BorderLayout.NORTH);
        panel.add(btnPanel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel crearPanelAlertas() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        JTextArea textArea = new JTextArea();
        textArea.setEditable(false);

        JButton verBtn = new JButton("Ver alertas");
        verBtn.addActionListener(e -> {
            textArea.setText("");
            try {
                String usuarioId = usuarioActual.getId();
                repository.AlertaMongoDAO dao = new repository.AlertaMongoDAO();
                java.util.List<modelo.Alerta> alertas = dao.listarPorUsuario(usuarioId);
                if (alertas.isEmpty()) {
                    textArea.setText("No hay alertas");
                } else {
                    for (modelo.Alerta a : alertas) {
                        textArea.append("[" + a.getSeveridad() + "] " + a.getTipo() + " - " + a.getDescripcion() + " (" + a.getFecha() + ")\n\n");
                    }
                }
            } catch (Exception ex) {
                textArea.setText("Error al obtener alertas: " + ex.getMessage());
            }
        });

        JPanel top = new JPanel();
        top.add(verBtn);

        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(textArea), BorderLayout.CENTER);
        return panel;
    }

    private JPanel crearHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        String nombre = (usuarioActual.getNombreCompleto() != null && !usuarioActual.getNombreCompleto().isBlank())
                ? usuarioActual.getNombreCompleto() : usuarioActual.getEmail();
        JLabel userLabel = new JLabel("Hola, " + (nombre != null ? nombre : "usuario") + "  (" + usuarioActual.getRol() + ")");
        header.add(userLabel, BorderLayout.WEST);

        JButton logoutBtn = new JButton("Cerrar sesion");
        logoutBtn.addActionListener(this::handleLogout);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        right.add(logoutBtn);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    private void handleLogout(java.awt.event.ActionEvent e) {
        try {
            AuthService.getInstance().logout(token);
        } catch (ErrorConexionRedisException ex) {
            JOptionPane.showMessageDialog(this, "No se pudo cerrar sesion: " + ex.getMessage(), "Aviso", JOptionPane.WARNING_MESSAGE);
        }
        SwingUtilities.invokeLater(() -> {
            new LoginFrame().setVisible(true);
            this.dispose();
        });
    }

    private JPanel crearPanelAdmin() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel titulo = new JLabel("Administracion");
        titulo.setFont(new Font("Arial", Font.BOLD, 16));
        panel.add(titulo, BorderLayout.NORTH);

        JPanel altaPanel = new JPanel(new GridBagLayout());
        altaPanel.setBorder(BorderFactory.createTitledBorder("Alta de Tecnico"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField nombreField = new JTextField(20);
        JTextField apellidoField = new JTextField(20);
        JTextField emailField = new JTextField(20);
        JPasswordField passField = new JPasswordField(20);
        JTextField telefonoField = new JTextField(20);
        JTextField direccionField = new JTextField(20);

        gbc.gridx = 0; gbc.gridy = 0; altaPanel.add(new JLabel("Nombre:"), gbc);
        gbc.gridx = 1; altaPanel.add(nombreField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; altaPanel.add(new JLabel("Apellido:"), gbc);
        gbc.gridx = 1; altaPanel.add(apellidoField, gbc);

        gbc.gridx = 0; gbc.gridy = 2; altaPanel.add(new JLabel("Email:"), gbc);
        gbc.gridx = 1; altaPanel.add(emailField, gbc);

        gbc.gridx = 0; gbc.gridy = 3; altaPanel.add(new JLabel("Contrasena:"), gbc);
        gbc.gridx = 1; altaPanel.add(passField, gbc);

        gbc.gridx = 0; gbc.gridy = 4; altaPanel.add(new JLabel("Telefono:"), gbc);
        gbc.gridx = 1; altaPanel.add(telefonoField, gbc);

        gbc.gridx = 0; gbc.gridy = 5; altaPanel.add(new JLabel("Direccion:"), gbc);
        gbc.gridx = 1; altaPanel.add(direccionField, gbc);

        gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.EAST; gbc.fill = GridBagConstraints.NONE;
        JButton crearBtn = new JButton("Crear Tecnico");
        altaPanel.add(crearBtn, gbc);

        crearBtn.addActionListener(ev -> {
            String nombre = nombreField.getText().trim();
            String apellido = apellidoField.getText().trim();
            String email = emailField.getText().trim();
            String pass = new String(passField.getPassword());
            String telefono = telefonoField.getText().trim();
            String direccion = direccionField.getText().trim();
            if (nombre.isEmpty() || apellido.isEmpty() || email.isEmpty() || pass.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Completa todos los campos", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            try {
                AuthService.getInstance().registrar(nombre, apellido, email, pass, "TECNICO", telefono, direccion);
                JOptionPane.showMessageDialog(this, "Tecnico creado correctamente", "Exito", JOptionPane.INFORMATION_MESSAGE);
                nombreField.setText("");
                apellidoField.setText("");
                emailField.setText("");
                passField.setText("");
                telefonoField.setText("");
                direccionField.setText("");
                cargarCatalogosMensajeria();
            } catch (ErrorConexionMongoException ex) {
                JOptionPane.showMessageDialog(this, "Error creando tecnico: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        panel.add(altaPanel, BorderLayout.CENTER);
        return panel;
    }
}
