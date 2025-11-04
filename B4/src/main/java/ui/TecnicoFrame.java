package ui;

import exceptions.ErrorConexionCassandraException;
import exceptions.ErrorConexionMongoException;
import exceptions.ErrorConexionMySQLException;
import exceptions.ErrorConexionRedisException;
import modelo.Proceso;
import modelo.Sensor;
import modelo.SolicitudProceso;
import modelo.Usuario;
import repository.ProcesoMongoDAO;
import services.AuthService;
import services.SensorService;
import services.SolicitudProcesoService;
import services.UsuarioService;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TecnicoFrame extends JFrame {
    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter FECHA_FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final String token;
    private Usuario usuarioActual;

    private final Map<String, String> procesoCache = new HashMap<>();
    private final Map<String, String> usuarioCache = new HashMap<>();
    private final Map<String, SolicitudProceso> solicitudCache = new HashMap<>();

    private DefaultTableModel pendientesModel;
    private JTable pendientesTable;

    private DefaultTableModel asignadasModel;
    private JTable asignadasTable;

    private DefaultTableModel sensoresModel;
    private JTable sensoresTable;
    private JComboBox<String> filtroEstadoSensores;
    private JTextField filtroTextoSensores;
    private final List<Sensor> sensoresCache = new ArrayList<>();

    public TecnicoFrame(String token) {
        this.token = token;
        setTitle("Panel del Técnico");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);

        if (!cargarUsuarioActual()) {
            dispose();
            return;
        }

        JPanel container = new JPanel(new BorderLayout());
        container.add(crearHeader(), BorderLayout.NORTH);
        container.add(crearTabs(), BorderLayout.CENTER);
        add(container);

        refrescarPendientes();
        refrescarAsignadas();
        refrescarSensores();
    }

    private boolean cargarUsuarioActual() {
        try {
            usuarioActual = AuthService.getInstance().validarToken(token);
            if (usuarioActual == null) {
                JOptionPane.showMessageDialog(this, "No se pudo validar la sesión del técnico.", "Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
            usuarioCache.put(usuarioActual.getId(), formatoUsuario(usuarioActual));
            return true;
        } catch (ErrorConexionRedisException | ErrorConexionMySQLException e) {
            JOptionPane.showMessageDialog(this, "Error validando sesión: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    private JPanel crearHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        String nombre = usuarioActual.getNombreCompleto() != null && !usuarioActual.getNombreCompleto().isBlank()
                ? usuarioActual.getNombreCompleto()
                : usuarioActual.getEmail();
        JLabel label = new JLabel("Técnico: " + (nombre != null ? nombre : usuarioActual.getId()));
        header.add(label, BorderLayout.WEST);

        JButton logoutBtn = new JButton("Cerrar sesión");
        logoutBtn.addActionListener(e -> {
            try {
                AuthService.getInstance().logout(token);
            } catch (ErrorConexionRedisException ex) {
                JOptionPane.showMessageDialog(this, "No se pudo cerrar la sesión: " + ex.getMessage(), "Aviso", JOptionPane.WARNING_MESSAGE);
            }
            SwingUtilities.invokeLater(() -> {
                new LoginFrame().setVisible(true);
                dispose();
            });
        });
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        right.add(logoutBtn);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    private JTabbedPane crearTabs() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Solicitudes", crearTabSolicitudes());
        tabs.addTab("Mantenimiento de sensores", crearTabSensores());
        return tabs;
    }

    private JPanel crearTabSolicitudes() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel pendientesPanel = construirPanelPendientes();
        JPanel asignadasPanel = construirPanelAsignadas();

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, pendientesPanel, asignadasPanel);
        split.setResizeWeight(0.55);
        split.setBorder(null);

        panel.add(split, BorderLayout.CENTER);
        return panel;
    }

    private JPanel construirPanelPendientes() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createTitledBorder("Solicitudes pendientes de toma"));

        pendientesModel = new DefaultTableModel(new Object[]{"ID", "Proceso", "Usuario", "Fecha", "Estado", "Asignado a"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        pendientesTable = new JTable(pendientesModel);
        pendientesTable.setFillsViewportHeight(true);
        pendientesTable.setAutoCreateRowSorter(true);

        panel.add(new JScrollPane(pendientesTable), BorderLayout.CENTER);

        JButton refrescarBtn = new JButton("Refrescar");
        refrescarBtn.addActionListener(e -> {
            refrescarPendientes();
            refrescarAsignadas();
        });

        JButton tomarBtn = new JButton("Tomar solicitud");
        tomarBtn.addActionListener(e -> asignarSeleccionado());

        JButton aprobarBtn = new JButton("Aprobar");
        aprobarBtn.addActionListener(e -> cambiarEstadoSeleccionado("aprobado"));

        JButton rechazarBtn = new JButton("Rechazar");
        rechazarBtn.addActionListener(e -> rechazarSeleccionado());

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        acciones.add(refrescarBtn);
        acciones.add(tomarBtn);
        acciones.add(aprobarBtn);
        acciones.add(rechazarBtn);
        panel.add(acciones, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel construirPanelAsignadas() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createTitledBorder("Mis solicitudes en progreso"));

        asignadasModel = new DefaultTableModel(new Object[]{"ID", "Proceso", "Estado", "Fecha", "Usuario", "Resultado"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        asignadasTable = new JTable(asignadasModel);
        asignadasTable.setFillsViewportHeight(true);
        asignadasTable.setAutoCreateRowSorter(true);

        panel.add(new JScrollPane(asignadasTable), BorderLayout.CENTER);

        JButton refrescarBtn = new JButton("Refrescar");
        refrescarBtn.addActionListener(e -> refrescarAsignadas());

        JButton iniciarBtn = new JButton("Iniciar ejecución");
        iniciarBtn.addActionListener(e -> ejecutarAsignadaSeleccionada());

        JButton completarBtn = new JButton("Marcar completada");
        completarBtn.addActionListener(e -> completarAsignadaSeleccionada());

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        acciones.add(refrescarBtn);
        acciones.add(iniciarBtn);
        acciones.add(completarBtn);
        panel.add(acciones, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel crearTabSensores() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        sensoresModel = new DefaultTableModel(new Object[]{"ID", "Nombre", "Tipo", "Ciudad", "País", "Estado"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        sensoresTable = new JTable(sensoresModel);
        sensoresTable.setFillsViewportHeight(true);
        sensoresTable.setAutoCreateRowSorter(true);
        sensoresTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String estado = (String) table.getValueAt(row, 5);
                if ("INACTIVO".equalsIgnoreCase(estado) && !isSelected) {
                    c.setForeground(new Color(180, 0, 0));
                } else {
                    c.setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());
                }
                return c;
            }
        });

        panel.add(new JScrollPane(sensoresTable), BorderLayout.CENTER);

        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filtros.add(new JLabel("Estado:"));
        filtroEstadoSensores = new JComboBox<>(new String[]{"Todos", "Activos", "Inactivos"});
        filtroEstadoSensores.addActionListener(e -> aplicarFiltrosSensores());
        filtros.add(filtroEstadoSensores);

        filtros.add(new JLabel("Buscar (nombre/ciudad):"));
        filtroTextoSensores = new JTextField(18);
        filtroTextoSensores.addActionListener(e -> aplicarFiltrosSensores());
        filtros.add(filtroTextoSensores);

        JButton aplicarFiltroBtn = new JButton("Aplicar filtro");
        aplicarFiltroBtn.addActionListener(e -> aplicarFiltrosSensores());
        filtros.add(aplicarFiltroBtn);

        JButton limpiarFiltroBtn = new JButton("Limpiar");
        limpiarFiltroBtn.addActionListener(e -> {
            filtroEstadoSensores.setSelectedIndex(0);
            filtroTextoSensores.setText("");
            aplicarFiltrosSensores();
        });
        filtros.add(limpiarFiltroBtn);

        panel.add(filtros, BorderLayout.NORTH);

        JButton refrescarBtn = new JButton("Refrescar");
        refrescarBtn.addActionListener(e -> refrescarSensores());

        JButton repararBtn = new JButton("Marcar operativo");
        repararBtn.addActionListener(e -> actualizarEstadoSensor("ACTIVO"));

        JButton deshabilitarBtn = new JButton("Marcar inactivo");
        deshabilitarBtn.addActionListener(e -> actualizarEstadoSensor("INACTIVO"));

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        acciones.add(refrescarBtn);
        acciones.add(repararBtn);
        acciones.add(deshabilitarBtn);

        panel.add(acciones, BorderLayout.SOUTH);
        return panel;
    }

    private void refrescarPendientes() {
        try {
            List<SolicitudProceso> pendientes = SolicitudProcesoService.getInstance().listarPendientes();
            pendientesModel.setRowCount(0);
            for (SolicitudProceso sp : pendientes) {
                String asignadoId = sp.getTecnicoAsignadoId();
                if (asignadoId != null && !asignadoId.isBlank() && !asignadoId.equals(usuarioActual.getId())) {
                    continue;
                }
                solicitudCache.put(sp.getId(), sp);
                pendientesModel.addRow(new Object[]{
                        sp.getId(),
                        obtenerNombreProceso(sp.getProcesoId()),
                        obtenerNombreUsuario(sp.getUsuarioId()),
                        sp.getFechaSolicitud() != null ? FECHA_FORMATO.format(sp.getFechaSolicitud()) : "-",
                        sp.getEstado() != null ? sp.getEstado().toUpperCase() : "-",
                        sp.getTecnicoAsignadoId() != null ? obtenerNombreUsuario(sp.getTecnicoAsignadoId()) : "-"
                });
            }
        } catch (ErrorConexionMongoException e) {
            JOptionPane.showMessageDialog(this, "No se pudieron cargar las solicitudes pendientes: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refrescarAsignadas() {
        try {
            List<SolicitudProceso> tareas = SolicitudProcesoService.getInstance().listarAsignadas(usuarioActual.getId());
            asignadasModel.setRowCount(0);
            for (SolicitudProceso sp : tareas) {
                solicitudCache.put(sp.getId(), sp);
                String detalle = "";
                if (sp.getResultado() != null && !sp.getResultado().isBlank()) {
                    detalle = sp.getResultado();
                }
                if (sp.getObservaciones() != null && !sp.getObservaciones().isBlank()) {
                    if (!detalle.isBlank()) {
                        detalle += "\n";
                    }
                    detalle += "Notas: " + sp.getObservaciones();
                }
                asignadasModel.addRow(new Object[]{
                        sp.getId(),
                        obtenerNombreProceso(sp.getProcesoId()),
                        sp.getEstado() != null ? sp.getEstado().toUpperCase() : "-",
                        sp.getFechaSolicitud() != null ? FECHA_FORMATO.format(sp.getFechaSolicitud()) : "-",
                        obtenerNombreUsuario(sp.getUsuarioId()),
                        detalle
                });
            }
        } catch (ErrorConexionMongoException e) {
            JOptionPane.showMessageDialog(this, "No se pudieron cargar tus solicitudes asignadas: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refrescarSensores() {
        try {
            List<Sensor> sensores = SensorService.getInstance().listarTodos();
            sensoresCache.clear();
            sensoresCache.addAll(sensores);
            aplicarFiltrosSensores();
        } catch (ErrorConexionCassandraException e) {
            JOptionPane.showMessageDialog(this, "No se pudieron cargar los sensores: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void aplicarFiltrosSensores() {
        if (sensoresModel == null) {
            return;
        }
        String estadoFiltro = filtroEstadoSensores != null ? (String) filtroEstadoSensores.getSelectedItem() : "Todos";
        String texto = filtroTextoSensores != null ? filtroTextoSensores.getText().trim().toLowerCase() : "";

        sensoresModel.setRowCount(0);
        for (Sensor s : sensoresCache) {
            String estado = s.getEstado() != null ? s.getEstado().toUpperCase() : "-";
            boolean coincideEstado = true;
            if ("Activos".equalsIgnoreCase(estadoFiltro)) {
                coincideEstado = "ACTIVO".equalsIgnoreCase(estado);
            } else if ("Inactivos".equalsIgnoreCase(estadoFiltro)) {
                coincideEstado = "INACTIVO".equalsIgnoreCase(estado);
            }

            boolean coincideTexto = texto.isEmpty()
                    || (s.getNombre() != null && s.getNombre().toLowerCase().contains(texto))
                    || (s.getCiudad() != null && s.getCiudad().toLowerCase().contains(texto));

            if (coincideEstado && coincideTexto) {
                sensoresModel.addRow(new Object[]{
                        s.getId(),
                        s.getNombre(),
                        s.getTipo(),
                        s.getCiudad(),
                        s.getPais(),
                        estado
                });
            }
        }
    }

    private void asignarSeleccionado() {
        int row = pendientesTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione una solicitud pendiente.", "Aviso", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int modelRow = pendientesTable.convertRowIndexToModel(row);
        String id = (String) pendientesModel.getValueAt(modelRow, 0);
        SolicitudProceso cached = solicitudCache.get(id);
        if (cached != null) {
            String asignadoId = cached.getTecnicoAsignadoId();
            if (asignadoId != null && !asignadoId.isBlank()) {
                if (asignadoId.equals(usuarioActual.getId())) {
                    JOptionPane.showMessageDialog(this, "Esta solicitud ya está en tu cola.", "Aviso", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, "Otro técnico ya tomó esta solicitud.", "Aviso", JOptionPane.WARNING_MESSAGE);
                }
                return;
            }
        }
        try {
            SolicitudProcesoService.getInstance().asignarTecnico(id, usuarioActual.getId());
            JOptionPane.showMessageDialog(this, "Solicitud añadida a tu cola de trabajo.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            refrescarPendientes();
            refrescarAsignadas();
        } catch (ErrorConexionMongoException e) {
            JOptionPane.showMessageDialog(this, "No se pudo asignar la solicitud: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cambiarEstadoSeleccionado(String estado) {
        int row = pendientesTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione una solicitud.", "Aviso", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int modelRow = pendientesTable.convertRowIndexToModel(row);
        String id = (String) pendientesModel.getValueAt(modelRow, 0);
        SolicitudProceso sp = solicitudCache.get(id);
        if (sp == null) {
            JOptionPane.showMessageDialog(this, "No se pudo localizar la solicitud seleccionada.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String asignadoId = sp.getTecnicoAsignadoId();
        if (asignadoId == null || asignadoId.isBlank()) {
            JOptionPane.showMessageDialog(this, "Tomá la solicitud antes de cambiar su estado.", "Aviso", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (!asignadoId.equals(usuarioActual.getId())) {
            JOptionPane.showMessageDialog(this, "La solicitud está asignada a otro técnico.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            if ("aprobado".equalsIgnoreCase(estado)) {
                SolicitudProcesoService.getInstance().aprobarSolicitud(id);
            } else {
                SolicitudProcesoService.getInstance().rechazarSolicitud(id);
            }
            JOptionPane.showMessageDialog(this, "Estado actualizado.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            refrescarPendientes();
            refrescarAsignadas();
        } catch (ErrorConexionMongoException e) {
            JOptionPane.showMessageDialog(this, "No se pudo actualizar el estado: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void rechazarSeleccionado() {
        cambiarEstadoSeleccionado("rechazado");
    }

    private void ejecutarAsignadaSeleccionada() {
        int row = asignadasTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione una solicitud.", "Aviso", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int modelRow = asignadasTable.convertRowIndexToModel(row);
        String id = (String) asignadasModel.getValueAt(modelRow, 0);
        String estado = (String) asignadasModel.getValueAt(modelRow, 2);
        SolicitudProceso sp = solicitudCache.get(id);
        if (sp == null) {
            JOptionPane.showMessageDialog(this, "No se pudo localizar la solicitud seleccionada.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (sp.getTecnicoAsignadoId() == null || !sp.getTecnicoAsignadoId().equals(usuarioActual.getId())) {
            JOptionPane.showMessageDialog(this, "La solicitud ya no está asignada a tu usuario.", "Aviso", JOptionPane.WARNING_MESSAGE);
            refrescarAsignadas();
            return;
        }
        if (!"APROBADO".equalsIgnoreCase(estado)) {
            JOptionPane.showMessageDialog(this, "La solicitud debe estar en estado APROBADO para iniciar la ejecución.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            SolicitudProcesoService.getInstance().ejecutarSolicitud(id);
            JOptionPane.showMessageDialog(this, "Solicitud en ejecución.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            refrescarAsignadas();
            refrescarPendientes();
        } catch (ErrorConexionMongoException | ErrorConexionCassandraException e) {
            JOptionPane.showMessageDialog(this, "No se pudo iniciar la ejecución: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void completarAsignadaSeleccionada() {
        int row = asignadasTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione una solicitud.", "Aviso", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int modelRow = asignadasTable.convertRowIndexToModel(row);
        String id = (String) asignadasModel.getValueAt(modelRow, 0);
        String estado = (String) asignadasModel.getValueAt(modelRow, 2);
        SolicitudProceso sp = solicitudCache.get(id);
        if (sp == null) {
            JOptionPane.showMessageDialog(this, "No se pudo localizar la solicitud seleccionada.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (sp.getTecnicoAsignadoId() == null || !sp.getTecnicoAsignadoId().equals(usuarioActual.getId())) {
            JOptionPane.showMessageDialog(this, "La solicitud ya no está asignada a tu usuario.", "Aviso", JOptionPane.WARNING_MESSAGE);
            refrescarAsignadas();
            return;
        }
        if (!"EN_EJECUCION".equalsIgnoreCase(estado)) {
            JOptionPane.showMessageDialog(this, "La solicitud debe estar en ejecución para marcarla como completada.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            SolicitudProcesoService.getInstance().completarSolicitud(id);
            JOptionPane.showMessageDialog(this, "Solicitud completada. Se generó la facturación correspondiente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            refrescarAsignadas();
            refrescarPendientes();
        } catch (ErrorConexionMongoException | ErrorConexionMySQLException e) {
            JOptionPane.showMessageDialog(this, "No se pudo completar la solicitud: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void actualizarEstadoSensor(String nuevoEstado) {
        int row = sensoresTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un sensor.", "Aviso", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int modelRow = sensoresTable.convertRowIndexToModel(row);
        String id = (String) sensoresModel.getValueAt(modelRow, 0);
        try {
            SensorService.getInstance().actualizarEstado(id, nuevoEstado);
            JOptionPane.showMessageDialog(this, "Estado del sensor actualizado a " + nuevoEstado + ".", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            refrescarSensores();
        } catch (ErrorConexionCassandraException e) {
            JOptionPane.showMessageDialog(this, "No se pudo actualizar el sensor: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String obtenerNombreProceso(String procesoId) {
        if (procesoId == null || procesoId.isBlank()) {
            return "-";
        }
        return procesoCache.computeIfAbsent(procesoId, id -> {
            try {
                ProcesoMongoDAO dao = new ProcesoMongoDAO();
                Proceso proceso = dao.buscarPorId(id);
                if (proceso != null && proceso.getNombre() != null && !proceso.getNombre().isBlank()) {
                    return proceso.getNombre();
                }
            } catch (ErrorConexionMongoException ignored) {
            }
            return id;
        });
    }

    private String obtenerNombreUsuario(String usuarioId) {
        if (usuarioId == null || usuarioId.isBlank()) {
            return "-";
        }
        return usuarioCache.computeIfAbsent(usuarioId, id -> {
            if (usuarioActual != null && id.equals(usuarioActual.getId())) {
                return formatoUsuario(usuarioActual);
            }
            try {
                Usuario usuario = UsuarioService.getInstance().obtenerPorId(id);
                if (usuario != null) {
                    return formatoUsuario(usuario);
                }
            } catch (ErrorConexionMongoException ignored) {
            }
            return id;
        });
    }

    private String formatoUsuario(Usuario usuario) {
        if (usuario == null) {
            return "-";
        }
        String nombre = usuario.getNombreCompleto();
        if (nombre != null && !nombre.isBlank()) {
            return nombre;
        }
        if (usuario.getEmail() != null && !usuario.getEmail().isBlank()) {
            return usuario.getEmail();
        }
        return usuario.getId();
    }
}
