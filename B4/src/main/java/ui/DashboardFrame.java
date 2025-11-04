package ui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import services.*;
import modelo.*;
import exceptions.*;
import java.awt.*;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


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
    private final Map<String, Proceso> procesoCache = new HashMap<>();
    private List<Proceso> procesosDisponibles = new ArrayList<>();

    private JComboBox<ProcesoItem> procesosCombo;
    private JTextArea detalleProcesoArea;
    private JTextField ciudadField;
    private JTextField zonaField;
    private JTextField paisField;
    private JTextField fechaInicioField;
    private JTextField fechaFinField;
    private JTextField periodicidadField;
    private JTextField observacionesField;
    private JComboBox<String> tipoSensorCombo;
    private DefaultTableModel solicitudesModel;
    private JTable solicitudesTable;

    private JLabel saldoLabel;
    private JTextField depositoField;
    private JTextField referenciaDepositoField;
    private DefaultTableModel facturasModel;
    private JTable facturasTable;
    private DefaultTableModel movimientosModel;
    private JTable movimientosTable;

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

        cargarProcesosDisponibles();

        tabbedPane = new JTabbedPane();
        // La gestión de sensores se realiza solo en AdminFrame
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

    private void cargarProcesosDisponibles() {
        try {
            procesosDisponibles = ProcesoService.getInstance().listarActivos();
            procesoCache.clear();
            for (Proceso proceso : procesosDisponibles) {
                if (proceso.getId() != null) {
                    procesoCache.put(proceso.getId(), proceso);
                }
            }
        } catch (ErrorConexionMongoException e) {
            procesosDisponibles = new ArrayList<>();
            System.err.println("No se pudieron cargar los procesos disponibles: " + e.getMessage());
        }
    }

    private JPanel crearPanelProcesos() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel label = new JLabel("Catálogo de Procesos Disponibles");
        label.setFont(new Font("Arial", Font.BOLD, 16));
        panel.add(label, BorderLayout.NORTH);

        DefaultTableModel model = new DefaultTableModel(new Object[]{"Nombre", "Tipo", "Costo", "Descripción"}, 0) {
            @Override public boolean isCellEditable(int r, int c){return false;}
        };
        JTable tabla = new JTable(model);
        tabla.setFillsViewportHeight(true);
        tabla.setAutoCreateRowSorter(true);
        tabla.setRowHeight(48);
        tabla.getColumnModel().getColumn(0).setPreferredWidth(220);
        tabla.getColumnModel().getColumn(1).setPreferredWidth(150);
        tabla.getColumnModel().getColumn(2).setPreferredWidth(110);
        tabla.getColumnModel().getColumn(3).setPreferredWidth(360);
        tabla.getColumnModel().getColumn(3).setCellRenderer(new DescripcionCellRenderer());

        if (procesosDisponibles != null && !procesosDisponibles.isEmpty()) {
            for (Proceso proceso : procesosDisponibles) {
                String costo = proceso.getCosto() != null ? String.format("$ %.2f", proceso.getCosto()) : "-";
                model.addRow(new Object[]{
                        proceso.getNombre(),
                        proceso.getTipo(),
                        costo,
                        proceso.getDescripcion()
                });
            }
        } else {
            model.addRow(new Object[]{"-", "-", "-", "No hay procesos disponibles en este momento"});
        }

        panel.add(new JScrollPane(tabla), BorderLayout.CENTER);

        return panel;
    }
    private static class DescripcionCellRenderer extends JTextArea implements TableCellRenderer {
        DescripcionCellRenderer() {
            setLineWrap(true);
            setWrapStyleWord(true);
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            String texto = value == null ? "" : value.toString();
            setText(texto);
            setFont(table.getFont());
            if (isSelected) {
                setForeground(table.getSelectionForeground());
                setBackground(table.getSelectionBackground());
            } else {
                setForeground(table.getForeground());
                setBackground(table.getBackground());
            }
            setSize(table.getColumnModel().getColumn(column).getWidth(), Short.MAX_VALUE);
            int preferred = getPreferredSize().height;
            int baseHeight = Math.max(48, preferred);
            if (table.getRowHeight(row) != baseHeight) {
                table.setRowHeight(row, baseHeight);
            }
            return this;
        }
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
        DefaultListModel<DestinatarioItem> miembrosModel = new DefaultListModel<>();
        JList<DestinatarioItem> miembrosList = new JList<>(miembrosModel);
        miembrosList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        miembrosList.setVisibleRowCount(8);
        // Toggle de selección por clic (sin necesidad de Ctrl)
        miembrosList.setSelectionModel(new DefaultListSelectionModel() {
            @Override
            public void setSelectionInterval(int index0, int index1) {
                if (isSelectedIndex(index0)) {
                    removeSelectionInterval(index0, index1);
                } else {
                    addSelectionInterval(index0, index1);
                }
            }
        });

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
        form.add(new JLabel("Miembros (seleccione):"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        JScrollPane miembrosScroll = new JScrollPane(miembrosList);
        miembrosScroll.setPreferredSize(new Dimension(220, 120));
        form.add(miembrosScroll, gbc);
        gbc.weightx = 0.0;

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.EAST;
        JButton crearGrupoButton = new JButton("Crear grupo");
        form.add(crearGrupoButton, gbc);

        crearGrupoButton.addActionListener(e -> {
            List<String> miembros = new ArrayList<>();
            for (DestinatarioItem it : miembrosList.getSelectedValuesList()) {
                if (it != null && it.getId() != null && !it.getId().isBlank()) {
                    if (!miembros.contains(it.getId())) miembros.add(it.getId());
                }
            }
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
                miembrosList.clearSelection();
                cargarCatalogosMensajeria();
            } catch (IllegalArgumentException iae) {
                JOptionPane.showMessageDialog(this, iae.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            } catch (ErrorConexionMongoException ex) {
                JOptionPane.showMessageDialog(this, "Error creando grupo: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        panel.add(form, BorderLayout.SOUTH);
        // Cargar usuarios activos para el selector de miembros
        try {
            List<Usuario> usuarios = UsuarioService.getInstance().listarUsuariosActivos();
            miembrosModel.clear();
            int selfIndex = -1;
            int idx = 0;
            for (Usuario u : usuarios) {
                String label = (u.getNombreCompleto() != null && !u.getNombreCompleto().isBlank()) ? u.getNombreCompleto() : u.getEmail();
                if (label == null || label.isBlank()) label = "Usuario " + u.getId();
                if (usuarioActual != null && u.getId() != null && u.getId().equals(usuarioActual.getId())) {
                    label = "Yo - " + label;
                    selfIndex = idx;
                }
                miembrosModel.addElement(new DestinatarioItem(u.getId(), label));
                idx++;
            }
            if (selfIndex == -1 && usuarioActual != null) {
                String base = (usuarioActual.getNombreCompleto() != null && !usuarioActual.getNombreCompleto().isBlank()) ? usuarioActual.getNombreCompleto() : usuarioActual.getEmail();
                if (base == null || base.isBlank()) base = "Usuario " + usuarioActual.getId();
                miembrosModel.add(0, new DestinatarioItem(usuarioActual.getId(), "Yo - " + base));
                selfIndex = 0;
            }
            if (selfIndex >= 0) {
                miembrosList.setSelectedIndex(selfIndex);
            }
        } catch (ErrorConexionMongoException ex) {
            // ignoramos carga fallida
        }
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

    private static class ProcesoItem {
        private final String id;
        private final String nombre;
        private final String descripcion;
        private final String tipo;
        private final Double costo;

        ProcesoItem(Proceso proceso) {
            if (proceso != null) {
                this.id = proceso.getId();
                this.nombre = proceso.getNombre();
                this.descripcion = proceso.getDescripcion();
                this.tipo = proceso.getTipo();
                this.costo = proceso.getCosto();
            } else {
                this.id = null;
                this.nombre = null;
                this.descripcion = null;
                this.tipo = null;
                this.costo = null;
            }
        }

        String getId() {
            return id;
        }

        @Override
        public String toString() {
            return nombre != null && !nombre.isBlank() ? nombre : "Selecciona un proceso";
        }
    }

    private JPanel crearPanelCuenta() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel titulo = new JLabel("Cuenta Corriente");
        titulo.setFont(new Font("Arial", Font.BOLD, 16));
        header.add(titulo);
        saldoLabel = new JLabel("Saldo: --");
        saldoLabel.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));
        header.add(saldoLabel);
        JPanel depositosPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        depositosPanel.setBorder(BorderFactory.createTitledBorder("Ingresar dinero"));
        depositosPanel.add(new JLabel("Monto:"));
        depositoField = new JTextField(8);
        depositosPanel.add(depositoField);
        depositosPanel.add(new JLabel("Referencia:"));
        referenciaDepositoField = new JTextField(12);
        depositosPanel.add(referenciaDepositoField);
        JButton depositarBtn = new JButton("Depositar");
        depositarBtn.addActionListener(e -> realizarDeposito());
        depositosPanel.add(depositarBtn);

        JPanel cabecera = new JPanel(new BorderLayout());
        cabecera.add(header, BorderLayout.NORTH);
        cabecera.add(depositosPanel, BorderLayout.SOUTH);
        panel.add(cabecera, BorderLayout.NORTH);

        facturasModel = new DefaultTableModel(new Object[]{"ID", "Descripción", "Monto", "Estado", "Emisión"}, 0) {
            @Override public boolean isCellEditable(int r,int c){return false;}
        };
        facturasTable = new JTable(facturasModel);
        facturasTable.setAutoCreateRowSorter(true);
        facturasTable.setFillsViewportHeight(true);

        JButton pagarBtn = new JButton("Pagar factura seleccionada");
        pagarBtn.addActionListener(e -> pagarFacturaSeleccionada());
        JButton refrescarCuentaBtn = new JButton("Refrescar");
        refrescarCuentaBtn.addActionListener(e -> refrescarCuenta());
        JPanel accionesFacturas = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        accionesFacturas.add(refrescarCuentaBtn);
        accionesFacturas.add(pagarBtn);

        JPanel facturasPanel = new JPanel(new BorderLayout());
        facturasPanel.setBorder(BorderFactory.createTitledBorder("Facturas"));
        facturasPanel.add(new JScrollPane(facturasTable), BorderLayout.CENTER);
        facturasPanel.add(accionesFacturas, BorderLayout.SOUTH);

        movimientosModel = new DefaultTableModel(new Object[]{"Fecha", "Tipo", "Monto", "Descripción"}, 0) {
            @Override public boolean isCellEditable(int r,int c){return false;}
        };
        movimientosTable = new JTable(movimientosModel);
        movimientosTable.setAutoCreateRowSorter(true);
        movimientosTable.setFillsViewportHeight(true);

        JPanel movimientosPanel = new JPanel(new BorderLayout());
        movimientosPanel.setBorder(BorderFactory.createTitledBorder("Movimientos"));
        movimientosPanel.add(new JScrollPane(movimientosTable), BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, facturasPanel, movimientosPanel);
        split.setResizeWeight(0.5);
        split.setBorder(null);
        panel.add(split, BorderLayout.CENTER);

        refrescarCuenta();

        return panel;
    }

    private JPanel crearPanelSolicitudes() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel formulario = new JPanel(new GridBagLayout());
        formulario.setBorder(BorderFactory.createTitledBorder("Solicitar un proceso"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;

        procesosCombo = new JComboBox<>();
        if (procesosDisponibles != null) {
            for (Proceso proceso : procesosDisponibles) {
                procesosCombo.addItem(new ProcesoItem(proceso));
            }
        }
        procesosCombo.addActionListener(e -> actualizarDetalleProceso());

        detalleProcesoArea = new JTextArea(4, 30);
        detalleProcesoArea.setLineWrap(true);
        detalleProcesoArea.setWrapStyleWord(true);
        detalleProcesoArea.setEditable(false);

        ciudadField = new JTextField(18);
        zonaField = new JTextField(18);
        paisField = new JTextField(18);
        fechaInicioField = new JTextField(12);
        fechaFinField = new JTextField(12);
        periodicidadField = new JTextField(12);
        observacionesField = new JTextField(25);
        tipoSensorCombo = new JComboBox<>(new String[]{"AUTO", "Temperatura", "Humedad"});

        formulario.add(new JLabel("Proceso:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        formulario.add(procesosCombo, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        gbc.weighty = 0;
        formulario.add(new JScrollPane(detalleProcesoArea), gbc);
        gbc.gridwidth = 1;
        gbc.weightx = 0;

        gbc.gridy++;
        formulario.add(new JLabel("Ciudad:"), gbc);
        gbc.gridx = 1;
        formulario.add(ciudadField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        formulario.add(new JLabel("Zona / Región:"), gbc);
        gbc.gridx = 1;
        formulario.add(zonaField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        formulario.add(new JLabel("País:"), gbc);
        gbc.gridx = 1;
        formulario.add(paisField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        formulario.add(new JLabel("Tipo de sensor:"), gbc);
        gbc.gridx = 1;
        formulario.add(tipoSensorCombo, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        formulario.add(new JLabel("Fecha inicio (AAAA-MM-DD):"), gbc);
        gbc.gridx = 1;
        formulario.add(fechaInicioField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        formulario.add(new JLabel("Fecha fin (AAAA-MM-DD):"), gbc);
        gbc.gridx = 1;
        formulario.add(fechaFinField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        formulario.add(new JLabel("Periodicidad (ej. mensual):"), gbc);
        gbc.gridx = 1;
        formulario.add(periodicidadField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        formulario.add(new JLabel("Observaciones:"), gbc);
        gbc.gridx = 1;
        formulario.add(observacionesField, gbc);

        JButton crearBtn = new JButton("Enviar solicitud");
        crearBtn.addActionListener(e -> crearSolicitudUsuario());
        JButton limpiarBtn = new JButton("Limpiar");
        limpiarBtn.addActionListener(e -> limpiarFormularioSolicitud());

        JPanel accionesForm = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        accionesForm.add(limpiarBtn);
        accionesForm.add(crearBtn);

        JPanel top = new JPanel(new BorderLayout());
        top.add(formulario, BorderLayout.CENTER);
        top.add(accionesForm, BorderLayout.SOUTH);

        panel.add(top, BorderLayout.NORTH);

        solicitudesModel = new DefaultTableModel(new Object[]{"ID", "Proceso", "Estado", "Técnico", "Sensor", "Fecha", "Resultado"}, 0) {
            @Override public boolean isCellEditable(int r,int c){return false;}
        };
        solicitudesTable = new JTable(solicitudesModel);
        solicitudesTable.setAutoCreateRowSorter(true);
        solicitudesTable.setFillsViewportHeight(true);

        JPanel tablaPanel = new JPanel(new BorderLayout());
        tablaPanel.setBorder(BorderFactory.createTitledBorder("Historial de solicitudes"));
        tablaPanel.add(new JScrollPane(solicitudesTable), BorderLayout.CENTER);
        JButton refrescarBtn = new JButton("Refrescar");
        refrescarBtn.addActionListener(e -> refrescarSolicitudesUsuario());
        JPanel accionesTabla = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        accionesTabla.add(refrescarBtn);
        tablaPanel.add(accionesTabla, BorderLayout.SOUTH);

        panel.add(tablaPanel, BorderLayout.CENTER);

        actualizarDetalleProceso();
        refrescarSolicitudesUsuario();

        return panel;
    }

    private void actualizarDetalleProceso() {
        if (detalleProcesoArea == null || procesosCombo == null) {
            return;
        }
        ProcesoItem item = (ProcesoItem) procesosCombo.getSelectedItem();
        if (item == null || item.id == null) {
            detalleProcesoArea.setText("Selecciona un proceso para ver sus detalles.");
            return;
        }
        StringBuilder detalle = new StringBuilder();
        detalle.append("Tipo: ").append(item.tipo != null ? item.tipo : "-").append("\n");
        if (item.costo != null) {
            detalle.append("Costo estimado: $").append(String.format("%.2f", item.costo)).append("\n");
        }
        if (item.descripcion != null && !item.descripcion.isBlank()) {
            detalle.append("\n").append(item.descripcion);
        }
        detalleProcesoArea.setText(detalle.toString());
    }

    private void limpiarFormularioSolicitud() {
        ciudadField.setText("");
        zonaField.setText("");
        paisField.setText("");
        fechaInicioField.setText("");
        fechaFinField.setText("");
        periodicidadField.setText("");
        observacionesField.setText("");
        if (tipoSensorCombo != null) {
            tipoSensorCombo.setSelectedIndex(0);
        }
    }

    private void crearSolicitudUsuario() {
        ProcesoItem item = (ProcesoItem) procesosCombo.getSelectedItem();
        if (item == null || item.id == null) {
            JOptionPane.showMessageDialog(this, "No hay un proceso seleccionado.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Map<String, Object> params = new HashMap<>();
        agregarParametro(params, "ciudad", ciudadField.getText());
        agregarParametro(params, "zona", zonaField.getText());
        agregarParametro(params, "pais", paisField.getText());
        agregarParametro(params, "fechaInicio", fechaInicioField.getText());
        agregarParametro(params, "fechaFin", fechaFinField.getText());
        agregarParametro(params, "periodicidad", periodicidadField.getText());
        agregarParametro(params, "observaciones", observacionesField.getText());
        if (item.tipo != null) {
            params.putIfAbsent("tipoProceso", item.tipo);
        }
        String tipoSensor = (String) tipoSensorCombo.getSelectedItem();
        if (tipoSensor != null && !"AUTO".equalsIgnoreCase(tipoSensor)) {
            params.put("tipoSensor", tipoSensor.toLowerCase());
        }

        try {
            String solicitudId = SolicitudProcesoService.getInstance()
                    .crearSolicitudUsuario(usuarioActual.getId(), item.id, params);
            JOptionPane.showMessageDialog(this,
                    "Solicitud enviada con éxito. ID: " + solicitudId,
                    "Éxito",
                    JOptionPane.INFORMATION_MESSAGE);
            limpiarFormularioSolicitud();
            refrescarSolicitudesUsuario();
        } catch (IllegalArgumentException | IllegalStateException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Aviso", JOptionPane.WARNING_MESSAGE);
        } catch (ErrorConexionMongoException | ErrorConexionCassandraException ex) {
            JOptionPane.showMessageDialog(this, "No se pudo crear la solicitud: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refrescarSolicitudesUsuario() {
        if (solicitudesModel == null) {
            return;
        }
        solicitudesModel.setRowCount(0);
        try {
            List<SolicitudProceso> solicitudes = SolicitudProcesoService.getInstance()
                    .listarPorUsuario(usuarioActual.getId());
            for (SolicitudProceso sp : solicitudes) {
                Map<String, Object> params = sp.getParametros();
                String sensor = extraerParametro(params, "sensorId");
                solicitudesModel.addRow(new Object[]{
                        sp.getId(),
                        obtenerNombreProceso(sp.getProcesoId()),
                        sp.getEstado() != null ? sp.getEstado().toUpperCase() : "-",
                        sp.getTecnicoAsignadoId() != null ? obtenerNombreUsuario(sp.getTecnicoAsignadoId()) : "-",
                        sensor != null ? sensor : "-",
                        sp.getFechaSolicitud() != null ? MENSAJE_FORMATO.format(sp.getFechaSolicitud()) : "-",
                        sp.getResultado() != null ? sp.getResultado() : ""
                });
            }
        } catch (ErrorConexionMongoException ex) {
            JOptionPane.showMessageDialog(this, "No se pudieron cargar las solicitudes: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refrescarCuenta() {
        try {
            CuentaCorriente cuenta = CuentaService.getInstance().obtenerCuentaUsuario(usuarioActual.getId());
            if (cuenta != null && saldoLabel != null) {
                double saldo = cuenta.getSaldo() != null ? cuenta.getSaldo() : 0.0;
                double limite = cuenta.getLimite() != null ? cuenta.getLimite() : 0.0;
                saldoLabel.setText(String.format("Saldo: $ %.2f  | Crédito disponible: $ %.2f",
                        saldo,
                        limite + saldo));
            } else if (saldoLabel != null) {
                saldoLabel.setText("Saldo: sin cuenta (ID no numérico)");
            }

            if (facturasModel != null) {
                facturasModel.setRowCount(0);
                for (Factura factura : CuentaService.getInstance().obtenerFacturas(usuarioActual.getId())) {
                    facturasModel.addRow(new Object[]{
                            factura.getId(),
                            factura.getDescripcion(),
                            factura.getMonto() != null ? String.format("$ %.2f", factura.getMonto()) : "-",
                            factura.getEstado(),
                            factura.getFechaEmision() != null ? MENSAJE_FORMATO.format(factura.getFechaEmision()) : "-"
                    });
                }
            }

            if (movimientosModel != null) {
                movimientosModel.setRowCount(0);
                for (Movimiento mov : CuentaService.getInstance().obtenerMovimientos(usuarioActual.getId())) {
                    movimientosModel.addRow(new Object[]{
                            mov.getFecha() != null ? MENSAJE_FORMATO.format(mov.getFecha()) : "-",
                            mov.getTipo(),
                            mov.getMonto() != null ? String.format("$ %.2f", mov.getMonto()) : "-",
                            mov.getDescripcion()
                    });
                }
            }
        } catch (ErrorConexionMySQLException ex) {
            JOptionPane.showMessageDialog(this, "No se pudo obtener la información de la cuenta: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void realizarDeposito() {
        try {
            double monto = Double.parseDouble(depositoField.getText().trim());
            String referencia = referenciaDepositoField.getText();
            CuentaService.getInstance().depositar(usuarioActual.getId(), monto, referencia);
            JOptionPane.showMessageDialog(this, "Depósito aplicado correctamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            depositoField.setText("");
            referenciaDepositoField.setText("");
            refrescarCuenta();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Ingrese un monto válido.", "Aviso", JOptionPane.WARNING_MESSAGE);
        } catch (IllegalArgumentException | IllegalStateException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Aviso", JOptionPane.WARNING_MESSAGE);
        } catch (ErrorConexionMySQLException e) {
            JOptionPane.showMessageDialog(this, "No se pudo registrar el depósito: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void pagarFacturaSeleccionada() {
        if (facturasTable == null || facturasModel == null) {
            return;
        }
        int row = facturasTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione una factura.", "Aviso", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int modelRow = facturasTable.convertRowIndexToModel(row);
        Object idObj = facturasModel.getValueAt(modelRow, 0);
        Object estadoObj = facturasModel.getValueAt(modelRow, 3);
        if (!(idObj instanceof Integer)) {
            JOptionPane.showMessageDialog(this, "Factura inválida.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (estadoObj != null && "pagada".equalsIgnoreCase(estadoObj.toString())) {
            JOptionPane.showMessageDialog(this, "La factura ya está pagada.", "Aviso", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        try {
            CuentaService.getInstance().pagarFactura(usuarioActual.getId(), (Integer) idObj, "SALDO_CUENTA");
            JOptionPane.showMessageDialog(this, "Factura pagada correctamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            refrescarCuenta();
        } catch (IllegalArgumentException | IllegalStateException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Aviso", JOptionPane.WARNING_MESSAGE);
        } catch (ErrorConexionMySQLException | SQLException e) {
            JOptionPane.showMessageDialog(this, "No se pudo realizar el pago: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void agregarParametro(Map<String, Object> params, String clave, String valor) {
        if (valor != null) {
            String trimmed = valor.trim();
            if (!trimmed.isEmpty()) {
                params.put(clave, trimmed);
            }
        }
    }

    private String extraerParametro(Map<String, Object> parametros, String clave) {
        if (parametros == null) {
            return null;
        }
        Object valor = parametros.get(clave);
        return valor != null ? valor.toString() : null;
    }

    private String obtenerNombreProceso(String procesoId) {
        if (procesoId == null || procesoId.isBlank()) {
            return "-";
        }
        if (procesoCache.containsKey(procesoId)) {
            Proceso proceso = procesoCache.get(procesoId);
            return proceso != null && proceso.getNombre() != null ? proceso.getNombre() : procesoId;
        }
        try {
            Proceso proceso = ProcesoService.getInstance().obtenerPorId(procesoId);
            if (proceso != null) {
                procesoCache.put(procesoId, proceso);
                return proceso.getNombre() != null ? proceso.getNombre() : procesoId;
            }
        } catch (ErrorConexionMongoException ignored) {
        }
        return procesoId;
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
