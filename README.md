# BookShop

Proyecto académico de una tienda de libros: backend Java 25 con servicios REST Jakarta EE y frontend Blazor en .NET 10 con Bootstrap.

El frontend consume la API desde el proyecto Web para los doce requisitos elegidos. Las imágenes se guardan como URLs; los archivos actuales del catálogo están en `BookShopFrontend/Web/wwwroot/images/catalogo`.

## Estructura

- `BookShopBackend/Model`: entidades y DTO.
- `BookShopBackend/DBManager`: conexiones y transacciones.
- `BookShopBackend/DAO`: persistencia JDBC y esquema MySQL.
- `BookShopBackend/Business`: reglas de negocio.
- `BookShopBackend/Service`: API REST Jakarta EE.
- `BookShopBackend/App`: ejecución por consola.
- `BookShopFrontend/Web`: interfaz, DTO y consumo REST.
- `BookShopFrontend/Pruebas`: comprobaciones del frontend.

## Ejecución local

Se requieren JDK 25, Maven, MySQL, GlassFish compatible con Jakarta EE y SDK .NET 10.

1. Completar `BookShopBackend/DBManager/src/main/resources/db.properties` usando la plantilla `db.example.properties` de la misma carpeta.
2. Para una base nueva, utilizar `BookShopBackend/DAO/src/main/resources/DDL.sql`. Este archivo recrea las tablas; no debe ejecutarse sobre una base con datos que se quieran conservar.
3. Ejecutar `mvn verify` desde `BookShopBackend` y desplegar `Service/target/bookshop.war` en GlassFish. La API utiliza `/bookshop/webresources`.
4. Revisar la dirección de la API en la configuración de Web y ejecutar:

```powershell
dotnet run --project BookShopFrontend/Web --launch-profile http
```

Abrir [BookShop local](http://localhost:5188). El backend y la base de datos deben estar disponibles.

## Pruebas

```powershell
mvn -f BookShopBackend/pom.xml verify
dotnet run --project BookShopFrontend/Pruebas
```

Las pruebas aisladas no sustituyen la revisión del sistema desplegado.

Las credenciales, claves, respaldos, archivos temporales y resultados de compilación se excluyen de Git.

Más detalles en los README de [Web](BookShopFrontend/Web/README.md), [Service](BookShopBackend/Service/README.md), [Business](BookShopBackend/Business/README.md) y [DAO](BookShopBackend/DAO/README.md).
