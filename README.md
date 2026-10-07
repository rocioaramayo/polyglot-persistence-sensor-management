# Polyglot Persistence - Sensor Management

Aplicación de escritorio desarrollada en Java para administrar sensores, mediciones, alertas, usuarios y procesos periódicos. El proyecto aplica **persistencia políglota**: cada tipo de información se almacena en la base de datos que mejor se adapta a sus características.

## Objetivo

Diseñar una solución capaz de combinar datos transaccionales, documentos, series temporales y sesiones sin forzar todos los casos de uso dentro de un único motor de persistencia.

## Arquitectura de datos

| Tecnología | Responsabilidad principal |
| --- | --- |
| **MySQL** | Usuarios, cuentas corrientes, facturas, pagos y movimientos transaccionales |
| **MongoDB** | Procesos, solicitudes, grupos, mensajes, reglas de alerta y configuraciones flexibles |
| **Apache Cassandra** | Sensores, mediciones e historial de ejecución orientado a series temporales |
| **Redis** | Sesiones activas, autenticación y datos temporales de acceso rápido |

## Funcionalidades

- Autenticación y control de acceso basado en roles.
- Gestión de sensores y generación automática de mediciones.
- Reglas y alertas automáticas.
- Procesamiento de solicitudes y tareas programadas.
- Gestión de usuarios, grupos, mensajes, facturación y pagos.
- Paneles diferenciados para administración y operación técnica.
- Terminal Cassandra de solo lectura integrada en la interfaz.

## Stack tecnológico

- Java 11
- Java Swing
- Maven
- MySQL
- MongoDB
- Apache Cassandra
- Redis / Jedis
- SLF4J

## Estructura del proyecto

```text
B4/
├── scripts/                     # Esquemas e inicialización de las bases
├── src/main/java/
│   ├── application/             # Punto de entrada
│   ├── connections/             # Conexiones a cada motor
│   ├── modelo/                  # Entidades de dominio
│   ├── repository/              # Acceso a datos
│   ├── services/                # Reglas de negocio y tareas programadas
│   ├── simulator/               # Simulación de sensores
│   └── ui/                      # Interfaz gráfica y paneles
├── src/main/resources/
│   └── application.properties   # Configuración local
└── pom.xml
```

## Ejecución local

### Requisitos

- JDK 11 o superior
- Maven 3.8+
- MySQL 8
- MongoDB
- Apache Cassandra 4
- Redis

### 1. Crear las estructuras de datos

Ejecutar, en orden y sobre cada motor correspondiente, los archivos de `B4/scripts/`:

1. `01_mysql_schema.sql`
2. `02_cassandra_schema.cql`
3. `03_mongodb_schema_clean.js`
4. `04_redis_init.sh`

### 2. Configurar las conexiones

Completar los valores locales en:

```text
B4/src/main/resources/application.properties
```

Los valores incluidos en el repositorio son ejemplos para desarrollo y deben reemplazarse antes de ejecutar la aplicación.

### 3. Compilar y ejecutar

```bash
cd B4
mvn clean package
java -jar target/polyglot-persistence-1.0.0.jar
```

## Decisiones de diseño

La separación por motores permite usar consistencia transaccional para facturación, flexibilidad documental para procesos y configuraciones, escritura eficiente para mediciones históricas y baja latencia para sesiones. La capa de repositorios desacopla estas decisiones del dominio y de la interfaz.

## Autora

**Rocío Aramayo**  
[LinkedIn](https://www.linkedin.com/in/rocio-aramayo-28bb85286) · [GitHub](https://github.com/rocioaramayo)
