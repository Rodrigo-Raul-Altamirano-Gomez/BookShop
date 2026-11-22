# Auditoría integral — BookShop (I1–I3)

**Fecha:** 22/11/2026 · **Commit revisado:** `8b64b25` (I3) · **Alcance:** backend, frontend, base de datos y despliegue, contra `ERS.md`, `UX-UI.md`, el prototipo de `docs/ui/` y `PLAN-DE-IMPLEMENTACION.md`.

---

## 1. Cómo se hizo

### 1.1 Revisión del código

- Se leyeron completos el ERS, el UX-UI, el plan (incluido el registro de decisiones de su §7), el guion de la demostración, el README, `mock.js` y los scripts SQL.
- Se revisó el código del backend capa por capa y el del front: utilidades, seguridad, estado de la sesión, componentes y páginas.
- `mvn compile` y `dotnet build` terminan **sin errores ni advertencias**.
- No hay métodos ni componentes sin usar.
- `theme.css` es idéntico al del prototipo.

### 1.2 Ejecución

Todo se ejecutó y se probó de verdad, sin modificar el código:

| Pieza | Cómo |
|---|---|
| Base de datos | Una base **aparte**, `bookshop_auditoria`, en el mismo RDS, creada con `01_esquema.sql` y `02_datos.sql` sin cambios. La base `bookshop` real no se tocó. Se comprobó escribiendo un usuario: aparece en la base de auditoría y no en la real. |
| Backend | WAR compilado con `mvn package` y desplegado en el GlassFish 8.0.5 local (JDK 25), con `DB_NAME=bookshop_auditoria`. |
| API | Más de 120 llamadas con `curl`: cada regla, cada error con su código HTTP, los algoritmos contrastados con SQL independiente, concurrencia real y los tres PDF. |
| Hilo | Se observaron los tiempos reales de cada paso, la emisión de cupones y su comportamiento ante un error. |
| Front | `dotnet run` y el navegador a 1280 px: visitante, cliente y administrador. |
| Despliegue | Dos redespliegues del WAR midiendo las conexiones abiertas en RDS. |

**No se pudo probar:** la imagen Docker y el despliegue en Render, porque esta máquina no tiene Docker.

**Estado al terminar:**

- GlassFish quedó detenido, y con él se cerraron las conexiones.
- El servidor del front quedó detenido.
- La imagen de prueba subida se borró.
- La base `bookshop_auditoria` sigue en el RDS, por si se quiere reproducir algo. Se puede borrar con `DROP DATABASE bookshop_auditoria;`.

### 1.3 Severidad

| Severidad | Significado |
|---|---|
| **Media** | Contradice un documento o se reprodujo un fallo. Conviene corregirlo. |
| **Baja** | Inconsistencia o falta de simplicidad que no rompe nada. |
| **Nota** | Se deja como está. Hay que saber explicarlo o anotarlo en el §7 del plan. |

---

## 2. Veredicto general

El flujo normal de **todos** los RF funciona en ejecución: registro, sesión y reactivación; perfil, direcciones, métodos y cupones; compra con revalidación, pago simulado y transacción; pedidos, cancelación, devolución y el hilo; reseñas, votos, reportes y moderación; listas y seguimientos; las cinco recomendaciones; y los tres PDF. Cada regla RN01–RN14 respondió con el código y el mensaje del UX §12.

El backend es simple y lineal, sin sobrevalidación ni seguridad de más. El rendimiento es bueno: todos los endpoints responden en menos de 0.55 s contra RDS.

Las pruebas en ejecución encontraron **cuatro fallos reales** que la lectura del código no confirmaba:

1. **Fuga de conexiones al redesplegar** (H-20). Cada redespliegue deja abierto el *pool* anterior: se pasó de 10 a 20 y luego a 30 conexiones, con un límite de 60 en el RDS compartido.
2. ***Deadlock* reproducible** (H-06). Dos compras simultáneas con los mismos libros en distinto orden fallaron con 500 en **9 de 25** intentos.
3. **El hilo muere para siempre ante un error inesperado** (H-05). Se reprodujo: después del error, ningún pedido volvió a avanzar.
4. **Con sesión iniciada, un fallo del API congela la página sin mensaje** (H-11). El circuito de Blazor se cae.

