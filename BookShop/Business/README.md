# Business de BookShop

Reglas de negocio basadas en Funcionalidades_acordadas.txt,
Requisitos_detallados.txt, Requirements_completo.txt, DisenoFrontend.md y
Obligaciones_implementacion.txt de `../../docs`.

Cada servicio tiene una interfaz BO y su BOImpl. Las interfaces reciben datos de
formularios y devuelven DTO, ids o resultados simples. No reciben conexiones ni
declaran SQLException. Las implementaciones usan directamente los DAO; no hay
un framework de servicios, CRUD genérico ni ejecutor de operaciones.

## Organización

| Paquete | Servicios |
| --- | --- |
| usuario | Usuario, DireccionEnvio, MetodoPago, LibroGuardado, Seguimiento |
| catalogo | Autor, Categoria, Editorial, Libro, Descuento |
| opinion | Resena, VotoUtil |
| compra | Carrito, Pedido, Cupon |
| solicitud | Solicitud, con devolución y reporte de reseña |
| configuracion | Configuracion |
| recomendacion | Recomendacion y las cuatro lógicas de cálculo |
| documentos | Reporte y Comprobante: preparación de datos |

Son 20 interfaces con sus 20 implementaciones. Los auxiliares generales
`Validaciones`, `Autorizacion` y `ProteccionDatos` están en `me.Business`.
`exceptions` reúne las excepciones que se manejan desde la siguiente capa.
Los auxiliares propios de un dominio permanecen junto a sus BO.

## Uso de los contratos

Los métodos privados o administrativos reciben `idActor` como primer argumento.
Este id debe proceder de la sesión autenticada del servidor; no se toma de un
campo que pueda elegir el navegador. Autorizacion comprueba cuenta activa, rol y
propiedad. En las consultas públicas que admiten actor, cero representa visitante.

```java
UsuarioBO usuarios = new UsuarioBOImpl();
AccesoUsuarioDTO acceso = usuarios.iniciarSesion(credenciales);
// Si requiereReactivacion es true, todavía no se habilitan operaciones privadas.
PerfilUsuarioDTO perfil = usuarios.obtenerPerfil(idDeLaSesion);
```

El servidor establece y cierra la sesión usando el resultado de acceso. Registra
el tiempo de sesión mediante cerrarSesion antes de invalidarla; si desactiva la
cuenta, registra ese tiempo antes de desactivar y después cierra su sesión.
Business valida credenciales y disponibilidad, sin depender de una tecnología web.

Los constructores normales crean los DAO correspondientes. Los constructores que
reciben DAO y Clock permiten probar los mismos casos con datos aislados y fechas
controladas. El reloj siempre se normaliza a Lima. Las probabilidades de pago y
cancelación se mantienen internas; DoubleSupplier permite sustituir el azar en
las pruebas, sin añadirlo a la configuración administrativa.

## Excepciones

Todas derivan de NegocioException y son checked. El consumidor puede tratar las
clases específicas y usar NegocioException como tratamiento común.

| Excepción | Situación |
| --- | --- |
| ValidacionException | Campo obligatorio, formato, importe, fecha, imagen o paginación inválidos; getCampo identifica el campo |
| CredencialesInvalidasException | Contraseña o credenciales incorrectas |
| CuentaInactivaException | Cuenta desactivada |
| AccesoDenegadoException | Sesión, rol, propiedad o elegibilidad insuficientes |
| RecursoNoEncontradoException | Recurso ausente o retirado |
| ConflictoException | Duplicado, estado incompatible o cambio concurrente |
| StockInsuficienteException | Cantidad que supera el stock disponible |
| PagoRechazadoException | Rechazo de la simulación del pago |
| CondicionesCompraCambiaronException | Importes nuevos que se deben revisar antes de volver a pagar |
| FueraPlazoException / LimiteIntentosException | Devolución no elegible por plazo o intentos |
| PersistenciaException | Fallo de acceso a datos, con causa técnica para diagnóstico interno |
| ProteccionDatosException | No se puede proteger o leer un dato cifrado |
| ConfiguracionException | Archivo de parámetros o clave inválido/inaccesible |

