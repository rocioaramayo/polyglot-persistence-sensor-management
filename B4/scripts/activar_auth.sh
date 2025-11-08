#!/bin/bash

# === CONFIG ===
CASS_DIR="$HOME/apps/apache-cassandra-4.0.12"
CQLSH="$CASS_DIR/bin/cqlsh"
YAML="$CASS_DIR/conf/cassandra.yaml"

echo "🔧 Activando autenticación en Cassandra..."

# 1. Backup del archivo original
cp "$YAML" "$YAML.bak"

# 2. Editar líneas necesarias
sed -i.bak \
-e "s/^authenticator:.*/authenticator: PasswordAuthenticator/" \
-e "s/^authorizer:.*/authorizer: CassandraAuthorizer/" \
-e "/^# role_manager/ a role_manager: CassandraRoleManager" \
"$YAML"

echo "✅ Archivo de configuración actualizado: $YAML"

# 3. Iniciar Cassandra
echo "🚀 Iniciando Cassandra..."
"$CASS_DIR/bin/cassandra"
sleep 15

# 4. Crear usuario de solo lectura
echo "👤 Creando usuario terminal_ro..."
"$CQLSH" -u cassandra -p cassandra -e "
ALTER ROLE cassandra WITH PASSWORD = 'TuPassFuerte_123' AND SUPERUSER = true;
CREATE ROLE IF NOT EXISTS terminal_ro
  WITH PASSWORD = 'terminal_ro_pass'
  AND LOGIN = true
  AND SUPERUSER = false;
GRANT SELECT ON ALL KEYSPACES TO terminal_ro;
"

echo "✅ Usuario 'terminal_ro' creado con éxito (solo lectura)"
echo "🔒 Cassandra lista con autenticación habilitada."