#!/bin/sh
set -eu
umask 077

# Los secretos solo se incorporan al WAR dentro del servidor, durante el arranque.
mkdir -p /run/bookshop/WEB-INF/classes
printf '%s' "$BOOKSHOP_DB_PROPERTIES_B64" | base64 -d > /run/bookshop/WEB-INF/classes/db.properties
printf '%s' "$BOOKSHOP_KEY_B64" | base64 -d > /run/bookshop/clave-datos.key
jar --update --file /app/bookshop.war -C /run/bookshop WEB-INF/classes/db.properties
export JAVA_TOOL_OPTIONS="-Dbookshop.clave.file=/run/bookshop/clave-datos.key"

# GlassFish despliega automáticamente el WAR al iniciar.
DOMINIO=/opt/glassfish8/glassfish/domains/domain1
sed -i "s/port=\"8080\"/port=\"${PORT:-8080}\"/g" "$DOMINIO/config/domain.xml"
cp /app/bookshop.war "$DOMINIO/autodeploy/bookshop.war"
exec asadmin start-domain --verbose domain1