Además se confirmaron en pantalla los dos problemas de lo inactivo (H-01 y H-02).

---

## 3. Qué se verificó y funciona

| Área | Prueba | Resultado |
|---|---|---|
| Scripts | `01` y `02` sobre una base vacía | ✔ 40 libros y 35 pedidos, sin errores |
| RF01 | Inicio de sesión correcto, clave incorrecta (401), cuenta desactivada (403), correo repetido, también en mayúsculas (409), dominio `@admin.bookshop` | ✔ Los textos del UX §12.2 |
| RF01 (UI) | Login con `volver`; registro con contraseñas distintas (error en línea y foco en la confirmación); registro correcto con la sesión iniciada | ✔ |
| RF02 | Cambio de contraseña (actual incorrecta: 401), método duplicado (409), métodos vigentes, foto de perfil subida, guardada y reflejada en el encabezado sin recargar | ✔ |
| RF04 | Descuento superpuesto (409, con el texto exacto), estado de los descuentos, conteo de libros, configuración, moderación de reportes y PDF desde el front | ✔ |
| RF07–RF08 (UI) | Inicio con sus estantes; catálogo con filtros combinados, chips, la URL sincronizada y *Atrás* | ✔ |
| RF11 (UI) | Agregar al carrito, cambiar la cantidad (importes al instante), complementarios | ✔ |
| RF15–RF16 | Total con cupón: 158.18 − 15.82 + 10 = **152.36** ✔; stock descontado; cupón desactivado; reusar el cupón da 412 | ✔ |
| RF15.4 (UI) | Precio cambiado con la página abierta: alerta ocre con el cambio y el resumen actualizado | ✔ |
| RF15.6 (UI) | Pago rechazado: alerta roja, selecciones conservadas y reintento posible (pendiente desde el I2) | ✔ |
| RF15.5 | 3 rechazos de 14 pagos (~20 %) | ✔ |
| Concurrencia | El último ejemplar comprado a la vez, en 6 rondas: nunca se aprueban los dos y el stock nunca queda negativo | ✔ |
| RF03 | Cancelación (stock repuesto), límite mensual (422), cancelar *En camino* (422); devolución con una pendiente a la vez, retiradas que cuentan, límite de 2, fuera de plazo; aceptación por el administrador (pedido *Devuelto*) | ✔ |
| RF03 (UI) | Detalle con el *timeline* y el botón deshabilitado con su explicación | ✔ |
| Hilo | Los pedidos avanzan solos; cupones emitidos a quienes completan 3 pedidos que cuentan (Carla, Jorge, Martín y Sofía) | ✔ |
| RF10 | Reseña publicada (promedio y distribución actualizados), voto y retiro del voto, reporte con copia del texto original, reporte repetido (422), ocultar (deja de contar), mostrar de nuevo | ✔ |
| RF06 y RF05 | Listas independientes, seguir y dejar de seguir, orden por fecha de seguimiento, estados vacíos | ✔ |
| RF12–RF14 | *Más vendidos* coincide con un SQL independiente; *Para ti* vacío sin historial; complementarios con *lift* > 1 | ✔ |
| RF17–RF19 | Los tres PDF abiertos y revisados contra el UX §11: datos, enmascarado, importes históricos, clientes sin compras con S/ 0.00, cuentas desactivadas incluidas y totales | ✔ |
| Roles | El administrador no entra al carrito; el visitante es enviado a iniciar sesión | ✔ |

---

## 4. Resumen de hallazgos

