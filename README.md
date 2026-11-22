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

1. Copiar `backend/bookshop-rs/src/main/resources/db.properties.ejemplo` como `db.properties` en la misma carpeta y completar los datos de la base. Ese archivo no se sube a git. También se pueden usar las variables de entorno `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER` y `DB_PASSWORD`, que tienen prioridad.
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

### Render (desarrollo)

Dos servicios web con Docker, conectados al repositorio y desplegados automáticamente al subir cambios a `main`:

| Servicio | Directorio raíz | Variables de entorno |
|---|---|---|
| Backend | `backend` | `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` |
| Front | `frontend` | `ApiUrl` (URL pública del backend terminada en `/bookshop/services/`) |

El `Dockerfile` del backend compila el WAR, lo despliega en GlassFish 8 al construir la imagen, fija la zona horaria de Lima y acota la memoria de la JVM al plan gratuito (512 MB).

### Otro servidor con Docker (por ejemplo, AWS EC2)

Las mismas imágenes sirven en cualquier máquina con Docker:

```
docker build -t bookshop-api backend
docker run -d -p 8080:8080 -e DB_HOST=<host> -e DB_PORT=3306 -e DB_NAME=bookshop -e DB_USER=<usuario> -e DB_PASSWORD=<contraseña> bookshop-api

docker build -t bookshop-web frontend
docker run -d -p 80:8080 -e ApiUrl=http://<servidor>:8080/bookshop/services/ bookshop-web
```

## Plantillas de los PDF

El comprobante y los dos reportes son plantillas de JasperReports 7 en `backend/bookshop-business-logic/src/main/resources/reportes`. Junto a cada `.jrxml` va su `.jasper` compilado para Java 11, que es el que usa la aplicación. Si se cambia una plantilla, hay que volver a compilarla (por ejemplo, con Jaspersoft Studio 7) y reemplazar el `.jasper`.