La siguiente capa muestra el mensaje de negocio y los campos correspondientes.
La causa y el stack trace son para el diagnóstico interno; no se serializan al
navegador. Los errores SQL se traducen dentro de Business.

## Cuenta y datos personales

Correo normalizado, contraseña de al menos ocho caracteres con confirmación,
dominio exacto admin.bookshop para administrador y reactivación de la misma cuenta.
Editar perfil conserva correo/rol; cambiar contraseña y desactivar comprueban la
contraseña actual. Imágenes JPEG/PNG de hasta 5 MiB se comprueban por contenido.

Direcciones y métodos respetan propietario, identidad, retiro lógico y un único
predeterminado válido. Desmarcar el elegido no selecciona un sustituto. Edición
y selección de predeterminado se confirman juntas. Tarjetas tienen 16 dígitos;
celulares, nueve y comienzo por 9. Los separadores de presentación se normalizan.
La unicidad compara números completos descifrados; billetera añade proveedor sin
distinguir mayúsculas. Registrar de nuevo un método propio retirado recupera su
id. Billetera vencida se renueva 365 días; editar una vigente conserva vencimiento.

ProteccionDatos concentra PBKDF2 con sal, AES-GCM y la máscara de últimos cuatro
dígitos. Son clases de la biblioteca estándar necesarias para proteger los datos,
con imports explícitos y comentarios locales. La clave se crea en el servidor al
usar el cifrado por primera vez y se conserva; no aparece en DTO ni en la base.

Ubicación por defecto: `config/clave-datos.key`, relativa al directorio de inicio
del servidor. Puede fijarse una ruta absoluta con `-Dbookshop.clave.file=...`.
El archivo debe conservarse con la base para poder descifrar los métodos existentes;
una clave inválida produce una excepción y nunca se reemplaza silenciosamente.
El nombre del archivo está excluido en .gitignore. Los datos de registers.sql
pertenecen al esquema/formato antiguo y no son fixtures de Business.

## Compra y solicitudes

Carrito y preparación son temporales por actor. Consultar y preparar no guarda
pedido/items, reserva stock ni mantiene una transacción mientras se navega.
Modificar el carrito descarta la preparación. Las líneas problemáticas siguen
visibles con su disponibilidad para poder corregirlas.

Los constructores normales de CarritoBOImpl y PedidoBOImpl comparten la memoria.
En pruebas o integración con dependencias inyectadas se pasa la misma instancia
de CarritoBOImpl a PedidoBOImpl. Los BO se mantienen durante el proceso, y el
monitor del estado del actor serializa cambios de carrito y pago.

Pago revalida cuenta, recursos propios, stock, descuentos, cupón e importes; la
simulación tiene 90% de éxito. El
rechazo simulado conserva preparación y selecciones. Si cambian importes, actualiza
el resumen y lanza CondicionesCompraCambiaronException antes de cobrar. El éxito
confirma stock, cupón, cabecera e items en una transacción y luego vacía el carrito.
Un reintento sobre esa preparación devuelve el pedido ya confirmado, también si
falló construir la respuesta después del commit. Ese recuerdo pertenece al estado
en memoria del proceso; no constituye un token de pago persistido entre reinicios.

Seguimiento avanza manualmente una etapa: salida tras 30 segundos del pago y
entrega tras 60 de la salida. Consultar no avanza. Primera entrega, contador y
eventual cupón se confirman juntos y no se repiten. Cancelación elegible usa 80%
de éxito y repone/libera beneficios una vez; su motivo no se persiste.

Solicitudes comprueban condiciones antes del formulario y de nuevo al enviar.
Una nueva devolución válida consume un intento en la misma transacción que su
alta; error revierte ambos. Editar, retirar o resolver no suma ni devuelve intentos.
Resolver una pendiente válida no vuelve a exigir el plazo/configuración del alta.
Aceptar devolución conserva importes y repone stock/cupón; aceptar reporte retira
la reseña junto con la resolución. Las consultas de referencias no habilitan
operaciones ni generación de comprobante.

