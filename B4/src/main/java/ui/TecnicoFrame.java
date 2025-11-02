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
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
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

    private DefaultTableModel pendientesModel;
    private JTable pendientesTable;

    private DefaultTableModel tareasModel;
    private JTable tareasTable;

    private DefaultTableModel sensoresModel;
    private JTable sensoresTable;

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
        refrescarTareas();
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
        tabs.addTab("Solicitudes pendientes", crearTabPendientes());
        tabs.addTab("Mis tareas", crearTabTareasAsignadas());
        tabs.addTab("Mantenimiento de sensores", crearTabSensores());
        return tabs;
    }

    private JPanel crearTabPendientes() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

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
        refrescarBtn.addActionListener(e -> refrescarPendientes());

        JButton asignarBtn = new JButton("Asignarme");
        asignarBtn.addActionListener(e -> asignarSeleccionado());

        JButton aprobarBtn = new JButton("Aprobar");
        aprobarBtn.addActionListener(e -> cambiarEstadoSeleccionado("aprobado"));

        JButton rechazarBtn = new JButton("Rechazar");
        rechazarBtn.addActionListener(e -> rechazarSeleccionado());

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        acciones.add(refrescarBtn);
        acciones.add(asignarBtn);
        acciones.add(aprobarBtn);
        acciones.add(rechazarBtn);
        panel.add(acciones, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel crearTabTareasAsignadas() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        tareasModel = new DefaultTableModel(new Object[]{"ID", "Proceso", "Estado", "Fecha", "Usuario", "Resultado"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tareasTable = new JTable(tareasModel);
        tareasTable.setFillsViewportHeight(true);
        tareasTable.setAutoCreateRowSorter(true);

        panel.add(new JScrollPane(tareasTable), BorderLayout.CENTER);

        JButton refrescarBtn = new JButton("Refrescar");
        refrescarBtn.addActionListener(e -> refrescarTareas());

        JButton iniciarBtn = new JButton("Iniciar ejecución");
        iniciarBtn.addActionListener(e -> ejecutarTareaSeleccionada());

        JButton completarBtn = new JButton("Marcar completada");
        completarBtn.addActionListener(e -> completarTareaSeleccionada());

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

        panel.add(new JScrollPane(sensoresTable), BorderLayout.CENTER);

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

    private void refrescarTareas() {
        try {
            List<SolicitudProceso> tareas = SolicitudProcesoService.getInstance().listarAsignadas(usuarioActual.getId());
            tareasModel.setRowCount(0);
            for (SolicitudProceso sp : tareas) {
                tareasModel.addRow(new Object[]{
                        sp.getId(),
                        obtenerNombreProceso(sp.getProcesoId()),
                        sp.getEstado() != null ? sp.getEstado().toUpperCase() : "-",
                        sp.getFechaSolicitud() != null ? FECHA_FORMATO.format(sp.getFechaSolicitud()) : "-",
                        obtenerNombreUsuario(sp.getUsuarioId()),
                        sp.getResultado() != null ? sp.getResultado() : ""
                });
            }
        } catch (ErrorConexionMongoException e) {
            JOptionPane.showMessageDialog(this, "No se pudieron cargar tus tareas: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refrescarSensores() {
        try {
            List<Sensor> sensores = SensorService.getInstance().listarTodos();
            sensoresModel.setRowCount(0);
            for (Sensor s : sensores) {
                sensoresModel.addRow(new Object[]{
                        s.getId(),
                        s.getNombre(),
                        s.getTipo(),
                        s.getCiudad(),
                        s.getPais(),
                        s.getEstado() != null ? s.getEstado().toUpperCase() : "-"
                });
            }
        } catch (ErrorConexionCassandraException e) {
            JOptionPane.showMessageDialog(this, "No se pudieron cargar los sensores: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
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
        try {
            SolicitudProcesoService.getInstance().asignarTecnico(id, usuarioActual.getId());
            JOptionPane.showMessageDialog(this, "Solicitud asignada a tu cola.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            refrescarPendientes();
            refrescarTareas();
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
        String asignado = (String) pendientesModel.getValueAt(modelRow, 5);
        if (!"-".equals(asignado) && !asignado.equals(obtenerNombreUsuario(usuarioActual.getId()))) {
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
            refrescarTareas();
        } catch (ErrorConexionMongoException e) {
            JOptionPane.showMessageDialog(this, "No se pudo actualizar el estado: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void rechazarSeleccionado() {
        cambiarEstadoSeleccionado("rechazado");
    }

    private void ejecutarTareaSeleccionada() {
        int row = tareasTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione una tarea.", "Aviso", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int modelRow = tareasTable.convertRowIndexToModel(row);
        String id = (String) tareasModel.getValueAt(modelRow, 0);
        String estado = (String) tareasModel.getValueAt(modelRow, 2);
        if (!"APROBADO".equalsIgnoreCase(estado)) {
            JOptionPane.showMessageDialog(this, "La tarea debe estar en estado APROBADO para iniciar la ejecución.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            SolicitudProcesoService.getInstance().ejecutarSolicitud(id);
            JOptionPane.showMessageDialog(this, "Solicitud en ejecución.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            refrescarTareas();
            refrescarPendientes();
        } catch (ErrorConexionMongoException | ErrorConexionCassandraException e) {
            JOptionPane.showMessageDialog(this, "No se pudo iniciar la ejecución: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void completarTareaSeleccionada() {
        int row = tareasTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione una tarea.", "Aviso", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int modelRow = tareasTable.convertRowIndexToModel(row);
        String id = (String) tareasModel.getValueAt(modelRow, 0);
        String estado = (String) tareasModel.getValueAt(modelRow, 2);
        if (!"EN_EJECUCION".equalsIgnoreCase(estado)) {
            JOptionPane.showMessageDialog(this, "La tarea debe estar en ejecución para marcarla como completada.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            SolicitudProcesoService.getInstance().completarSolicitud(id);
            JOptionPane.showMessageDialog(this, "Solicitud completada. Se generó la facturación correspondiente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            refrescarTareas();
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