| ID | Severidad | Área | Hallazgo | En ejecución |
|---|---|---|---|---|
| H-20 | **Media** | Despliegue | El *pool* de conexiones no se cierra al redesplegar: +10 conexiones por despliegue | Reproducido |
| H-06 | **Media** | Concurrencia | *Deadlock* con carritos en orden inverso: 500 en 9 de 25 rondas | Reproducido |
| H-05 | **Media** | Hilo | El hilo muere ante cualquier error que no sea de negocio | Reproducido |
| H-11 | **Media** | Front | Con sesión, un fallo del API congela la página sin aviso | Reproducido |
| H-01 | **Media** | Funcionalidad | Autores, categorías y editoriales inactivos siguen visibles | Reproducido |
| H-02 | **Media** | Funcionalidad | Un libro inactivo se abre y se "agrega" al carrito, que luego aparece vacío | Reproducido |
| H-03 | **Media** | Simplicidad | `buscarCambios` agrupa validaciones (plan §2.6) | Código |
| H-04 | **Media** | Consistencia | Dos formas de paginar; Explorar y Seguimientos resueltos distinto | Código |
| H-08 | **Media** | Despliegue | Las imágenes subidas se guardan en el disco del front (Render las pierde) | Local ✔; Render no probado |
| H-21 | Baja | Accesibilidad | *Elegir foto* y *Elegir imagen* no se alcanzan con el teclado | Comprobado |
| H-07 | Baja | Legibilidad | Filas de los reportes como `Map<String, Object>` con *casts* | Código |
| H-10 | Baja | Fidelidad UX | El autor ve su propia reseña oculta | Comprobado |
| H-12 | Baja | Higiene | `.idea/` versionado y `Clave.pem` sin ignorar en la raíz | Comprobado |
| H-22 | Nota | Hilo | Cada paso del seguimiento tarda hasta unos 3 minutos, no 1–2 | Medido |
| H-13 | Nota | Transacciones | Bloque completo de transacción en operaciones de una sola escritura | Código |
| H-14 | Nota | Funcionalidad | Eliminar una reseña borra sus reportes, también los ajenos | Reproducido |
| H-15 | Nota | Funcionalidad | En *Para ti*, el historial suficiente se mide dentro del período | Código |
| H-16 | Nota | Front | No existe `RequiereSesion` (UX §14.6) | Código |
| H-17 | Nota | Despliegue | Docker usa Java 21 y GlassFish 8.0.0; el plan dice Java 25 y 8.0.5 | Código |
| H-18 | Nota | Concurrencia | La edición del stock por el administrador puede pisar una venta | Código |
| H-19 | Nota | Sesión | La comparación no se borra al cerrar sesión | Código |
| H-23 | Nota | API | Entradas fuera del flujo normal responden 500 | Reproducido |

H-09 (verificaciones pendientes) se cerró casi entero con esta auditoría; lo que queda está en H-08 y H-17.

---

## 5. Hallazgos en detalle

### H-20 · El *pool* de conexiones no se cierra al redesplegar — Media

**Qué pasa.** [`DBManager`](../backend/bookshop-db-manager/src/main/java/pe/edu/pucp/bookshop/dbmanager/DBManager.java) crea un `HikariDataSource` con 10 conexiones y nunca lo cierra. [`SeguimientoListener.contextDestroyed`](../backend/bookshop-rs/src/main/java/pe/edu/pucp/bookshop/rs/SeguimientoListener.java) detiene el hilo pero no el *pool*. Al redesplegar, el *pool* viejo sigue vivo con sus 10 conexiones abiertas en RDS.

**Medido** (conexiones del usuario `admin` en `information_schema.processlist`):

| Momento | Conexiones |
|---|---|
| Primer despliegue | 10 |
| Tras el 1.er redespliegue | 20 (las 10 viejas inactivas) |
| Tras el 2.º redespliegue | 30 |

El RDS tiene `max_connections = 60`, y lo comparten el GlassFish local, el backend de Render y cualquier cliente SQL. Con 4 o 5 redespliegues en una sesión de trabajo, la base rechaza conexiones (*Too many connections*) **para todos**, también para Render. El §7 del plan incluso pide redesplegar cada vez que se recrea la base, así que el caso es frecuente.

