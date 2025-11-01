-- Cassandra Schema para Persistencia Poliglota
-- Refactorizado para contener: Mediciones (millones), Sensores (datos maestros) 
-- y Historial de Ejecución de procesos
-- Justificación: Escritura masiva y escalabilidad horizontal. Diseñada para series 
-- de tiempo y alto volumen de ingesta de mediciones. Almacena eficientemente el 
-- histórico de ejecución de procesos.

CREATE KEYSPACE IF NOT EXISTS polyglot_ks 
WITH replication = {
    'class': 'SimpleStrategy', 
    'replication_factor': 3
};

USE polyglot_ks;

-- Tabla de Sensores (Datos Maestros)
-- Almacena información básica de cada sensor
CREATE TABLE IF NOT EXISTS sensores (
    id TEXT PRIMARY KEY,
    nombre TEXT,
    codigo TEXT,
    tipo_sensor TEXT,
    latitud DOUBLE,
    longitud DOUBLE,
    ciudad TEXT,
    zona TEXT,
    pais TEXT,
    estado TEXT,
    fecha_inicio_emision TIMESTAMP,
    fecha_instalacion TIMESTAMP,
    ultima_actualizacion TIMESTAMP,
    observaciones TEXT
);

-- Eliminados índices secundarios - no son efectivos con masividad de datos

-- Tabla de Mediciones (Time Series - Millones de registros)
-- Optimizada para escrituras masivas y consultas por rango de tiempo
-- Partition key: sensor_id para distribuir datos
-- Clustering key: fecha DESC para ordenar por tiempo (más recientes primero)
CREATE TABLE IF NOT EXISTS mediciones (
    sensor_id TEXT,
    fecha TIMESTAMP,
    id TEXT,
    temperatura DOUBLE,
    humedad DOUBLE,
    presion DOUBLE,
    velocidad_viento DOUBLE,
    direccion_viento TEXT,
    precipitacion DOUBLE,
    calidad_lectura TEXT,
    PRIMARY KEY (sensor_id, fecha)
) WITH CLUSTERING ORDER BY (fecha DESC)
  AND compaction = {'class': 'TimeWindowCompactionStrategy', 'compaction_window_size': 1, 'compaction_window_unit': 'DAYS'}
  AND default_time_to_live = 31536000;

-- Tabla de Mediciones por Ciudad (Vista materializada para consultas agregadas)
-- Permite consultas eficientes por ciudad y rango de fechas
CREATE TABLE IF NOT EXISTS mediciones_por_ciudad (
    ciudad TEXT,
    fecha TIMESTAMP,
    sensor_id TEXT,
    temperatura DOUBLE,
    humedad DOUBLE,
    PRIMARY KEY (ciudad, fecha, sensor_id)
) WITH CLUSTERING ORDER BY (fecha DESC, sensor_id ASC);

-- Tabla de Mediciones por País (Vista materializada para consultas agregadas)
-- Permite consultas eficientes por país y rango de fechas
CREATE TABLE IF NOT EXISTS mediciones_por_pais (
    pais TEXT,
    fecha TIMESTAMP,
    sensor_id TEXT,
    ciudad TEXT,
    temperatura DOUBLE,
    humedad DOUBLE,
    PRIMARY KEY (pais, fecha, sensor_id)
) WITH CLUSTERING ORDER BY (fecha DESC, sensor_id ASC);

-- Tabla de Historial de Ejecución de Procesos
-- Almacena el registro completo de todas las ejecuciones de procesos
-- Partition key: solicitud_id para agrupar todas las ejecuciones de una solicitud
-- Clustering key: fecha_ejecucion DESC para ordenar cronológicamente
CREATE TABLE IF NOT EXISTS historial_ejecucion (
    solicitud_id TEXT,
    fecha_ejecucion TIMESTAMP,
    id TEXT,
    proceso_id TEXT,
    usuario_id INT,
    tecnico_id INT,
    estado TEXT,
    resultado TEXT,
    parametros MAP<TEXT, TEXT>,
    sensores_utilizados SET<TEXT>,
    registros_procesados BIGINT,
    tiempo_ejecucion_ms BIGINT,
    fecha_inicio TIMESTAMP,
    fecha_fin TIMESTAMP,
    errores TEXT,
    observaciones TEXT,
    PRIMARY KEY (solicitud_id, fecha_ejecucion)
) WITH CLUSTERING ORDER BY (fecha_ejecucion DESC);

-- Eliminados índices secundarios del historial de ejecución

-- Tabla de Historial por Usuario (Vista para consultas de usuario)
-- Permite que cada usuario consulte rápidamente su historial
CREATE TABLE IF NOT EXISTS historial_por_usuario (
    usuario_id INT,
    fecha_ejecucion TIMESTAMP,
    solicitud_id TEXT,
    proceso_id TEXT,
    estado TEXT,
    resultado TEXT,
    PRIMARY KEY (usuario_id, fecha_ejecucion, solicitud_id)
) WITH CLUSTERING ORDER BY (fecha_ejecucion DESC, solicitud_id ASC);

-- Tabla de Historial por Técnico (Vista para consultas de técnico)
-- Permite que cada técnico consulte rápidamente sus ejecuciones
CREATE TABLE IF NOT EXISTS historial_por_tecnico (
    tecnico_id INT,
    fecha_ejecucion TIMESTAMP,
    solicitud_id TEXT,
    proceso_id TEXT,
    estado TEXT,
    tiempo_ejecucion_ms BIGINT,
    PRIMARY KEY (tecnico_id, fecha_ejecucion, solicitud_id)
) WITH CLUSTERING ORDER BY (fecha_ejecucion DESC, solicitud_id ASC);

-- Eliminada tabla estadisticas_sensores - no es necesaria para el sistema

-- Comentarios sobre el diseño:
-- 1. Las mediciones usan sensor_id como partition key para distribuir la carga
-- 2. El clustering por fecha DESC permite consultas eficientes de datos recientes
-- 3. Las vistas materializadas (mediciones_por_ciudad, mediciones_por_pais) 
--    facilitan consultas agregadas sin impactar el rendimiento
-- 4. El historial de ejecución usa solicitud_id como partition key para agrupar
--    todas las ejecuciones relacionadas con una solicitud
-- 5. TTL de 1 año en mediciones para gestión automática de datos antiguos
-- 6. TimeWindowCompactionStrategy optimiza el almacenamiento de series de tiempo
-- 7. Sin índices secundarios para mantener rendimiento óptimo con masividad de datos
