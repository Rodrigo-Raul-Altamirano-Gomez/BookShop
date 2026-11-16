# Despliegue de la aplicación web

Publicado en [BookShop](https://bookshop-demo.onrender.com), mediante un
**Web Service de Render**, con runtime Docker y plan Free.
Esta versión consume el backend Java. Configurar `Backend__Url` con la URL HTTPS
pública o accesible desde el servidor C#, terminada en `/bookshop/webresources/`.
El backend debe estar desplegado y tener acceso a MySQL antes de usar la web.
Backend y frontend se despliegan desde la rama main del mismo repositorio BookShop.

Repositorio privado: [BookShop](https://github.com/Rodrigo-Raul-Altamirano-Gomez/BookShop).
Servicio: [panel de Render](https://dashboard.render.com/web/srv-db3gso7avr4c739oakog).

## Fuente del servicio

Conservar esta carpeta dentro del repositorio BookShop,
junto a BookShopBackend. Los archivos
`.gitignore` y `.dockerignore` excluyen los resultados de compilación y limitan
el contenido de la imagen a los archivos de Web.

Render necesita acceso a ese repositorio. Se puede crear un Blueprint usando
`render.yaml`, o un Web Service con estos ajustes:

| Campo | Valor |
| --- | --- |
| Runtime | Docker |
| Plan | Free |
| Dockerfile Path | `BookShopFrontend/Web/Dockerfile` |
| Docker Build Context | `BookShopFrontend/Web` |
| Health Check Path | `/salud` |
| Variable de entorno | `ASPNETCORE_FORWARDEDHEADERS_ENABLED=true` |
| Backend REST | `Backend__Url=https://HOST/bookshop/webresources/` |

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
3. Comprobar búsqueda, registro e inicio de sesión, carrito y navegación sin recargar.
4. Confirmar que estilos e imágenes cargan y que la interacción permanece conectada.

El plan gratuito puede suspenderse después de 15 minutos sin actividad; al volver
a abrirlo, el arranque puede tardar alrededor de un minuto. La sesión de la interfaz
pertenece al circuito Blazor; los datos de la cuenta y pedidos permanecen en la base de datos del backend.

Documentación: [Docker](https://render.com/docs/docker),
[Web Services](https://render.com/docs/web-services),
[plan gratuito](https://render.com/docs/free),
[WebSockets](https://render.com/docs/websocket).
