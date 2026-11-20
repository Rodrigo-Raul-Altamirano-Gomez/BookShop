# Especificación de Requisitos Funcionales — BookShop

**Curso:** 1INF30 Programación 3 (2026-2) · **Tipo:** Tarea académica · **Base:** `docs/Requisitos.docx`

---

## 1. Introducción

### 1.1 Propósito

Este documento precisa los 19 requisitos funcionales de `Requisitos.docx`. Conserva sus capacidades y resuelve lo ambiguo: qué datos se capturan y muestran, cómo se calcula cada cosa y qué parámetros controlan el comportamiento.

### 1.2 Criterio de redacción

- Se mantienen los **códigos, nombres, módulos y prioridades** del documento original.
- Cada capacidad se especifica en su **forma más simple**: solo el flujo normal, sin casos excepcionales ni mecanismos de robustez. La aplicación es una demostración académica mínima y funcional.
- Los atributos mostrados y los órdenes disponibles se ajustaron a los importantes.
- Los conceptos que usan varios requisitos se definen una sola vez (sección 2) y luego se referencian.
- Las decisiones tomadas ante ambigüedades se listan en la sección 7.

### 1.3 Actores

| Actor | Descripción |
|---|---|
| **Visitante** | Sin sesión. Ve el inicio, el catálogo, el detalle de libros, las páginas de autores, categorías y editoriales, y las recomendaciones generales. Puede registrarse e iniciar sesión. |
| **Cliente** | Usuario con correo de un dominio distinto de `@admin.bookshop`. Compra y gestiona su perfil, pedidos, listas, seguimientos y reseñas. |
| **Administrador** | Usuario con correo `@admin.bookshop`. Gestiona catálogo, moderación, configuración y reportes. No compra. |

---

## 2. Conceptos comunes

### 2.1 Modelo de datos

Estas entidades se guardan en la base de datos.

| Entidad | Atributos |
|---|---|
| **Usuario** | id, correo (único), nombreCompleto, fechaNacimiento, contrasena, foto (opcional), rol (`CLIENTE`/`ADMINISTRADOR`), activo, fechaRegistro |
| **Direccion** | id, usuario, alias, direccion, distrito, ciudad, predeterminada, activa |
| **MetodoPago** | id, usuario, tipo (`TARJETA`/`BILLETERA`), titular, numero, fechaVencimiento (solo tarjeta), proveedor (solo billetera: Yape, Plin), predeterminado, activo |
| **Autor** | id, nombre, biografia, imagen, activo |
| **Categoria** | id, nombre, descripcion, imagen, activo |
| **Editorial** | id, nombre, descripcion, imagen, activo |
| **Libro** | id, isbn, titulo, sinopsis, idioma, paginas, anioPublicacion, precio, stock, portada, activo, fechaRegistro, autor, categoria, editorial |
| **Descuento** | id, libro, porcentaje, fechaInicio, fechaFin |
| **Pedido** | id, usuario, direccion, metodoPago, cupon (opcional), estado, fechaCompra, fechaSalida, fechaEntrega, fechaCancelacion, fechaDevolucion, codigoTransaccion, codigoSeguimiento, subtotal, descuentoCupon, costoEnvio, total, contadoParaCupon |
| **DetallePedido** | id, pedido, libro, cantidad, precioUnitario, descuentoUnitario, subtotal |
| **Cupon** | id, usuario, codigo, porcentaje, fechaEmision, fechaVencimiento, activo |
| **SolicitudDevolucion** | id, pedido, motivo, comentario, estado, fecha |
| **Resena** | id, usuario, libro, puntuacion (1–5), comentario, fecha, activa |
| **VotoUtil** | usuario, resena |
| **ReporteResena** | id, usuario, resena, motivo, comentario, contenidoReportado, estado, fecha |
| **SeguimientoEntidad** | usuario, tipo (`AUTOR`/`CATEGORIA`/`EDITORIAL`), idEntidad, fecha |
| **ListaLibro** | usuario, libro, lista (`DESEADOS`/`FAVORITOS`) |
| **Configuracion** | parámetros de la sección 3.1 |

El **carrito** y la **selección de comparación** no se guardan en la base de datos: viven en la memoria de la sesión del usuario y se pierden al cerrarla.

### 2.2 Estados

| Elemento | Estados |
|---|---|
| Pedido | `EN_ALMACEN` → `EN_CAMINO` → `ENTREGADO`; además `CANCELADO` y `DEVUELTO` |
| Solicitud de devolución y reporte de reseña | `PENDIENTE`, `ACEPTADA`, `RECHAZADA`, `RETIRADA` |

El cupón no tiene estado: es **utilizable** si está `activo` y su fecha de vencimiento no ha pasado. Al usarse en una compra se desactiva.

### 2.3 Definiciones

