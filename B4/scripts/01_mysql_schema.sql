-- MySQL Schema para Persistencia Poliglota
-- Refactorizado para contener SOLO Facturas, Pagos y Cuenta Corriente
-- Justificación: Transacciones ACID para máxima garantía de integridad y consistencia
-- en el control de pagos y acreditación en cuenta corriente

CREATE DATABASE IF NOT EXISTS polyglot_db;
USE polyglot_db;

-- Tabla de Cuentas Corrientes
-- Gestiona el saldo y límite de crédito de cada usuario
CREATE TABLE IF NOT EXISTS cuentas_corrientes (
    id INT PRIMARY KEY AUTO_INCREMENT,
    usuario_id INT UNIQUE NOT NULL,
    saldo DECIMAL(10, 2) DEFAULT 0.00,
    limite_credito DECIMAL(10, 2) DEFAULT 5000.00,
    fecha_apertura DATETIME DEFAULT CURRENT_TIMESTAMP,
    estado VARCHAR(20) DEFAULT 'ACTIVA',
    INDEX idx_usuario (usuario_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Tabla de Facturas
-- Registra todas las facturas generadas por procesos completados
CREATE TABLE IF NOT EXISTS facturas (
    id INT PRIMARY KEY AUTO_INCREMENT,
    numero_factura VARCHAR(50) UNIQUE NOT NULL,
    usuario_id INT NOT NULL,
    solicitud_proceso_id VARCHAR(36),
    monto DECIMAL(10, 2) NOT NULL,
    fecha_emision DATETIME DEFAULT CURRENT_TIMESTAMP,
    fecha_vencimiento DATETIME,
    estado VARCHAR(20) DEFAULT 'PENDIENTE',
    descripcion TEXT,
    INDEX idx_usuario (usuario_id),
    INDEX idx_estado (estado),
    INDEX idx_fecha_emision (fecha_emision)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Tabla de Pagos
-- Registra todos los pagos realizados contra facturas
CREATE TABLE IF NOT EXISTS pagos (
    id INT PRIMARY KEY AUTO_INCREMENT,
    factura_id INT NOT NULL,
    usuario_id INT NOT NULL,
    monto DECIMAL(10, 2) NOT NULL,
    fecha_pago DATETIME DEFAULT CURRENT_TIMESTAMP,
    metodo_pago VARCHAR(50),
    referencia VARCHAR(100),
    estado VARCHAR(20) DEFAULT 'COMPLETADO',
    FOREIGN KEY (factura_id) REFERENCES facturas(id) ON DELETE RESTRICT,
    INDEX idx_factura (factura_id),
    INDEX idx_usuario (usuario_id),
    INDEX idx_fecha_pago (fecha_pago)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Tabla de Movimientos (Historial de Cuenta Corriente)
-- Registra todos los movimientos de la cuenta corriente (débitos y créditos)
CREATE TABLE IF NOT EXISTS movimientos (
    id INT PRIMARY KEY AUTO_INCREMENT,
    cuenta_id INT NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    monto DECIMAL(10, 2) NOT NULL,
    saldo_anterior DECIMAL(10, 2) NOT NULL,
    saldo_nuevo DECIMAL(10, 2) NOT NULL,
    fecha DATETIME DEFAULT CURRENT_TIMESTAMP,
    descripcion TEXT,
    referencia_id VARCHAR(100),
    FOREIGN KEY (cuenta_id) REFERENCES cuentas_corrientes(id) ON DELETE RESTRICT,
    INDEX idx_cuenta (cuenta_id),
    INDEX idx_fecha (fecha),
    INDEX idx_tipo (tipo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Eliminados todos los triggers según requerimientos
-- La lógica de actualización de saldos y estados se manejará en la capa de aplicación
-- Fin del script