**Recomendación (simple).** Un método `cerrar()` en `DBManager` que llame a `pool.close()`, invocado desde `contextDestroyed` después de detener el hilo. Son tres líneas.

### H-06 · *Deadlock* al pagar carritos con los mismos libros — Media

**Qué pasa.** [`pagar`](../backend/bookshop-business-logic/src/main/java/pe/edu/pucp/bookshop/bl/impl/PedidoBLImpl.java:117) bloquea los libros (`select … for update`) en el orden del carrito.

**Reproducido.** Dos clientes pagan a la vez [12, 13] y [13, 12]. En **9 de 25 rondas**, uno recibió **500** *"No se pudo completar la operación"*, y el registro muestra `MySQLTransactionRollbackException: Deadlock found`.

El caso del último ejemplar, en cambio, funciona bien: en 6 rondas nunca se aprobaron las dos compras.

**Recomendación.** Bloquear en orden de id, ordenando los ítems antes del `for`. Es una línea y es exactamente el caso de concurrencia que el plan §2.3 pone como ejemplo de uso normal.

### H-05 · El hilo muere ante cualquier error inesperado — Media

**Qué pasa.** [`SeguimientoHilo`](../backend/bookshop-business-logic/src/main/java/pe/edu/pucp/bookshop/bl/hilo/SeguimientoHilo.java:22) solo captura `BusinessLogicException`.

**Reproducido.** Se dejó un pedido *En camino* sin fecha de salida y se observó:

- a las 09:30:15, el hilo lanzó un `NullPointerException` en `PedidoBLImpl.avanzarSeguimiento` (línea 369) y terminó;
- la vuelta de las 09:31:15 nunca ocurrió (no hubo un segundo error);
- un pedido nuevo (N.º 1097) siguió *En almacén* a los 151 s;
- solo avanzó cuando un redespliegue arrancó un hilo nuevo.

El plan §3.4 pide que *"la vuelta siguiente lo vuelva a intentar"*. Con un solo error, el seguimiento y los cupones se detienen hasta volver a desplegar, y en plena demostración nadie lo notaría.

**Recomendación.** `catch (Exception ex)` en lugar de `catch (BusinessLogicException ex)`.

### H-11 · Con sesión iniciada, un fallo del API congela la página — Media

**Qué pasa.** [`Encabezado.OnInitializedAsync`](../frontend/BookShopWeb/Components/Layout/Encabezado.razor.cs:22) pide los datos del usuario (`usuarios/{id}`) y está **fuera** de `ManejoErrores`, porque [`MainLayout`](../frontend/BookShopWeb/Components/Layout/MainLayout.razor:5) solo envuelve `@Body`.

**Reproducido con el backend detenido:**

- **Como visitante:** aparece la alerta *"No se pudo completar la operación"* con *Reintentar*. Funciona.
- **Como cliente:** la consola muestra *"There was an unhandled exception on the current circuit, so this circuit will be terminated"*. La página queda con los *placeholders* quietos para siempre, sin mensaje y sin reacción a ningún clic.

El plan §2.5 pide que un fallo inesperado *"muestre un aviso y no rompa la página"*.

**Recomendación.** Rodear con `try/catch` la carga de `UsuarioActual` en el encabezado; si falla, el encabezado usa el nombre de la *cookie*. La otra opción es mover `ManejoErrores` para que envuelva también el encabezado.

### H-01 · Autores, categorías y editoriales inactivos siguen visibles — Media

**Reproducido.** Se desactivó a *Lucía Ferrán*. Desaparece de Explorar, del inicio y del alta de libro, pero:

- sus 3 libros siguen en el catálogo y llevan su nombre;
- sigue como opción del filtro *Autor*;
- `/autores/3` muestra su página completa, con *Seguir* y sus libros.

Esto contradice RF04.2 (*"lo inactivo no se muestra"*) y el UX §10.4 (*"no se muestra en la tienda"*). El guion (§6.1) desactiva un autor en plena demostración.

