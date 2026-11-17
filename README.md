# BookShop

Proyecto académico de una tienda de libros: backend Java 25 con servicios REST Jakarta EE y frontend Blazor en .NET 10 con Bootstrap.

El frontend consume la API desde el proyecto Web. Las imágenes se guardan como URLs; los archivos actuales del catálogo están en `BookShopFrontend/Web/wwwroot/images/catalogo`.

El entregable incluye los recorridos de RF01, RF02, RF03 y RF05 a RF19. De RF04 se incluyen Administración, Reportes y la moderación de solicitudes; siguen pendientes las pantallas de mantenimiento del catálogo, configuración y Todas las reseñas. Esto no equivale a certificar cada condición de los diecinueve requisitos: por ejemplo, las imágenes todavía se manejan como URLs.

## Estructura

- `BookShopBackend/Model`: entidades y DTO.
- `BookShopBackend/DBManager`: conexiones y transacciones.
- `BookShopBackend/DAO`: persistencia JDBC y esquema MySQL.
- `BookShopBackend/Business`: reglas de negocio.
- `BookShopBackend/Service`: API REST Jakarta EE.
- `BookShopBackend/App`: ejecución por consola.
- `BookShopFrontend/Web`: interfaz, DTO y consumo REST.

## Ejecutar el frontend con el backend desplegado

El backend está en Render y la dirección ya está configurada en `BookShopFrontend/Web/appsettings.json`:

https://bookshop-backend-0qyo.onrender.com/bookshop/webresources/

Ejecutar el proyecto `BookShopFrontend/Web` con el perfil `http`, o usar desde la carpeta del proyecto:

```powershell
dotnet run --project BookShopFrontend/Web --launch-profile http
```