| Concepto | Definición |
|---|---|
| **Precio final** | `precio × (1 − porcentaje del descuento vigente / 100)`. Un descuento está vigente si la fecha actual está entre su fechaInicio y su fechaFin. Sin descuento vigente, el precio final es igual al precio. |
| **Venta válida** | Unidades de un detalle cuyo pedido no está `CANCELADO` ni `DEVUELTO`. Recomendaciones y reportes cuentan solo ventas válidas. |
| **Período** | Fecha de inicio y fecha de fin, ambas inclusivas y opcionales; sin período se usa todo el historial. Regla general: **en lo que se calcula con ventas** (recomendaciones Complementarios, Para ti, Más populares y Más vendidos, y reportes RF18 y RF19) delimita las ventas, pedidos y reseñas que se consideran; **en lo demás** (catálogo, búsqueda, páginas de autor, categoría y editorial, listas personales y Similares) filtra los libros por su fecha de registro. |
| **Vista de resultados** | Lista de libros en tarjetas, con la búsqueda y los filtros de RF08, dividida en páginas de 12. Las vistas comunes permiten además elegir el orden. Las vistas de las secciones de libros del inicio (Novedades y las recomendaciones) no, porque siempre muestran el orden propio de la sección. Los criterios se aplican sobre todo el conjunto antes de dividirlo, y cambiar cualquiera vuelve a la página 1. |

---

## 3. Parámetros

### 3.1 Configurables por el Administrador (RF04)

Se guardan en la base de datos y se conservan al reiniciar.

| Parámetro | Uso | Valor inicial |
|---|---|---|
| costoEnvio | Costo fijo de envío por pedido | S/ 10.00 |
| diasDevolucion | Días después de la entrega en que se puede solicitar una devolución | 7 |
| maxSolicitudesDevolucion | Solicitudes de devolución permitidas por pedido | 2 |
| maxCancelacionesMes | Cancelaciones permitidas a cada cliente por mes calendario | 3 |
| cuponMontoMinimo | Total mínimo de un pedido para que cuente para cupón | S/ 100.00 |
| cuponCadaPedidos | Pedidos que cuentan para cupón que debe acumular un cliente para recibir uno | 3 |
| cuponPorcentaje | Descuento del cupón emitido | 10 % |
| cuponDiasVigencia | Días de vigencia del cupón emitido | 30 |

### 3.2 Constantes del sistema

Definidas en el código; no se editan desde la aplicación.

| Constante | Valor |
|---|---|
| Probabilidad de rechazo del pago simulado | 20 % |
| Tiempo de cada cambio de estado del seguimiento (`EN_ALMACEN` → `EN_CAMINO` y `EN_CAMINO` → `ENTREGADO`) | Aleatorio, entre 1 y 2 minutos |
| Libros por página en una vista de resultados | 12 |
| Libros por sección en la página de inicio | 8 |
| Historial suficiente para *Para ti* | Ventas válidas de al menos 3 libros distintos |

Los tiempos de seguimiento son de minutos para poder demostrar el flujo en clase.

---

## 4. Requisitos funcionales

### 4.1 Resumen

| Código | Requisito | Módulo | Prioridad |
|---|---|---|---|
| RF01 | Registro y autenticación de usuarios | Usuario | Alta |
| RF02 | Gestión de perfil | Usuario | Media |
| RF03 | Historial y seguimiento de pedidos | Usuario | Alta |
| RF04 | Gestión administrativa del catálogo | Usuario | Alta |
| RF05 | Búsqueda y seguimiento de autores, categorías y editoriales | Usuario | Media |
| RF06 | Libros deseados y favoritos | Usuario | Media |
| RF07 | Visualización del catálogo | Catálogo | Alta |
| RF08 | Búsqueda y filtrado de productos | Catálogo | Media |
| RF09 | Detalle de producto | Catálogo | Alta |
| RF10 | Reseñas y comentarios | Catálogo | Media |
| RF11 | Carrito de compras | Carrito | Alta |
| RF12 | Sugerencia de productos relacionados | Sugerencias | Media |
| RF13 | Recomendaciones personalizadas | Sugerencias | Alta |
| RF14 | Más vendidos | Sugerencias | Media |
| RF15 | Simulación de pago | Pago | Alta |
| RF16 | Confirmación transaccional y registro de pedido | Pago | Alta |
| RF17 | Comprobante de compra | Reportes | Alta |
| RF18 | Reporte de libros por monto vendido | Reportes | Media |
| RF19 | Reporte de usuarios por monto gastado | Reportes | Media |

---

### 4.2 Módulo de Usuario

#### RF01 — Registro y autenticación de usuarios

**Registro**

1. El visitante ingresa: correo, nombre completo, fecha de nacimiento, contraseña, confirmación de contraseña y, opcionalmente, una foto.
2. El sistema valida que el correo no esté registrado y que la contraseña y su confirmación coincidan. Si alguna validación falla, muestra el mensaje.
3. El rol se asigna por el correo: `@admin.bookshop` → Administrador; cualquier otro → Cliente.
4. Al registrarse, el usuario queda con la sesión iniciada.

