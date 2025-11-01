package services;

import modelo.Factura;
import modelo.CuentaCorriente;
import modelo.Movimiento;
import repository.FacturaMySQLRepository;
import repository.CuentaMySQLRepository;
import repository.MovimientoMySQLRepository;
import exceptions.ErrorConexionMySQLException;

public class FacturaService {
    private static FacturaService instance;

    private FacturaService() {}

    public static FacturaService getInstance() {
        if (instance == null) {
            instance = new FacturaService();
        }
        return instance;
    }

    public void crearFactura(Integer usuarioId, Double monto, String descripcion) 
            throws ErrorConexionMySQLException {
        Factura factura = new Factura(usuarioId, monto, descripcion);
        FacturaMySQLRepository.getInstance().crear(factura);

        // Registrar cargo en cuenta corriente
        CuentaCorriente cuenta = CuentaMySQLRepository.getInstance().obtenerPorUsuario(usuarioId);
        if (cuenta != null) {
            Double nuevoSaldo = cuenta.getSaldo() - monto;
            CuentaMySQLRepository.getInstance().actualizarSaldo(cuenta.getId(), nuevoSaldo);

            Movimiento movimiento = new Movimiento(cuenta.getId(), "CARGO", monto, descripcion);
            MovimientoMySQLRepository.getInstance().crear(movimiento);
        }
    }

    public void registrarPago(Integer facturaId, Double monto) throws ErrorConexionMySQLException {
        // Aquí se implementaría la lógica de pago
        // Por ahora es un placeholder
    }
}
