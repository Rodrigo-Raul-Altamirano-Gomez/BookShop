# BookShop

Proyecto académico de una tienda de libros: backend Java 25 con servicios REST Jakarta EE y frontend Blazor en .NET 10 con Bootstrap.

El frontend consume la API desde el proyecto Web para los doce requisitos elegidos. Las imágenes se guardan como URLs; los archivos actuales del catálogo están en `BookShopFrontend/Web/wwwroot/images/catalogo`.

El entregable abarca RF01, RF02, RF05, RF06, RF07, RF08, RF09, RF11, RF12, RF14, RF15 y RF16, además de los documentos PDF RF17, RF18 y RF19. Las funcionalidades fuera de este alcance muestran un aviso de pendiente.

## Documentos PDF (RF17, RF18 y RF19)

Se generan con JasperReports 7.0.6 a partir de las plantillas de `BookShopBackend/Business/src/main/resources/reportes`. `GeneradorPDF` compila cada `.jrxml` la primera vez que se usa y conserva el resultado en memoria.

| Requisito | Endpoint (GET, `application/pdf`) | Acceso | Pantalla |
|---|---|---|---|
| RF17 Comprobante de compra | `documentos/comprobantes/{idPedido}` | Dueño del pedido | Botón en la confirmación del pedido |
| RF18 Ventas por libro | `documentos/ventas-libros?inicio=aaaa-mm-dd&fin=aaaa-mm-dd&cantidad=n` | Administrador | `/administracion/reportes` |
| RF19 Usuarios por gasto | `documentos/usuarios?mes=aaaa-mm&cantidad=n` | Administrador | `/administracion/reportes` |

`cantidad` es opcional: si se omite, el reporte no tiene límite. Web descarga el PDF con la sesión del circuito y lo entrega al navegador mediante `bookshopDescargar` (`wwwroot/ui.js`). Para cambiar el diseño, edita el `.jrxml` en Jaspersoft Studio 7.x y vuelve a copiarlo en esa carpeta. `GeneradorPDFTest` deja ejemplos en `Business/target/reportes-prueba`.

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

El despliegue utiliza `BookShopBackend/Dockerfile`. La variable privada `BOOKSHOP_DB_PROPERTIES_B64` contiene la configuración de MySQL en Base64. No debe publicarse su valor. El arranque restaura ese archivo dentro del servidor.

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

La clave de cifrado de los métodos de pago está definida en `ProteccionDatos.java` y debe conservarse para leer los datos ya guardados. La configuración de BD se incluye al empaquetar el WAR: no publicar ese archivo compilado con credenciales.

## Conceptos del curso en el proyecto

Las entidades de Model muestran encapsulamiento y relaciones entre objetos; los métodos de pago utilizan herencia y polimorfismo. Business y DAO conservan interfaces e implementaciones, organizadas en paquetes y módulos. Los DTO transportan datos entre Java y Blazor mediante REST. Los componentes de la interfaz reúnen HTML, CSS y eventos de C#.

El servidor atiende peticiones en distintos hilos. Carrito y Pedido utilizan `synchronized` sobre el mismo carrito para que cambiar cantidades y pagar no se ejecuten simultáneamente sobre sus datos. El mapa de carritos tiene un monitor para crear un único carrito por usuario. Las consultas de recomendaciones guardadas utilizan el monitor de su instancia. La prueba `CarritoConcurrenciaTest` ejecuta dos hilos sobre el mismo carrito y comprueba que se conservan las cantidades.

Las transacciones de DBManager mantienen juntas las escrituras relacionadas en la base de datos. Los monitores protegen los objetos compartidos en memoria; ambas responsabilidades se mantienen separadas. No se incorporan semáforos ni tareas en segundo plano sin una necesidad concreta. JasperReports queda fuera de esta etapa.

## Verificación local

```powershell
mvn -f BookShopBackend/pom.xml verify
dotnet run --project BookShopFrontend/Pruebas
```

Las pruebas aisladas no sustituyen la revisión del sistema desplegado.

Con el backend y una base de desarrollo disponibles, `dotnet run --project BookShopFrontend/Pruebas -- --real` ejecuta pruebas integradas que crean un usuario, una dirección, una tarjeta ficticia y un pedido. Esos datos permanecen en la base.

Las credenciales, claves, respaldos, archivos temporales y resultados de compilación se excluyen de Git.
