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

        Movimiento movimiento = new Movimiento(cuenta.getId(), "ABONO", monto,
                referencia != null && !referencia.isBlank() ? referencia : "Depósito en cuenta corriente");
        MovimientoMySQLRepository.getInstance().crear(movimiento);
    }

    public void pagarFactura(String usuarioId, int facturaId, String metodoPago)
            throws ErrorConexionMySQLException, SQLException {
        CuentaCorriente cuenta = obtenerOCrearCuenta(usuarioId);
        if (cuenta == null) {
            throw new IllegalStateException("El usuario no tiene una cuenta corriente en MySQL");
        }
        Factura factura = FacturaMySQLRepository.getInstance().obtenerPorId(facturaId);
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

        CuentaMySQLRepository.getInstance().actualizarSaldo(cuenta.getId(), nuevoSaldo);
        Movimiento movimiento = new Movimiento(cuenta.getId(), "PAGO", factura.getMonto(),
                "Pago factura #" + facturaId);
        MovimientoMySQLRepository.getInstance().crear(movimiento);

        Pago pago = new Pago(facturaId, usuarioId, factura.getMonto(), metodoPago != null ? metodoPago : "SALDO_CUENTA");
        PagoMySQLRepository.getInstance().insertar(pago);

        FacturaMySQLRepository.getInstance().actualizarEstado(facturaId, "pagada");
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
