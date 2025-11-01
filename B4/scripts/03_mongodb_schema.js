// MongoDB Schema para Persistencia Poliglota
// Refactorizado para contener: Usuarios, Roles, Procesos, Solicitudes de Proceso,
// Mensajes, Grupos, Alertas y Control de Sensores
// Justificación: Flexibilidad de esquema para estructuras variables, ideal para
// mensajería, gestión de alertas y entidades con parámetros dinámicos

// Script para mongosh: asumir que ya estas en la DB polyglot_mongodb (variable global db disponible)
// Helper para crear colecciones sin fallar si existen
function createColl(name, options) {
  try {
    if (options) db.createCollection(name, options); else db.createCollection(name);
  } catch (e) { /* probablemente ya existe */ }
}

    createColl("usuarios", {
      validator: {
        $jsonSchema: {
          bsonType: "object",
          required: ["nombre", "email", "password", "rol", "activo"],
          properties: {
            _id: { bsonType: "objectId" },
            nombre: { bsonType: "string", description: "Nombre completo del usuario" },
            apellido: { bsonType: "string", description: "Apellido del usuario" },
            email: { bsonType: "string", pattern: "^.+@.+$", description: "Email único del usuario" },
            password: { bsonType: "string", description: "Contraseña encriptada" },
            rol: {
              bsonType: "string",
              enum: ["ADMINISTRADOR", "TECNICO", "USUARIO"],
              description: "Rol del usuario en el sistema",
            },
            fecha_registro: { bsonType: "date", description: "Fecha de registro (automática)" },
            activo: { bsonType: "bool", description: "Estado del usuario (true por defecto)" },
            telefono: { bsonType: "string" },
            direccion: { bsonType: "string" },
          },
        },
      },
    })

    createColl("roles", {
      validator: {
        $jsonSchema: {
          bsonType: "object",
          required: ["nombre", "descripcion"],
          properties: {
            _id: { bsonType: "objectId" },
            nombre: {
              bsonType: "string",
              enum: ["ADMINISTRADOR", "TECNICO", "USUARIO"],
              description: "Nombre del rol",
            },
            descripcion: { bsonType: "string" },
            fecha_creacion: { bsonType: "date" },
          },
        },
      },
    })

    createColl("procesos", {
      validator: {
        $jsonSchema: {
          bsonType: "object",
          required: ["nombre", "tipo", "descripcion", "costo", "activo"],
          properties: {
            _id: { bsonType: "objectId" },
            nombre: { bsonType: "string" },
            tipo: {
              bsonType: "string",
              description: "Tipo de proceso (string flexible sin enum)",
            },
            descripcion: { bsonType: "string" },
            costo: { bsonType: "double", minimum: 0 },
            parametros_requeridos: {
              bsonType: "array",
              items: { bsonType: "string" },
              description: "Lista de parámetros que requiere el proceso",
            },
            activo: { bsonType: "bool" },
            fecha_creacion: { bsonType: "date" },
          },
        },
      },
    })

    // Colección de Solicitudes de Proceso
    createColl("solicitudes_proceso", {
      validator: {
        $jsonSchema: {
          bsonType: "object",
          required: ["usuario_id", "proceso_id", "fecha_solicitud", "estado"],
          properties: {
            _id: { bsonType: "objectId" },
            usuario_id: { bsonType: "int", description: "ID del usuario que solicita" },
            proceso_id: { bsonType: "objectId", description: "ID del proceso solicitado" },
            tecnico_asignado_id: { bsonType: "int", description: "ID del técnico asignado" },
            parametros: {
              bsonType: "object",
              description: "Parámetros variables según el tipo de proceso (flexibilidad de esquema)",
            },
            fecha_solicitud: { bsonType: "date" },
            fecha_aprobacion: { bsonType: "date" },
            fecha_completado: { bsonType: "date" },
            estado: {
              bsonType: "string",
              enum: ["PENDIENTE", "APROBADO", "EN_EJECUCION", "COMPLETADO", "RECHAZADO"],
              description: "Estado de la solicitud",
            },
            resultado: { bsonType: "string", description: "Resultado o informe generado" },
            observaciones: { bsonType: "string" },
          },
        },
      },
    })

    // Colección de Mensajes
        // Colecci�n de Mensajes (alineado con la app: ids string y campos camelCase)
    createColl("mensajes", {
      validator: {
        $jsonSchema: {
          bsonType: "object",
          required: ["remitenteId", "destinatarioId", "contenido", "tipo", "fechaHora"],
          properties: {
            _id: { bsonType: "objectId" },
            remitenteId: { bsonType: "string" },
            destinatarioId: { bsonType: "string" },
            contenido: { bsonType: "string" },
            fechaHora: { bsonType: "date" },
            tipo: { bsonType: "string", enum: ["PRIVADO", "GRUPAL", "privado", "grupal"] },
            leido: { bsonType: "bool" },
            adjuntos: { bsonType: "array", items: { bsonType: "string" } }
          },
        },
      },
    })

        const gruposValidator = {
      validator: {
        $jsonSchema: {
          bsonType: "object",
          required: ["nombre", "miembros", "fecha_creacion"],
          properties: {
            _id: { bsonType: "objectId" },
            nombre: { bsonType: "string" },
            descripcion: { bsonType: "string" },
            miembros: { bsonType: "array", items: { bsonType: "string" } },
            administradores: { bsonType: "array", items: { bsonType: "string" } },
            fecha_creacion: { bsonType: "date" },
            activo: { bsonType: "bool" },
          },
        },
      },
    };
    createColl("grupos", gruposValidator)

    // Colección de Alertas
    createColl("alertas", {
      validator: {
        $jsonSchema: {
          bsonType: "object",
          required: ["tipo", "fecha", "descripcion", "estado"],
          properties: {
            _id: { bsonType: "objectId" },
            tipo: {
              bsonType: "string",
              enum: ["SENSOR", "CLIMATICA", "SISTEMA"],
              description: "Tipo de alerta",
            },
            sensor_id: { bsonType: "string", description: "ID del sensor (si aplica)" },
            usuario_id: { bsonType: "int", description: "ID del usuario destinatario (si aplica)" },
            fecha: { bsonType: "date" },
            descripcion: { bsonType: "string" },
            severidad: {
              bsonType: "string",
              enum: ["BAJA", "MEDIA", "ALTA", "CRITICA"],
            },
            estado: {
              bsonType: "string",
              enum: ["ACTIVA", "RESUELTA", "IGNORADA"],
              description: "Estado de la alerta",
            },
            fecha_resolucion: { bsonType: "date" },
            resuelto_por: { bsonType: "int", description: "ID del usuario que resolvió la alerta" },
          },
        },
      },
    })

    // Colección de Control de Sensores
    createColl("control_sensores", {
      validator: {
        $jsonSchema: {
          bsonType: "object",
          required: ["sensor_id", "fecha_revision", "estado_sensor"],
          properties: {
            _id: { bsonType: "objectId" },
            sensor_id: { bsonType: "string", description: "ID del sensor revisado" },
            tecnico_id: { bsonType: "int", description: "ID del técnico que realizó la revisión" },
            fecha_revision: { bsonType: "date" },
            estado_sensor: {
              bsonType: "string",
              enum: ["ACTIVO", "INACTIVO", "FALLA", "MANTENIMIENTO"],
              description: "Estado del sensor",
            },
            observaciones: { bsonType: "string" },
            acciones_realizadas: {
              bsonType: "array",
              items: { bsonType: "string" },
              description: "Lista de acciones de mantenimiento realizadas",
            },
            proxima_revision: { bsonType: "date" },
          },
        },
      },
    })

    // Crear índices para optimización de consultas
    db.usuarios.createIndex({ email: 1 }, { unique: true })
    db.usuarios.createIndex({ activo: 1 })
    db.usuarios.createIndex({ rol: 1 })

    db.roles.createIndex({ nombre: 1 }, { unique: true })

    db.procesos.createIndex({ tipo: 1 })
    db.procesos.createIndex({ activo: 1 })

    db.solicitudes_proceso.createIndex({ usuario_id: 1, fecha_solicitud: -1 })
    db.solicitudes_proceso.createIndex({ tecnico_asignado_id: 1, estado: 1 })
    db.solicitudes_proceso.createIndex({ estado: 1 })

    db.mensajes.createIndex({ remitente_id: 1, fecha: -1 })
    db.mensajes.createIndex({ destinatario_id: 1, leido: 1 })
    db.mensajes.createIndex({ grupo_id: 1, fecha: -1 })

    db.grupos.createIndex({ miembros: 1 })

    db.alertas.createIndex({ sensor_id: 1, fecha: -1 })
    db.alertas.createIndex({ usuario_id: 1, estado: 1 })
    db.alertas.createIndex({ tipo: 1, estado: 1 })
    db.alertas.createIndex({ fecha: -1 })

    db.control_sensores.createIndex({ sensor_id: 1, fecha_revision: -1 })
    db.control_sensores.createIndex({ tecnico_id: 1 })
    db.control_sensores.createIndex({ estado_sensor: 1 })

    db.roles.insertMany([
      {
        nombre: "ADMINISTRADOR",
        descripcion: "Administrador del sistema con acceso completo. Puede crear técnicos.",
        fecha_creacion: new Date(),
      },
      {
        nombre: "TECNICO",
        descripcion: "Técnico encargado de ejecutar procesos y mantenimiento",
        fecha_creacion: new Date(),
      },
      {
        nombre: "USUARIO",
        descripcion: "Usuario cliente del sistema (rol por defecto al iniciar sesión)",
        fecha_creacion: new Date(),
      },
    ])

    db.usuarios.insertOne({
      nombre: "Administrador del Sistema",
      apellido: "Del Sistema",
      email: "admin@polyglot.com",
      // Hash SHA-256 de "admin123" para compatibilidad con AuthService.hashPassword
      password: "240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9",
      rol: "ADMINISTRADOR",
      fecha_registro: new Date(),
      activo: true,
      telefono: "+54 11 1234-5678",
      direccion: "Oficina Central",
    })

    print("MongoDB schema creado: colecciones, roles y admin por defecto")
