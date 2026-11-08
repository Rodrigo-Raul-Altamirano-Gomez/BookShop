# Servicio REST de BookShop

Módulo Jakarta EE 11 empaquetado como `bookshop.war`, compilado con Java 25.
Su alcance son RF01, RF02, RF05, RF06, RF07, RF08, RF09, RF11, RF12, RF14,
RF15 y RF16 de [Requisitos unificados](../../docs/Requisitos_unificados.md).

Los recursos delegan en Business. No acceden a los DAO ni calculan reglas, precios,
permisos o probabilidades. La consulta de reseñas y valoración pertenece al
detalle de RF09; no se exponen publicación, votos ni reportes de RF10.

## Organización

Los paquetes siguen los dominios de Business. Las clases REST usan el sufijo RS:

```text
me.Service
├── catalogo       AutorRS, CategoriaRS, EditorialRS, LibroRS
├── usuario        UsuarioRS, DireccionEnvioRS, MetodoPagoRS, LibroGuardadoRS, SeguimientoRS
├── compra         CarritoRS, CuponRS, PedidoRS
├── opinion        ResenaRS, solo consulta para el detalle
└── recomendacion  RecomendacionRS, similares y complementarios
```

El arranque y los auxiliares comunes de sesión y HTTP permanecen en
`me.Service`, al igual que los auxiliares compartidos de Business están en su raíz.
Las pruebas HTTP que recorren varios dominios están en `me.Service`.

La conversión JSON utiliza el proveedor JSON-B del servidor. Los DTO record tienen
un único constructor canónico; sus valores iniciales se obtienen con `porDefecto()`.
Service no incluye un proveedor JSON propio ni dependencias Jackson.

## Compilar y desplegar

Desde `BookShopBackend`, con Maven ejecutándose con JDK 25:

```powershell
mvn -pl Service -am clean verify
```

El resultado es `Service/target/bookshop.war`. Desplegar en un servidor Jakarta
EE 11 compatible con Java 25, por ejemplo Payara 7. El servidor aporta Jakarta
REST, CDI y Servlet; no se incluyen esas implementaciones dentro del WAR.
La base de la API es `https://HOST:PUERTO/bookshop/api`.

Preparar MySQL con el esquema vigente y completar
`DBManager/src/main/resources/db.properties` antes de empaquetar, según el README
raíz. Esa configuración local quedará dentro del WAR: no publicar el archivo.
Este módulo no ejecuta el DDL ni crea datos de prueba.

La clave de métodos de pago debe conservarse entre despliegues. Fijar la propiedad
JVM `bookshop.clave.file` a una ruta estable del servidor, según Business.
Los BO y el pool duran lo mismo que la aplicación; el cierre del módulo cierra el pool.
Carrito y preparación comparten la memoria por actor que ya proporciona Business.

## Sesión y solicitudes

- Iniciar sesión, registrar o reactivar devuelve `AccesoUsuarioDTO`. Solo un
  resultado sin reactivación pendiente abre la sesión y renueva su identificador.
- El cliente conserva la cookie `JSESSIONID` y la envía en llamadas posteriores.
  La cookie es HttpOnly y Secure; usar HTTPS también en las pruebas con sesión.
- Las operaciones privadas obtienen el actor exclusivamente de esa sesión.
  No admiten un parámetro de identidad elegido por el cliente. Business verifica
  cuenta activa, rol y propiedad.
- La sesión vence tras treinta minutos de inactividad. El cierre explícito registra
  el tiempo antes de invalidarla. Si falla ese registro, la sesión se invalida de
  todas formas y se informa del error. Desactivar verifica la contraseña y registra
  el tiempo antes de desactivar e invalidar.
- Para POST, PUT y DELETE, enviar `X-BookShop-Cliente: web`. El filtro exige esta
  cabecera para impedir envíos simples desde otros orígenes con cookies.
  No es una credencial. No se habilita CORS.