La causa está en el código: [`EntidadBLImpl.obtener`](../backend/bookshop-business-logic/src/main/java/pe/edu/pucp/bookshop/bl/impl/EntidadBLImpl.java:35) no mira `activo`, y [`listarActivos`](../backend/bookshop-dao/src/main/java/pe/edu/pucp/bookshop/dao/impl/LibroDAOImpl.java:43) solo filtra el libro.

**Opciones (decide el usuario; el ERS no lo resuelve):**

- **A.** Ocultar solo la entidad: su página responde "no encontrado" a quien no es administrador.
- **B.** Ocultar también sus libros: `and a.activo = 1 and c.activo = 1 and e.activo = 1` en `listarActivos`.

### H-02 · Un libro inactivo se abre y se "agrega" al carrito — Media

**Reproducido** en la interfaz con *Mareas del sur* desactivado:

1. El detalle, abierto desde *Mis pedidos*, muestra *"En stock: 5 unidades"* y *Agregar al carrito*.
2. Al pulsarlo aparece *"Agregado al carrito."* y el contador sube a 1.
3. El carrito muestra *"Tu carrito está vacío."*, sin explicación.

RF04.2 y RF11.1 dicen que solo se agregan libros activos.

**Recomendación.** Si el libro está inactivo y quien mira no es administrador, ocultar la zona de compra y las listas y mostrar *No disponible* (UX §5.3).

### H-03 · `buscarCambios` agrupa validaciones — Media

[`buscarCambios`](../backend/bookshop-business-logic/src/main/java/pe/edu/pucp/bookshop/bl/impl/PedidoBLImpl.java:185) se usa una sola vez y reúne las comprobaciones de RF15.4. El plan §2.6 lo prohíbe expresamente (*"métodos que agrupan validaciones (`validarPedido`, `verificarDatos`)… se evita a toda costa"*).

Su resultado es correcto en ejecución: muestra el mensaje exacto del cambio de precio, del cupón usado y del libro agotado. El problema es solo de forma.

**Recomendación.** Devolverlo a `pagar` con el comentario `// RF15.4`, o corregir el §2.6 del plan. **Decide el usuario.**

### H-04 · Dos formas de paginar y de buscar — Media

- **SQL con `limit`** (la convención del plan §3.4): Explorar y las tablas de administración.
- **`subList` en Java**, con el mismo bloque copiado: [`LibroBLImpl`](../backend/bookshop-business-logic/src/main/java/pe/edu/pucp/bookshop/bl/impl/LibroBLImpl.java:467), [`PedidoBLImpl`](../backend/bookshop-business-logic/src/main/java/pe/edu/pucp/bookshop/bl/impl/PedidoBLImpl.java:239), [`SolicitudDevolucionBLImpl`](../backend/bookshop-business-logic/src/main/java/pe/edu/pucp/bookshop/bl/impl/SolicitudDevolucionBLImpl.java:95), [`ReporteResenaBLImpl`](../backend/bookshop-business-logic/src/main/java/pe/edu/pucp/bookshop/bl/impl/ReporteResenaBLImpl.java:86) y [`SeguimientoBLImpl`](../backend/bookshop-business-logic/src/main/java/pe/edu/pucp/bookshop/bl/impl/SeguimientoBLImpl.java:46).

Explorar y Seguimientos tienen el mismo patrón según el UX §9.7, y uno se resuelve en SQL y el otro en Java. Además, `ResultadoPagina` no trae `totalPaginas`, así que el front lo calcula en 7 páginas.

Las vistas de libros sí se justifican en Java, porque las recomendaciones calculan en memoria.

**Recomendación.** Fijar la regla (SQL si se filtra en SQL; Java si el BL ya tiene el conjunto en memoria), pasar Seguimientos a SQL y agregar `totalPaginas` a `ResultadoPagina`.

### H-08 · Imágenes subidas en el disco del front — Media