La publicación de reseña bloquea la compra válida para coordinarse con cancelación
o devolución. Una retirada se sustituye por una nueva publicación comprobando de
nuevo compra y ausencia de reseña activa. Editar una existente no exige mantener
la compra. Votos y sus contadores cambian juntos, sin duplicados.

## Configuración y recomendaciones

ConfiguracionBO carga los siete valores de `config/negocio.properties`, o utiliza
los iniciales si no existe. La ruta puede fijarse con `-Dbookshop.config.file=...`.
Valida todos antes de guardar y sustituye el archivo mediante un temporal en el
mismo directorio. Archivo inválido o fallo de escritura se informa sin reemplazar
la configuración anterior. Las instancias consultan los datos guardados vigentes.

Valores iniciales: envío 5%, devolución 30 días y 3 intentos, premio por cada 5
primeras entregas, cupón 10%, mínimo S/ 100.00 y vigencia de 30 días. Pedidos y
cupones anteriores conservan sus propios porcentajes, importes y condiciones.

Las cuatro lógicas se mantienen en clases cortas del dominio:

- Similitud: coincidencias de categoría, autor, editorial, proveedor y saga,
  coincidencia de palabras de título/sinopsis y cercanía de precio.
- Complementarios: confianza y lift por pedidos, sin convertir ejemplares en
  transacciones distintas; excluye los libros del carrito.
- Para ti: Jaccard entre conjuntos comprados, con tres pedidos válidos mínimos;
  excluye los ya comprados y restringe la consulta a su propietario.
- Popularidad: ventas de 90 días, recencia y valoración bayesiana. Pesos internos
  60%, 15% y 25%; cinco reseñas de referencia para moderar muestras pequeñas.

Los coeficientes de contenido son internos: categoría 4, autor 3, editorial 2,
proveedor 1, saga 3, palabras hasta 3 y precio hasta 1. No añaden parámetros al
formulario de configuración. Popularidad también respalda resultados insuficientes
de asociación y filtrado colaborativo.

abrirPopulares/abrirParaTi devuelven un idConsulta. consultar reutiliza ese conjunto
para ordenar todo antes de paginar. Refresca sus atributos y precios sin recalcular
el algoritmo ni añadir libros nuevos; las imágenes se cargan solo para la página.
El consumidor conserva el id mientras la vista está abierta y llama descartar al
cerrarla. Reabrir crea otro conjunto. Comparación recibe dos libros distintos y
devuelve sus detalles actuales; la elección del primer libro se conserva en sesión.

## Datos para documentos y verificación

ReporteBO prepara ReporteVentasLibrosDTO y ReporteUsuariosDTO con autorización,
período completo, límite global opcional y fecha de generación en Lima.
ComprobanteBO prepara el detalle histórico e identidad del comprador sin foto;
el método ya está enmascarado. **No generan archivos ni añaden bibliotecas PDF**.
La generación se implementará con Jaspersoft Studio en la fase indicada por el
usuario. Preparar esos datos no repite ni confirma el pago.

Desde BookShop:

```powershell
mvn -pl Business -am clean test
```

En el equipo de esta implementación se usó la alternativa sin Maven, con las
dependencias ya presentes en la caché y compilación desde fuentes --release 21:

```powershell
./scripts/pruebas-backend.ps1
```

La ejecución conjunta pasó **251/251 pruebas** de Model, DBManager, DAO y Business.
Incluye permisos, fechas Lima, validaciones, protección de datos, relaciones,
rollback, importes, reintentos, estados, premios, intentos, recomendaciones y
configuración conservada tras reinicio. H2 y Mockito aíslan las pruebas; no se
usó la base real ni se ejecutó el DDL. MySQL real, integración HTTP/Blazor, tiempos
en el entorno completo y generación de documentos pertenecen a sus verificaciones
posteriores. App conserva su demostración antigua y sigue fuera del reactor normal.