**Inicio y cierre de sesión**

5. El usuario ingresa correo y contraseña. Si no coinciden, se muestra "Correo o contraseña incorrectos".
6. Si la cuenta está desactivada (RF02) y la contraseña es correcta, el sistema ofrece reactivarla. Al aceptar, la cuenta se activa con todo su historial y se inicia sesión.
7. El usuario puede cerrar sesión desde el encabezado.

#### RF02 — Gestión de perfil

**Datos personales**

1. El usuario consulta su perfil: correo, nombre completo, fecha de nacimiento, foto y fecha de registro. Los datos se muestran primero en modo consulta.
2. Desde el perfil, con el botón de edición, puede editar el nombre completo y la fecha de nacimiento. El correo es solo de consulta.
3. Puede cambiar su foto o quitarla.
4. Desde el mismo perfil puede cambiar su contraseña ingresando la actual, la nueva y la confirmación.
5. Puede desactivar su cuenta tras confirmar. La sesión se cierra y se conserva todo su historial.

**Direcciones** (Cliente)

6. Registra, edita y elimina direcciones con: alias (por ejemplo "Casa"), dirección, distrito y ciudad.
7. Marca una como predeterminada; solo una lo es a la vez.

**Métodos de pago** (Cliente)

8. Registra, edita y elimina métodos de pago de dos tipos:
   - **Tarjeta:** titular, número (16 dígitos) y fecha de vencimiento (MM/AA).
   - **Billetera digital:** proveedor (Yape o Plin), titular y celular (9 dígitos).
9. Un cliente no puede registrar dos métodos con el mismo número.
10. El número se muestra enmascarado, con solo los 4 últimos dígitos visibles (`**** **** **** 1234`).
11. Marca uno como predeterminado.

**Cupones** (Cliente)

12. Consulta sus cupones: código, porcentaje, fecha de vencimiento y si es utilizable (sección 2.2).

**General**

13. Eliminar una dirección o un método de pago lo desactiva: deja de ofrecerse al pagar, pero los pedidos que lo usaron lo siguen mostrando.
14. En cualquier formulario del perfil, *Cancelar* vuelve a la vista anterior sin guardar cambios.

#### RF03 — Historial y seguimiento de pedidos

**Historial**

1. El cliente ve sus pedidos con: portadas de sus libros, número, fecha de compra, cantidad de libros, total y estado.
2. Puede filtrarlos por estado y ordenarlos por fecha o por total, en orden ascendente o descendente.
3. En el detalle de un pedido ve:
   - libros (título, cantidad, precio unitario, descuento y subtotal);
   - dirección, método de pago enmascarado y cupón usado;
   - importes: subtotal, descuento del cupón, envío y total;
   - el seguimiento: código de seguimiento y las fechas de compra, salida, entrega, cancelación o devolución que ya ocurrieron.

**Seguimiento simulado y cupones**

4. Un hilo del sistema se ejecuta cada minuto y hace dos cosas:
   - **Avanza el seguimiento.** Pasa los pedidos `EN_ALMACEN` a `EN_CAMINO` (y guarda `fechaSalida`), y los `EN_CAMINO` a `ENTREGADO` (y guarda `fechaEntrega`), cuando se cumple el tiempo aleatorio de la sección 3.2. La regla es la siguiente, desde el último cambio:
     - si pasó menos de 1 minuto, el pedido no cambia;
     - si pasaron entre 1 y 2 minutos, cambia con un 50 % de probabilidad en cada revisión;
     - si pasaron 2 minutos, cambia siempre.
   - **Emite cupones.** Un pedido cuenta para cupón cuando cumple todo lo siguiente:
     - lleva `ENTREGADO` más de `diasDevolucion` días;
     - no tiene una devolución pendiente ni aceptada;
     - su total es mayor o igual que `cuponMontoMinimo`.

     El hilo marca con `contadoParaCupon = true` cada pedido que cuenta y aún no estaba marcado. Cada vez que los pedidos marcados de un cliente llegan a un múltiplo de `cuponCadaPedidos`, le emite un cupón activo con `cuponPorcentaje`, que vence a los `cuponDiasVigencia` días.

**Cancelación**

5. El cliente puede cancelar un pedido `EN_ALMACEN` siempre que no haya alcanzado `maxCancelacionesMes` cancelaciones en el mes calendario. La cancelación es inmediata: el pedido pasa a `CANCELADO`, se guarda `fechaCancelacion` y se repone el stock.

**Devolución**

