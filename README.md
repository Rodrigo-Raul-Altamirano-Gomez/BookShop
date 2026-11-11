# BookShop

Proyecto académico de una tienda de libros: backend Java 25 con servicios REST Jakarta EE y frontend Blazor en .NET 10 con Bootstrap.

El frontend consume la API desde el proyecto Web para los doce requisitos elegidos. Las imágenes se guardan como URLs; los archivos actuales del catálogo están en `BookShopFrontend/Web/wwwroot/images/catalogo`.

El entregable abarca RF01, RF02, RF05, RF06, RF07, RF08, RF09, RF11, RF12, RF14, RF15 y RF16. Las funcionalidades fuera de este alcance muestran un aviso de pendiente.

## Estructura

- `BookShopBackend/Model`: entidades y DTO.
- `BookShopBackend/DBManager`: conexiones y transacciones.
- `BookShopBackend/DAO`: persistencia JDBC y esquema MySQL.
- `BookShopBackend/Business`: reglas de negocio.
- `BookShopBackend/Service`: API REST Jakarta EE.
- `BookShopBackend/App`: ejecución por consola.
- `BookShopFrontend/Web`: interfaz, DTO y consumo REST.
- `BookShopFrontend/Pruebas`: comprobaciones del frontend.

## Ejecutar el frontend con el backend desplegado

El backend está en Render y la dirección ya está configurada en `BookShopFrontend/Web/appsettings.json`:

https://bookshop-backend-0qyo.onrender.com/bookshop/webresources/

Ejecutar el proyecto `BookShopFrontend/Web` con el perfil `http`, o usar desde la carpeta del proyecto:

```powershell
dotnet run --project BookShopFrontend/Web --launch-profile http
```

Abrir [BookShop local](http://localhost:5188). Para esta opción no se necesita iniciar GlassFish localmente. El servicio gratuito de Render puede tardar varios minutos en arrancar después de un período sin uso.

El despliegue utiliza `BookShopBackend/Dockerfile` y `BookShopBackend/despliegue/iniciar.sh`. Las variables privadas `BOOKSHOP_DB_PROPERTIES_B64` y `BOOKSHOP_KEY_B64` contienen la configuración de MySQL y la clave de cifrado en Base64. No deben publicarse sus valores. El arranque restaura esos archivos dentro del servidor.

## Ejecutar también el backend localmente

Se requieren JDK 25, Maven, MySQL, GlassFish compatible con Jakarta EE y SDK .NET 10.

1. Completar `BookShopBackend/DBManager/src/main/resources/db.properties` usando la plantilla `db.example.properties` de la misma carpeta.
2. Para una base nueva, utilizar `BookShopBackend/DAO/src/main/resources/DDL.sql`. Este archivo recrea las tablas; no debe ejecutarse sobre una base con datos que se quieran conservar.
3. Ejecutar `mvn verify` desde `BookShopBackend` y desplegar `Service/target/bookshop.war` en GlassFish. La API utiliza `/bookshop/webresources`.
4. Configurar `Backend:Url` en `BookShopFrontend/Web/appsettings.json`. La dirección local es `https://localhost:8181/bookshop/webresources/`; el certificado de GlassFish debe ser de confianza en Windows. Ejecutar:

```powershell
dotnet run --project BookShopFrontend/Web --launch-profile http
```

Abrir [BookShop local](http://localhost:5188). El backend y la base de datos deben estar disponibles.

La clave de cifrado de los métodos de pago debe conservarse entre despliegues. Su ubicación se puede configurar mediante la propiedad JVM `bookshop.clave.file`. La configuración de BD se incluye al empaquetar el WAR: no publicar ese archivo compilado con credenciales.

## Funcionamiento

Los recursos REST delegan las reglas en Business, que utiliza los DAO para acceder a MySQL. El servidor convierte los DTO Java a JSON con JSON-B. Web consume ese JSON con Newtonsoft.Json y clases DTO en `Modelos/DTO`.

Las pantallas Blazor están en `Componentes`, con marcado `.razor`, lógica `.razor.cs` y estilos `.razor.css`. Los servicios de Web gestionan las llamadas REST, la sesión y los datos compartidos. El componente `Cargando` muestra la espera o el error mientras se consultan datos.

La autenticación utiliza una cookie de sesión por circuito Blazor. Una recarga completa requiere iniciar sesión nuevamente. Las llamadas de escritura incluyen la cabecera `X-BookShop-Cliente: web`; las operaciones con sesión usan HTTPS.

Las imágenes se reciben y devuelven como texto en los DTO, por ejemplo `"portada": "https://ejemplo.com/libro.jpg"`. Registro y perfil permiten ingresar una URL HTTP/HTTPS o retirar la imagen. La BD guarda texto de hasta 2048 caracteres; las imágenes nuevas deben estar alojadas previamente.

## Pruebas

```powershell
mvn -f BookShopBackend/pom.xml verify
dotnet run --project BookShopFrontend/Pruebas
```

Las pruebas aisladas no sustituyen la revisión del sistema desplegado.

Con el backend y una base de desarrollo disponibles, `dotnet run --project BookShopFrontend/Pruebas -- --real` ejecuta pruebas integradas que crean un usuario, una dirección, una tarjeta ficticia y un pedido. Esos datos permanecen en la base.

Las credenciales, claves, respaldos, archivos temporales y resultados de compilación se excluyen de Git.
