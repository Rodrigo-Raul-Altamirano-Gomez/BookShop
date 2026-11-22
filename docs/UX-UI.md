# Diseño UX/UI — BookShop

**Base:** `docs/ERS.md` · **Front-end:** Blazor (C#) + Bootstrap 5.3 · **Solo escritorio** · **Prototipo:** `docs/ui/index.html`

---

## 1. Propósito

Este documento define **cómo se ve y cómo se usa** BookShop. El ERS dice *qué* hace la aplicación; este documento dice dónde está cada cosa, cómo se presenta, qué ve cada rol y cómo responde la interfaz.

Se usa junto con el **prototipo navegable** de `docs/ui/`:

| Pieza | Para qué sirve |
|---|---|
| Prototipo (`docs/ui/`) | Revisar el diseño: 38 pantallas enlazadas, con menús, diálogos y avisos funcionando y datos de ejemplo. Se entra por `index.html`. |
| Este documento | Implementar: reglas, componentes, estados, textos y comportamientos que una pantalla no explica por sí sola. Cada sección remite a su archivo del prototipo. |

Criterios:

- **No agrega ni quita funcionalidades.** Cada pantalla y cada control se traza a un requisito del ERS (sección 13).
- **Mantiene la simplicidad del ERS:** un patrón por problema, reutilizado en toda la aplicación.
- **Solo escritorio:** ancho mínimo de 1200 px. No hay versiones para tableta ni móvil.

---

## 2. Principios de diseño

| # | Principio | Cómo se aplica |
|---|---|---|
| 1 | **El libro es el protagonista.** | Las portadas y los títulos dominan la pantalla. La interfaz es sobria: bordes finos, casi sin sombras. |
| 2 | **Un patrón, muchas pantallas.** | Catálogo, páginas de autor/categoría/editorial, listas personales y recomendaciones usan la misma *vista de resultados* (§6.7). Quien aprende una, las sabe usar todas. |
| 3 | **Siempre se sabe dónde se está.** | Título de página, migas de pan en páginas internas y opción de menú activa resaltada. |
| 4 | **Cada acción tiene respuesta.** | Toda acción produce un aviso, un mensaje en la página o un cambio visible. |
| 5 | **Lo irreversible se confirma.** | Cancelar, eliminar, desactivar, vaciar y moderar piden confirmación en un diálogo. |
| 6 | **La acción principal está a la vista.** | El precio y *Agregar al carrito* van junto al título; *Pagar* y *Continuar* quedan fijos en el panel lateral. |
| 7 | **Solo lo que ayuda a la tarea actual.** | Se muestran los datos que el usuario usa en ese momento: el total de resultados de un listado, la distribución de estrellas de un libro, las cancelaciones que le quedan antes de cancelar. Se evitan los que no usa ahí y que obligan a consultas extra: por ejemplo, la cantidad de elementos de pestañas que no tiene abiertas, o el pedido en que recibió un libro al reseñarlo. |

---

## 3. Arquitectura de información

### 3.1 Mapa de pantallas

`{id}` es el identificador del registro. Las variantes del prototipo se abren desde `index.html`.

**Tienda**

| Pantalla | Prototipo | Ruta Blazor | Rol | RF | Ficha |
|---|---|---|---|---|---|
| Inicio | `inicio.html` | `/` | Todos | RF07, RF13, RF14 | §8.1 |
| Catálogo y búsqueda | `catalogo.html` | `/catalogo?q=` | Todos | RF07, RF08 | §8.2 |
| Detalle de libro | `libro.html?id=` | `/libros/{id}` | Todos | RF09, RF10, RF12 | §8.3 |
| Comparación | `comparar.html` | `/comparar` | Todos | RF09 | §8.4 |
| Explorar | `explorar.html?tipo=` | `/explorar/{autores\|categorias\|editoriales}` | Todos | RF05 | §8.5 |
| Autor, categoría o editorial | `entidad.html?tipo=` | `/autores/{id}` · `/categorias/{id}` · `/editoriales/{id}` | Todos | RF05 | §8.6 |
| Secciones de libros (*Ver todos*: Novedades y recomendaciones) | `recomendaciones.html?tipo=` | `/secciones/{tipo}` | Todos | RF07, RF12–RF14 | §8.7 |
| Iniciar sesión | `login.html` | `/login?volver=` | Visitante | RF01 | §8.8 |
| Registro | `registro.html` | `/registro` | Visitante | RF01 | §8.9 |
| Carrito | `carrito.html` | `/carrito` | Cliente | RF11, RF12 | §8.10 |
| Resumen de compra | `compra.html` | `/compra` | Cliente | RF15 | §8.11 |
| Confirmación | `confirmacion.html` | `/compra/confirmacion/{id}` | Cliente | RF16 | §8.12 |

`{tipo}` de secciones: `novedades`, `mas-vendidos`, `mas-populares`, `para-ti`, `similares/{libroId}`, `complementarios`.

**Mi cuenta** (cliente)

| Pantalla | Prototipo | Ruta Blazor | RF | Ficha |
|---|---|---|---|---|
| Pedidos | `cuenta-pedidos.html` | `/cuenta/pedidos` | RF03 | §9.2 |
| Detalle de pedido | `pedido.html?estado=` | `/cuenta/pedidos/{id}` | RF03, RF17 | §9.3 |
| Mis solicitudes | `cuenta-solicitudes.html` | `/cuenta/solicitudes` | RF03 | §9.4 |
| Cupones | `cuenta-cupones.html` | `/cuenta/cupones` | RF02 | §9.5 |
| Mis listas (Deseados · Favoritos) | `listas.html?lista=` | `/listas?lista=deseados|favoritos` | RF06 | §9.6 |
| Seguimientos | `cuenta-seguimientos.html` | `/cuenta/seguimientos` | RF05 | §9.7 |
| Mis reseñas | `cuenta-resenas.html` | `/cuenta/resenas` | RF10 | §9.8 |
| Perfil (datos personales y contraseña) | `cuenta-datos.html` | `/cuenta/perfil` | RF02 | §9.9 |
| Direcciones | `cuenta-direcciones.html` | `/cuenta/direcciones` | RF02 | §9.10 |
| Métodos de pago | `cuenta-metodos.html` | `/cuenta/metodos-pago` | RF02 | §9.11 |

**Administración**

| Pantalla | Prototipo | Ruta Blazor | RF | Ficha |
|---|---|---|---|---|
| Libros | `admin-libros.html` | `/admin/libros` | RF04 | §10.2 |
| Nuevo y editar libro | `admin-libro.html?id=` | `/admin/libros/nuevo` · `/admin/libros/{id}` | RF04 | §10.3 |
| Autores · Categorías · Editoriales | `admin-autores.html` · `admin-categorias.html` · `admin-editoriales.html` | `/admin/autores` · `/admin/categorias` · `/admin/editoriales` | RF04 | §10.4 |
| Devoluciones | `admin-devoluciones.html` | `/admin/devoluciones` | RF04 | §10.5 |
| Reportes de reseñas | `admin-reportes-resenas.html` | `/admin/reportes-resenas` | RF04 | §10.6 |
| Configuración | `admin-configuracion.html` | `/admin/configuracion` | RF04 | §10.7 |
| Reportes PDF | `admin-reportes.html` | `/admin/reportes` | RF18, RF19 | §10.8 |

**Documentos PDF** (JasperReports)

| Documento | Prototipo | RF | Ficha |
|---|---|---|---|
| Comprobante de compra (A4 vertical) | `comprobante.html` | RF17 | §11.1 |
| Libros por monto vendido (A4 horizontal) | `reporte-libros.html` | RF18 | §11.2 |
| Clientes por monto gastado (A4 horizontal) | `reporte-clientes.html` | RF19 | §11.3 |

### 3.2 Navegación por rol

**Encabezado de la tienda** (fijo arriba al hacer scroll, en todas las páginas de la tienda y de Mi cuenta):

| Zona | Contenido |
|---|---|
| Izquierda | Logotipo *Book**Shop*** (enlace al inicio) · **Catálogo** · **Explorar** (menú: Autores, Categorías, Editoriales). |
| Centro | Buscador de libros por título: envía a `/catalogo?q=`. |
| Derecha | Depende del rol (abajo). |

| Rol | Zona derecha |
|---|---|
| Visitante | Icono de carrito (sin contador) · botón **Iniciar sesión** · enlace *Registrarse*. |
| Cliente | Icono de carrito con contador de libros · avatar con nombre y menú de cuenta. |
| Administrador | Botón **Panel de administración** · *Cerrar sesión*. Sin carrito. |

**Menú de cuenta del cliente** (desplegable, alineado a la derecha):

| Grupo | Opciones |
|---|---|
| Compras | Pedidos · Mis solicitudes · Cupones |
| Mi biblioteca | Mis listas · Seguimientos · Mis reseñas |
| Cuenta | Perfil |
| — | Cerrar sesión |

**Menú lateral de Mi cuenta:** repite los grupos del menú de cuenta. *Mis listas* abre su propia pantalla a todo el ancho (§9.6). El grupo *Cuenta* se despliega en Perfil, Direcciones y Métodos de pago. Arriba muestra el avatar, el nombre y el correo, y al final *Cerrar sesión*. Ocupa 3 de 12 columnas y queda fijo al hacer scroll. La opción actual va resaltada con fondo verde suave.

**Menú lateral de administración** (240 px, fijo, a toda la altura):

| Grupo | Opciones |
|---|---|
| Catálogo | Libros · Autores · Categorías · Editoriales |
| Moderación | Devoluciones · Reportes de reseñas |
| Sistema | Configuración · Reportes PDF |

La barra superior de administración contiene *Ver tienda*, el avatar con *Administrador* y *Cerrar sesión*. Al entrar, el administrador llega a **Libros**.

**Pie de página de la tienda:** logotipo con la leyenda del trabajo académico, y enlaces a Catálogo y Explorar.

### 3.3 Acciones que requieren cuenta

El visitante ve los mismos botones que el cliente (RF07). Al pulsar una de estas acciones, el sistema lo lleva a `/login?volver={página actual}`:

- abrir el carrito;
- agregar al carrito;
- agregar a Deseados o Favoritos;
- seguir un autor, categoría o editorial;
- votar *Útil* o reportar una reseña.

La página de inicio de sesión muestra arriba el aviso *"Inicia sesión para continuar."*. Al iniciar sesión, el usuario vuelve a la página indicada en `volver`. Comparar libros no requiere cuenta.

### 3.4 Qué cambia en las pantallas según el rol

| Elemento | Visitante | Cliente | Administrador |
|---|---|---|---|
| Sección *Para ti* del inicio | No | Sí, con historial suficiente | No |
| Cantidad y *Agregar al carrito* en el detalle | Sí (pide sesión) | Sí | No |
| Botones Favoritos y Deseados | Sí (pide sesión) | Sí | No |
| *Comparar* | Sí | Sí | Sí |
| Formulario de reseña | No | Si cumple RF10 | No |
| Votar y reportar reseñas | Sí (pide sesión) | Sí, en reseñas ajenas | No |
| Reseñas ocultas | No | Solo la propia, con la etiqueta *Oculta* | Sí, con fondo hundido y la etiqueta *Oculta* |
| Botones *Ocultar* y *Mostrar* | No | No | Sí |

---

## 4. Flujos principales

Cada flujo se puede recorrer en el prototipo.

### 4.1 Compra (RF11, RF15, RF16)

```
Detalle de libro ──Agregar al carrito──▶ aviso "Agregado al carrito" + enlace "Ver carrito"
Carrito ──Continuar──▶ Resumen de compra
Resumen: dirección · método de pago · cupón (opcional) ──Pagar──▶ botón "Procesando pago…"
   ├─ Datos cambiaron (RF15.4) ──▶ misma página, alerta ocre con la lista de cambios
   ├─ Pago rechazado (RF15.6)  ──▶ misma página, alerta roja, selecciones conservadas
   └─ Pago aprobado            ──▶ Confirmación ──▶ Descargar comprobante · Ver pedido · Seguir comprando
```

### 4.2 Cancelar o devolver un pedido (RF03)

```
Mi cuenta › Pedidos ──fila──▶ Detalle del pedido
   ├─ EN_ALMACEN, bajo el límite ──Cancelar pedido──▶ confirmación ──▶ pedido CANCELADO
   ├─ EN_ALMACEN, límite alcanzado ──▶ botón deshabilitado + explicación
   ├─ ENTREGADO, dentro del plazo ──Solicitar devolución──▶ diálogo (motivo + comentario)
   │        ──Enviar──▶ aviso "Solicitud enviada…" + enlace a Mis solicitudes
   └─ ENTREGADO, con solicitud pendiente ──▶ alerta "Solicitud de devolución en revisión"
```

### 4.3 Reseñar (RF10)

```
Detalle de libro › Reseñas
   ├─ Cliente con el libro ENTREGADO y sin reseña ──▶ formulario "Escribe tu reseña"
   ├─ Cliente que ya reseñó ──▶ su reseña primero, con Editar y Eliminar
   └─ Reseña ajena ──▶ "Útil (n)" (alterna) y "Reportar" (diálogo con motivo y comentario)
```

### 4.4 Moderación (RF04)

```
Panel › Devoluciones ──Aceptar / Rechazar──▶ confirmación ──▶ la fila sale de la lista + aviso
Panel › Reportes de reseñas ──Ocultar reseña / Mantener visible──▶ confirmación ──▶ sale de la lista
Detalle de libro (administrador) ──Ocultar / Mostrar──▶ aviso
```

### 4.5 Reporte PDF (RF18, RF19)

```
Panel › Reportes PDF ──Desde, Hasta, Límite (opcional)──Generar PDF──▶ el PDF se abre en una pestaña nueva
```

### 4.6 Registro e inicio de sesión (RF01)

```
Registrarse ──Crear cuenta──▶ sesión iniciada, vuelve al inicio
Iniciar sesión ──correo y contraseña──▶
   ├─ correo @admin.bookshop ──▶ Panel de administración (Libros)
   ├─ datos incorrectos ──▶ alerta "Correo o contraseña incorrectos."
   ├─ cuenta desactivada ──▶ diálogo "Tu cuenta está desactivada" ──Reactivar e ingresar──▶ inicio
   └─ correcto ──▶ página de "volver" o el inicio
```

---

## 5. Sistema visual

Todos los valores están en `docs/ui/theme.css`, que es el mismo archivo que usará la aplicación.

### 5.1 Dirección visual

**"Librería de barrio"**:

- papel cálido de fondo y tinta oscura para el texto;
- un verde de biblioteca para las acciones y un rojo lacre solo para los descuentos;
- títulos en serif editorial e interfaz en una sans neutra;
- bordes finos en lugar de sombras y esquinas poco redondeadas.

Se evitan a propósito los recursos genéricos: degradados, efectos de vidrio, tarjetas flotantes con sombras grandes, iconos decorativos en cada título y colores neón o violetas.

### 5.2 Color

| Token | Valor | Uso |
|---|---|---|
| `--bk-paper` | `#F7F4EE` | Fondo de página. |
| `--bk-surface` | `#FFFFFF` | Paneles, tablas, campos, menús. |
| `--bk-sunken` | `#EFEAE1` | Fondos de apoyo: ficha técnica, cabeceras de tabla, campos bloqueados, avatar. |
| `--bk-ink` | `#1E1A16` | Texto principal y títulos. |
| `--bk-ink-2` | `#4A433B` | Texto secundario. |
| `--bk-muted` | `#6E655B` | Metadatos y ayudas de campo. |
| `--bk-line` | `#E3DCD0` | Bordes y separadores. |
| `--bk-line-strong` | `#CFC6B8` | Bordes de campos y de botones neutros. |
| `--bk-green` | `#24513F` | **Primario:** botones principales, enlaces, foco, selección, opción activa. |
| `--bk-green-700` | `#1B3E30` | Primario en *hover* y *active*. |
| `--bk-green-50` | `#E6EEE9` | Fondo de selección (opción elegida, menú activo, filas que difieren). |
| `--bk-lacre` | `#9C3D25` | **Descuento:** precio rebajado, etiqueta `−N %` y descuento del cupón. |
| `--bk-lacre-50` | `#F7E7E1` | Fondo de la etiqueta de descuento. |
| `--bk-ochre` / `-50` | `#7A5400` / `#FBF1D6` | Pendiente, devuelto, advertencias. |
| `--bk-star` | `#B0790F` | Estrellas de valoración. |
| `--bk-red` / `-50` | `#B3261E` / `#FBE9E7` | Errores, acciones destructivas, agotado, cancelado. |
| `--bk-blue` / `-50` | `#2C5D7C` / `#E3EDF4` | Información, "En camino", programado. |

Reglas:

1. El verde es el **único color de acción**. Solo hay un botón verde relleno por zona.
2. El lacre se reserva para descuentos: si algo es lacre, es una rebaja.
3. Ningún estado se comunica solo con color; siempre va con su texto.
4. Todo texto cumple el contraste AA (≥ 4.5:1) sobre su fondo.

### 5.3 Estados (`StatusBadge`)

Etiqueta de texto de 12 px, peso 600, con un punto del mismo color a la izquierda.

| Clase | Colores | Estados |
|---|---|---|
| `bk-status-neutral` | `--bk-ink-2` sobre `--bk-sunken` | En almacén, Retirada, Inactivo, Predeterminada, Oculta, Finalizado, No disponible |
| `bk-status-info` | azul | En camino, Programado |
| `bk-status-ok` | verde | Entregado, Aceptada, Activo, Disponible, Vigente |
| `bk-status-warn` | ocre | Pendiente, Devuelto |
| `bk-status-bad` | rojo | Cancelado, Rechazada |

### 5.4 Tipografía

Fuentes de Google Fonts: **Newsreader** (serif editorial) y **Public Sans** (sans neutra).

| Estilo | Fuente | Tamaño | Uso |
|---|---|---|---|
| `.bk-display` | Newsreader 500 | 40 / 44 px | Título del detalle de libro, portada del inicio, confirmación. |
| `h1` | Newsreader 500 | 32 / 38 px | Título de página. |
| `h2` | Newsreader 500 | 24 / 30 px | Título de sección. |
| `h3` | Public Sans 600 | 18 / 26 px | Subsecciones, títulos de panel y de diálogo. |
| Cuerpo | Public Sans 400 | 16 / 24 px | Texto general (botones a 15 px). |
| `small` | Public Sans 400 | 14 / 20 px | Metadatos, tablas, ayudas. |
| `.bk-caption` | Public Sans 600, mayúsculas, +0.06em | 12 / 16 px | Etiquetas de grupo (*COMPRAS*, *FILTROS*), cabeceras de tabla. |
| Precio | Newsreader 600, cifras tabulares | 18 px tarjeta · 32 px detalle | `PriceTag`. |
| Título de libro en tarjeta | Newsreader 500 | 17 px | Máximo 2 líneas con puntos suspensivos. El autor va en 13 px, 1 línea. |
| Códigos | Monoespaciada del sistema | 14 px sobre `--bk-sunken` | Códigos de transacción, seguimiento y cupón. |

Todas las cifras que se alinean en columnas usan `.bk-num` (cifras tabulares).

### 5.5 Espacio y retícula

| Token | Valor | Uso |
|---|---|---|
| 1 | 4 px | Entre icono y texto. |
| 2 | 8 px | Dentro de grupos (chips, botones contiguos). |
| 3 | 16 px | Relleno de paneles pequeños y separación de grillas. |
| 4 | 24 px | Relleno de paneles (`.bk-panel`) y separación entre bloques. |
| 5 | 48 px | Separación entre secciones de una página. |

- **Contenedor:** `.container-xl` (1140 px) en la tienda; contenido de administración de hasta 1200 px.
- **Retícula:** 12 columnas de Bootstrap.
- **Grillas propias:**
  - `.bk-shelf`: 8 columnas, para estantes;
  - `.bk-grid-4`: vista de resultados (siempre a todo el ancho, 4 columnas);
  - `.bk-grid-6`: entidades.

### 5.6 Bordes, radios y sombras

| Elemento | Valor |
|---|---|
| Borde de paneles, tablas y tarjetas de opción | 1 px `--bk-line` |
| Radio de campos, botones y etiquetas | 4 px |
| Radio de paneles, alertas y tablas | 6 px |
| Radio de diálogos | 10 px |
| Sombra emergente (menús, diálogos, avisos, barra de comparación) | `0 8px 24px rgba(30,26,22,.12)` |
| Sombra de portada | `0 1px 2px rgba(30,26,22,.18)` |

### 5.7 Iconografía

Bootstrap Icons de línea: 16 px en texto y 20 px en botones de solo icono. Se usan solo donde ayudan a reconocer la acción.

| Acción | Icono |
|---|---|
| Carrito | `bi-bag` |
| Favoritos | `bi-heart` / `bi-heart-fill` |
| Deseados | `bi-bookmark` / `bi-bookmark-fill` |
| Comparar | `bi-layout-split` |
| Buscar | `bi-search` |
| Útil | `bi-hand-thumbs-up` / `-fill` |
| Reportar | `bi-flag` |
| Descargar comprobante | `bi-file-earmark-arrow-down` |
| Generar PDF | `bi-file-earmark-pdf` |
| Agregar | `bi-plus-lg` |
| Referencia bloqueada | `bi-lock` |
| Tarjeta · Billetera | `bi-credit-card` · `bi-phone` |
| Alertas | `bi-x-circle` (error) · `bi-exclamation-triangle` (advertencia) · `bi-info-circle` (información) |

Menú de administración: `bi-book`, `bi-person`, `bi-tags`, `bi-building`, `bi-arrow-return-left`, `bi-flag`, `bi-sliders` y `bi-file-earmark-bar-graph`.

### 5.8 Imágenes

- **Portadas:** proporción 2:3, `object-fit: cover` y radio de 2 px.
- **Portada tipográfica** (libro sin imagen): color sólido según `id % 6`. Las clases van de `.bk-c0` a `.bk-c5`: verde, lacre, azul, marrón, oliva y tinta. Encima va el título en Newsreader claro y el autor en mayúsculas pequeñas, con un filete interior y un lomo oscuro. El texto escala con el ancho de la portada (unidades `cqw`).
- **Autor:** imagen circular. **Categoría y editorial:** cuadrada, con radio de 6 px. Si no hay imagen, se muestran las iniciales en Newsreader sobre `--bk-sunken`.
- **Imágenes de ejemplo:** `docs/ui/img/` tiene 36 imágenes SVG, una por cada autor, categoría y editorial de los datos de prueba:
  - **autores:** retratos ilustrados sobre un círculo de color;
  - **categorías:** un icono sobre un color de la paleta, con una trama diagonal y el nombre;
  - **editoriales:** un sello con la inicial y el nombre, sobre fondo blanco.

  Se pueden cargar en la base de datos como imágenes iniciales para la demostración. Se muestran en el inicio, en Explorar, en la página de cada entidad, en Seguimientos y en la administración.
- **Foto de perfil:** circular; sin foto, las iniciales.

### 5.9 Movimiento

Solo hay transiciones funcionales de 150 ms `ease-out`:

- color de botones y bordes en *hover*;
- la portada sube 2 px en *hover*;
- apertura de menús y diálogos (las de Bootstrap).

Con `prefers-reduced-motion` se desactivan todas.

---

## 6. Componentes

Componentes Razor reutilizables en `Shared/`. Su clase CSS ya está en `theme.css`.

### 6.1 Botones

| Variante | Clase | Uso |
|---|---|---|
| Primario | `.btn-primary` | Acción principal de la zona: *Agregar al carrito*, *Continuar*, *Pagar*, *Guardar*. |
| Secundario | `.btn-outline-primary` | *Comparar*, *Seguir*, Favoritos y Deseados, *Agregar descuento*. |
| Neutro | `.btn-outline-secondary` | *Cancelar*, *Volver*, *Elegir imagen*. |
| Peligro | `.btn-outline-danger`, y `.btn-danger` dentro del diálogo | *Cancelar pedido*, *Rechazar*, *Desactivar*. |
| Enlace | `.btn-link` | Acciones de fila: *Editar*, *Quitar*, *Eliminar* (este último en rojo). |

| Estado | Comportamiento |
|---|---|
| Hover | El primario oscurece a `--bk-green-700`; los de borde reciben un fondo suave. |
| Foco | Anillo de 3 px `rgba(36,81,63,.35)`. |
| Deshabilitado | 45 % de opacidad, con una ayuda debajo que explica el motivo. |
| Procesando | *Spinner* a la izquierda, texto en gerundio (*"Procesando pago…"*) y botón deshabilitado. |
| Alternable | Los de Favoritos, Deseados, Útil y Seguir usan `aria-pressed`. Al activarse, el icono se rellena y el texto cambia (*"En favoritos"*, *"Siguiendo"*). |

Tamaños: normal de 40 px, `.btn-sm` de 32 px y `.btn-lg` de 48 px (*Agregar al carrito*, *Continuar*, *Pagar*).

### 6.2 `BookCover`

| Parámetro | Tipo | Notas |
|---|---|---|
| `Libro` | Libro | Muestra la portada o, si no hay imagen, la portada tipográfica (§5.8). |
| `Tamano` | normal \| `sm` | `sm` (miniaturas en tablas y resúmenes) oculta el autor. |

Lleva `role="img"` y `aria-label="Portada de {título}"`.

### 6.3 `BookCard`

```
┌────────────┐
│  PORTADA   │  ← nada superpuesto
└────────────┘
Título del libro en dos     ← Newsreader 17 px, 2 líneas
Nombre del autor            ← 13 px muted, 1 línea
S/ 48.00  S/ 60.00          ← precio final (lacre si hay descuento) + original tachado
[Agotado]                   ← solo si stock = 0
```

| Parámetro | Notas |
|---|---|
| `Libro` | Toda la tarjeta es un único enlace al detalle. |
| `PermiteQuitar` | Agrega un botón circular `×` arriba a la derecha de la portada (Deseados y Favoritos), con el aviso *"Quitado de la lista"*. |

- **Hover:** la portada sube 2 px y el título se subraya.
- **Agotado:** la portada se muestra al 60 % de opacidad y debajo del precio aparece la etiqueta roja *Agotado*.
- Nada se superpone a la portada, para no tapar el título. La etiqueta `−N %` solo aparece en el detalle, el carrito y las tablas.

### 6.4 `EntityCard`

Imagen de hasta 96 px (circular para autores) con el nombre debajo, centrado, en 1 línea. Toda la tarjeta enlaza a la página de la entidad. En *hover*, el borde de la imagen y el nombre se vuelven verdes.

### 6.5 `PriceTag`

| Parámetro | Notas |
|---|---|
| `Precio`, `PrecioFinal` | Con descuento vigente, el final va en lacre y el original tachado en muted. |
| `Grande` | 32 px en el detalle; en ese tamaño se agrega la etiqueta `−N %`. |

Formato de moneda: `S/ 1,234.50`.

### 6.6 `RatingStars`

- **Solo lectura:** estrellas en `--bk-star`, las vacías en `--bk-line-strong`, y el promedio con un decimal: `★★★★☆ 4.2 (18 reseñas)`.
- **Editable:** 5 radios con forma de estrella de 24 px. *Hover* y selección pintan hasta la estrella elegida. Cada estrella tiene etiqueta (*"4 de 5 estrellas"*) y se puede elegir con las flechas del teclado.

### 6.7 `ResultsView` — vista de resultados (RF08, ERS 2.3)

Se usa en el catálogo, las páginas de entidad, Mis listas y todas las recomendaciones.

```
┌─ 3 col ───────┐ ┌─ 9 col ─────────────────────────────────────────┐
│ FILTROS       │ │ 124 libros [Categoría: Novela ×]               │
│ Título        │ │ [Categoría: Fantasía ×] Limpiar filtros         │
│ Categoría ②   │ │                              Ordenar por [▾]    │
│  Limpiar      │ │ ─────────────────────────────────────────────── │
│ [🔍 Buscar…]  │ │  ▢  ▢  ▢  ▢     ← BookCard                      │
│ ☑ Novela      │ │  ▢  ▢  ▢  ▢                                     │
│ ☐ Ensayo      │ │  ▢  ▢  ▢  ▢                                     │
│ ☐ … (5)       │ │              ‹ 1 2 3 … 11 ›                     │
│ ☑ Fantasía    │ │                                                 │
│ Ver más (7)   │ │                                                 │
│ Autor …       │ │                                                 │
│ Editorial …   │ │                                                 │
│ Idioma      ▾ │ │                                                 │
│ Precio S/–S/  │ │                                                 │
│ Período       │ │                                                 │
└───────────────┘ └─────────────────────────────────────────────────┘
```

**Filtro de selección múltiple (`CheckFilter`)** — se usa para Categoría, Autor y Editorial:

| Parte | Comportamiento |
|---|---|
| Encabezado | El nombre del filtro y, cuando hay valores elegidos, un contador verde con la cantidad y el enlace *Limpiar*, que solo quita los de ese filtro. |
| Buscador propio | Si el filtro tiene más de 8 opciones, un campo pequeño (*"Buscar autores"*) filtra la lista mientras se escribe. Si nada coincide, aparece *"Sin coincidencias."*. Durante la búsqueda se muestran todas las coincidencias y se oculta *Ver más*. |
| Lista de casillas | Se ven las primeras 5 opciones en orden alfabético, más las ya marcadas aunque estén más abajo, para no esconder lo elegido. |
| *Ver más (n)* / *Ver menos* | Despliega el resto de la lista, con scroll propio a partir de 15 rem de alto. Lleva `aria-expanded`. |
| Lógica | Dentro de un filtro, las opciones se suman (Novela **o** Fantasía). Entre filtros se deben cumplir todos (ERS RF08). |

**Limpiar todo:** en la cabecera del panel, junto a *FILTROS*, aparece el enlace *Limpiar todo* en cuanto hay cualquier filtro activo (casillas, título, idioma, precio o período). Quita todos los filtros de una vez. Hace lo mismo que *Limpiar filtros* de la barra de resultados; está en los dos lugares porque cada uno queda a la vista según dónde esté el usuario. El *Limpiar* de cada grupo quita solo ese grupo.

Idioma sigue como lista desplegable, porque solo tiene dos opciones y una sola elección. Precio y Período no cambian.

| Componente Razor | Parámetros |
|---|---|
| `CheckFilter` | `Titulo`, `Opciones`, `Seleccionadas` (lista), `SeleccionadasChanged`, `Visibles = 5`, `ConBuscador` (si hay más de 8 opciones) |

| Parámetro | Valores | Efecto |
|---|---|---|
| `PermiteOrden` | sí \| no | Con *sí*, aparece el selector *Ordenar por*: Título A–Z (por defecto), Título Z–A, Precio menor a mayor, Precio mayor a menor. No hay orden por fecha ni por ventas, porque Novedades y Más vendidos tienen su propia sección. Con *no* (secciones de libros del inicio), aparece el texto *"Ordenado por {criterio}"* con el criterio propio de cada sección (§8.7). |
| `CriterioOrden` | texto | Solo en secciones de libros: el criterio que se muestra en lugar del selector. |
| `SignificadoPeriodo` | registro \| ventas | Ayuda del filtro de período: *"Fecha de registro del libro."* o *"Fecha de las ventas consideradas."* |
| `FiltroOculto` | categoría \| autor \| editorial | En la página de una entidad se oculta su propio filtro. |

Comportamiento:

- Los filtros se aplican al cambiarlos, sin botón *Aplicar*, y vuelven a la página 1.
- Búsqueda, filtros, orden y página viajan en la URL, para que *Atrás* y los enlaces funcionen. Los filtros múltiples repiten el parámetro: `?categoria=novela&categoria=fantasia&autor=…`.
- El total (*"124 libros"*) va siempre al inicio de la barra. A su lado, cada valor elegido es un chip propio (*"Categoría: Novela ×"*, *"Categoría: Fantasía ×"*); su `×` desmarca solo ese valor. *Limpiar filtros* aparece cuando hay al menos uno y los quita todos.
- **Paginación:** 12 libros por página. Se muestran anterior, siguiente, la primera página, la última y las vecinas de la actual. Al cambiar de página, la vista vuelve arriba.
- Sin resultados: `EmptyState` con *"No se encontraron libros"* y *Limpiar filtros*.
- El panel de filtros queda fijo al hacer scroll. Si es más alto que la ventana, tiene su propio scroll, para que ningún filtro quede fuera de alcance.

### 6.8 `ShelfSection` — estante

Encabezado con el `h2` a la izquierda, una línea de explicación opcional debajo y *Ver todos →* a la derecha, separado por un filete. Debajo, una fila de **8** tarjetas (`.bk-shelf`). Si el conjunto está vacío, la sección no se muestra.

| Parámetro | Notas |
|---|---|
| `Titulo`, `Explicacion` | La explicación se usa en *Para ti* y en *También suelen llevarse*. |
| `Items` | Libros o entidades. |
| `VerTodosUrl` | Destino del enlace. |

### 6.9 Formularios

- La etiqueta va siempre arriba del campo, nunca solo el *placeholder*.
- Los campos obligatorios no se marcan; los opcionales llevan *"(opcional)"*.
- La ayuda va en 13 px muted, debajo del campo.
- **Error:** `.is-invalid` con borde y mensaje en rojo debajo. El foco va al primer campo con error.
- **Botones:** al pie a la derecha, con *Cancelar* a la izquierda del primario y separados del formulario por un filete. *Cancelar* descarta los cambios sin preguntar (RF02.14).
- Las contraseñas tienen el botón *Mostrar* / *Ocultar* en el campo.
- Los campos de solo lectura (correo, referencias bloqueadas) van con fondo `--bk-sunken`.
- Los importes llevan el prefijo `S/` y los porcentajes y días el sufijo `%` o `días`, como `input-group`.

### 6.10 Mensajes

| Tipo | Componente | Comportamiento |
|---|---|---|
| Aviso | `ToastHost` + `AvisoService.Mostrar(texto, enlace?)` | Abajo a la derecha, fondo tinta, 4 s y botón cerrar. Lleva `role="status"`. |
| Alerta en página | `.alert` con icono | No se cierra. Va arriba del bloque al que se refiere. |
| Error de campo | `.invalid-feedback` | Debajo del campo. |
| Confirmación | `ConfirmModal` | Ver abajo. |

**`ConfirmModal`:**

| Parámetro | Notas |
|---|---|
| `Titulo` | Pregunta concreta: *"¿Cancelar el pedido N.º 1046?"* |
| `Mensaje` | La consecuencia, en una frase. |
| `TextoAccion` | Verbo concreto: *"Sí, cancelar pedido"*. Junto a él va siempre *Volver*. |
| `Peligro` | Si es verdadero, el botón de acción va en rojo relleno. |

### 6.11 `EmptyState`

Centrado: una ilustración de línea de un libro abierto (64 px, `--bk-line-strong`), un título `h3`, una frase de ayuda y como máximo una acción (`.btn-outline-primary`). Los textos están en §12.3.

### 6.12 Tablas

- Envolvente `.bk-table-wrap` con borde y radio de 6 px.
- **Cabecera:** fondo `--bk-sunken`, texto `caption` en mayúsculas muted.
- **Filas:** 48 px, sin rayas, solo con borde inferior, y *hover* `--bk-paper`. Las filas enlazables (pedidos, libros en administración) son clicables en toda su extensión.
- **Alineación:** números a la derecha con cifras tabulares; acciones en la última columna, alineadas a la derecha.
- **Acciones de fila** (administración): enlaces apilados, con *Editar* arriba, las acciones propias del registro en medio (en Libros, *Agregar descuento*, que abre el mismo diálogo que la consulta del libro) y *Desactivar* (en rojo) o *Activar* al final. Desactivar y Activar piden confirmación (`ConfirmModal`) y avisan que lo ya registrado no cambia. Cuando la fila es clicable, el clic en una acción no abre la fila.
- **Pie de tabla:** a la izquierda, el rango mostrado (*"1–20 de 124"*); a la derecha, la paginación pequeña. Se muestran 20 filas por página en las tablas y 10 tarjetas por página en listas de tarjetas.

### 6.13 `TrackingTimeline`

Línea vertical con los hitos *Compra*, *Salida del almacén* y *Entrega*.

- **Hito alcanzado:** punto verde relleno, línea verde, etiqueta en negrita y la fecha y hora (`fechaCompra`, `fechaSalida`, `fechaEntrega`).
- **Hito pendiente:** punto vacío y el texto *"Pendiente"* en muted.
- **Cancelado:** se ocultan los hitos no alcanzados y se agrega *Cancelación* (rojo) con `fechaCancelacion`.
- **Devuelto:** se agrega *Devolución* (ocre) con `fechaDevolucion`.

Encima de la línea van el título *Seguimiento* y el código de seguimiento.

### 6.14 `CompareBar`

Barra fija abajo al centro, con fondo tinta. Se muestra en las páginas de la tienda cuando hay al menos un libro elegido; no se muestra en la página de comparación.

```
Comparar  [Título A ×]  [Elige otro libro]                  [Ver comparación]
```

- *Ver comparación* se habilita solo con dos libros.
- Con dos libros elegidos, pulsar *Comparar* en un tercero abre el diálogo *"¿Qué libro quieres reemplazar?"*, con los dos actuales como botones.
- Los avisos que usa son *"Agregado a la comparación"*, *"Este libro ya está en la comparación"* y *"Comparación actualizada"*.

### 6.15 `QuantityStepper`

Botones `−` y `+` alrededor de un campo numérico, con valores de 1 al stock. En los límites, el botón correspondiente se deshabilita. Tiene `aria-label="Cantidad"`. En el carrito se usa la variante pequeña (`.bk-stepper-sm`).

### 6.16 `ChoiceCard`

Tarjeta con un radio, usada para elegir dirección y método de pago en la compra.

- **Contenido:** el título en negrita (alias, o tipo con su icono), el detalle en 14 px muted y la etiqueta *Predeterminada* si corresponde.
- **Seleccionada:** borde verde de 2 px y fondo `--bk-green-50`. Toda la tarjeta es clicable.
- **Variante "agregar":** borde punteado, texto verde *"+ Agregar…"*; abre el diálogo de alta.
- **Variante "cupón"** (`.bk-choice-coupon`): usa el lenguaje visual de la tarjeta de cupón (§6.17), con el borde izquierdo punteado verde, el porcentaje grande en Newsreader, el código y la fecha de vencimiento. Así el cupón se reconoce igual en *Mis cupones* y al pagar.

### 6.17 Tarjeta de cupón

Panel con un borde izquierdo punteado verde. Muestra el porcentaje en Newsreader de 36 px, el código en monoespaciada, la fecha de vencimiento (o *"Usado en el pedido N.º…"*) y el estado a la derecha. Los cupones no utilizables se muestran al 50 % de opacidad, con el borde gris y la etiqueta *No disponible*.

### 6.18 Diseños (layouts)

| Diseño | Contenido | Usado en |
|---|---|---|
| `MainLayout` | Encabezado (§3.2), `main`, pie de página, `CompareBar`, `ToastHost`. | Tienda |
| `AccountLayout` | `MainLayout` + fila de 3/9 columnas con el menú lateral de Mi cuenta. | Mi cuenta |
| `AuthLayout` | Logotipo centrado y una columna centrada (440 px para iniciar sesión, 576 px para el registro). | Iniciar sesión, Registro |
| `AdminLayout` | Menú lateral de 240 px y barra superior; contenido con relleno de 28 × 32 px. | Administración |

### 6.19 `CoverStack` — pila de portadas

Identifica un pedido por sus libros en las listas de pedidos (Mis pedidos, Devoluciones del administrador).

- **Contenido:** hasta **3** portadas en miniatura (30 px de ancho, `BookCover` pequeño), superpuestas hacia la derecha y separadas por un borde blanco de 2 px. Si el pedido tiene más libros, va al final una etiqueta *"+n"* sobre `--bk-sunken`.
- **Orden:** el de los libros dentro del pedido.
- **Accesibilidad:** `role="img"`, con `aria-label` que nombra los títulos.

| Parámetro | Notas |
|---|---|
| `Libros` | Libros del pedido; se muestran los 3 primeros. |
| `Maximo = 3` | Cantidad de portadas visibles. |

---

## 7. Comportamientos comunes

| Situación | Comportamiento |
|---|---|
| Acción exitosa | Aviso breve. Cuando hay un paso natural siguiente, lleva un enlace (*Ver carrito*, *Ver*). |
| Acción irreversible | `ConfirmModal`; al confirmar se ejecuta y se muestra un aviso, o se navega al resultado. |
| Elemento eliminado de una lista | Desaparece de inmediato y se muestra un aviso. |
| Envío en curso | El botón entra en estado *procesando* (§6.1). |
| Carga de página | Bloques de relleno (`.placeholder-glow` de Bootstrap) con la forma del contenido, para que la página no salte. |
| Error de validación | Ver §6.9. |
| Resultado que hay que leer | Alerta en la página, sin cerrar. |
| Botón no disponible | Deshabilitado, con una ayuda que explica por qué. Nunca desaparece sin explicación. |

### 7.1 Consulta y edición

Los registros que se revisan más de lo que se cambian se abren primero en **modo consulta**. Así se leen con comodidad y no se modifican por accidente. Se aplica en *Perfil* (§9.9), *Libro* en administración (§10.3) y *Configuración* (§10.7).

- **Consulta:** los datos se muestran como listas de definiciones (etiqueta pequeña en gris, valor debajo). La cabecera lleva el botón principal **Editar {objeto}** (`bi-pencil`) y, si aplica, acciones secundarias (*Cambiar contraseña*, *Desactivar*).
- **Edición:** reemplaza la consulta **en la misma pantalla**, sin cambiar de página, con el mismo orden de secciones y los campos cargados. El foco va al primer campo.
  - **Cancelar:** vuelve a la consulta sin guardar.
  - **Guardar:** vuelve a la consulta y muestra un aviso.
- **Entrada directa a la edición:** un enlace *Editar* de una lista puede abrir la pantalla directo en edición (`?editar=1`).
- **Excepción:** los formularios cortos de registros que pertenecen a una lista (direcciones, métodos de pago, autores, categorías, editoriales, descuentos) se editan en un **diálogo**, sin pantalla de consulta propia.

---

## 8. Pantallas de la tienda

### 8.1 Inicio — `inicio.html` (RF07, RF13, RF14)

- **Portada editorial:** a la izquierda (7 col) van la etiqueta *LIBRERÍA EN LÍNEA*, el título `display` *"Encuentra tu próxima lectura."*, una línea de apoyo y el buscador grande. A la derecha (5 col), tres portadas inclinadas como objeto decorativo (`aria-hidden`). Un filete inferior la separa del resto.
- **Estantes** (`ShelfSection`), en este orden:
  1. *Para ti*: solo para clientes con historial suficiente, con la explicación debajo. *Ver todos* lleva a `/secciones/para-ti`.
  2. *Novedades*: los 8 libros registrados más recientemente. *Ver todos* lleva a `/secciones/novedades`.
  3. *Más vendidos*: *Ver todos* lleva a `/secciones/mas-vendidos`.
  4. *Más populares*: *Ver todos* lleva a `/secciones/mas-populares`.
- **Explorar:** un panel con tres filas separadas por filetes: *Categorías*, *Autores* y *Editoriales*. Cada fila tiene 8 `EntityCard` (los primeros 8 que se recuperan) y *Ver todos* hacia Explorar en su pestaña.

### 8.2 Catálogo — `catalogo.html` (RF07, RF08)

- Migas: Inicio › Catálogo.
- Título *"Catálogo"*, o *"Resultados para «término»"* si se llega desde el buscador, con el total a la derecha.
- `ResultsView` con orden, período por registro y 4 columnas. El orden por defecto es *Título A–Z*.

### 8.3 Detalle de libro — `libro.html?id=` (RF09, RF10, RF12)

Migas: Inicio › Catálogo › {Categoría} › {Título}.

**Bloque principal** (portada 4 col · información 7 col):

1. Título en `display`.
2. *"de {Autor} · {Editorial} · {Categoría}"*, con un enlace en cada uno a su página.
3. Estrellas con el promedio y *"(N reseñas)"*, que enlazan a la sección de reseñas.
4. Filete; `PriceTag` grande y la etiqueta `−N %` si hay descuento.
5. Disponibilidad:
   - con stock: *"✓ En stock: N unidades"*;
   - sin stock: *"Agotado · Sin stock por ahora"* en rojo, con el selector de cantidad y el carrito deshabilitados.
6. `QuantityStepper` + **Agregar al carrito** (grande).
7. Botones pequeños: *Favoritos*, *Deseados* (alternables) y *Comparar*.
8. Filete; *Sinopsis*.
9. Ficha técnica sobre `--bk-sunken`, en 4 columnas: ISBN, Idioma, Páginas, Año.

**Reseñas** (a todo el ancho, 4 col · 8 col):

- **Resumen** (izquierda): el promedio en 48 px, las estrellas, el total de reseñas y la distribución por estrellas en barras (5 a 1).
- **Lista** (derecha):
  - El formulario *Escribe tu reseña* va primero, solo si el cliente cumple RF10. Contiene `RatingStars` editable, el comentario y *Publicar reseña*.
  - Si el cliente ya reseñó, en lugar del formulario aparece **su reseña primero**, con *Editar* y *Eliminar* (este con confirmación). Variante del prototipo: `?resena=propia`.
  - **Cantidad visible:** se muestran las 5 reseñas más recientes. Debajo, el botón *"Ver más reseñas (n)"* (`.btn-outline-secondary`, a todo el ancho de la columna) agrega las 5 siguientes en la misma lista, sin recargar ni cambiar de página. El número indica cuántas faltan, y el botón desaparece cuando ya se ven todas. La reseña propia del cliente va siempre arriba y no cuenta entre las 5. El resumen de la izquierda (promedio, total y barras) siempre considera todas las reseñas. Es el mismo patrón *Ver más* de los filtros (§6.7).
  - Cada reseña muestra el avatar con iniciales, el nombre, la fecha, las estrellas a la derecha y el comentario. Al pie lleva *Útil (n)* (alternable) y *Reportar*, que abre un diálogo con el motivo en radios (Ofensiva, Spam, No relacionada con el libro, Otro), el comentario y *Enviar reporte*.

**Libros similares:** `ShelfSection` con *Ver todos* hacia `/secciones/similares/{id}`.

**Vista de administrador:**

- No aparecen la cantidad, el carrito, las listas ni los votos.
- Cada reseña tiene *Ocultar*; las ocultas se ven con fondo hundido, la etiqueta *Oculta* y el botón *Mostrar*.

### 8.4 Comparación — `comparar.html` (RF09)

- Migas: Inicio › Comparación.
- **Tabla de 3 columnas:** Atributo, Libro 1 y Libro 2.
  - La primera fila tiene la portada, el título (enlace) y *Quitar*.
  - Siguen autor, categoría, editorial, idioma, páginas, año, precio final, puntuación y disponibilidad.
- Las filas donde los libros difieren se marcan con `--bk-green-50` en la celda del atributo; una leyenda bajo la tabla lo explica.
- **Con menos de dos libros:** `EmptyState` *"Elige dos libros para compararlos"*, con *Ir al catálogo*.

### 8.5 Explorar — `explorar.html?tipo=` (RF05)

- Migas: Inicio › Explorar.
- Pestañas subrayadas: *Autores · Categorías · Editoriales*.
- **Barra de herramientas** (`.bk-toolbar`), con la misma disposición que la vista de resultados:
  - a la izquierda, el buscador por nombre (*"Buscar autores por nombre"*) y el total;
  - a la derecha, *Ordenar por*: **Nombre A–Z** (por defecto) y **Nombre Z–A**.
- Buscar o cambiar el orden reordena la cuadrícula y vuelve a la página 1. Igual que en `ResultsView`, la búsqueda, el orden y la página van en la URL (`?q=&orden=&pagina=`).
- Cuadrícula de `EntityCard` de 6 columnas, en páginas de 24.

### 8.6 Autor, categoría o editorial — `entidad.html?tipo=` (RF05)

- Migas: Inicio › {Autores|Categorías|Editoriales} › {Nombre}.
- **Cabecera:** imagen de 120 px (circular si es autor), la etiqueta del tipo, el nombre en `h1` y la descripción o biografía. A la derecha, el botón alternable **Seguir** / **Siguiendo**. Un filete la separa del resto.
- *Libros* (`h2`) + `ResultsView` con orden, período por registro y el filtro de la propia entidad oculto.

### 8.7 Secciones de libros — `recomendaciones.html?tipo=` (RF07, RF12–RF14)

Es la vista *Ver todos* de cada sección de libros del inicio y de los estantes de Similares y Complementarios. Tiene migas, un título `h1`, una línea que explica el criterio y un `ResultsView` **sin selector de orden**: cada sección ya tiene su orden propio, que se muestra como texto.

| Tipo | Título | Explicación | Texto de orden | Período |
|---|---|---|---|---|
| `novedades` | Novedades | *Los libros agregados más recientemente al catálogo.* | *Ordenado por fecha de registro* | registro |
| `mas-vendidos` | Más vendidos | *Libros con más unidades vendidas en el período elegido.* | *Ordenado por unidades vendidas* | ventas |
| `mas-populares` | Más populares | *Combina ventas recientes, valoraciones y novedad.* | *Ordenado por popularidad* | ventas |
| `para-ti` | Para ti | *Basado en lo que compran lectores con gustos parecidos a los tuyos.* | *Ordenado por afinidad con tus compras* | ventas |
| `similares` | Similares a «Título» | *Libros con características parecidas.* | *Ordenado por similitud* | registro |
| `complementarios` | También suelen llevarse | *Libros que suelen comprarse junto con los de tu carrito.* | *Ordenado por frecuencia de compra conjunta* | ventas |

### 8.8 Iniciar sesión — `login.html` (RF01)

- `AuthLayout` de 440 px.
- **Alertas** encima del panel, según el caso:
  - *"Inicia sesión para continuar."* (información), si se llega desde una acción que requiere cuenta;
  - *"Correo o contraseña incorrectos."* (error), si los datos no coinciden.
- **Panel:** título *Iniciar sesión*, Correo, Contraseña (con *Mostrar*), **Iniciar sesión** a todo el ancho y *"¿No tienes cuenta? Regístrate"*.
- **Cuenta desactivada:** diálogo *"Tu cuenta está desactivada"* con el texto *"¿Quieres reactivarla? Recuperarás todo tu historial: pedidos, listas y reseñas."* y los botones *Volver* / **Reactivar e ingresar**.

### 8.9 Registro — `registro.html` (RF01)

`AuthLayout` de 576 px.

- Título *Crear cuenta* y la línea *"Con tu cuenta podrás comprar, guardar libros y escribir reseñas."*.
- Foto: avatar circular de 64 px con *Elegir foto* y la ayuda *"Foto (opcional). JPG o PNG."*.
- Nombre completo · Correo (7 col) + Fecha de nacimiento (5 col) · Contraseña + Confirmar contraseña (6 + 6 col).
- **Crear cuenta** a todo el ancho y *"¿Ya tienes cuenta? Inicia sesión"*.
- Errores en línea: *"Ya existe una cuenta con este correo."* · *"Las contraseñas no coinciden."*.

### 8.10 Carrito — `carrito.html` (RF11, RF12)

- Título *"Carrito (N libros)"* y *Vaciar carrito* (rojo, con confirmación) a la derecha.
- **Lista** (8 col), en un panel con filas separadas por filetes. Cada fila tiene:
  - miniatura de portada de 72 px;
  - título (enlace) y autor;
  - `PriceTag`, con la etiqueta `−N %` si hay descuento;
  - `QuantityStepper` pequeño y *Quitar*;
  - subtotal a la derecha.
- **Resumen** (4 col, fijo): Subtotal, Envío, filete, Total en `price`, **Continuar** grande y la nota *"Elegirás la dirección y el pago en el siguiente paso."*.
- **También suelen llevarse:** `ShelfSection` de complementarios.
- Al cambiar una cantidad, los importes se recalculan al instante; al quitar un libro se muestra el aviso *"Libro quitado"*.
- **Vacío:** `EmptyState` *"Tu carrito está vacío."* con *Explorar el catálogo*.

### 8.11 Resumen de compra — `compra.html` (RF15)

- Migas: Carrito › Resumen de compra.
- **Columna izquierda** (8 col), tres pasos numerados (círculo tinta con el número y un título `h3`). La numeración es real: es el orden en que se completa el formulario.
  1. **Dirección de envío:** `ChoiceCard` en 2 columnas, con la predeterminada seleccionada, más la tarjeta *"+ Agregar dirección"*. Esta abre un diálogo con alias, dirección, distrito y ciudad; la dirección queda guardada en el perfil.
  2. **Método de pago:** igual, con los métodos activos y no vencidos y *"+ Agregar método de pago"*. Esta abre un diálogo con las pestañas *Tarjeta* (titular, número, vencimiento) y *Billetera digital* (proveedor, titular, celular).
  3. **Cupón (opcional):** `ChoiceCard` en 2 columnas, igual que dirección y método de pago: una tarjeta de variante "cupón" por cada cupón utilizable y, al final, la tarjeta *"Sin cupón"* (*"Pagar el precio completo"*). Por defecto viene seleccionado el cupón que vence antes. Al cambiar la selección, el descuento, el total y el botón *Pagar* se actualizan al instante. Debajo va la nota *"Solo se muestran tus cupones utilizables."* con el enlace *Ver mis cupones*. Si el cliente no tiene cupones utilizables, en lugar de las tarjetas se muestra el texto *"No tienes cupones disponibles."*.
- **Columna derecha** (4 col, fija):
  - Alertas, cuando corresponde:
    - **Datos cambiados** (ocre): *"Algunos datos cambiaron desde que preparaste la compra. Revisa el resumen y vuelve a pagar."*, con la lista de cambios.
    - **Pago rechazado** (rojo): *"Pago rechazado. Puedes intentarlo de nuevo o elegir otro método de pago."*.
  - **Tu pedido:** miniaturas con título × cantidad e importe; Subtotal, Cupón (en lacre, con signo menos), Envío, Total; el botón **Pagar S/ {total}** y la nota *"Pago simulado: no se realiza ningún cargo real."*.

### 8.12 Confirmación — `confirmacion.html` (RF16, RF17)

Panel centrado de 640 px con:

- un círculo verde con check;
- *"¡Gracias por tu compra!"* en `display`;
- *"Tu pedido N.º {n} fue registrado y ya está en el almacén."*;
- un bloque hundido con el código de transacción, el código de seguimiento y el total pagado;
- los botones **Descargar comprobante** (abre el PDF en una pestaña nueva) y *Ver pedido*, y el enlace *Seguir comprando*.

---

## 9. Pantallas de Mi cuenta

### 9.1 Diseño común

`AccountLayout` (§6.18). Cada página empieza con su `h1` y, cuando aplica, una línea de explicación y la acción principal a la derecha.

### 9.2 Pedidos — `cuenta-pedidos.html` (RF03)

- **Chips de estado** alternables: Todos, En almacén, En camino, Entregado, Cancelado, Devuelto. El seleccionado va relleno en tinta.
- **Ordenar por** (fecha de compra o total): Más recientes, Más antiguos, Mayor total, Menor total.
- **Tabla:** Pedido, Fecha de compra, Libros (unidades), Total, Estado (`StatusBadge`) y *Ver detalle*. Toda la fila es clicable.
- **Columna Pedido:** `CoverStack` con las portadas de los libros del pedido y, a su derecha, el número (*N.º 1045*). Las portadas permiten reconocer un pedido de un vistazo; el número sirve para referirse a él.
- **Vacío:** *"Aún no tienes pedidos."* con *Explorar el catálogo*.

### 9.3 Detalle de pedido — `pedido.html?estado=` (RF03, RF17)

- Migas: Pedidos › N.º {n}. Título *"Pedido N.º {n}"* con su `StatusBadge` y *"Fecha de compra: {fecha}, {hora}"*.
- **Columna principal** (7 col): tabla de libros (miniatura, título con enlace, cantidad, precio con el original tachado si hubo descuento, subtotal). Debajo, alineado a la derecha: Subtotal, Cupón, Envío y Total.
- **Panel lateral** (5 col):
  - `TrackingTimeline`;
  - un panel con la dirección, el método de pago enmascarado y el código de transacción;
  - las acciones según el estado:

| Estado | Acciones |
|---|---|
| Siempre | **Descargar comprobante** (abre el PDF en una pestaña nueva). |
| `EN_ALMACEN` bajo el límite | *Cancelar pedido* (borde rojo). Confirmación: *"¿Cancelar el pedido N.º {n}?"* — *"Se repondrá el stock y no podrás deshacerlo. Te quedan {k} cancelaciones este mes."* — **Sí, cancelar pedido**. |
| `EN_ALMACEN` con el límite alcanzado | *Cancelar pedido* deshabilitado, con la ayuda *"Alcanzaste el límite de cancelaciones de este mes."* |
| `EN_CAMINO` | *Cancelar pedido* deshabilitado, con la ayuda *"Solo se pueden cancelar pedidos en almacén."* |
| `ENTREGADO` dentro del plazo | *Solicitar devolución*, con la ayuda *"Puedes solicitarla hasta el {fecha}."* Abre un diálogo con el motivo en radios (Producto dañado, Producto equivocado, Ya no lo quiero, Otro), el comentario y **Enviar solicitud**. |
| `ENTREGADO` con solicitud pendiente | Alerta ocre *"Solicitud de devolución en revisión."* con enlace a Mis solicitudes. |
| `ENTREGADO` fuera del plazo, `CANCELADO`, `DEVUELTO` | Solo *Descargar comprobante*. |

### 9.4 Mis solicitudes — `cuenta-solicitudes.html` (RF03)

Las devoluciones y los reportes son cosas distintas: una se refiere a un pedido propio y la otra a la reseña de otra persona. Por eso van en **dos pestañas** separadas, con el mismo estilo de pestañas que Explorar.

- **Pestañas:** *Devoluciones* y *Reportes de reseñas*. La pestaña activa va en la URL (`?tipo=devoluciones|reportes`); por defecto se abre *Devoluciones*. El subtítulo de la página cambia con la pestaña.
- **Devoluciones:** subtítulo *"Devoluciones que pediste de tus pedidos entregados."*. Cada panel muestra:
  - el `CoverStack` del pedido y el título *Pedido N.º {n}*, ambos con enlace al detalle del pedido (igual que en Mis pedidos);
  - *"Solicitada el {fecha} · Motivo: {motivo}"*;
  - el comentario;
  - el `StatusBadge` a la derecha.
- **Reportes de reseñas:** subtítulo *"Reseñas de otros clientes que reportaste para revisión."*. Cada panel muestra:
  - la portada pequeña del libro y el título *Reseña en «Título»*, con enlace al libro;
  - *"Reportada el {fecha} · Motivo: {motivo}"*;
  - la reseña reportada como cita (autor, estrellas y texto), igual que la ve el Administrador (§10.6);
  - *"Tu comentario: …"*;
  - el `StatusBadge` a la derecha.
- **Pendientes** (en las dos pestañas): al pie llevan *Editar*, que abre un diálogo con el motivo (con las opciones de ese tipo) y el comentario, y *Retirar*, que pide confirmación:
  - devolución: *"La solicitud pasará a «Retirada» y contará como un intento de devolución."*;
  - reporte: *"Se retirará el reporte y la reseña no será revisada."*.
- **Vacío:**
  - devoluciones: *"No has pedido devoluciones."*, con *Ver mis pedidos*;
  - reportes: *"No has reportado reseñas."*.

### 9.5 Cupones — `cuenta-cupones.html` (RF02)

- Subtítulo *"Los recibes al acumular pedidos entregados. Úsalos en el resumen de compra."*.
- **Acción principal:** botón **Ir al catálogo** arriba a la derecha (`bi-book`). Quien revisa sus cupones normalmente quiere usarlos, y el cupón se aplica al comprar. El botón lleva directo a elegir libros.
- Lista de tarjetas de cupón (§6.17), de 640 px de ancho como máximo, con los utilizables primero. El código solo se muestra para reconocer el cupón: en el resumen de compra se elige con una tarjeta y no se escribe, así que no hay botón de copiar.
- **Vacío:** *"Aún no tienes cupones."* con *Explorar el catálogo*.

### 9.6 Mis listas — `listas.html?lista=` (RF06)

Es una excepción del diseño de *Mi cuenta*: usa `MainLayout`, **a todo el ancho y sin el menú lateral**. Su contenido es una vista de resultados con panel de filtros, y junto al menú lateral los filtros quedaban demasiado estrechos. A todo el ancho es la misma vista que el catálogo. Se llega desde *Mis listas* del menú de cuenta.

- **Migas:** Mi cuenta › Mis listas. Debajo, el título *Mis listas* en `h1` y un subtítulo que cambia con la pestaña:
  - Deseados: *"Libros que quieres comprar más adelante. Solo tú ves esta lista."*;
  - Favoritos: *"Los libros que más te gustaron. Solo tú ves esta lista."*.
- **Pestañas:** *Deseados* y *Favoritos*, con el mismo estilo que Explorar y Mis solicitudes. La pestaña activa va en la URL (`?lista=`); por defecto se abre *Deseados*.
- **Contenido:** `ResultsView` con orden, 4 columnas y `PermiteQuitar`, igual que el catálogo.
- **Vacío:** *"Aún no guardaste libros aquí. Usa el botón del detalle de cada libro."* con *Explorar el catálogo*.

### 9.7 Seguimientos — `cuenta-seguimientos.html` (RF05)

Usa el mismo patrón que *Explorar* (§8.5), pero solo con lo que el cliente sigue. Así la pantalla no crece sin límite aunque siga muchos autores, categorías o editoriales.

- **Pestañas:** *Autores*, *Categorías* y *Editoriales*, sin contadores, igual que en Explorar. La pestaña activa va en la URL (`?tipo=autor|categoria|editorial`); por defecto se abre *Autores*.
- **Barra de herramientas** (`.bk-toolbar`):
  - a la izquierda, el buscador por nombre (*"Buscar autores por nombre"*) y el total;
  - a la derecha, *Ordenar por*: **Nombre A–Z** (por defecto), **Nombre Z–A** y **Seguido recientemente**.
- **Cuadrícula:** `EntityCard` en 5 columnas (`.bk-grid-5`), una menos que en Explorar porque comparte espacio con el menú lateral. Debajo de cada tarjeta va *Dejar de seguir*: al pulsarlo, la tarjeta sale de la lista y aparece el aviso *"Dejaste de seguir a {nombre}"*.
- **Paginación:** páginas de 24, igual que en Explorar.
- **Vacío por pestaña:**
  - el título es *"No sigues a ningún autor."*, *"No sigues a ninguna categoría."* o *"No sigues a ninguna editorial."*;
  - debajo va *"Lo que sigas desde su página aparecerá aquí."*;
  - el botón es *Explorar autores*, *Explorar categorías* o *Explorar editoriales*, y abre Explorar en esa pestaña.

### 9.8 Mis reseñas — `cuenta-resenas.html` (RF10)

- Panel con filas separadas por filetes. Cada fila tiene la miniatura, el título (enlace al libro), las estrellas a la derecha, la fecha y el comentario.
- **Vacío:** *"No has escrito reseñas."*.

### 9.9 Perfil — `cuenta-datos.html` (RF02)

La pantalla sigue el patrón de consulta y edición (§7.1) y abre en **modo consulta**: los datos se leen, no se editan por accidente. Desde ahí, dos botones llevan a cada cambio. Las variantes del prototipo son `?editar=1` para el modo edición y el diálogo de contraseña.

- **Modo consulta** (panel de 640 px):
  - **Cabecera:** el avatar de 72 px, el nombre en `h3` y *"Cliente desde el {fecha}"*.
  - **Datos** en lista de definiciones de 2 columnas: Nombre completo, Fecha de nacimiento, Correo y Contraseña. La contraseña se muestra como `••••••••` y nunca en claro.
  - **Pie a la derecha:** *Cambiar contraseña* (secundario, `bi-key`) y **Editar datos** (primario, `bi-pencil`).
- **Modo edición:** *Editar datos* reemplaza el panel por un formulario en el mismo lugar, sin cambiar de página. El foco va al primer campo.
  - **Contenido:** título *Editar datos*; avatar con *Cambiar foto* y *Quitar foto*; Nombre completo; Fecha de nacimiento; Correo en solo lectura, con la ayuda *"El correo no se puede cambiar."*.
  - **Pie:** *Cancelar* vuelve al modo consulta sin guardar (RF02.14). **Guardar cambios** vuelve al modo consulta y muestra el aviso *"Cambios guardados"*.
- **Cambiar contraseña:** abre un diálogo, igual que los formularios cortos de Direcciones y Métodos de pago.
  - **Campos:** Contraseña actual, Nueva contraseña y Confirmar nueva contraseña.
  - **Botones:** *Cancelar* y **Cambiar contraseña**, que muestra el aviso *"Contraseña actualizada"*.
  - **Error:** *"Las contraseñas no coinciden."*, debajo del campo de confirmación.
- **Desactivar cuenta:** es un panel aparte, debajo del perfil y del mismo ancho, solo en modo consulta. Va separado y al final para que no se pulse por error.
  - **Contenido:** el título *Desactivar cuenta* en `h3` y una línea con la consecuencia: *"Se cerrará tu sesión. Tus pedidos, listas, reseñas y cupones se conservan, y puedes reactivarla al iniciar sesión."*.
  - **Botón** *Desactivar cuenta* (`.btn-outline-danger`), a la derecha. Abre el `ConfirmModal`: *"¿Desactivar tu cuenta?"* — *"Se cerrará tu sesión. Podrás reactivarla iniciando sesión con tu contraseña."* — **Sí, desactivar**. Al confirmar, se cierra la sesión y se va al inicio.

### 9.10 Direcciones — `cuenta-direcciones.html` (RF02)

- **Agregar dirección** arriba a la derecha.
- Tarjetas en 2 columnas, cada una con:
  - el alias en negrita y *Predeterminada* si corresponde;
  - la dirección completa;
  - las acciones *Editar*, *Usar como predeterminada* (solo en las que no lo son) y *Eliminar* (con confirmación: *"Dejará de ofrecerse al pagar. Los pedidos que la usaron la seguirán mostrando."*).
- Diálogo de alta y edición: Alias (con la ayuda *"Un nombre corto para reconocerla."*), Dirección, Distrito y Ciudad.

### 9.11 Métodos de pago — `cuenta-metodos.html` (RF02)

- Igual que Direcciones.
- La tarjeta muestra el tipo con su icono, *Predeterminado* si corresponde, el titular, el número enmascarado y el vencimiento.
- El diálogo tiene las pestañas *Tarjeta* / *Billetera digital*.
- Error de número repetido bajo el campo: *"Ya registraste un método de pago con este número."*.

---

## 10. Pantallas de administración

### 10.1 Diseño común

- **Diseño:** `AdminLayout` (§6.18). La cabecera de cada página (`.bk-page-head`) lleva el título y el subtítulo a la izquierda, y la acción principal a la derecha.
- **Tablas:** usan las acciones de fila y el pie de tabla de §6.12. Así ninguna lista crece sin límite.
- **Consulta antes que edición:** el libro y la configuración se abren primero en modo consulta (§7.1), igual que el Perfil del cliente.

### 10.2 Libros — `admin-libros.html` (RF04)

- **Cabecera:** subtítulo *"{N} libros · {M} activos"* y el botón **Nuevo libro**.
- **Filtros:** buscador por título y filtro *Estado* (Todos, Activos, Inactivos).
- **Tabla:** miniatura, título (Newsreader) y autor, categoría, precio final (con la etiqueta del descuento vigente), stock (en rojo si es 0), estado y acciones.
  - **Clic en la fila:** abre la **consulta** del libro.
  - **Acciones de fila** (§6.12), apiladas: *Editar* abre el libro directo en modo edición (`?editar=1`); *Agregar descuento* abre el diálogo de descuento sin salir de la lista (el mismo de la consulta, §10.3), con el título del libro debajo de su encabezado; y al final *Desactivar* (o *Activar* si está inactivo), con confirmación.
- **Pie de tabla:** páginas de 20 filas.

### 10.3 Libro: consulta, edición y alta — `admin-libro.html?id=` (RF04)

Migas: Libros › {Título | Nuevo libro}.

**Consulta** (por defecto, con `?id`):

- **Cabecera:**
  - a la izquierda, el título del libro en `h1`, el autor y su `StatusBadge` (Activo o Inactivo);
  - a la derecha, *Desactivar* (`.btn-outline-danger`, con confirmación) o *Activar*, y **Editar libro** (primario, `bi-pencil`).
- **Columna izquierda** (3 col): la portada.
- **Columna derecha** (9 col), en paneles con listas de definiciones:
  1. **Datos:** Título, ISBN, Sinopsis (a todo el ancho), Idioma, Páginas y Año de publicación.
  2. **Referencias:** Autor, Categoría y Editorial, cada una con enlace a su página.
  3. **Precio y stock:** Precio, *Precio final hoy* (con `PriceTag`) y Stock (en rojo si es 0).
  4. **Descuentos:** se gestionan aquí y no dentro del formulario de edición, porque son registros propios del libro, igual que las direcciones lo son del cliente.
     - *Agregar descuento* (`.btn-outline-primary`, a la derecha del título) abre un diálogo con porcentaje, desde y hasta. Si las fechas se superponen con otro descuento, se marcan los dos campos de fecha y aparece *"Se superpone con el descuento del {desde} al {hasta}."*.
     - **Tabla:** porcentaje, desde, hasta, estado (*Vigente*, *Programado*, *Finalizado*) y *Eliminar* (solo en los no finalizados, con confirmación). Van primero los vigentes y programados, y luego los finalizados del más reciente al más antiguo.
     - **Cantidad:** se ven 5 filas. *"Ver más descuentos (n)"* agrega las 5 siguientes y desaparece al llegar al final. Es el mismo patrón que las reseñas (§8.3).

**Edición** (botón *Editar libro*, o *Editar* de la lista):

- Título *Editar libro*. Contiene el mismo formulario que el alta, con los datos cargados:
  - **Portada:** la imagen actual y *Elegir imagen*;
  - **Datos:** Título (8 col) + ISBN (4 col), Sinopsis, Idioma, Páginas y Año (4 + 4 + 4 col);
  - **Referencias:** campos bloqueados con el icono de candado y la ayuda *"No se pueden cambiar después de registrar el libro."*;
  - **Precio y stock:** Precio (`S/`) y Stock.
- El estado no se edita aquí: se cambia con *Desactivar* / *Activar*.
- **Barra de acciones** fija al pie: *Cancelar* vuelve a la consulta sin guardar, y **Guardar cambios** vuelve a la consulta y muestra el aviso *"Cambios guardados"*.

**Alta** (*Nuevo libro*, sin `?id`): solo el formulario, con el título *Nuevo libro*.

- **Referencias:** listas con solo los activos y la ayuda *"…No podrán cambiarse después de registrar el libro."*.
- **Interruptor Activo:** con la ayuda *"Los libros inactivos no se muestran en la tienda."*.
- **Barra de acciones:** *Cancelar* (vuelve a la lista) y **Registrar libro**.

### 10.4 Autores, categorías y editoriales — `admin-autores.html`, `admin-categorias.html`, `admin-editoriales.html` (RF04)

Las tres pantallas tienen la misma estructura:

- **Cabecera:** subtítulo *"{N} registrados"* y el botón **Nuevo autor**, **Nueva categoría** o **Nueva editorial**.
- **Filtros:** buscador por nombre y filtro *Estado*.
- **Tabla:** imagen en miniatura (circular para autores), nombre, cantidad de libros, estado y acciones de fila (§6.12): *Editar* y, debajo, *Desactivar* o *Activar*, con confirmación.
- **Pie de tabla:** páginas de 20 filas.
- **Diálogo de Nuevo y Editar:** se mantiene como diálogo porque son pocos campos. Contiene:
  - la imagen actual y *Elegir imagen*;
  - el Nombre;
  - la Biografía (autores) o Descripción;
  - solo al crear, el interruptor **Activo**, con la ayuda *"Si está inactivo, no se muestra en la tienda ni se ofrece al registrar libros."*.

### 10.5 Devoluciones — `admin-devoluciones.html` (RF04)

- **Cabecera:** subtítulo *"Solicitudes pendientes de revisión."*.
- **Tabla:** pedido (`CoverStack` + número, igual que en Mis pedidos), cliente (nombre y correo), fecha de entrega, total, motivo (en negrita) y comentario, y las acciones **Aceptar** / *Rechazar*.
- **Confirmaciones:**
  - Aceptar: *"El pedido pasará a «Devuelto» y se repondrá el stock de sus libros."*;
  - Rechazar: *"El pedido seguirá como «Entregado». El cliente verá la solicitud como rechazada."*.
- Al confirmar, la fila sale de la lista y se muestra un aviso.
- **Pie de tabla:** páginas de 20 filas.
- **Vacío:** *"No hay solicitudes pendientes."*.

### 10.6 Reportes de reseñas — `admin-reportes-resenas.html` (RF04)

- **Cabecera:** subtítulo *"Reseñas que los clientes reportaron. También puedes ocultar cualquier reseña desde el detalle del libro."*.
- **Tarjetas** de 832 px de ancho como máximo. Cada una tiene:
  - **a la izquierda**, la **portada** del libro (52 px, con enlace al detalle en vista de administrador), para reconocerlo de un vistazo, igual que en *Mis solicitudes › Reportes*;
  - **a la derecha:**
    - la etiqueta *RESEÑA EN* y el título del libro (enlace), con *"Escrita por {autor} · {fecha}"* y el estado;
    - el **contenido reportado** como cita (borde izquierdo de 3 px): estrellas y texto;
    - *"Reportado por {nombre} · Motivo: {motivo}"* y el comentario;
    - las acciones **Ocultar reseña**, que acepta el reporte, y *Mantener visible*, que lo rechaza. Ambas piden confirmación.
- **Pie:** páginas de 10 tarjetas.
- **Vacío:** *"No hay reportes pendientes."*.

### 10.7 Configuración — `admin-configuracion.html` (RF04)

**Consulta** (por defecto):

- **Cabecera:** título y subtítulo *"Valores que rigen las operaciones nuevas de la tienda."*, y el botón **Editar configuración** (`bi-pencil`).
- **Paneles:** tres paneles de 832 px de ancho como máximo, con listas de definiciones:
  - **Envío:** costo de envío;
  - **Devoluciones y cancelaciones:** plazo de devolución, solicitudes por pedido y cancelaciones por mes;
  - **Cupones:** monto mínimo del pedido, pedidos para un cupón, descuento del cupón y vigencia.

**Edición:**

- Título *Editar configuración* y un `.alert-info`: *"Los cambios se aplican a las operaciones nuevas. Los pedidos, cupones y solicitudes ya registrados no cambian."*.
- Los mismos tres paneles, con estos campos y ayudas:

| Panel | Campos y ayudas |
|---|---|
| Envío | Costo de envío (`S/`): *"Se suma al total de cada pedido."* |
| Devoluciones y cancelaciones | Plazo de devolución (días): *"Desde la entrega del pedido."* · Solicitudes por pedido: *"Incluye las rechazadas y retiradas."* · Cancelaciones por mes: *"Por cliente, en el mes calendario."* |
| Cupones | Monto mínimo del pedido (`S/`): *"Solo cuentan los pedidos con este total o más."* · Pedidos para un cupón: *"Se emite un cupón cada vez que el cliente junta esta cantidad."* · Descuento del cupón (%) · Vigencia del cupón (días) |

- Pie con *Cancelar* (vuelve a la consulta sin guardar) y **Guardar configuración**, que vuelve a la consulta y muestra el aviso *"Configuración guardada"*.

### 10.8 Reportes PDF — `admin-reportes.html` (RF18, RF19)

- Subtítulo: *"No cuentan los pedidos cancelados ni devueltos. El PDF se abre en una pestaña nueva."*.
- **Dos paneles lado a lado:** *Libros por monto vendido* y *Clientes por monto gastado*. Cada uno tiene una descripción de una línea, Desde, Hasta, *Límite de resultados (opcional)* con *"Todos"* como texto de ejemplo, y **Generar PDF**.
- Mientras se genera, el botón muestra *"Generando…"*.

---

## 11. Documentos PDF

Diseño de referencia para JasperSoft Studio. Ninguno lleva código QR.

**Estilo común:**

- logotipo arriba a la izquierda;
- tipografías Newsreader y Public Sans;
- tablas con la cabecera en mayúsculas de 10 px sobre un filete grueso y filas separadas por filetes finos;
- números alineados a la derecha.

### 11.1 Comprobante — `comprobante.html` (RF17)

A4 vertical.

- **Cabecera:** el logotipo, y a la derecha *"Comprobante de compra"* con el número de pedido, la fecha y la hora.
- **Bloques en 2 columnas:**
  - *Cliente*: nombre y correo;
  - *Envío*: alias y dirección;
  - *Pago*: método enmascarado y código de transacción;
  - *Seguimiento*: código de seguimiento.
- **Tabla:** Libro (con el autor debajo), Cantidad, Precio unitario, Descuento y Subtotal.
- **Totales** a la derecha: Subtotal, Cupón, Envío y **Total**.
- **Pie:** *"Pago simulado con fines académicos. Este documento no tiene validez tributaria."*.

### 11.2 Libros por monto vendido — `reporte-libros.html` (RF18)

A4 horizontal.

- **Cabecera:** el título, y a la derecha el período, la fecha de generación y la cantidad de resultados.
- **Tabla:** #, Título, Categoría, Autor, Editorial, Cantidad y Monto vendido, con una fila de **Total** al final.
- **Pie:** *"No incluye pedidos cancelados ni devueltos."*.

### 11.3 Clientes por monto gastado — `reporte-clientes.html` (RF19)

A4 horizontal.

- **Cabecera:** como en §11.2.
- **Tabla:** #, Cliente (con el correo debajo), Estado, Días de uso, Gasto, y la Categoría, el Autor y la Editorial más comprados (*"—"* si no compró), con una fila de **Total**.
- **Pie:** explica cómo se calcula el gasto y las preferencias.

---

## 12. Textos

### 12.1 Reglas de redacción

- Se tutea al usuario, en español neutro: *"Tu carrito"*, *"Inicia sesión"*.
- Solo va con mayúscula la primera palabra: *"Agregar al carrito"*.
- Los botones empiezan con un verbo y dicen qué pasará: *"Guardar cambios"*, no *"Aceptar"*.
- Los errores dicen qué pasó y qué hacer, sin culpar al usuario.
- Fechas en formato `dd/mm/aaaa`; con hora, `dd/mm/aaaa hh:mm`. Moneda: `S/ 1,234.50`. Pedidos: `N.º 1045`.

### 12.2 Avisos y mensajes

| Situación | Texto |
|---|---|
| Correo ya registrado | *Ya existe una cuenta con este correo.* |
| Contraseñas distintas | *Las contraseñas no coinciden.* |
| Inicio de sesión fallido | *Correo o contraseña incorrectos.* |
| Requiere cuenta | *Inicia sesión para continuar.* |
| Agregado al carrito | *Agregado al carrito.* · enlace *Ver carrito* |
| Libro quitado del carrito | *Libro quitado* |
| Quitado de una lista | *Quitado de la lista* |
| Guardado | *Cambios guardados* · *Dirección guardada* · *Método de pago guardado* · *Configuración guardada* · *Libro registrado* |
| Guardado desde la compra | *Dirección guardada en tu perfil* · *Método de pago guardado en tu perfil* |
| Método de pago duplicado | *Ya registraste un método de pago con este número.* |
| Descuento superpuesto | *Se superpone con el descuento del {desde} al {hasta}.* |
| Pago rechazado | *Pago rechazado. Puedes intentarlo de nuevo o elegir otro método de pago.* |
| Datos cambiados al pagar | *Algunos datos cambiaron desde que preparaste la compra. Revisa el resumen y vuelve a pagar.* |
| Pedido cancelado | *Pedido cancelado. El stock fue repuesto.* |
| Solicitud enviada | *Solicitud enviada. Puedes verla en Mis solicitudes.* · enlace *Ver* |
| Solicitud o reporte retirado | *Solicitud retirada* · *Reporte retirado* |
| Reseña publicada · eliminada | *Reseña publicada.* · *Reseña eliminada* |
| Reporte enviado | *Gracias, revisaremos esta reseña.* |
| Moderación | *Devolución aceptada* · *Devolución rechazada* · *Reseña ocultada* · *Reseña visible de nuevo* · *Reporte rechazado* |
| Seguimiento de entidades | *Dejaste de seguir a {nombre}* |
| Comparación | *Agregado a la comparación* · *Este libro ya está en la comparación* · *Comparación actualizada* |

### 12.3 Estados vacíos

| Lugar | Título | Acción |
|---|---|---|
| Vista de resultados | *No se encontraron libros.* | Limpiar filtros |
| Carrito | *Tu carrito está vacío.* | Explorar el catálogo |
| Comparación | *Elige dos libros para compararlos.* | Ir al catálogo |
| Pedidos | *Aún no tienes pedidos.* | Explorar el catálogo |
| Mis listas (cada pestaña) | *Aún no guardaste libros aquí.* | Explorar el catálogo |
| Seguimientos (cada pestaña) | *No sigues a ningún autor.* (o categoría, editorial) | Explorar autores (o categorías, editoriales) |
| Cupones | *Aún no tienes cupones.* | Explorar el catálogo |
| Mis solicitudes › Devoluciones | *No has pedido devoluciones.* | Ver mis pedidos |
| Mis solicitudes › Reportes de reseñas | *No has reportado reseñas.* | — |
| Mis reseñas | *No has escrito reseñas.* | — |
| Devoluciones (admin) | *No hay solicitudes pendientes.* | — |
| Reportes de reseñas (admin) | *No hay reportes pendientes.* | — |

---

## 13. Accesibilidad

| Aspecto | Regla |
|---|---|
| Contraste | Texto ≥ 4.5:1; componentes ≥ 3:1. La paleta de §5.2 cumple. |
| Foco | Anillo visible de 3 px en todo elemento interactivo (`:focus-visible`, en `theme.css`). |
| Teclado | La aplicación se usa con el mouse; el teclado sirve para escribir y para enviar con `Enter`. Los diálogos de Bootstrap cierran con `Esc`. |
| Semántica | `header`, `nav` (con `aria-label`), `main` y `footer`. Un solo `h1` por página, sin saltar niveles. Las migas usan `nav aria-label="Ruta"`. |
| Imágenes | Portadas con `aria-label="Portada de {título}"`; la decoración del inicio lleva `aria-hidden`. |
| Formularios | Un `label` asociado a cada campo; los grupos de radios usan `fieldset` + `legend`. Los errores se enlazan con `aria-describedby`. |
| Estados | `aria-pressed` en los botones alternables y `aria-current="page"` en la página o pestaña activa. Los avisos llevan `role="status"` y las alertas de error `role="alert"`. |
| Iconos | Los botones de solo icono llevan `aria-label`. |
| Movimiento | Se respeta `prefers-reduced-motion`. |

---

## 14. Implementación en Blazor

1. **Estilos:**
   - Copiar `docs/ui/theme.css` a `wwwroot/css/theme.css`.
   - Cargarlo **después** de Bootstrap 5.3 y Bootstrap Icons 1.11.
   - Agregar el `<link>` de Google Fonts que está en la cabecera de cualquier página del prototipo.
   - No hace falta Sass: el tema se aplica con variables CSS sobre las de Bootstrap.
2. **Clases:** las propias llevan el prefijo `bk-`; para todo lo demás se usan las de Bootstrap.
3. **Páginas:** cada pantalla de §3.1 es una página Razor con su ruta, dentro de su diseño (§6.18). El HTML del prototipo es la base del marcado; los datos de ejemplo se reemplazan por los del back-end.
4. **Componentes:** lo que se repite entre pantallas es un componente de §6, en `Shared/`.
5. **Estado en la sesión:** `CarritoService` y `ComparacionService` tienen alcance de sesión (`Scoped`), como indica el ERS. El contador del carrito del encabezado escucha el evento de cambio del servicio.
6. **Rol:** el diseño y los componentes leen el rol del usuario autenticado para aplicar §3.2–§3.4. Las acciones que requieren cuenta pasan por un componente envoltorio `RequiereSesion` que redirige a `/login?volver=`.
7. **Lo que no se lleva a Blazor:**
   - `mock.js`, que solo arma el prototipo con datos de ejemplo, diseños compartidos y simulación de diálogos;
   - los textos marcados como *"Prototipo:"*;
   - los parámetros de URL para ver variantes (`?rol=`, `?estado=`, `?vacio=`, `?cambios=`, `?resena=`, `?error=`, `?desactivada=`). Se conservan solo los que forman parte del diseño: búsqueda, filtros, orden, página, pestaña (`?tipo=`, `?lista=`) y `?editar=1`.
8. **El JavaScript es solo del prototipo.** El prototipo usa JavaScript porque es HTML estático y tiene que simular el comportamiento. En la aplicación, ese comportamiento se programa en C# dentro de los componentes Razor; lo único que se reutiliza tal cual es el CSS.

| Comportamiento del prototipo (`mock.js`) | En Blazor |
|---|---|
| Encabezado, menú de cuenta, menú de administración | Diseños `MainLayout`, `AccountLayout` y `AdminLayout` (§6.18), con el rol del usuario autenticado. |
| Libros, pedidos, autores… de ejemplo | Datos de los servicios del back-end. |
| Vista de resultados, filtros con casillas, chips, *Ver más* | `ResultsView` y `CheckFilter` con estado en C# (`@bind`, eventos `@onchange` y `@onclick`). |
| Consulta y edición (`data-vista`) | Un campo `bool editando` en la página, con `@if`. |
| Pestañas (`?tipo=`, `?lista=`) | Parámetro de ruta o de consulta (`[SupplyParameterFromQuery]`). |
| Confirmaciones y avisos | `ConfirmModal` y `ToastHost` + `AvisoService` (§6.10). |
| Diálogos y menús desplegables | Se puede mantener el JavaScript de Bootstrap (`bootstrap.bundle`), que funciona con Blazor sin código propio, o abrirlos desde C# con el estado del componente. |
| Comparación en la sesión | `ComparacionService` (`Scoped`). |
| Requiere cuenta (`data-auth`) | Componente `RequiereSesion` (punto 6). |

---

## 15. Trazabilidad con el ERS

| RF | Pantallas y componentes |
|---|---|
| RF01 | §8.8 Iniciar sesión, §8.9 Registro, diálogo de reactivación |
| RF02 | §9.5 Cupones, §9.9 Perfil (con desactivar cuenta), §9.10 Direcciones, §9.11 Métodos de pago, diálogos de dirección y método de pago (§8.11) |
| RF03 | §9.2 Pedidos, §9.3 Detalle de pedido (`TrackingTimeline`), §9.4 Mis solicitudes |
| RF04 | §10.2 – §10.8 Administración, moderación en §8.3 |
| RF05 | §8.5 Explorar, §8.6 Página de entidad, §9.7 Seguimientos, bloque Explorar del inicio |
| RF06 | Botones del detalle (§8.3), §9.6 Mis listas |
| RF07 | §3.2 Navegación, §3.3 Acciones que requieren cuenta, §8.1 Inicio, §8.7 Novedades, `BookCard`, `EntityCard` |
| RF08 | `ResultsView` (§6.7), §8.2 Catálogo |
| RF09 | §8.3 Detalle, §8.4 Comparación, `CompareBar` |
| RF10 | Reseñas en §8.3, §9.8 Mis reseñas |
| RF11 | §8.10 Carrito |
| RF12 | Similares (§8.3), complementarios (§8.10), §8.7 |
| RF13 | *Para ti* y *Más populares* (§8.1), §8.7 |
| RF14 | *Más vendidos* (§8.1), §8.7 |
| RF15 | §8.11 Resumen de compra |
| RF16 | §8.12 Confirmación |
| RF17 | *Descargar comprobante* (§8.12, §9.3), §11.1 |
| RF18 | §10.8, §11.2 |
| RF19 | §10.8, §11.3 |