6. El cliente puede solicitar la devolución completa de un pedido `ENTREGADO` dentro de los `diasDevolucion` días posteriores a la entrega. Indica un motivo (producto dañado, producto equivocado, ya no lo quiero u otro) y un comentario.
7. Cada pedido admite hasta `maxSolicitudesDevolucion` solicitudes, contando las rechazadas y las retiradas, y solo una `PENDIENTE` a la vez.
8. Si el Administrador acepta la solicitud (RF04), el pedido pasa a `DEVUELTO`, se guarda `fechaDevolucion` y se repone el stock.

**Mis solicitudes**

9. Reúne, en dos secciones separadas, las solicitudes de devolución y los reportes de reseñas (RF10) del usuario:
   - **Devoluciones:** pedido (portadas y número), fecha, motivo, comentario y estado;
   - **Reportes de reseñas:** libro, contenido de la reseña reportada, fecha, motivo, comentario y estado.
10. Solo las solicitudes `PENDIENTE` se pueden editar (motivo y comentario) o retirar; al retirarlas pasan a `RETIRADA`.

**Importes históricos**

11. Un pedido cancelado o devuelto conserva sus importes originales.

#### RF04 — Gestión administrativa del catálogo de productos

**Catálogo**

1. El Administrador lista, consulta, registra y edita libros, autores, categorías y editoriales, con sus imágenes.
2. Activa o desactiva cada uno. Lo inactivo no se muestra a clientes ni a visitantes, pero se conserva en los pedidos, listas y reseñas existentes.
3. Al registrar un libro elige autor, categoría y editorial entre los activos. Estas referencias no se pueden cambiar después.
4. Actualiza el precio y el stock de cada libro.

**Descuentos**

5. Registra y elimina descuentos de un libro: porcentaje, fecha de inicio y fecha de fin.
6. Los descuentos de un mismo libro no pueden superponerse en fechas.

**Moderación**

7. Ve las solicitudes de devolución pendientes con: número de pedido, cliente, fecha de entrega, total, motivo y comentario. Acepta o rechaza cada una (efecto en RF03).
8. Ve los reportes de reseñas pendientes con: libro, autor de la reseña, contenido reportado, motivo y comentario.
   - **Aceptar** oculta la reseña.
   - **Rechazar** la deja visible.
9. Ve todas las reseñas en el detalle de cada libro, incluidas las ocultas, y puede ocultar o volver a mostrar cualquiera por su cuenta.

**Configuración**

10. Consulta y edita los parámetros de la sección 3.1. Los cambios rigen solo para las operaciones posteriores; los pedidos, cupones y solicitudes ya registrados no cambian.

**Reportes**

11. Genera los reportes RF18 y RF19 desde su panel.

#### RF05 — Búsqueda y seguimiento de autores, categorías y editoriales

1. El usuario busca autores, categorías y editoriales por nombre (coincidencia parcial) y puede ordenar los resultados por nombre, ascendente o descendente. Los resultados se muestran en tarjetas con imagen y nombre, en páginas.
2. Cada autor, categoría y editorial tiene su página con imagen, nombre, descripción (biografía en el caso del autor) y sus libros en una vista de resultados.
3. Desde esa página, el cliente puede seguirlo o dejar de seguirlo.
4. En *Mis seguimientos*, el cliente ve lo que sigue en una sección por tipo (autores, categorías y editoriales). En cada sección puede buscar por nombre y ordenar por nombre (ascendente o descendente) o por fecha de seguimiento. Los resultados se muestran en páginas.

#### RF06 — Gestión y descubrimiento de libros deseados y favoritos

1. Desde el detalle del libro, el cliente lo agrega o lo quita de **Deseados** y de **Favoritos**. Las dos listas son independientes.
2. Un libro puede estar en ambas listas, pero solo una vez en cada una.
3. Las dos listas se consultan en una misma pantalla, con una sección para cada una. Cada sección es una vista de resultados.
4. Las listas son privadas.

---

### 4.3 Módulo de Catálogo de Productos

#### RF07 — Visualización del catálogo de productos

**Tarjetas**

1. Tarjeta de libro: portada, título, autor y precio final. Si tiene descuento, muestra también el precio original tachado y el porcentaje. Si su stock es 0, muestra "Agotado".
2. Tarjeta de autor, categoría o editorial: imagen y nombre.

**Página de inicio**

3. Tiene cuatro secciones de 8 tarjetas, cada una con un enlace *Ver todos* que abre su vista de resultados:
   - *Novedades*: los libros ordenados por fecha de registro, del más reciente al más antiguo. Su vista no tiene selector de orden (RF08).
   - *Más vendidos* (RF14).
   - *Más populares* (RF13).
   - *Para ti* (RF13): solo para clientes con historial suficiente.

   Además tiene tres secciones de 8 tarjetas: *Categorías*, *Autores* y *Editoriales*. Cada una muestra simplemente los primeros 8 que recupera, sin un criterio propio. Al pulsar una tarjeta se abre su página (RF05), y el enlace *Ver todos* abre la búsqueda de ese tipo (RF05) con todos sus resultados.