**Probado en local:** la foto de perfil se sube, se previsualiza, se sirve (200, `image/png`), se guarda y aparece en el encabezado sin recargar.

**El riesgo sigue para el despliegue.** [`Imagenes.Guardar`](../frontend/BookShopWeb/Code/Imagenes.cs:12) escribe en `wwwroot/img/subidas` del contenedor:

- en Render, el disco se borra en cada despliegue y en cada suspensión del plan gratuito;
- en EC2, las imágenes se pierden si se recrea el contenedor.

No se pudo probar porque no hay Docker en esta máquina.

**Recomendación.** Un volumen en EC2 (`-v bookshop-img:/app/wwwroot/img/subidas`) y una nota en el README sobre Render.

### H-21 · *Elegir foto* no es operable con el teclado — Baja

En los cuatro formularios con imagen ([`Registro`](../frontend/BookShopWeb/Components/Pages/Cuenta/Registro.razor:21), `Perfil`, `AdminLibro` y `AdminEntidades`), el `InputFile` lleva `class="d-none"` y el botón visible es un `<label>`. Un elemento con `display: none` no recibe foco y un `<label>` no es una parada de tabulación, así que no se puede elegir una imagen sin ratón. El plan §2.5 y el UX §13 piden que *"todo sea operable con teclado"*.

**Recomendación.** Ocultar el `input` con `visually-hidden` en lugar de `d-none` y mostrarle al foco el estilo del botón, o usar un `<button>` que abra el `input`.

### H-07 · Filas de los reportes como mapas — Baja

[`ReporteBLImpl`](../backend/bookshop-business-logic/src/main/java/pe/edu/pucp/bookshop/bl/impl/ReporteBLImpl.java:58) usa `Map<String, Object>` con *casts* como `(Integer) fila.get("cantidad")`. Los PDF salen correctos, pero es lo menos legible del backend.

**Opcional:** dos clases de filas con *getters*, llenadas con `JRBeanCollectionDataSource`, como el comprobante.

### H-10 · El autor ve su propia reseña oculta — Baja

**Comprobado.** Tras ocultar una reseña, el visitante ya no la ve y deja de contar (cantidad 0), pero su autora la sigue viendo, marcada como *Oculta* ([`ResenaBLImpl:45`](../backend/bookshop-business-logic/src/main/java/pe/edu/pucp/bookshop/bl/impl/ResenaBLImpl.java:45)). El UX §3.4 dice que el cliente no ve reseñas ocultas.

Es razonable, porque sin esto el cliente no entendería por qué no puede volver a reseñar. Pero no está documentado: mantenerlo y anotarlo, o quitarlo. **Decide el usuario.**

### H-12 · Higiene del repositorio — Baja

- `.idea/` está en `.gitignore`, pero sus archivos ya estaban versionados (`workspace.xml` cambia en cada sesión). Se arregla con `git rm -r --cached .idea`.
- **`Clave.pem` está en la raíz del repositorio sin versionar y no está en `.gitignore`.** No se abrió. Si es una llave de EC2, un `git add .` la publicaría. Conviene moverla fuera del repositorio o agregar `*.pem` a `.gitignore`.

---

### Notas (se dejan como están)