- Los cuerpos usan `Content-Type: application/json` y las respuestas JSON.
  No se guardan respuestas en caché.
- Los filtros complejos se envían mediante POST a `/busqueda`; la operación es
  una consulta y no confirma cambios. Las búsquedas conservan los criterios de Business.
- Los nombres de propiedades son los de los records DTO de Model. Los enums usan
  sus nombres Java, por ejemplo `MAS_RECIENTES`, `AUTOR` y `CLIENTE`.
  Las fechas usan ISO: `AAAA-MM-DD` y fecha/hora sin cambio de zona de los DTO.
- `ImagenDTO` usa `contenido` como arreglo JSON de bytes y `tipoMime`, con el
  formato estándar de JSON-B. Business valida el contenido
  JPEG/PNG y el límite de 5 MiB. PUT con cuerpo JSON `null` retira una imagen.
- Los parámetros escalares que no aparecen entre llaves en las rutas se envían
  en la consulta: `cantidad`, `favorito`, `pagina`, `tamanioPagina`, etc.
  `complementarios` recibe un arreglo JSON de ids de libros.
- `cantidad` es obligatorio al agregar o cambiar una línea del carrito. Solo un
  cero enviado explícitamente permite retirarla mediante el cambio de cantidad.
- En guardar, retirar y buscar libros guardados, `favorito` es obligatorio:
  `true` elige Favoritos y `false` Deseados. Otro valor se rechaza sin operar sobre
  ninguna lista.
- Todos los listados con paginación en la URL usan `pagina` y `tamanioPagina`.
- Sin resultado se devuelve 204. Las operaciones que devuelven DTO, listas o ids
  responden 200. Los listados conservan `items`, `totalResultados`, `pagina`
  y `tamanioPagina`; el cliente calcula páginas a partir del total.

### Cuenta

| Método | Ruta relativa a /api | Entrada o resultado |
| --- | --- | --- |
| POST | `/cuenta/registro` | RegistroUsuarioDTO → AccesoUsuarioDTO |
| POST | `/cuenta/sesion` | CredencialesDTO → AccesoUsuarioDTO |
| POST | `/cuenta/reactivacion` | CredencialesDTO → AccesoUsuarioDTO |
| DELETE | `/cuenta/sesion` | Cierra la sesión |
| GET | `/cuenta` | PerfilUsuarioDTO propio |
| PUT | `/cuenta` | DatosPerfilDTO → PerfilUsuarioDTO |
| PUT | `/cuenta/imagen` | ImagenDTO o null → PerfilUsuarioDTO |
| PUT | `/cuenta/contrasenia` | CambioContraseniaDTO |
| POST | `/cuenta/desactivacion` | `{"contraseniaActual":"..."}` |

### Catálogo, cuenta y compra

Los tipos de entrada y resultado corresponden al método Business indicado.
Los cuerpos complejos se reciben directamente como el DTO del contrato, sin
envoltorios adicionales.