**Encabezado**

4. Contiene el buscador de libros, el acceso al carrito y el menú de cuenta. Las opciones dependen del rol:
   - **Visitante:** *Iniciar sesión* y *Registrarse*.
   - **Cliente:** perfil, pedidos, mis solicitudes, mis reseñas, mis listas (deseados y favoritos), seguimientos, cupones y cerrar sesión.
   - **Administrador:** panel de administración y cerrar sesión. No tiene carrito.
5. El visitante ve las mismas páginas y botones que el cliente. Si pulsa una acción que requiere cuenta, el sistema lo lleva a iniciar sesión. Esas acciones son: abrir el carrito, agregar al carrito, agregar a deseados o favoritos, seguir, y publicar, votar o reportar reseñas. Comparar libros no requiere cuenta (RF09).

**Catálogo**

6. El catálogo es la vista de resultados de todos los libros activos.

#### RF08 — Búsqueda y filtrado de productos

Define los criterios de toda vista de resultados (sección 2.3).

1. **Búsqueda:** por título, con coincidencia parcial y sin distinguir mayúsculas.
2. **Filtros**, combinables entre sí:
   - categorías: una o más;
   - autores: uno o más;
   - editoriales: una o más;
   - idioma;
   - rango de precio final (mínimo y máximo);
   - período (sección 2.3).

   Dentro de un mismo filtro, los valores elegidos se suman: un libro aparece si cumple cualquiera de ellos (por ejemplo, Novela **o** Fantasía). Entre filtros distintos se deben cumplir todos (por ejemplo, Novela o Fantasía, **y** de la editorial Lumbre).
3. **Órdenes**, ascendentes o descendentes y solo en las vistas comunes:
   - precio final;
   - título.

   Las vistas de las secciones de libros del inicio no tienen selector de orden: Novedades se ordena por fecha de registro (RF07) y las recomendaciones (RF12–RF14) por su algoritmo. Por eso no hay un orden por fecha de registro ni por ventas: ya tienen su propia sección.
4. Si no hay resultados, se muestra "No se encontraron libros".

#### RF09 — Visualización de detalle de producto

**Contenido**

1. Muestra:
   - portada, título, sinopsis;
   - autor, categoría y editorial, con enlace a sus páginas;
   - precio final, y precio original y porcentaje si hay descuento;
   - disponibilidad: stock, o "Agotado".
2. Ficha técnica: ISBN, idioma, páginas y año de publicación.
3. Reseñas (RF10), con la puntuación promedio y la cantidad de reseñas.
4. Libros similares (RF12).

**Acciones del cliente**

5. Elegir una cantidad, de 1 hasta el stock, y agregar el libro al carrito.
6. Agregarlo o quitarlo de Deseados y de Favoritos (RF06).

**Comparación**

7. Cualquier usuario puede pulsar *Comparar* en dos libros distintos.
8. La página de comparación los muestra lado a lado: portada, título, autor, categoría, editorial, idioma, páginas, año, precio final, puntuación promedio y disponibilidad.
9. Al elegir un tercer libro, el usuario indica cuál de los dos reemplaza; el otro se conserva.
10. La selección vive en la sesión (sección 2.1).

#### RF10 — Reseñas y comentarios en productos

**Publicación**

1. Un cliente que tiene el libro en un pedido `ENTREGADO` puede publicar una reseña sobre él, con una puntuación de 1 a 5 estrellas y un comentario.
2. Tiene una reseña por libro como máximo, y puede editarla o eliminarla.
3. En *Mis reseñas* ve sus reseñas con el libro, la puntuación y la fecha.

**Visualización y votos**

4. Cada reseña muestra su autor, puntuación, comentario, fecha y la cantidad de votos útiles.
5. Un cliente puede marcar como útil una reseña ajena, una sola vez. Si vuelve a pulsar, retira su voto.

**Reportes**

6. Un cliente puede reportar una reseña ajena indicando un motivo (ofensiva, spam, no relacionada u otro) y un comentario.
7. El reporte guarda una copia del texto y la puntuación de la reseña en ese momento, para que el Administrador vea el contenido original.
8. No puede reportar otra vez la misma reseña mientras tenga un reporte propio `PENDIENTE` sobre ella.
9. Las reseñas ocultas por moderación (RF04) no se muestran ni cuentan para el promedio.

---

### 4.4 Módulo de Carrito de Compras

#### RF11 — Gestión del carrito de compras

**Contenido**

1. El cliente agrega libros activos con stock, cambia cantidades (de 1 hasta el stock), retira libros o vacía el carrito.
2. Si agrega un libro que ya está en el carrito, se suma la cantidad.
3. El carrito vive en la sesión (sección 2.1).

**Resumen**