- **H-22 · Tiempos del seguimiento.** Medidos en ejecución: entre 60 y **163 s** por paso. Como el hilo revisa cada 60 s, un pedido que no cambia en la revisión de los 119 s espera a la de los 179 s. Es consecuencia de la regla del ERS (RF03.4), no del código. Pero el guion (§3.5) dice *"uno o dos minutos por paso"*: conviene decir *"de uno a tres minutos"*.
- **H-13 · Transacción en operaciones de una sola escritura** (`retirar`, `rechazar` y `cambiarVisibilidad`). La razón: el método DAO también participa en `aceptar` y usa `TransactionContext`. Es coherente con la convención; basta con saber explicarlo.
- **H-14 · Eliminar una reseña borra sus reportes.** Comprobado: el reporte *Aceptado* de Luis desapareció de su historial. Está anotado en el §7.
- **H-15 · *Para ti* con período.** El historial suficiente se cuenta dentro del período elegido ([`LibroBLImpl:195`](../backend/bookshop-business-logic/src/main/java/pe/edu/pucp/bookshop/bl/impl/LibroBLImpl.java:195)). Es la aplicación literal del ERS §2.3.
- **H-16 · `RequiereSesion`.** La redirección a `/login?volver=` está escrita en tres sitios. En ejecución funciona bien (el visitante va a iniciar sesión y vuelve a la página). Conviene anotarlo.
- **H-17 · Versiones del despliegue.** El [`Dockerfile`](../backend/Dockerfile:2) usa Java 21 y GlassFish 8.0.0; en local se usa JDK 25 y GlassFish 8.0.5. No se pudo probar la imagen.
- **H-18 · Stock editado por el administrador.** Puede pisar una venta simultánea. Es una coincidencia rebuscada según el plan §2.3: **no corregir**.
- **H-19 · Comparación al cerrar sesión.** `/auth/logout` borra el carrito pero no la comparación. Es defendible.
- **H-23 · Entradas fuera del flujo normal dan 500**, con la página de error HTML de GlassFish:
  - `?pagina=0` o una fecha inválida escrita a mano en la URL;
  - agregar dos veces el mismo libro a una lista o seguir dos veces la misma entidad, llamando al API;
  - una segunda reseña del mismo libro.

  La interfaz no permite ninguno de esos casos: los botones alternan y se deshabilitan mientras procesan. Según el plan §2.2, **no se valida**.

---

## 6. Lo que está bien y no hay que tocar

- **Validaciones mínimas.** Solo las de RN01–RN14, RF15.4 y el bloqueo del stock. En ejecución, cada una respondió con su código y su texto del UX §12.2.
- **Sin fragmentación.** Los métodos auxiliares que hay se reutilizan de verdad (`filtrar`, `mapear`, `hash`, `masComprado`, `generarPdf` y `leer`), salvo `buscarCambios` (H-03).
- **Convenciones del curso:**
  - interfaz e implementación en DAO y BL;
  - `PreparedStatement` y `try` con recursos;
  - `where 1 = 1` en los filtros opcionales;
  - `java.time` para las fechas;
  - excepciones por familia, con su código HTTP;
  - ningún error comunicado con `null`.
- **Lógica en el backend.** Importes, cupones, descuentos, las acciones del pedido, las reseñas y las recomendaciones se calculan en el backend.
- **Transacción y concurrencia** del caso del último ejemplar: correctas en ejecución (H-06 es otra situación).
- **Algoritmos.** Siguen las fórmulas del ERS, y *Más vendidos* coincide con el SQL independiente.
- **Rendimiento.** Todo responde en menos de 0.55 s contra RDS. Cargar los libros y filtrar en memoria es lo simple y alcanza. El primer PDF tarda 1.8 s (Jasper se carga una sola vez) y los siguientes, 0.3 s.
- **Front.** Fiel al prototipo:
  - estados de carga y vacíos con los textos del UX §12.3;
  - la URL sincronizada en las vistas de resultados;
  - botones en estado *procesando* y los textos del UX §12;
  - alertas de compra según el UX §8.11;
  - avisos tras cada acción y el encabezado actualizado sin recargar.

---

## 7. Plan de acción sugerido

1. **H-20, H-06 y H-05:** cerrar el *pool* en `contextDestroyed`, ordenar los ítems antes de bloquearlos y usar `catch (Exception ex)` en el hilo. Son unas cinco líneas en total y eliminan tres fallos reproducidos.
2. **H-11:** `try/catch` en la carga del usuario del encabezado.
3. **H-12:** poner `Clave.pem` a salvo y sacar `.idea/` de git.
4. **H-01 y H-02:** qué se oculta al desactivar **(decide el usuario)**; después, pocas líneas.
5. **H-03:** devolver `buscarCambios` a `pagar` o corregir el plan **(decide el usuario)**.
6. **H-08 y H-17:** volumen Docker para las imágenes y una prueba de las imágenes Docker y de Render en una máquina con Docker.
7. **H-04, H-21, H-07 y H-10:** según el tiempo que quede.
8. **Notas:** anotar en el §7 del plan las decisiones que faltan (H-10, H-16, H-17 y H-22) y corregir los tiempos en el guion.

