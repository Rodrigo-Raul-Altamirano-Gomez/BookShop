# Despliegue de la interfaz de demostración

Publicado en [BookShop](https://bookshop-demo.onrender.com), mediante un
**Web Service de Render**, con runtime Docker y plan Free.
Solo se publica Web; sus datos de ejemplo son temporales y no utiliza el backend.

Repositorio privado: [BookShop-Web](https://github.com/Rodrigo-Raul-Altamirano-Gomez/BookShop-Web).
Servicio: [panel de Render](https://dashboard.render.com/web/srv-db3gso7avr4c739oakog).

## Fuente del servicio

Subir el contenido de esta carpeta a la raíz de un repositorio de GitHub,
preferiblemente privado. No subir las otras carpetas del proyecto. Los archivos
`.gitignore` y `.dockerignore` excluyen los resultados de compilación y limitan
el contenido de la imagen a los archivos de Web.

Render necesita acceso a ese repositorio. Se puede crear un Blueprint usando
`render.yaml`, o un Web Service con estos ajustes:

| Campo | Valor |
| --- | --- |
| Runtime | Docker |
| Plan | Free |
| Dockerfile Path | `./Dockerfile` |
| Docker Build Context | `.` |
| Health Check Path | `/salud` |
| Variable de entorno | `ASPNETCORE_FORWARDEDHEADERS_ENABLED=true` |

## Qué ejecuta

Docker compila con el SDK .NET 10 y ejecuta Web con ASP.NET Core 10 en Production,
como usuario sin privilegios, escuchando en el puerto 10000. Render proporciona
el enlace HTTPS público y admite las conexiones WebSocket utilizadas por Blazor.
La variable de encabezados reenviados reconoce el HTTPS terminado por el proxy
de Render. Este ajuste está destinado a ese alojamiento.

No se necesita Docker en tu computadora para que Render construya la imagen.

La publicación Release se generó y se ejecutó localmente en Production. Inicio,
estilos y `/salud` respondieron HTTP 200; la negociación de Blazor anunció WebSockets.
Render construye la imagen Docker, sin necesitar Docker local. Los componentes
`.razor` se copian antes de `dotnet restore`: Blazor necesita detectarlos en ese
paso para incluir los scripts `_framework` que activan la interacción.

## Comprobación posterior al despliegue

1. Confirmar que el despliegue termina en estado Live.
2. Abrir la URL HTTPS asignada y `/salud`, que debe devolver `ok`.
3. Comprobar búsqueda, acceso de ejemplo, carrito y navegación sin recargar.
4. Confirmar que estilos e imágenes cargan y que la interacción permanece conectada.

El plan gratuito puede suspenderse después de 15 minutos sin actividad; al volver
a abrirlo, el arranque puede tardar alrededor de un minuto. Los datos de prueba
se restablecen al iniciar otro circuito o reiniciar el servicio.

Documentación: [Docker](https://render.com/docs/docker),
[Web Services](https://render.com/docs/web-services),
[plan gratuito](https://render.com/docs/free),
[WebSockets](https://render.com/docs/websocket).
