# BookShop

Proyecto académico de una tienda de libros en línea. Reúne un backend en Java y una
interfaz web en Blazor para explorar el catálogo, gestionar una cuenta, recorrer el
proceso de compra y administrar la tienda.

**Estado actual: desarrollo por capas, pendiente de integración.** La lógica de negocio
y la persistencia están implementadas en el backend. El frontend ofrece pantallas y
recorridos interactivos con datos ficticios; todavía no consume el backend.

## Funcionalidades

- Catálogo de libros, autores, categorías y editoriales, con búsqueda, filtros y comparación.
- Cuenta de usuario, perfil, favoritos, deseados, seguimientos, direcciones y métodos de pago.
- Carrito, preparación de compra, pedidos, seguimiento, cancelaciones, devoluciones y cupones.
- Reseñas, votos de utilidad, reportes y moderación.
- Administración de catálogo, stock, precios, descuentos y configuración de negocio.
- Recomendaciones y preparación de datos para reportes y comprobantes.

La interfaz permite recorrer estas funciones mediante ejemplos. Los pagos y las
operaciones de tienda no se ejecutan contra servicios reales desde el frontend.

## Estado por componente

| Componente | Estado |
| --- | --- |
| Modelo Java | Entidades, enumeraciones y DTO definidos. |
| Persistencia Java | DAO con JDBC, pool de conexiones y coordinación de transacciones. |
| Negocio Java | Servicios BO, validaciones, permisos y reglas implementados; incluye pruebas. |
| Base de datos | DDL para MySQL incluido; falta verificar el conjunto en una instancia MySQL real. |
| Frontend Web | Interfaz de escritorio e interacción implementadas con datos de demostración. |
| Integración | Pendiente: API de comunicación, consumo desde Blazor y conexión de los recorridos reales. |
| Documentos | Backend prepara los datos; la generación de archivos PDF está pendiente. |

Las pruebas del backend usan H2, Mockito y un driver de prueba para aislar dependencias.
Su resultado no sustituye las verificaciones de MySQL real ni las pruebas del sistema integrado.

## Estructura

```text
BookShop/                       Backend Java y proyecto Maven multimódulo
├── Model/                      Entidades y DTO
├── DBManager/                  Pool JDBC y contexto de transacciones
├── DAO/                        Persistencia, consultas y esquema SQL
├── Business/                   Servicios y reglas de negocio
├── App/                        Demostración antigua de consola, fuera del reactor normal
└── scripts/                    Alternativa local para ejecutar las pruebas

BookShopFrontend/               Solución .NET
├── Web/                        Aplicación Blazor
│   ├── Componentes/            Páginas, estructura y componentes compartidos
│   ├── Modelos/                Modelos de presentación
│   ├── Demostracion/           Datos ficticios y estado temporal
│   └── wwwroot/               CSS, JavaScript e ilustraciones
└── Integration/                Proyecto reservado para la integración, todavía sin implementar
```

El frontend utiliza **code-behind**: el marcado está en archivos `.razor`, y los eventos
y el estado de pantalla están en sus archivos `.razor.cs`. Los estilos específicos
acompañan al componente en archivos `.razor.css`.

## Tecnologías

- Backend: Java 21, Maven, JDBC, MySQL Connector/J y HikariCP.
- Pruebas: JUnit 5, Mockito y H2.
- Frontend: .NET 10, ASP.NET Core Blazor con interactividad en servidor, C#, CSS y JavaScript.
- Demostración desplegada: contenedor Docker en Render.

## Ejecutar la interfaz

Requisito: SDK de **.NET 10**. Desde la raíz del repositorio:

```powershell
dotnet restore BookShopFrontend/Web/Web.csproj
dotnet run --project BookShopFrontend/Web/Web.csproj --launch-profile http
```

Abrir [http://localhost:5188](http://localhost:5188). Esta demostración funciona sin
Java, MySQL ni un servidor del backend.

- Cliente de prueba: cualquier correo válido y una contraseña no vacía.
- Administrador de prueba: `demo@admin.bookshop`, con una contraseña no vacía.
- Los cambios son temporales y se restablecen al iniciar otro circuito de Blazor,
  por ejemplo al recargar la página.

Son accesos simulados, sin autenticación real. Utilizar únicamente datos ficticios.
La interfaz está orientada a escritorio; la adaptación completa a móviles sigue pendiente.

## Compilar y probar el backend

Requisitos: **JDK 21 o posterior** y **Maven**. Desde `BookShop`:

```powershell
cd BookShop
mvn clean test
```

El reactor normal incluye `Model`, `DBManager`, `DAO` y `Business`. No inicia un servidor HTTP.
`App` conserva una demostración de consola anterior y no es el punto de entrada de una API.

En Windows también se incluye `scripts/pruebas-backend.ps1` como alternativa cuando
las dependencias Maven ya están descargadas. El script admite `-Jdk` y `-RepositorioMaven`.

### Configuración de MySQL

Para usar los DAO contra MySQL, copiar la plantilla de configuración y completar
los valores de una base local:

```powershell
# Desde BookShop
Copy-Item DBManager/src/main/resources/db.example.properties DBManager/src/main/resources/db.properties
```

El esquema está en `DAO/src/main/resources/DDL.sql` y requiere MySQL 8.0.16 o posterior.
El DDL **recrea las tablas**: debe utilizarse en una base de desarrollo preparada para ello.
Los datos antiguos de `registers.sql` no forman parte de esta publicación.

`db.properties` y las claves de cifrado son locales y están excluidos de Git. Las
pruebas incluyen una configuración independiente con valores ficticios.

## Demostración y despliegue

La interfaz de demostración está publicada en
[BookShop en Render](https://bookshop-demo.onrender.com). El repositorio de despliegue
de esa interfaz es independiente de este repositorio, que reúne ambos proyectos.

Los archivos `Dockerfile` y `render.yaml` están dentro de `BookShopFrontend/Web` y
describen únicamente el despliegue de la interfaz. Su contexto de construcción es esa
carpeta, no la raíz de este repositorio.

## Próximos pasos

1. Implementar la capa de comunicación con el backend y completar `Integration`.
2. Sustituir los datos ficticios por las operaciones reales y conectar la gestión de sesión.
3. Verificar esquema, persistencia y concurrencia en MySQL.
4. Implementar la generación de reportes y comprobantes.
5. Completar las pruebas de los recorridos integrados y ajustar la interfaz.

## Contenido del repositorio

Se incluyen código fuente, proyectos, pruebas, esquema SQL, recursos de la interfaz
y configuración de ejemplo. Se excluyen las referencias internas de `docs`, las
credenciales, las claves, los datos antiguos, los resultados de compilación y el
estado local de los editores.

Más detalles: [frontend](BookShopFrontend/Web/README.md),
[negocio](BookShop/Business/README.md) y [persistencia](BookShop/DAO/README.md).
