// MongoDB schema (mongosh) for polyglot_mongodb
// Usage: in mongosh run: use polyglot_mongodb; then load this file or paste it.

// Helper: create collection only if missing
function ensureCollection(name, options) {
  if (db.getCollectionInfos({ name }).length === 0) {
    if (options) db.createCollection(name, options); else db.createCollection(name);
  }
}

// ========== USUARIOS ==========
ensureCollection("usuarios", {
  validator: {
    $jsonSchema: {
      bsonType: "object",
      required: ["nombre", "email", "password", "rol", "activo"],
      properties: {
        _id: { bsonType: "objectId" },
        nombre: { bsonType: "string" },
        apellido: { bsonType: "string" },
        email: { bsonType: "string", pattern: "^.+@.+$" },
        password: { bsonType: "string" },
        rol: { bsonType: "string", enum: ["ADMINISTRADOR", "TECNICO", "USUARIO"] },
        fecha_registro: { bsonType: "date" },
        activo: { bsonType: "bool" },
        telefono: { bsonType: "string" },
        direccion: { bsonType: "string" }
      }
    }
  }
});

db.usuarios.createIndex({ email: 1 }, { unique: true });
db.usuarios.createIndex({ activo: 1 });
db.usuarios.createIndex({ rol: 1 });

// Admin seed (SHA-256 of "admin123")
db.usuarios.updateOne(
  { email: "admin@polyglot.com" },
  {
    $setOnInsert: {
      nombre: "Administrador",
      apellido: "Del Sistema",
      email: "admin@polyglot.com",
      password: "240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9",
      rol: "ADMINISTRADOR",
      fecha_registro: new Date(),
      activo: true,
      telefono: "+54 11 1234-5678",
      direccion: "Oficina Central"
    }
  },
  { upsert: true }
);

// ========== ROLES ==========
ensureCollection("roles", {
  validator: {
    $jsonSchema: {
      bsonType: "object",
      required: ["nombre", "descripcion"],
      properties: {
        _id: { bsonType: "objectId" },
        nombre: { bsonType: "string", enum: ["ADMINISTRADOR", "TECNICO", "USUARIO"] },
        descripcion: { bsonType: "string" },
        fecha_creacion: { bsonType: "date" }
      }
    }
  }
});
db.roles.createIndex({ nombre: 1 }, { unique: true });
[{ nombre: "ADMINISTRADOR", descripcion: "Acceso completo. Carga tecnicos.", fecha_creacion: new Date() },
 { nombre: "TECNICO", descripcion: "Ejecucion y mantenimiento.", fecha_creacion: new Date() },
 { nombre: "USUARIO", descripcion: "Cliente final.", fecha_creacion: new Date() }]
.forEach(r => db.roles.updateOne({ nombre: r.nombre }, { $setOnInsert: r }, { upsert: true }));

// ========== PROCESOS ==========
ensureCollection("procesos", {
  validator: {
    $jsonSchema: {
      bsonType: "object",
      required: ["nombre", "tipo", "descripcion", "costo", "activo"],
      properties: {
        _id: { bsonType: "objectId" },
        nombre: { bsonType: "string" },
        tipo: { bsonType: "string" },
        descripcion: { bsonType: "string" },
        costo: { bsonType: "double" },
        parametros_requeridos: { bsonType: "array", items: { bsonType: "string" } },
        activo: { bsonType: "bool" },
        fecha_creacion: { bsonType: "date" }
      }
    }
  }
});
db.procesos.createIndex({ tipo: 1 });
db.procesos.createIndex({ activo: 1 });

// ========== SOLICITUDES DE PROCESO ==========
ensureCollection("solicitudes_proceso", {
  validator: {
    $jsonSchema: {
      bsonType: "object",
      required: ["usuario_id", "proceso_id", "fecha_solicitud", "estado"],
      properties: {
        _id: { bsonType: "objectId" },
        usuario_id: { bsonType: "int" },
        proceso_id: { bsonType: "objectId" },
        tecnico_asignado_id: { bsonType: "int" },
        parametros: { bsonType: "object" },
        fecha_solicitud: { bsonType: "date" },
        fecha_aprobacion: { bsonType: "date" },
        fecha_completado: { bsonType: "date" },
        estado: { bsonType: "string", enum: ["PENDIENTE", "APROBADO", "EN_EJECUCION", "COMPLETADO", "RECHAZADO"] },
        resultado: { bsonType: "string" },
        observaciones: { bsonType: "string" }
      }
    }
  }
});
db.solicitudes_proceso.createIndex({ usuario_id: 1, fecha_solicitud: -1 });
db.solicitudes_proceso.createIndex({ tecnico_asignado_id: 1, estado: 1 });
db.solicitudes_proceso.createIndex({ estado: 1 });

// ========== MENSAJES (camelCase) ==========
ensureCollection("mensajes", {
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
        tipo: { bsonType: "string", enum: ["PRIVADO", "GRUPAL"] },
        leido: { bsonType: "bool" },
        adjuntos: { bsonType: "array", items: { bsonType: "string" } }
      }
    }
  }
});
db.mensajes.createIndex({ remitenteId: 1, fechaHora: -1 });
db.mensajes.createIndex({ destinatarioId: 1, leido: 1 });

// ========== GRUPOS ==========
ensureCollection("grupos", {
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
        activo: { bsonType: "bool" }
      }
    }
  }
});
db.grupos.createIndex({ miembros: 1 });

// ========== ALERTAS ==========
ensureCollection("alertas", {
  validator: {
    $jsonSchema: {
      bsonType: "object",
      required: ["tipo", "fecha", "descripcion", "estado"],
      properties: {
        _id: { bsonType: "objectId" },
        tipo: { bsonType: "string", enum: ["SENSOR", "CLIMATICA", "SISTEMA"] },
        sensor_id: { bsonType: "string" },
        usuario_id: { bsonType: "string" },
        fecha: { bsonType: "date" },
        descripcion: { bsonType: "string" },
        severidad: { bsonType: "string", enum: ["BAJA", "MEDIA", "ALTA", "CRITICA"] },
        estado: { bsonType: "string", enum: ["ACTIVA", "RESUELTA", "IGNORADA"] },
        fecha_resolucion: { bsonType: "date" },
        resuelto_por: { bsonType: "string" }
      }
    }
  }
});
db.alertas.createIndex({ usuario_id: 1, estado: 1 });
db.alertas.createIndex({ fecha: -1 });

// ========== CONTROL DE SENSORES ==========
ensureCollection("control_sensores", {
  validator: {
    $jsonSchema: {
      bsonType: "object",
      required: ["sensor_id", "fecha_revision", "estado_sensor"],
      properties: {
        _id: { bsonType: "objectId" },
        sensor_id: { bsonType: "string" },
        tecnico_id: { bsonType: "int" },
        fecha_revision: { bsonType: "date" },
        estado_sensor: { bsonType: "string", enum: ["ACTIVO", "INACTIVO", "FALLA", "MANTENIMIENTO"] },
        observaciones: { bsonType: "string" },
        acciones_realizadas: { bsonType: "array", items: { bsonType: "string" } },
        proxima_revision: { bsonType: "date" }
      }
    }
  }
});

print("Mongo schema ready: colecciones creadas y administracion/roles/usuarios iniciales cargados.");

