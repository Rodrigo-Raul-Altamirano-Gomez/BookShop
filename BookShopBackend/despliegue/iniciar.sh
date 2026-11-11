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
# Solo el puerto HTTP de la aplicación queda accesible desde Render.
sed -i '/<network-listener .*protocol="admin-listener"/s/ address="[^"]*"//g' "$DOMINIO/config/domain.xml"
sed -i '/<network-listener .*protocol="http-listener-2"/s/ address="[^"]*"//g' "$DOMINIO/config/domain.xml"
sed -i '/<network-listener /s/protocol="admin-listener"/address="127.0.0.1" protocol="admin-listener"/' "$DOMINIO/config/domain.xml"
sed -i '/<network-listener /s/protocol="http-listener-2"/address="127.0.0.1" protocol="http-listener-2"/' "$DOMINIO/config/domain.xml"
sed -i '/<iiop-listener /s/address="[^"]*"/address="127.0.0.1"/g' "$DOMINIO/config/domain.xml"
sed -i '/<jmx-connector /s/address="[^"]*"/address="127.0.0.1"/g' "$DOMINIO/config/domain.xml"
cp /app/bookshop.war "$DOMINIO/autodeploy/bookshop.war"
exec asadmin start-domain --verbose domain1