| Método | Ruta relativa a /api | Contrato Business | Acceso |
| --- | --- | --- | --- |
| POST | `/autores/busqueda` | AutorBO.buscar | Público; Business comprueba el alcance |
| POST | `/autores/opciones` | AutorBO.buscarOpciones | Público; Business comprueba el alcance |
| GET | `/autores/{id}` | AutorBO.obtenerDetalle | Público; Business comprueba el alcance |
| POST | `/categorias/busqueda` | CategoriaBO.buscar | Público; Business comprueba el alcance |
| POST | `/categorias/opciones` | CategoriaBO.buscarOpciones | Público; Business comprueba el alcance |
| GET | `/categorias/{id}` | CategoriaBO.obtenerDetalle | Público; Business comprueba el alcance |
| POST | `/editoriales/busqueda` | EditorialBO.buscar | Público; Business comprueba el alcance |
| POST | `/editoriales/opciones` | EditorialBO.buscarOpciones | Público; Business comprueba el alcance |
| GET | `/editoriales/{id}` | EditorialBO.obtenerDetalle | Público; Business comprueba el alcance |
| POST | `/libros/busqueda` | LibroBO.buscar | Público; Business comprueba el alcance |
| POST | `/libros/por-autor/{idAutor}` | LibroBO.buscarPorAutor | Público; Business comprueba el alcance |
| POST | `/libros/por-categoria/{idCategoria}` | LibroBO.buscarPorCategoria | Público; Business comprueba el alcance |
| POST | `/libros/por-editorial/{idEditorial}` | LibroBO.buscarPorEditorial | Público; Business comprueba el alcance |
| GET | `/libros/idiomas` | LibroBO.buscarIdiomas | Público; Business comprueba el alcance |
| GET | `/libros/{idLibro}` | LibroBO.obtenerDetalle | Público; Business comprueba el alcance |
| GET | `/libros/comparacion` | LibroBO.comparar | Público; Business comprueba el alcance |
| POST | `/cuenta/direcciones` | DireccionEnvioBO.registrar | Sesión; Business comprueba rol y propiedad |
| PUT | `/cuenta/direcciones/{idDireccion}` | DireccionEnvioBO.modificar | Sesión; Business comprueba rol y propiedad |
| GET | `/cuenta/direcciones/{idDireccion}` | DireccionEnvioBO.consultar | Sesión; Business comprueba rol y propiedad |
| GET | `/cuenta/direcciones` | DireccionEnvioBO.listar | Sesión; Business comprueba rol y propiedad |
| DELETE | `/cuenta/direcciones/{idDireccion}` | DireccionEnvioBO.eliminar | Sesión; Business comprueba rol y propiedad |
| PUT | `/cuenta/direcciones/{idDireccion}/predeterminada` | DireccionEnvioBO.establecerPredeterminado | Sesión; Business comprueba rol y propiedad |
| POST | `/cuenta/metodos` | MetodoPagoBO.registrar | Sesión; Business comprueba rol y propiedad |
| PUT | `/cuenta/metodos/{idMetodo}` | MetodoPagoBO.modificar | Sesión; Business comprueba rol y propiedad |
| GET | `/cuenta/metodos/{idMetodo}` | MetodoPagoBO.consultar | Sesión; Business comprueba rol y propiedad |
| GET | `/cuenta/metodos` | MetodoPagoBO.listar | Sesión; Business comprueba rol y propiedad |
| DELETE | `/cuenta/metodos/{idMetodo}` | MetodoPagoBO.eliminar | Sesión; Business comprueba rol y propiedad |
| PUT | `/cuenta/metodos/{idMetodo}/predeterminado` | MetodoPagoBO.establecerPredeterminado | Sesión; Business comprueba rol y propiedad |
| PUT | `/cuenta/libros-guardados/{idLibro}` | LibroGuardadoBO.guardar | Sesión; Business comprueba rol y propiedad |
| DELETE | `/cuenta/libros-guardados/{idLibro}` | LibroGuardadoBO.retirar | Sesión; Business comprueba rol y propiedad |
| POST | `/cuenta/libros-guardados/busqueda` | LibroGuardadoBO.listar | Sesión; Business comprueba rol y propiedad |
| PUT | `/cuenta/seguimientos/{tipo}/{idReferencia}` | SeguimientoBO.guardar | Sesión; Business comprueba rol y propiedad |
| DELETE | `/cuenta/seguimientos/{tipo}/{idReferencia}` | SeguimientoBO.retirar | Sesión; Business comprueba rol y propiedad |
| POST | `/cuenta/seguimientos/busqueda` | SeguimientoBO.listar | Sesión; Business comprueba rol y propiedad |
| GET | `/carrito` | CarritoBO.consultar | Sesión; Business comprueba rol y propiedad |
| POST | `/carrito/libros/{idLibro}` | CarritoBO.agregar | Sesión; Business comprueba rol y propiedad |
| PUT | `/carrito/libros/{idLibro}` | CarritoBO.cambiarCantidad | Sesión; Business comprueba rol y propiedad |
| DELETE | `/carrito/libros/{idLibro}` | CarritoBO.retirar | Sesión; Business comprueba rol y propiedad |
| DELETE | `/carrito` | CarritoBO.vaciar | Sesión; Business comprueba rol y propiedad |
| GET | `/cuenta/cupones` | CuponBO.listar | Sesión; Business comprueba rol y propiedad |
| POST | `/pedidos/preparacion` | PedidoBO.preparar | Sesión; Business comprueba rol y propiedad |
| GET | `/pedidos/preparacion` | PedidoBO.consultarPreparacion | Sesión; Business comprueba rol y propiedad |
| PUT | `/pedidos/preparacion` | PedidoBO.seleccionar | Sesión; Business comprueba rol y propiedad |
| POST | `/pedidos/confirmacion` | PedidoBO.pagar | Sesión; Business comprueba rol y propiedad |
| POST | `/resenas/libro/{idLibro}/busqueda` | ResenaBO.listarPorLibro | Público; Business comprueba el alcance |
| GET | `/resenas/libro/{idLibro}/valoracion` | ResenaBO.obtenerValoracion | Público; Business comprueba el alcance |
| GET | `/recomendaciones/similares/{idLibro}` | RecomendacionBO.similares | Público; Business comprueba el alcance |
| POST | `/recomendaciones/complementarios` | RecomendacionBO.complementarios | Público; Business comprueba el alcance |