4. Por cada libro muestra: título, precio original, descuento, precio final, cantidad y subtotal.
5. Al final muestra el subtotal, el costo de envío y el total (`subtotal + costoEnvio`).
6. Debajo aparecen las sugerencias complementarias (RF12).

**Preparación de compra**

7. *Continuar* muestra el resumen de compra (RF15). No reserva stock ni registra el pedido.
8. Si el cliente modifica el carrito, la preparación se descarta.

---

### 4.5 Módulo de Sugerencias

Reglas comunes a RF12, RF13 y RF14:

- Solo se consideran libros activos y ventas válidas.
- Cada recomendación calcula un **puntaje** por libro y produce un conjunto con **todos** los candidatos, ordenados por ese puntaje de mayor a menor. Ese orden es fijo: no se puede cambiar.
- Donde se muestra un extracto (inicio, detalle o carrito), aparecen los primeros 8 libros y un enlace *Ver todos*, que abre el conjunto completo como vista de resultados.
- El período (sección 2.3) se aplica igual en todas: en Complementarios, Para ti, Más populares y Más vendidos delimita las ventas y reseñas que usa el algoritmo; en Similares filtra por fecha de registro. Los extractos se calculan sin período.
- El conjunto se calcula al abrir la vista y solo se recalcula si cambia el período. La búsqueda, los demás filtros y el cambio de página no lo alteran.

#### RF12 — Sugerencia de productos relacionados

**Similares** (en el detalle del libro L)

1. Cada libro se representa con un vector de características:
   - categoría, autor, editorial e idioma, codificados como *one-hot* (1 si el libro tiene ese valor, 0 si no);
   - precio, páginas y año de publicación, normalizados entre 0 y 1 (`(valor − mínimo) / (máximo − mínimo)` sobre todos los libros activos).
2. El puntaje de cada libro B respecto de L es la **similitud del coseno**: `cos(L, B) = (L · B) / (|L| × |B|)`.
3. Se muestran los libros distintos de L, de mayor a menor similitud.

**Complementarios** (en el carrito)

4. Se usan reglas de asociación sobre los pedidos que no están `CANCELADO` ni `DEVUELTO`. Para un libro X del carrito y un libro Y que no está en el carrito:
   - `confianza(X→Y) = pedidos con X e Y / pedidos con X`
   - `lift(X→Y) = confianza(X→Y) / (pedidos con Y / total de pedidos)`
5. Para cada Y se toma la regla de mayor confianza entre todos los X del carrito.
6. Se muestran los Y con `lift > 1`, de mayor a menor confianza; en caso de empate, primero el de mayor lift.

#### RF13 — Recomendaciones personalizadas basadas en historial de compras

**Para ti** (filtrado colaborativo, solo para clientes con historial suficiente)

1. Sea A el conjunto de libros comprados por el cliente y B el conjunto de otro cliente. Su similitud es `Jaccard = |A ∩ B| / |A ∪ B|`.
2. Se toman los 10 clientes con mayor Jaccard, siempre que sea mayor que 0.
3. Cada libro que ellos compraron y el cliente no tiene como puntaje la suma de los Jaccard de los clientes que lo compraron.

**Más populares** (para todos)

4. Para cada libro con al menos una venta válida o una reseña se calculan tres valores:
   - **Ventas con decaimiento:** `V = Σ cantidad × 0.5^(díasDesdeLaVenta / 30)`. Una venta pierde la mitad de su peso cada 30 días.
   - **Valoración bayesiana:** `B = (5 × m + suma de puntuaciones) / (5 + n)`, donde `n` es el número de reseñas del libro y `m` es el promedio de todas las reseñas del sistema.
   - **Recencia:** `R = 0.5^(díasDesdeElRegistroDelLibro / 90)`.
5. El puntaje es `0.5 × (V / Vmáx) + 0.3 × (B / 5) + 0.2 × R`, donde `Vmáx` es el mayor V de todos los libros.

#### RF14 — Recomendaciones más vendidos según la plataforma

1. El puntaje de cada libro es la cantidad de unidades vendidas en ventas válidas del período elegido.
2. *Ver todos* abre la vista con el mismo criterio que la sección de inicio (sin período). Desde ahí el usuario elige el período del ranking.
3. Es visible para visitantes.
4. Se diferencia de *Más populares* en que solo cuenta unidades vendidas, sin decaimiento, valoración ni recencia.

---

### 4.6 Módulo de Pago

#### RF15 — Simulación de procesamiento de pago

**Resumen de compra**

1. Muestra los libros con sus importes y pide tres selecciones:
   - **Dirección:** una dirección activa del cliente; la predeterminada viene seleccionada.
   - **Método de pago:** uno activo y no vencido; el predeterminado viene seleccionado.
   - **Cupón:** opcional; uno propio utilizable (sección 2.2).
