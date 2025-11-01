#!/bin/bash
# Redis Initialization Script para Persistencia Poliglota
# Refactorizado para: Facturación por Tiempo de Servicio, Cola de Procesos y Cola de Mantenimiento
# Justificación: Velocidad In-Memory con TTL. Control de tiempo de uso del servicio en línea,
# gestión de cola única de procesos por orden de llegada y cola de mantenimiento de sensores

echo "=========================================="
echo "Inicializando Redis para Persistencia Poliglota"
echo "=========================================="

# Verificar si Redis está corriendo
if ! redis-cli ping > /dev/null 2>&1; then
    echo "Error: Redis no está corriendo. Por favor inicie Redis primero."
    exit 1
fi

redis-cli << EOF

# ==========================================
# LIMPIAR DATOS ANTERIORES (Opcional - Comentar en producción)
# ==========================================
# FLUSHDB

# ==========================================
# CONFIGURACIÓN DE SESIONES ACTIVAS
# ==========================================

SET config:session:default_ttl 3600


# Crear conjunto de sesiones activas (para monitoreo)
SADD active_sessions:index "INIT"
DEL active_sessions:index

# ==========================================
# FACTURACIÓN POR TIEMPO DE SERVICIO
# ==========================================

# Cuando un usuario accede al servicio, se registra service:active:<ID_USUARIO>
# con el timestamp de inicio en milisegundos

# Configurar tarifa por segundo de uso (en unidades monetarias)
SET config:billing:rate_per_second 0.01

# Configurar tiempo mínimo facturable (en segundos)
SET config:billing:minimum_seconds 60

# ==========================================
# COLA DE PROCESOS (ÚNICA, POR ORDEN DE LLEGADA)
# ==========================================

LPUSH queue:processes "INIT"
RPOP queue:processes

# Configurar límite de reintentos para procesos fallidos
SET config:queue:max_retries 3

# Configurar timeout para procesos (30 minutos = 1800 segundos)
SET config:queue:process_timeout 1800

# ==========================================
# COLA DE MANTENIMIENTO DE SENSORES
# ==========================================

# Cuando una Alerta de tipo SENSOR es creada, el sensor_id se agrega aquí
LPUSH queue:mantenimiento "INIT"
RPOP queue:mantenimiento

# ==========================================
# COLA DE FACTURACIÓN
# ==========================================

# Después de calcular el tiempo de uso, se empuja una tarea aquí
LPUSH queue:billing "INIT"
RPOP queue:billing

# ==========================================
# CONTADOR DE TIEMPO DE SERVICIO
# ==========================================

# Formato: service:time:total:<ID_USUARIO> -> segundos totales acumulados
SET service:time:total:example 0

# ==========================================
# DATOS DE EJEMPLO PARA DESARROLLO
# ==========================================

# Ejemplo de sesión activa
HSET session:user:1:abc123 user_id 1 role "USUARIO" login_time "2025-01-15T10:00:00Z" last_activity "2025-01-15T10:30:00Z"
EXPIRE session:user:1:abc123 3600
SADD active_sessions:index "session:user:1:abc123"

# Ejemplo de sesión de técnico
HSET session:user:2:def456 user_id 2 role "TECNICO" login_time "2025-01-15T09:00:00Z" last_activity "2025-01-15T10:30:00Z"
EXPIRE session:user:2:def456 3600
SADD active_sessions:index "session:user:2:def456"

# Ejemplo de sesión de administrador
HSET session:user:3:ghi789 user_id 3 role "ADMINISTRADOR" login_time "2025-01-15T08:00:00Z" last_activity "2025-01-15T10:30:00Z"
EXPIRE session:user:3:ghi789 3600
SADD active_sessions:index "session:user:3:ghi789"

# Ejemplo de usuario usando el servicio en línea
HSET service:active:1 user_id 1 fecha_inicio 1736936400000 servicio "CONSULTA_SENSORES"

# Ejemplo de proceso en cola única
LPUSH queue:processes '{"solicitud_id":"sol-001","proceso_id":"proc-temp-avg","usuario_id":1,"fecha_solicitud":"2025-01-15T10:00:00Z"}'

# Ejemplo de sensor en cola de mantenimiento
LPUSH queue:mantenimiento '{"sensor_id":"sensor-001","alerta_id":"alerta-123","fecha_creacion":"2025-01-15T10:00:00Z","descripcion":"Sensor con falla de lectura"}'

# Ejemplo de tarea de facturación pendiente
LPUSH queue:billing '{"usuario_id":1,"tiempo_uso_segundos":3600,"monto":36.00,"fecha":"2025-01-15T11:00:00Z"}'

# Ejemplo de tiempo acumulado de servicio
SET service:time:total:1 7200

# ==========================================
# ÍNDICES PARA BÚSQUEDAS RÁPIDAS
# ==========================================

# Índice de sesiones por usuario
SADD index:sessions:user:1 "session:user:1:abc123"
SADD index:sessions:user:2 "session:user:2:def456"
SADD index:sessions:user:3 "session:user:3:ghi789"

# Índice de procesos por usuario
SADD index:processes:user:1 "sol-001"

# Índice de usuarios con servicio activo
SADD index:service:active "1"

# ==========================================
# CONFIGURACIÓN DE ALERTAS Y NOTIFICACIONES
# ==========================================

# Canal de publicación/suscripción para alertas en tiempo real
PUBLISH channel:alerts:system "Sistema Redis inicializado correctamente"

# ==========================================
# VERIFICACIÓN
# ==========================================

PING

EOF

# Verificar el resultado
if [ $? -eq 0 ]; then
    echo ""
    echo "=========================================="
    echo "Redis inicializado correctamente"
    echo "=========================================="
    echo ""
    echo "Estructuras creadas:"
    echo "  - Sesiones activas con TTL configurable"
    echo "  - Control de tiempo de servicio en línea (service:active:<ID_USUARIO>)"
    echo "  - Cola única de procesos por orden de llegada (queue:processes)"
    echo "  - Cola de mantenimiento de sensores (queue:mantenimiento)"
    echo "  - Cola de facturación (queue:billing)"
    echo "  - Contador de tiempo total de servicio por usuario"
    echo "  - Índices para búsquedas rápidas"
    echo ""
    echo "Configuración:"
    redis-cli << VERIFY
    ECHO "  TTL Sesión Default: \$(GET config:session:default_ttl) segundos"
    ECHO "  Tarifa por Segundo: \$(GET config:billing:rate_per_second) unidades"
    ECHO "  Max Reintentos Cola: \$(GET config:queue:max_retries)"
VERIFY
    echo ""
else
    echo "Error al inicializar Redis"
    exit 1
fi