### Ejemplo de consulta

```json
{
  "titulo": null,
  "idsCategorias": [],
  "idsAutores": [],
  "idsEditoriales": [],
  "idiomas": [],
  "precioMinimo": null,
  "precioMaximo": null,
  "soloActivos": true,
  "soloNuevos": false,
  "orden": "MAS_RECIENTES",
  "pagina": 1,
  "tamanioPagina": 12
}
```

Enviar a `POST /libros/busqueda`. RF14 se consulta con el orden `MAS_VENDIDOS`;
Business aplica su período de treinta días. Para Nuevos, usar `soloNuevos: true`.

Para comprar: iniciar sesión, gestionar los recursos propios, modificar carrito,
POST `/pedidos/preparacion`, PUT a la misma ruta con `SeleccionCompraDTO` y
POST `/pedidos/confirmacion`. Ante cambio de importes, consultar la preparación
actualizada y pedir otra confirmación al usuario. Ante rechazo simulado se
conservan preparación y selecciones. Los reintentos conservan las garantías de Business.

## Errores

La respuesta tiene `codigo`, `mensaje` y `campo` cuando corresponde.
JSON-B omite las propiedades nulas.
No se serializan excepciones, SQL ni causas técnicas.

| Estado | Situación |
| --- | --- |
| 400 | JSON, parámetro o validación de campo inválidos |
| 401 | Sin sesión o credenciales incorrectas |
| 403 | Cuenta inactiva, permiso insuficiente o cabecera de cliente ausente |
| 404 | Recurso no encontrado |
| 409 | Conflicto, stock insuficiente o importes que cambiaron antes del pago |
| 422 | Rechazo del pago simulado u otra condición de negocio |
| 500 | Error interno con diagnóstico solo en el registro del servidor |

## Verificación

Las pruebas HTTP usan Jersey 4 en memoria y Business sustituido por mocks.
Comprueban rutas, JSON, fechas, sesión, actor del servidor, privacidad de errores,
parámetros omitidos o inválidos, peticiones sin cuerpo y ausencia de endpoints
fuera de los doce RF. La invalidación concurrente de sesiones y el filtro Servlet
tienen pruebas propias. No sustituyen desplegar el WAR en el servidor elegido, comprobar su
cookie HTTPS ni recorrer el sistema con MySQL real y el frontend.