Abrir [BookShop local](http://localhost:5188). Para esta opción no se necesita iniciar GlassFish localmente. El servicio gratuito de Render puede tardar varios minutos en arrancar después de un período sin uso.

El despliegue utiliza `BookShopBackend/Dockerfile`. La variable privada `BOOKSHOP_DB_PROPERTIES_B64` contiene la configuración de MySQL en Base64. No debe publicarse su valor. El arranque restaura ese archivo dentro del servidor.

## Ejecutar también el backend localmente

Se requieren JDK 25, Maven, MySQL, GlassFish compatible con Jakarta EE y SDK .NET 10.

1. Completar `BookShopBackend/DBManager/src/main/resources/db.properties` usando la plantilla `db.example.properties` de la misma carpeta.
2. Para una base nueva, utilizar `BookShopBackend/DAO/src/main/resources/DDL.sql`. Este archivo recrea las tablas; no debe ejecutarse sobre una base con datos que se quieran conservar.
3. Ejecutar `mvn package` desde `BookShopBackend` y desplegar `Service/target/bookshop.war` en GlassFish. La API utiliza `/bookshop/webresources`.
4. Configurar `Backend:Url` en `BookShopFrontend/Web/appsettings.json`. La dirección local es `https://localhost:8181/bookshop/webresources/`; el certificado de GlassFish debe ser de confianza en Windows. Ejecutar:

```powershell
dotnet run --project BookShopFrontend/Web --launch-profile http
```

Abrir [BookShop local](http://localhost:5188). El backend y la base de datos deben estar disponibles.

La clave de cifrado de los métodos de pago está definida en `ProteccionDatos.java` y debe conservarse para leer los datos ya guardados. La configuración de BD se incluye al empaquetar el WAR: no publicar ese archivo compilado con credenciales.

## Funcionamiento

Cada recurso REST crea el BO que necesita (`new XBOImpl()`) y le delega las reglas. Business que utiliza los DAO para acceder a MySQL. El servidor convierte los DTO Java a JSON con JSON-B. Web consume ese JSON con Newtonsoft.Json y clases DTO en `Modelos/DTO`.

Las pantallas Blazor están en `Componentes`, con marcado `.razor`, lógica `.razor.cs` y estilos `.razor.css`. Los servicios de Web gestionan las llamadas REST, la sesión y los datos compartidos. El componente `Cargando` muestra la espera o el error mientras se consultan datos.

Inicio ofrece Más populares y, con tres pedidos válidos, Para ti. Sus páginas calculan una consulta al abrir y reutilizan sus libros al buscar, filtrar o paginar, conservando el orden del algoritmo. Más vendidos y Libros nuevos también conservan su único orden. La búsqueda del encabezado actúa sobre el listado abierto: títulos dentro de su ámbito o nombres de autores, categorías y editoriales. El detalle del libro permite publicar, votar y reportar reseñas; Cuenta reúne Mis reseñas, Mis pedidos y Mis solicitudes.

El seguimiento avanza únicamente al pulsar Actualizar seguimiento: treinta segundos desde el pago para salir y sesenta desde la salida para entregar. Business guarda juntos la entrega y su premio, o la cancelación/devolución con la reposición y el cupón. Mis solicitudes permite editar o retirar pendientes; Moderación permite resolverlas. Las referencias modales muestran información sin acciones sobre el pedido o la reseña.

DAO y Business conservan una interfaz y su implementación por responsabilidad. Business coordina las operaciones: cambiar una referencia y sus vetos, guardar un pedido y sus líneas, o registrar un voto y actualizar su contador. Los DAO ejecutan las consultas SQL y convierten sus resultados en entidades o DTO. Business confirma o revierte las operaciones que deben guardarse juntas mediante DBManager.

En Web, la pantalla de compra conserva la preparación recibida del backend y utiliza su selección directamente. Los filtros guardados en la sesión son los mismos que utiliza la pantalla, para volver del detalle sin copiar su estado. Favorito y Deseado pertenecen al detalle del libro. Direcciones, métodos de pago, cupones, reseñas y confirmación muestran los DTO recibidos. Los formularios editan un borrador separado; no se crean modelos intermedios que repitan esos datos.

Los formularios utilizan los campos obligatorios del navegador. El esquema define los límites y las restricciones de los datos; Business conserva reglas de operación como ISBN único, stock, importes y vigencia para pagar, sin repetir validadores completos de los formularios de catálogo y direcciones. Los avisos se cierran con la X. Al perder la conexión, Blazor intenta reconectar y el diálogo permite recargar la página.

La autenticación utiliza una cookie de sesión por circuito Blazor. Una recarga completa requiere iniciar sesión nuevamente. Las operaciones con sesión usan HTTPS.

Las imágenes se reciben y devuelven como texto en los DTO, por ejemplo `"portada": "https://ejemplo.com/libro.jpg"`. Registro y perfil permiten ingresar una URL HTTP/HTTPS o retirar la imagen. La BD guarda texto de hasta 2048 caracteres; las imágenes nuevas deben estar alojadas previamente.

## Conceptos del curso en el proyecto

Las entidades de Model muestran encapsulamiento y relaciones entre objetos; los métodos de pago utilizan herencia y polimorfismo. Business y DAO conservan interfaces e implementaciones, organizadas en paquetes y módulos. Los DTO transportan datos entre Java y Blazor mediante REST. Los componentes de la interfaz reúnen HTML, CSS y eventos de C#.

El servidor atiende peticiones en distintos hilos. Carrito y Pedido utilizan `synchronized` sobre el mismo carrito para que cambiar cantidades y pagar no se ejecuten simultáneamente sobre sus datos. El mapa de carritos tiene un monitor para crear un único carrito por usuario. Las consultas de recomendaciones guardadas están en un mapa estático protegido con `synchronized`.

Las transacciones de DBManager mantienen juntas las escrituras relacionadas en la base de datos. Los monitores protegen los objetos compartidos en memoria; ambas responsabilidades se mantienen separadas. No se incorporan semáforos ni tareas en segundo plano sin una necesidad concreta. JasperReports genera el comprobante y los dos reportes a partir de plantillas JRXML, sin guardar los PDF en la base.

## Verificación local

```powershell
mvn -f BookShopBackend/pom.xml package
dotnet build BookShopFrontend/Web
```

`BookShopBackend/App` contiene un `Main` que recorre el flujo de compra por consola (consultar libro, carrito, preparación y pago simulado) contra la base de desarrollo. Ese recorrido registra un pedido real en la base.

Las credenciales, claves, respaldos, archivos temporales y resultados de compilación se excluyen de Git.

Los administradores acceden desde Cuenta a Administración y abren Reportes en un modal con pestañas Libros y Usuarios. El frontend controla ese acceso por rol. Los endpoints de documentos exigen sesión; el comprobante conserva la comprobación de propiedad del pedido, y los reportes no añaden una comprobación administrativa en Business. Las plantillas se compilan al generar cada PDF, sin caché adicional.

## Errores y fechas

Business lanza `NegocioException` para las reglas del negocio y cuatro subclases para casos con un código HTTP propio: `ValidacionException` (400), `AccesoDenegadoException` (403), `RecursoNoEncontradoException` (404) y `PersistenciaException` (500, fallos de la BD). `Errores` convierte cualquier excepción en un JSON `{"mensaje": "..."}`; las demás reglas responden 409. El frontend muestra ese mensaje.

Las fechas del negocio se obtienen con `LocalDateTime.now()`. El contenedor del backend fija `TZ=America/Lima` y la conexión MySQL usa la misma zona.
