#!/bin/sh
set -eu
umask 077

# Los secretos solo se incorporan al WAR dentro del servidor, durante el arranque.
mkdir -p /run/bookshop/WEB-INF/classes
printf '%s' "$BOOKSHOP_DB_PROPERTIES_B64" | base64 -d > /run/bookshop/WEB-INF/classes/db.properties
printf '%s' "$BOOKSHOP_KEY_B64" | base64 -d > /run/bookshop/clave-datos.key
jar --update --file /app/bookshop.war -C /run/bookshop WEB-INF/classes/db.properties
export JAVA_TOOL_OPTIONS="-Dbookshop.clave.file=/run/bookshop/clave-datos.key"

asadmin start-domain domain1
asadmin set "server-config.network-config.network-listeners.network-listener.http-listener-1.port=${PORT:-8080}"
asadmin deploy --name bookshop --contextroot /bookshop /app/bookshop.war
asadmin stop-domain domain1
exec asadmin start-domain --verbose domain1