2. Si el cliente quiere usar una dirección o un método nuevo, lo registra desde el mismo resumen y queda guardado en su perfil (RF02) antes de pagar.
3. Los importes se calculan así:
   - `descuentoCupon = subtotal × porcentaje del cupón / 100`
   - `total = subtotal − descuentoCupon + costoEnvio`

**Pago**

4. Al pulsar *Pagar*, el sistema vuelve a comprobar los datos, porque el Administrador pudo cambiar algo desde que se preparó la compra:
   - que cada libro siga activo y tenga stock suficiente;
   - que el cupón siga siendo utilizable;
   - los importes, recalculados con los precios y descuentos vigentes.

   Si algo cambió, muestra el resumen actualizado indicando el cambio y no continúa; el cliente revisa y vuelve a pulsar *Pagar*.
5. Luego simula la respuesta del pago: rechazado con un 20 % de probabilidad y aprobado en el resto de casos. No hay pasarela real.
6. **Pago rechazado:** se muestra "Pago rechazado" y no se registra nada. El carrito y la preparación se conservan para reintentar.
7. **Pago aprobado:** se pasa a RF16.

#### RF16 — Confirmación transaccional y registro de pedido

1. Con el pago aprobado, el sistema hace en **una sola transacción** de base de datos:
   - registrar el pedido en `EN_ALMACEN`, con su `fechaCompra`, sus importes, y un código de transacción y uno de seguimiento generados;
   - registrar sus detalles, con el precio y el descuento de ese momento;
   - descontar el stock;
   - desactivar el cupón, si se usó.

   Si algún paso falla, no se guarda nada.
2. Luego vacía el carrito y muestra la confirmación: número de pedido, código de transacción, código de seguimiento, total y un enlace al comprobante (RF17).
3. No se genera código QR.

---

### 4.7 Módulo de Reportes

Los tres reportes son PDF que se generan al solicitarlos y no se almacenan.

#### RF17 — Generación de comprobante de compra

1. El cliente descarga el comprobante de un pedido propio desde la confirmación o desde el detalle del pedido.
2. El comprobante contiene:
   - número de pedido, fecha de compra, código de transacción y código de seguimiento;
   - nombre y correo del cliente;
   - dirección de envío y método de pago enmascarado;
   - libros: título, cantidad, precio unitario, descuento y subtotal;
   - subtotal, descuento del cupón, envío y total, con los importes históricos del pedido.
3. No incluye código QR.

#### RF18 — Reporte de productos ordenado por el monto total

1. El Administrador indica un período y, opcionalmente, un límite de resultados (por ejemplo, el top 10).
2. El reporte lista los libros con ventas válidas en el período, ordenados por monto vendido de mayor a menor. Por cada libro muestra: posición, título, categoría, autor, editorial, cantidad vendida y monto vendido.
3. El monto vendido es la suma de los subtotales de sus detalles.
4. El encabezado indica el período, la fecha de generación y la cantidad de resultados. Al final aparecen los totales de cantidad y monto.

#### RF19 — Reporte de usuarios ordenado por el monto gastado

1. El Administrador indica un período y, opcionalmente, un límite de resultados.
2. El reporte lista a todos los clientes, también a los que no compraron en el período (gasto S/ 0.00), ordenados por gasto de mayor a menor.
3. Por cada cliente muestra:
   - nombre, correo y estado de la cuenta (activa o desactivada);
   - tiempo de uso, en días desde su registro;
   - gasto: suma de los totales de sus pedidos del período que no están `CANCELADO` ni `DEVUELTO`;
   - categoría, autor y editorial más comprados en el período, por unidades; "—" si no compró.
4. El encabezado indica el período, la fecha de generación y la cantidad de resultados.

---

## 5. Reglas de negocio

| Código | Regla | RF |
|---|---|---|
| RN01 | El correo es único, y el dominio `@admin.bookshop` define el rol Administrador. | RF01 |
| RN02 | Una cuenta desactivada no inicia sesión hasta que se reactiva con su contraseña. | RF01, RF02 |
| RN03 | Usuarios, direcciones, métodos de pago, libros, autores, categorías y editoriales no se borran: se desactivan. | RF02, RF04 |
| RN04 | Un cliente no repite el número de un método de pago. | RF02 |
| RN05 | Los descuentos de un mismo libro no se superponen en fechas. | RF04 |
| RN06 | El autor, la categoría y la editorial de un libro no cambian después de registrarlo. | RF04 |
| RN07 | Solo se cancela un pedido `EN_ALMACEN`, y cada cliente puede hacer hasta `maxCancelacionesMes` cancelaciones por mes. | RF03 |
| RN08 | Solo se solicita la devolución de un pedido `ENTREGADO` dentro de `diasDevolucion`, hasta `maxSolicitudesDevolucion` veces por pedido y con una sola pendiente a la vez. | RF03 |
| RN09 | Las cancelaciones y las devoluciones aceptadas reponen el stock y conservan los importes históricos. | RF03 |
| RN10 | Un pedido cuenta para cupón cuando pasa el plazo de devolución desde su entrega, sin devolución pendiente ni aceptada y con un total de al menos `cuponMontoMinimo`. Se emite un cupón cada `cuponCadaPedidos` pedidos que cuentan. | RF03 |
| RN11 | Recomendaciones y reportes cuentan solo ventas válidas. | RF12–RF14, RF18, RF19 |
| RN12 | El registro de un pedido es una sola transacción: se guarda todo o nada. | RF16 |
| RN13 | Los cambios de configuración solo afectan a las operaciones posteriores. | RF04 |
| RN14 | Un cliente tiene como máximo una reseña por libro, un voto útil por reseña y un reporte pendiente por reseña. | RF10 |

