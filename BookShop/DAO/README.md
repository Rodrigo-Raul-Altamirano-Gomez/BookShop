# DAO de BookShop

Implementación JDBC basada en las referencias de `../../docs`, las entidades y los
DTO de Model. Cada tabla tiene una interfaz DAO y una implementación con su SQL,
parámetros tipados y mapeos. No hay un ejecutor genérico de consultas ni asignación
de campos con `setObject`.

## Interfaces por tabla

| Tabla | Paquete `me.DAO` | Interfaz | Implementación |
| --- | --- | --- | --- |
| AUTOR | catalogo | AutorDAO | AutorDAOImpl |
| CATEGORIA | catalogo | CategoriaDAO | CategoriaDAOImpl |
| EDITORIAL | catalogo | EditorialDAO | EditorialDAOImpl |
| LIBRO | catalogo | LibroDAO | LibroDAOImpl |
| DESCUENTO | catalogo | DescuentoDAO | DescuentoDAOImpl |
| RESENA | catalogo | ResenaDAO | ResenaDAOImpl |
| VOTO_UTIL | catalogo | VotoUtilDAO | VotoUtilDAOImpl |
| USUARIO | usuario | UsuarioDAO | UsuarioDAOImpl |
| DIRECCION_ENVIO | usuario | DireccionEnvioDAO | DireccionEnvioDAOImpl |
| METODO_PAGO | usuario | MetodoPagoDAO | MetodoPagoDAOImpl |
| LIBRO_GUARDADO | usuario | LibroGuardadoDAO | LibroGuardadoDAOImpl |
| SEGUIMIENTO | usuario | SeguimientoDAO | SeguimientoDAOImpl |
| CUPON | venta | CuponDAO | CuponDAOImpl |
| PEDIDO | venta | PedidoDAO | PedidoDAOImpl |
| ITEM_PEDIDO | venta | ItemPedidoDAO | ItemPedidoDAOImpl |
| SOLICITUD | solicitud | SolicitudDAO | SolicitudDAOImpl |

Las clases con herencia comparten DAO: `MetodoPagoDAO` maneja tarjeta y billetera;
`SeguimientoDAO`, autor, categoría y editorial; `SolicitudDAO`, devolución y reporte
de reseña. El discriminador de la tabla determina el subtipo al leer.

`CrudDAO<T>` reúne insertar, modificar, leer y eliminar cuando esas operaciones
tienen sentido. Pedidos, cupones, items, votos y relaciones usan contratos propios
para conservar su historial y expresar las operaciones permitidas.

## Consultas y persistencia

- Las entidades leídas contienen sus referencias por id. Pedido también recupera
  sus items; las consultas de pantallas construyen los DTO correspondientes.
- Catálogo filtra y ordena antes de paginar, incluido el precio final redondeado.
  Los filtros de varias opciones usan OR dentro del grupo y AND entre grupos.
  Búsquedas literales escapan los comodines de LIKE y los órdenes tienen desempate.
- Favoritos y deseados usan el mismo catálogo con ámbito de usuario y tipo de lista.
  Las relaciones retiradas se recuperan sin crear duplicados.
- Las imágenes se guardan como contenido y MIME. Editar datos conserva la imagen;
  `cambiarImagen` permite sustituirla o retirarla.
- Los pedidos guardan importes, porcentajes, cantidades y datos de pago históricos.
  `ITEM_PEDIDO.porcentaje_descuento` conserva el porcentaje nominal: 19.99 con 15%
  queda en 16.99 y sigue mostrando 15.00%, aunque cambie después el precio del libro.
  Los DTO de items históricos no presentan stock ni disponibilidad actuales.
- Las modificaciones conservan ids, propietarios, referencias y auditoría de alta.
  Las solicitudes solo se editan o retiran pendientes; su resolución y los cambios
  de estado de pedido comprueban el estado anterior en el UPDATE.
- Los descuentos vigentes se consultan por intervalo inclusivo y retiro lógico,
  sin depender de que una tarea haya actualizado previamente `activo`. Las altas
  y cambios bloquean el libro para impedir intervalos superpuestos.
- Los vetos se recalculan sumando el veto directo y las tres referencias. Reactivar
  una referencia conserva los vetos que proceden de las otras.
- Los reportes devuelven el conjunto completo o su máximo opcional. Ventas excluye
  pedidos cancelados y devueltos; actividad incluye usuarios sin compras.
- Recomendaciones obtiene candidatos sin imágenes y valoraciones agregadas.
  `listarTarjetasPorIds` obtiene las tarjetas disponibles en el orden solicitado.
  El algoritmo corresponde a Business. Esta capa prepara los datos de documentos;
  su generación con Jaspersoft Studio se incorporará posteriormente.

## Conexiones y transacciones

Las interfaces no reciben `Connection`. `DBManager.getConnection()` siempre
obtiene una conexión independiente del pool. Las consultas normales y las
escrituras individuales la cierran con try-with-resources; no abren una transacción
del contexto ni ven las escrituras aún no confirmadas de ese contexto.

Los métodos compuestos y las lecturas con bloqueo utilizan
`TransactionContext.getConnection()`. Esta llamada inicia una transacción con
autocommit deshabilitado si todavía no existe, o reutiliza la del mismo hilo.
El DAO cierra sus statements y resultados, pero deja abierta esa conexión.
Business debe llamar `commit()` al completar la operación, `rollback()` si falla y
siempre `close()` al terminar. No hay confirmaciones ni rollbacks dentro del DAO.

Métodos que participan en el contexto y necesitan ese cierre del coordinador:

| DAO | Operaciones |
| --- | --- |
| Todos los que las ofrecen | `leerParaActualizar` / `listarParaActualizar` |
| Autor, Categoria, Editorial | `cambiarActivo`, `eliminar` y recálculo de vetos asociado |
| Libro | `insertar`, `descontarStock`, `reponerStock`, `recalcularVetosPor…` |
| Descuento | `insertar`, `modificar`, `eliminar`, `haySuperposicion` |
| DireccionEnvio | `insertar`, `modificar`, `establecerPredeterminado`, `quitarPredeterminado` |
| MetodoPago | `insertar`, `modificar`, `recuperar`, `listarParaComprobarNumeros`, `establecerPredeterminado`, `quitarPredeterminado` |
| Usuario | `incrementarPedidosEntregados` |
| Pedido | `insertar`, `actualizarEstado`, `incrementarIntentos`, `haCompradoLibroParaActualizar` |
| ItemPedido | `insertar` |
| Cupon | `insertar`, `consumir`, `liberar` |
| Solicitud | `insertar`, `resolver` |
| Resena | `insertarEnTransaccion`, `eliminar` |
| VotoUtil | `cambiarVoto` |

Por ejemplo, registrar un libro requiere confirmar explícitamente:

```java
try {
    libroDAO.insertar(libro);
    TransactionContext.commit();
} catch (SQLException | RuntimeException e) {
    TransactionContext.rollback();
    throw e;
} finally {
    TransactionContext.close();
}
```

El coordinador mantiene en el mismo hilo y transacción los efectos relacionados:

- Compra: bloquear y comprobar datos vigentes, descontar stock, consumir cupón y
  registrar pedido/items. `PedidoDAO.insertar` registra cabecera e items juntos y
  rechaza EN_ESPERA: solo se persisten compras confirmadas.
- Cancelación o devolución: cambiar el estado, reponer stock y liberar el cupón
  vigente. La resolución de la devolución forma parte de esa operación.
- Primera entrega: cambiar el estado y, solo si cambió una fila, aumentar el
  contador de entregados y emitir el cupón correspondiente.
- Solicitud de devolución: bloquear pedido, comprobar elegibilidad y ausencia de
  pendiente, incrementar intentos y registrar solicitud. Editarla o retirarla no
  altera ese contador.
- Moderación: resolver el reporte y, cuando corresponda, retirar la reseña.
- Voto: modificar la relación y el contador juntos; repetir la misma elección no
  vuelve a sumar.
- Dirección o método predeterminado: bloquear propietario y comprobar el candidato
  antes de quitar el anterior. El alta no lo selecciona automáticamente; si el
  usuario lo eligió, alta y selección se confirman juntas.
- Método de pago: comparar números descifrados bajo bloqueo y guardar solo el
  cifrado, conservando identidad y subtipo al recuperar uno retirado.

Los ids asignados al insertar son provisionales hasta el commit. Después de un
rollback, Business descarta los objetos de esa operación.

## Responsabilidades de Business

Business valida permisos y propiedad, formatos, hash de contraseña, cifrado,
enmascarado, importes, elegibilidad, plazos, simulaciones y algoritmos. Las entidades
de métodos de pago contienen el cifrado para uso interno; Business produce los DTO
con el número enmascarado. Las consultas administrativas con idUsuario cero
requieren autorización previa.

`actualizarVigencias` y `desactivarVencidos` son escrituras explícitas para los
eventos pertinentes. Las lecturas no actualizan registros. Vigencia y consumo del
cupón son independientes: consumir conserva activo y marca consumido; liberar
requiere consumo previo, vigencia y ausencia de otro pedido que lo esté usando.

## Esquema y verificación

`src/main/resources/DDL.sql` crea el esquema de MySQL e incluye restricciones,
auditoría, relaciones, imágenes y los tres discriminadores de herencia. También
incluye el porcentaje nominal histórico de los items. Recrea tablas: no es una
migración y no se ejecutó sobre la base del proyecto. Una base ya creada debe
adaptarse al esquema antes de utilizar estos DAO. `registers.sql` conserva sus
datos antiguos y no debe usarse con este DDL sin adaptarlos primero.

Desde BookShop, con Maven y un JDK 21 o posterior:

```powershell
mvn -pl DAO -am clean test
```

En este equipo no hay Maven disponible. Se compiló desde las fuentes de Model,
DBManager y DAO con `--release 21`, usando las dependencias de la caché local:

```powershell
./scripts/pruebas-dao.ps1
# También admite -Jdk 'C:/ruta/al/jdk' y -RepositorioMaven 'C:/ruta/al/repository'.
```

El script usa una carpeta nueva en target por ejecución para evitar clases
compiladas antiguas y guarda `resultado.txt`. Al completar DAO pasaron **154 de
154 pruebas**, sin omisiones. Business añadió comprobaciones sobre estos contratos;
la verificación conjunta se documenta en `../Business/README.md`. Incluye mapeos, filtros,
paginación, historial de precios, relaciones, vigencias, vetos, predeterminados,
auditoría, estados, rollbacks y liberación de recursos.

Las pruebas usan H2 en modo MySQL y un driver simulado para DBManager, sin las
credenciales de la base real. H2 no reproduce todas las restricciones ni los
bloqueos de MySQL; queda validar el DDL y la concurrencia en una instancia MySQL
aislada. Business ya está implementado y registrado en el reactor Maven, con sus
contratos en `../Business/README.md`. App se incorpora explícitamente con el perfil
`aplicacion` cuando se adapte a Business. El frontend continúa en su fase.