---

## 8. Correcciones aplicadas (22/11/2026)

Lo que se decidió corregir y cómo quedó. Todo se verificó en ejecución contra `bookshop_auditoria`.

| ID | Cambio | Verificación |
|---|---|---|
| H-20 | `DBManager.cerrar()` (cierra el *pool*), llamado desde `SeguimientoListener.contextDestroyed` | Dos redespliegues seguidos: siguen 10 conexiones (antes 30) |
| H-06 | `pagar` bloquea los libros en orden de id (una copia ordenada de los ítems) | Carritos [12, 13] y [13, 12] a la vez, 25 rondas: **0** errores 500 (antes 9) |
| H-05 | `SeguimientoHilo` captura `Exception` | Con el pedido incoherente, el error se registra en cada vuelta (una por minuto) y el hilo sigue funcionando |
| H-11 | `Encabezado` rodea con `try/catch` la carga del usuario y se queda con el nombre de la sesión | Ver más abajo |
| H-01 | `listarActivos` exige libro, autor, categoría y editorial activos; `CONSULTA_LIBRO` trae el `activo` de las tres referencias | Con la autora 3 inactiva: el catálogo pasa de 39 a 36, la autora no aparece como filtro y el carrito descarta su libro |
| H-02 | El detalle del libro (si él o una referencia están inactivos) y la página de una entidad inactiva muestran "No encontramos lo que buscas." a quien no es administrador | Comprobado como cliente en `/libros/3` y `/autores/3`; el administrador sigue viendo el libro |
| H-04 | Seguimientos pagina en SQL (`contar` y `listarPagina` en `SeguimientoDAO`), igual que Explorar; la regla de paginación quedó en el §7 del plan | Orden por nombre (ascendente y descendente), por fecha (comparado con la base), búsqueda y páginas |
| H-07 | Filas de los reportes con `FilaReporteLibro` y `FilaReporteCliente` (*getters*) y `JRBeanCollectionDataSource`; las plantillas no cambian | Los dos PDF se generan y se ven igual que antes |
| H-08 | README: despliegue en EC2 sin contenedores y cómo conservar `wwwroot/img/subidas`; nota sobre Render | Documentación |
| H-10 | ERS RF10.9 y §7, y UX §3.4: el autor ve su reseña oculta | Documentación |
| H-12 | `*.pem` en `.gitignore`; `.idea/` fuera del índice (`git rm --cached`) | `Clave.pem` ya no aparece como archivo nuevo |
| H-18 | `PUT libros/{id}?stockAnterior=N` y `update … where stock = ?`; si cambió, 412 *"El stock cambió a N mientras editabas…"* y la edición sigue abierta con el stock actual | En la interfaz: venta simulada con el formulario abierto, aviso, stock 6 → 5 y el siguiente guardado correcto |
| H-19 | `/auth/logout` borra también `bk-comparacion` | La barra y la *cookie* desaparecen al cerrar sesión |
| H-21 | Se quitaron *Saltar al contenido* (3 layouts) y los `tabindex="-1"` de las portadas duplicadas. Se mantienen `enfocar` y los `tabindex` de los diálogos. Plan §2.5 y UX §13: la aplicación se usa con el mouse | Compilación |

**No se cambian:** H-03 (separación decidida por el usuario), H-13 a H-17, H-22 y H-23 (anotados en el §7 del plan; el guion dice ahora "uno a tres minutos por paso").

Además, el ERS §7 registra la decisión de H-01 y el §7 del plan, las de esta auditoría.
