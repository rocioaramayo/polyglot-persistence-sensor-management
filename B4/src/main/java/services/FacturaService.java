package services;

import modelo.Factura;
import modelo.CuentaCorriente;
import modelo.Movimiento;
import repository.FacturaMySQLRepository;
import repository.CuentaMySQLRepository;
import repository.MovimientoMySQLRepository;
import exceptions.ErrorConexionMySQLException;
import connections.MySQLPool;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

public class FacturaService {
    private static FacturaService instance;

    private FacturaService() {}

    public static FacturaService getInstance() {
        if (instance == null) {
            instance = new FacturaService();
        }
        return instance;
    }

    public void crearFactura(String usuarioId, Double monto, String descripcion)
            throws ErrorConexionMySQLException {
        Factura factura = new Factura(usuarioId, monto, descripcion);
        if (factura.getSolicitudId() == null || factura.getSolicitudId().isBlank()) {
            factura.setSolicitudId(UUID.randomUUID().toString());
        }

        Connection conn = null;
        try {
            conn = MySQLPool.getInstance().getConnection();
            conn.setAutoCommit(false);

            // 1) Crear factura
            FacturaMySQLRepository.getInstance().crear(conn, factura);

            // 2) Obtener/crear cuenta y actualizar saldo
            CuentaCorriente cuenta = CuentaMySQLRepository.getInstance().obtenerPorUsuario(conn, usuarioId);
            if (cuenta == null) {
                cuenta = new CuentaCorriente(usuarioId);
                cuenta.setSaldo(0.0);
                CuentaMySQLRepository.getInstance().crear(conn, cuenta);
                // volver a leerla para obtener el id
                cuenta = CuentaMySQLRepository.getInstance().obtenerPorUsuario(conn, usuarioId);
            }

            double saldoAnterior = (cuenta.getSaldo() != null) ? cuenta.getSaldo() : 0.0;
            double nuevoSaldo = saldoAnterior - monto;
            CuentaMySQLRepository.getInstance().actualizarSaldo(conn, cuenta.getId(), nuevoSaldo);

            // 3) Registrar movimiento CARGO
            Movimiento movimiento = new Movimiento(
                    cuenta.getId(),
                    "CARGO",
                    monto,
                    saldoAnterior,
                    nuevoSaldo,
                    descripcion,
                    factura.getSolicitudId());
            MovimientoMySQLRepository.getInstance().crear(conn, movimiento);

            conn.commit();
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            throw new ErrorConexionMySQLException("Error al crear factura de forma transaccional", e);
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {}
            }
        }
    }

    public void registrarPago(Integer facturaId, Double monto) throws ErrorConexionMySQLException {
        // Aquí se implementaría la lógica de pago
        // Por ahora es un placeholder
    }
}
