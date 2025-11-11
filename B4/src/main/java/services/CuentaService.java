package services;

import exceptions.ErrorConexionMySQLException;
import modelo.CuentaCorriente;
import modelo.Factura;
import modelo.Movimiento;
import modelo.Pago;
import repository.CuentaMySQLRepository;
import repository.FacturaMySQLRepository;
import repository.MovimientoMySQLRepository;
import repository.PagoMySQLRepository;
import connections.MySQLPool;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

public class CuentaService {
    private static CuentaService instance;

    private CuentaService() {}

    public static CuentaService getInstance() {
        if (instance == null) {
            instance = new CuentaService();
        }
        return instance;
    }

    public CuentaCorriente obtenerOCrearCuenta(String usuarioId) throws ErrorConexionMySQLException {
        if (usuarioId == null || usuarioId.isBlank()) {
            return null;
        }
        CuentaCorriente cuenta = CuentaMySQLRepository.getInstance().obtenerPorUsuario(usuarioId);
        if (cuenta == null) {
            cuenta = new CuentaCorriente(usuarioId);
            CuentaMySQLRepository.getInstance().crear(cuenta);
            cuenta = CuentaMySQLRepository.getInstance().obtenerPorUsuario(usuarioId);
        }
        if (cuenta != null && cuenta.getLimite() == null) {
            cuenta.setLimite(1000.0);
        }
        return cuenta;
    }

    public CuentaCorriente obtenerCuentaUsuario(String usuarioId) throws ErrorConexionMySQLException {
        if (usuarioId == null || usuarioId.isBlank()) {
            return null;
        }
        CuentaCorriente cuenta = CuentaMySQLRepository.getInstance().obtenerPorUsuario(usuarioId);
        if (cuenta != null && cuenta.getLimite() == null) {
            cuenta.setLimite(1000.0);
        }
        return cuenta;
    }

    public void depositar(String usuarioId, double monto, String referencia) throws ErrorConexionMySQLException {
        if (monto <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor a cero");
        }
        CuentaCorriente cuenta = obtenerOCrearCuenta(usuarioId);
        if (cuenta == null) {
            throw new IllegalStateException("El usuario no tiene una cuenta corriente en MySQL");
        }
        double saldoActual = cuenta.getSaldo() != null ? cuenta.getSaldo() : 0.0;
        double nuevoSaldo = saldoActual + monto;
        CuentaMySQLRepository.getInstance().actualizarSaldo(cuenta.getId(), nuevoSaldo);
        cuenta.setSaldo(nuevoSaldo);

        String descripcion = referencia != null && !referencia.isBlank()
                ? referencia : "Depósito en cuenta corriente";
        Movimiento movimiento = new Movimiento(cuenta.getId(), "ABONO", monto, saldoActual, nuevoSaldo,
                descripcion, referencia);
        MovimientoMySQLRepository.getInstance().crear(movimiento);
    }

    public void pagarFactura(String usuarioId, int facturaId, String metodoPago)
            throws ErrorConexionMySQLException, SQLException {
        Connection conn = null;
        try {
            conn = MySQLPool.getInstance().getConnection();
            conn.setAutoCommit(false);

            // Obtener cuenta en la misma conexión
            CuentaCorriente cuenta = CuentaMySQLRepository.getInstance().obtenerPorUsuario(conn, usuarioId);
            if (cuenta == null) {
                // Crear cuenta en cero si no existe
                cuenta = new CuentaCorriente(usuarioId);
                cuenta.setSaldo(0.0);
                CuentaMySQLRepository.getInstance().crear(conn, cuenta);
                cuenta = CuentaMySQLRepository.getInstance().obtenerPorUsuario(conn, usuarioId);
            }

            // Obtener factura
            Factura factura = FacturaMySQLRepository.getInstance().obtenerPorId(conn, facturaId);
            if (factura == null) {
                throw new IllegalArgumentException("Factura no encontrada");
            }
            if ("pagada".equalsIgnoreCase(factura.getEstado())) {
                throw new IllegalStateException("La factura ya está pagada");
            }
            if (factura.getMonto() == null) {
                throw new IllegalStateException("Factura inválida");
            }

            double saldoDisponible = cuenta.getSaldo() != null ? cuenta.getSaldo() : 0.0;
            double nuevoSaldo = saldoDisponible - factura.getMonto();
            if (nuevoSaldo < -cuenta.getLimite()) {
                throw new IllegalStateException("El pago excede el límite de crédito disponible");
            }

            // 1) Actualizar saldo
            CuentaMySQLRepository.getInstance().actualizarSaldo(conn, cuenta.getId(), nuevoSaldo);

            // 2) Registrar movimiento PAGO
            Movimiento movimiento = new Movimiento(cuenta.getId(), "PAGO", factura.getMonto(),
                    saldoDisponible, nuevoSaldo, "Pago factura #" + facturaId, String.valueOf(facturaId));
            MovimientoMySQLRepository.getInstance().crear(conn, movimiento);

            // 3) Registrar pago
            Pago pago = new Pago(facturaId, usuarioId, factura.getMonto(),
                    metodoPago != null ? metodoPago : "SALDO_CUENTA");
            PagoMySQLRepository.getInstance().insertar(conn, pago);

            // 4) Actualizar estado de la factura
            FacturaMySQLRepository.getInstance().actualizarEstado(conn, facturaId, "pagada");

            conn.commit();
        } catch (Exception e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            if (e instanceof ErrorConexionMySQLException) throw (ErrorConexionMySQLException) e;
            if (e instanceof SQLException) throw (SQLException) e;
            throw new ErrorConexionMySQLException("Error al pagar factura de forma transaccional", e);
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {}
            }
        }
    }

    public List<Movimiento> obtenerMovimientos(String usuarioId) throws ErrorConexionMySQLException {
        if (usuarioId == null || usuarioId.isBlank()) {
            return Collections.emptyList();
        }
        CuentaCorriente cuenta = CuentaMySQLRepository.getInstance().obtenerPorUsuario(usuarioId);
        if (cuenta == null) {
            return Collections.emptyList();
        }
        return MovimientoMySQLRepository.getInstance().obtenerPorCuenta(cuenta.getId());
    }

    public List<Factura> obtenerFacturas(String usuarioId) throws ErrorConexionMySQLException {
        if (usuarioId == null || usuarioId.isBlank()) {
            return Collections.emptyList();
        }
        return FacturaMySQLRepository.getInstance().obtenerPorUsuario(usuarioId);
    }
}
