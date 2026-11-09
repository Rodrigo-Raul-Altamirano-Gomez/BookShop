# Frontend de BookShop

Aplicación Blazor Interactive Server (.NET 10). Consume los REST de Java desde
el mismo proyecto Web. La interfaz mantiene sus pantallas, textos y estilos.

## Ejecutar

Desplegar el backend y configurar `Backend:Url` en `appsettings.json`.
La dirección local es `https://localhost:8181/bookshop/webresources/`.
El certificado localhost de GlassFish debe ser de confianza en Windows.
La validación HTTPS permanece habilitada.

```powershell
dotnet run --project BookShopFrontend/Web --launch-profile http
```

Abrir http://localhost:5188.

## Organización

- `Modelos/DTO`: clases normales, una por archivo, con propiedades y `JsonProperty`
  para los nombres que utiliza la API Java. Newtonsoft.Json serializa y deserializa.
- `Servicios/HttpClientUtils`: métodos Get, Post, Put y Delete; conexión, JSON,
  cookie de sesión y mensaje de error. Las pantallas no manipulan JSON.
- `Servicios/SesionUsuario`: identidad, avisos y selección de navegación.
- `Servicios/CatalogoServicio`: referencias e idiomas usados en filtros y datos
  compartidos de los libros visibles.
- `Servicios/CuentaServicio`: direcciones, métodos de pago y listas personales.
- `Servicios/CompraServicio`: carrito, preparación y confirmación mostrada.
- `Modelos`: datos específicos de presentación y formularios. Se mantienen cuando
  la pantalla combina o muestra de otra forma los campos del contrato REST.
- `Servicios/Presentacion`: adaptación a esas pantallas e imágenes. No convierte
  JSON, no valida compras ni calcula precios del negocio.
- `Componentes`: marcado .razor, eventos .razor.cs y estilos .razor.css.

Las llamadas sencillas se leen directamente en la pantalla: `Api.Get<T>(ruta)`.
Las operaciones que actualizan datos compartidos tienen métodos explícitos,
como `Compras.AgregarLibro(id, cantidad)` o `Cuenta.GuardarDireccion(datos)`.
No hay un ejecutor genérico de operaciones ni un estado que concentre toda la aplicación.

El acceso carga el perfil y el carrito; las demás páginas consultan sus datos
al utilizarlas. Los formularios muestran los mensajes que devuelve Java.
Business mantiene las reglas, stock, descuentos, envío y pago simulado.
Cada circuito conserva sus cookies; no se comparten entre usuarios. Una recarga
completa crea otro circuito y requiere iniciar sesión nuevamente.

## Alcance

RF01, RF02, RF05, RF06, RF07, RF08, RF09, RF11, RF12, RF14, RF15 y RF16.
Las rutas de funcionalidades que no pertenecen al entregable conservan su aviso
de funcionalidad pendiente. Se retiraron sus componentes de demostración sin uso.

## Comprobar

```powershell
dotnet build BookShopFrontend/BookShopFrontend.slnx
dotnet run --project BookShopFrontend/Pruebas
```

Las comprobaciones usan un servidor temporal y renderizan componentes reales.
Cubren JSON, fechas, imágenes, cookies separadas, sesión vencida, paginación de
idiomas, importes recibidos y contrato de compra.

Con GlassFish y la base de desarrollo disponibles:

```powershell
dotnet run --project BookShopFrontend/Pruebas -- --real
```

Esta última prueba crea un usuario, dirección, tarjeta ficticia y pedido de prueba.
Comprueba los servicios C# contra MySQL real y el reintento sin duplicar el pedido.
Los datos de prueba permanecen en la base.

## Estado de carga

`Componentes/Compartidos/Cargando.razor` muestra un spinner de Bootstrap con los
colores de BookShop. Recibe EnCurso y mantiene oculto ChildContent hasta terminar;
si Error tiene un mensaje, muestra ese error en lugar de un estado vacío.
Cada pantalla controla sus consultas con try/catch/finally. Se utiliza en inicio,
catálogo, referencias, detalle de libro/referencia, comparación, cuenta, carrito y
preparación de compra, incluyendo filtros, orden y paginación del catálogo y reseñas.
Las pantallas sin consultas nuevas, como la confirmación que muestra el pedido
recién recibido, no introducen una espera artificial.

Las pruebas incluyen una consulta retenida: durante ella no se renderizan libros
ni mensajes de lista vacía; después se muestra el contenido. También verifican
que un error finalice el indicador y se presente como error.

## Imágenes mediante URLs

La base almacena URLs en columnas VARCHAR(2048), sin blobs ni campos MIME.
ImagenDTO utiliza únicamente la propiedad `url`. Web utiliza esa dirección en
`img src`; no convierte imágenes a Base64 ni envía arreglos de bytes.
Registro y perfil permiten introducir una URL HTTP/HTTPS y retirar la imagen.
Las imágenes anteriores se conservaron en `wwwroot/images/catalogo` y sus rutas
`/images/catalogo/...` están guardadas en MySQL. Esos archivos forman parte del
despliegue de Web. Para imágenes nuevas se introduce una dirección ya alojada.
