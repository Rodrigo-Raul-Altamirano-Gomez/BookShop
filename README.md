# BookShop

Librería en línea académica: tienda pública, cuentas de cliente, compra con pago simulado, pedidos con seguimiento automático, devoluciones, cupones, listas, seguimientos, reseñas, recomendaciones y reportes PDF para el administrador.

| Documento | Contenido |
|---|---|
| [docs/ERS.md](docs/ERS.md) | Requisitos funcionales y reglas de negocio |
| [docs/UX-UI.md](docs/UX-UI.md) | Pantallas, componentes y textos |
| [docs/ui/](docs/ui/) | Prototipo navegable en HTML |
| [docs/PLAN-DE-IMPLEMENTACION.md](docs/PLAN-DE-IMPLEMENTACION.md) | Arquitectura, convenciones, incrementos y registro de decisiones |
| [docs/GUION-DEMO.md](docs/GUION-DEMO.md) | Recorrido de la demostración (unos 15 minutos) |

## Estructura

```
BookShop/
├── database/   01_esquema.sql (crea la base) y 02_datos.sql (datos de prueba)
├── backend/    Maven multimódulo: model, db-manager, dao, business-logic y rs (WAR con el API REST)
└── frontend/   Blazor Web App (.NET 10) que consume el API
```

## Requisitos

- MySQL 8 (en desarrollo, AWS RDS).
- JDK 25 y Maven 3.9 o superior.
- GlassFish 8.
- SDK de .NET 10.

## Instalación local

### 1. Base de datos

Ejecutar los dos scripts en orden, con un usuario que pueda crear bases de datos:

```
mysql -h <host> -u <usuario> -p < database/01_esquema.sql
mysql -h <host> -u <usuario> -p < database/02_datos.sql
```

`01_esquema.sql` borra y vuelve a crear la base `bookshop`. Las fechas del historial de `02_datos.sql` son relativas al momento de la carga, así que los estados de los pedidos, los plazos de devolución y los descuentos siempre se ven coherentes.

### 2. Backend

1. Crear `backend/bookshop-rs/src/main/resources/db.properties` con los datos de la base (el archivo no se sube a git):

```
host=<host>
port=3306
database=bookshop
user=<usuario>
password=<contraseña>
```

   También se pueden usar las variables de entorno `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER` y `DB_PASSWORD`, que tienen prioridad. Ojo: `mvn package` incluye `db.properties` dentro del WAR, así que ese WAR no debe compartirse.
2. Compilar y desplegar:

```
cd backend
mvn clean package
asadmin start-domain
asadmin deploy --force=true --name bookshop --contextroot bookshop bookshop-rs/target/bookshop.war
```

El API queda en `http://localhost:8080/bookshop/services/`. Al arrancar, la aplicación inicia el hilo que avanza el seguimiento de los pedidos y emite los cupones cada minuto.

Si se recrea la base de datos con el backend en marcha, hay que volver a desplegar el WAR para renovar las conexiones del *pool*.

### 3. Front

La URL del API se toma de `ApiUrl` en `frontend/BookShopWeb/appsettings.json` (por defecto, el backend local) o de la variable de entorno `ApiUrl`, terminada en `/services/`.

```
dotnet run --project frontend/BookShopWeb --launch-profile http
```

La tienda queda en `http://localhost:5245`.

## Usuarios de prueba

Todos tienen la contraseña `123456`.

| Correo | Rol | Para qué sirve |
|---|---|---|
| `admin@admin.bookshop` | Administrador | Catálogo, configuración, devoluciones, reportes de reseñas y reportes PDF |
| `ana@correo.com` | Cliente | El historial más completo: pedidos entregados, cancelados y devueltos, cupones, devoluciones, reseñas, reportes, listas y seguimientos |
| `luis@correo.com` | Cliente | Una devolución pendiente de revisión y reseñas propias |
| `rosa@correo.com` | Cliente | Cuenta desactivada, para mostrar la reactivación al iniciar sesión |
| `carla@correo.com`, `jorge@correo.com`, `lucia@correo.com`, `martin@correo.com`, `sofia@correo.com` | Cliente | Historial de compras y reseñas que alimenta las recomendaciones y los reportes |
| `andres@correo.com` | Cliente | Cliente sin compras: aparece en el reporte de clientes con monto 0 |

## Despliegue

### AWS EC2 (sin contenedores)

Backend y front en la misma máquina, contra una base MySQL en AWS RDS (ejecutar antes `01_esquema.sql` y `02_datos.sql`, como en la instalación local).

- **Backend:** GlassFish 8 y el WAR, como en la instalación local: `cd backend && mvn clean package`, y desplegar `bookshop-rs/target/bookshop.war` con `asadmin deploy --force=true --name bookshop --contextroot bookshop`. La zona horaria del servidor debe ser `America/Lima` (las fechas de los pedidos y del seguimiento las genera Java). Los datos de la base van en `db.properties` (queda dentro del WAR) o en las variables `DB_*`.
- **Front:** `dotnet publish frontend/BookShopWeb -c Release -o <carpeta>` y ejecutar `dotnet BookShopWeb.dll` en esa carpeta (por ejemplo, como servicio). Como el front llama al API desde el servidor, con ambos en la misma máquina basta el valor por defecto de `ApiUrl` (`http://localhost:8080/bookshop/services/`) y no depende de la IP pública. Si el backend estuviera en otra máquina, se define la variable de entorno `ApiUrl` (terminada en `/services/`) o se cambia en `appsettings.json`.
- **Red:** en el grupo de seguridad solo hace falta abrir el puerto del front; el 8080 del backend no tiene que ser público.
- **Imágenes subidas:** las fotos, portadas e imágenes de autores, categorías y editoriales que se suben desde la aplicación viven en `<carpeta>/wwwroot/img/subidas`. Al publicar una versión nueva, copiarla **encima** de la anterior, sin borrar esa carpeta; si se borra, las rutas que guarda la base de datos quedan sin imagen.

## Plantillas de los PDF

El comprobante y los dos reportes son plantillas de JasperReports 7 en `backend/bookshop-business-logic/src/main/resources/reportes`. Junto a cada `.jrxml` va su `.jasper` compilado para Java 11, que es el que usa la aplicación. Si se cambia una plantilla, hay que volver a compilarla (por ejemplo, con Jaspersoft Studio 7) y reemplazar el `.jasper`.