---

## 6. Relación con el sílabo

| Unidad | Dónde se aplica |
|---|---|
| U2 Programación orientada a objetos | Clases del modelo (sección 2.1), enumerados de estados, roles y tipos, y la lógica de negocio de cada RF. |
| U3 Librerías y componentes reutilizables | Separación en componentes: modelo, acceso a datos, lógica de negocio y servicios. |
| U4 Acceso a base de datos | Persistencia de las entidades y la transacción única de RF16. |
| U5–U6 Programación web e interfaces | Formularios de registro, perfil, pago y administración; eventos del carrito, de los filtros y de la comparación. |
| U7 Programación distribuida | Servicios web consumidos por el front-end. |
| U8 Reportes | RF17, RF18 y RF19 en JasperSoft Studio. |
| U9 Programación concurrente | Hilo periódico de seguimiento simulado y de cupones (RF03). |

---

## 7. Decisiones de interpretación

| RF | Ambigüedad | Decisión |
|---|---|---|
| RF01 | Cómo se reactiva la cuenta | Al iniciar sesión en una cuenta desactivada con la contraseña correcta, el sistema ofrece reactivarla. |
| RF02 | Atributos de dirección y método de pago | Dirección: alias, dirección, distrito y ciudad. Tarjeta: titular, número y vencimiento. Billetera: proveedor, titular y celular. |
| RF02 | Alcance de "números únicos" | Únicos por cliente. |
| RF03 | Cómo se simula el seguimiento | Un hilo avanza los estados tras un tiempo aleatorio de entre 1 y 2 minutos, y guarda la fecha de cada cambio en el pedido. |
| RF03 | Condiciones del cupón | Un pedido cuenta tras cerrarse su plazo de devolución, sin devolución pendiente ni aceptada y con total ≥ `cuponMontoMinimo`. El cupón se emite cada `cuponCadaPedidos` pedidos que cuentan. |
| RF03 | Máximo de intentos de cancelación | Cancelaciones por cliente por mes calendario; la cancelación es inmediata mientras no se supere ese límite. |
| RF03 | Máximo de intentos de devolución | Solicitudes por pedido, contando las rechazadas y las retiradas. |
| RF04 | "Configurar el envío" | Un costo fijo por pedido. |
| RF04 | Moderar reseñas | El Administrador puede ocultar cualquier reseña por su cuenta; además, aceptar un reporte oculta la reseña reportada. |
| RF08 | Filtros y órdenes | Se conservan todos los filtros y se añade el período. Solo quedan los órdenes por precio y por título. Las secciones de libros del inicio (Novedades y recomendaciones) tienen su propia vista, sin selector de orden. |
| RF08, RF12–RF14 | Significado del período | Una sola regla: en lo calculado con ventas delimita las ventas y reseñas usadas; en lo demás filtra por fecha de registro. |
| RF10 | Qué es una "compra válida" | El libro está en un pedido `ENTREGADO`. |
| RF11 | Dónde vive el carrito | En la memoria de la sesión. |
| RF12 | Algoritmo de similitud | Similitud del coseno sobre un vector de características *one-hot* y numéricas normalizadas. |
| RF12 | Combinar varios libros del carrito | Para cada candidato se toma la regla de mayor confianza. |
| RF13 | Combinar los factores de *Más populares* | Suma ponderada 0.5 / 0.3 / 0.2 de ventas con decaimiento, valoración bayesiana y recencia. |
| RF14 | "Período reciente" | Lo elige el usuario con el filtro de período. |
| RF15 | "Comprobará disponibilidad e importes" | Al pagar se revalidan stock, estado de los libros, cupón e importes, por si el Administrador cambió algo. |
| RF15 | Probabilidad de rechazo del pago | 20 %. |
| RF16 | "Registrará la transacción y el seguimiento" | El pedido guarda su código de transacción y su código de seguimiento; no hay entidades aparte. |
| RF17 | Seguimiento en el comprobante | Solo el código de seguimiento, sin estados ni fechas del envío. |
| RF18, RF19 | Período de los reportes | Fecha de inicio y fecha de fin en ambos. |
