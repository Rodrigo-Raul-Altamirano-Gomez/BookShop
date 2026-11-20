/* =========================================================
   BookShop — soporte del prototipo navegable (solo docs/ui).
   Pone los diseños compartidos (encabezado, Mi cuenta, administración),
   datos de ejemplo, avisos, confirmaciones y la barra de comparación.
   En la aplicación, cada pieza es un componente Razor (UX-UI.md §6).

   Cada página declara en <body>:
     data-layout = tienda | cuenta | admin | auth
     data-nav    = opción activa del menú
     data-rol    = rol por defecto (cliente si no se indica)
   El rol se puede forzar con ?rol=visitante | cliente | admin.
   ========================================================= */

/* ---------- Datos de ejemplo ---------- */
const LIBROS = [
  { id: 1,  t: "La casa de los relojes",        a: "Inés Valdivia",   cat: "Novela",   ed: "Ediciones Faro", idi: "Español", pag: 312, anio: 2024, p: 60.00, d: 20, s: 12, r: 4.2, n: 18 },
  { id: 2,  t: "Mareas del sur",                a: "Tomás Arguedas",  cat: "Novela",   ed: "Lumbre",         idi: "Español", pag: 268, anio: 2023, p: 54.90, d: 0,  s: 7,  r: 4.5, n: 11 },
  { id: 3,  t: "El cartógrafo ciego",           a: "Lucía Ferrán",    cat: "Fantasía", ed: "Altamar",        idi: "Español", pag: 420, anio: 2025, p: 69.00, d: 0,  s: 3,  r: 4.0, n: 6 },
  { id: 4,  t: "Diccionario de lo invisible",   a: "Marco Salazar",   cat: "Ensayo",   ed: "Papel Vivo",     idi: "Español", pag: 196, anio: 2022, p: 45.00, d: 15, s: 20, r: 3.8, n: 9 },
  { id: 5,  t: "Noches en Barranco",            a: "Ana Lucía Pardo", cat: "Cuento",   ed: "Orilla",         idi: "Español", pag: 154, anio: 2021, p: 39.90, d: 0,  s: 0,  r: 4.1, n: 7 },
  { id: 6,  t: "Breve historia del silencio",   a: "Julián Ortega",   cat: "Historia", ed: "Sextante",       idi: "Español", pag: 344, anio: 2020, p: 72.00, d: 0,  s: 9,  r: 4.3, n: 14 },
  { id: 7,  t: "El jardín de sal",              a: "Carmen Rivas",    cat: "Poesía",   ed: "Grulla",         idi: "Español", pag: 98,  anio: 2024, p: 35.00, d: 10, s: 15, r: 4.6, n: 5 },
  { id: 8,  t: "Todo lo que cabe en un río",    a: "Diego Montalvo",  cat: "Novela",   ed: "Casa del Río",   idi: "Español", pag: 288, anio: 2025, p: 58.00, d: 0,  s: 4,  r: 3.9, n: 4 },
  { id: 9,  t: "Manual para perderse",          a: "Rosa Quiroga",    cat: "Viajes",   ed: "Altamar",        idi: "Español", pag: 210, anio: 2023, p: 49.00, d: 0,  s: 11, r: 4.0, n: 8 },
  { id: 10, t: "La última biblioteca",          a: "Hugo Benavides",  cat: "Fantasía", ed: "Lumbre",         idi: "Español", pag: 512, anio: 2025, p: 64.50, d: 25, s: 6,  r: 4.7, n: 21 },
  { id: 11, t: "Cartas a una ciudad dormida",   a: "Elena Castañeda", cat: "Novela",   ed: "Ediciones Faro", idi: "Español", pag: 236, anio: 2022, p: 52.00, d: 0,  s: 8,  r: 4.1, n: 6 },
  { id: 12, t: "Geometría de la lluvia",        a: "Pablo Iturbe",    cat: "Ciencia",  ed: "Sextante",       idi: "Inglés",  pag: 380, anio: 2021, p: 78.00, d: 0,  s: 5,  r: 4.4, n: 10 },
  { id: 13, t: "Los años de ceniza",            a: "Mariana Soto",    cat: "Historia", ed: "Orilla",         idi: "Español", pag: 402, anio: 2019, p: 66.00, d: 0,  s: 2,  r: 3.7, n: 3 },
  { id: 14, t: "Un idioma para los pájaros",    a: "Inés Valdivia",   cat: "Poesía",   ed: "Grulla",         idi: "Español", pag: 112, anio: 2023, p: 32.00, d: 0,  s: 14, r: 4.8, n: 9 },
  { id: 15, t: "El hombre que contaba puertas", a: "Tomás Arguedas",  cat: "Cuento",   ed: "Papel Vivo",     idi: "Español", pag: 176, anio: 2024, p: 41.00, d: 20, s: 10, r: 4.0, n: 7 },
  { id: 16, t: "Desierto adentro",              a: "Lucía Ferrán",    cat: "Novela",   ed: "Casa del Río",   idi: "Español", pag: 298, anio: 2025, p: 57.00, d: 0,  s: 7,  r: 4.2, n: 5 }
];
const AUTORES = ["Inés Valdivia", "Tomás Arguedas", "Lucía Ferrán", "Marco Salazar", "Ana Lucía Pardo", "Julián Ortega", "Carmen Rivas", "Diego Montalvo", "Rosa Quiroga", "Hugo Benavides", "Elena Castañeda", "Pablo Iturbe"];
const CATEGORIAS = ["Novela", "Ensayo", "Poesía", "Cuento", "Historia", "Fantasía", "Viajes", "Ciencia", "Biografía", "Infantil", "Teatro", "Arte"];
const EDITORIALES = ["Ediciones Faro", "Casa del Río", "Lumbre", "Papel Vivo", "Grulla", "Altamar", "Orilla", "Sextante", "Bruma", "Tinta Sur", "El Telar", "Mirador"];

/* Pedidos de ejemplo del cliente (los usan Mis pedidos y Detalle de pedido).
   libros: [idLibro, cantidad]; fecha: fecha de compra */
const PEDIDOS = [
  { estado: "almacen",   num: 1046, fecha: "20/11/2026", hora: "18:05", libros: [[10, 1]],                        cupon: false },
  { estado: "camino",    num: 1045, fecha: "19/11/2026", hora: "10:42", libros: [[1, 2], [2, 1]],                 cupon: true },
  { estado: "entregado", num: 1032, fecha: "12/11/2026", hora: "09:15", libros: [[3, 1], [7, 2]],                 cupon: false },
  { estado: "devuelto",  num: 1019, fecha: "28/10/2026", hora: "16:30", libros: [[6, 1]],                         cupon: false },
  { estado: "cancelado", num: 1011, fecha: "15/10/2026", hora: "11:20", libros: [[4, 1], [14, 1], [9, 1], [12, 1]], cupon: false }
];
const ESTADOS = { almacen: ["En almacén", "neutral"], camino: ["En camino", "info"], entregado: ["Entregado", "ok"], devuelto: ["Devuelto", "warn"], cancelado: ["Cancelado", "bad"] };
const ENVIO = 10;

/* ---------- Utilidades ---------- */
const soles = n => "S/ " + n.toLocaleString("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
const final = b => b.d ? +(b.p * (1 - b.d / 100)).toFixed(2) : b.p;
const libro = id => LIBROS.find(b => b.id === +id) || LIBROS[0];
const iniciales = s => s.split(" ").filter(w => w.length > 2 || w === s).slice(0, 2).map(w => w[0]).join("").toUpperCase();
const estrellas = r => "★".repeat(Math.round(r)) + `<span class="off">${"★".repeat(5 - Math.round(r))}</span>`;
const ROL = new URLSearchParams(location.search).get("rol") || document.body?.dataset.rol || "cliente";

/* ---------- Piezas de libro (BookCover, PriceTag, BookCard) ---------- */
function cover(b, size = "") {
  return `<div class="bk-cover bk-cover-type bk-c${b.id % 6} ${size}" role="img" aria-label="Portada de ${b.t}">
      <span class="t">${b.t}</span><span class="a">${b.a}</span></div>`;
}
function price(b, extra = "") {
  return b.d
    ? `<div class="bk-price is-sale ${extra}"><span class="now">${soles(final(b))}</span><span class="was">${soles(b.p)}</span></div>`
    : `<div class="bk-price ${extra}"><span class="now">${soles(b.p)}</span></div>`;
}
function bookCard(b, quitar = false) {
  return `<div class="position-relative">
    <a class="bk-book ${b.s === 0 ? "is-out" : ""}" href="libro.html?id=${b.id}" title="${b.t}">
      ${cover(b)}
      <div class="bk-book-title">${b.t}</div>
      <div class="bk-book-author">${b.a}</div>
      ${price(b)}${b.s === 0 ? '<span class="bk-tag bk-tag-out mt-1">Agotado</span>' : ""}</a>
    ${quitar ? `<button class="btn btn-sm btn-light bk-card-remove" data-toast="Quitado de la lista" data-remove-card aria-label="Quitar ${b.t} de la lista"><i class="bi bi-x-lg"></i></button>` : ""}</div>`;
}
/* Importes de un pedido: subtotal con precios finales, cupón del 10 % si lo usó, envío fijo */
function importes(p) {
  const subtotal = p.libros.reduce((s, [id, q]) => s + final(libro(id)) * q, 0);
  const cupon = p.cupon ? +(subtotal * 0.10).toFixed(2) : 0;
  return { subtotal, cupon, envio: ENVIO, total: +(subtotal - cupon + ENVIO).toFixed(2), unidades: p.libros.reduce((s, [, q]) => s + q, 0) };
}

/* Pila de portadas (CoverStack, UX-UI.md §6.19): hasta 3 portadas superpuestas y "+n" si hay más */
function coverStack(ids) {
  const visibles = ids.slice(0, 3), resto = ids.length - visibles.length;
  return `<div class="bk-cover-stack" aria-label="${ids.map(id => libro(id).t).join(", ")}" role="img">
    ${visibles.map(id => `<div class="bk-cover-stack-item">${cover(libro(id), "bk-cover-sm")}</div>`).join("")}
    ${resto > 0 ? `<span class="bk-cover-stack-more">+${resto}</span>` : ""}</div>`;
}

/* <div data-books="desde,cantidad[,quitar]"> */
function renderBooks() {
  document.querySelectorAll("[data-books]").forEach(el => {
    const [from, n, q] = el.dataset.books.split(",");
    const items = Array.from({ length: +n }, (_, i) => LIBROS[(+from + i) % LIBROS.length]);
    el.innerHTML = items.map(b => `<div class="col">${bookCard(b, q === "quitar")}</div>`).join("");
  });
}
function entityCard(nombre, tipo) {
  const round = tipo === "autor" ? "round" : "";
  return `<a class="bk-entity" href="entidad.html?tipo=${tipo}&n=${encodeURIComponent(nombre)}"><div class="bk-entity-img ${round}">${imgEntidad(nombre, tipo)}</div><div class="bk-entity-name">${nombre}</div></a>`;
}
/* Imagen de autor, categoría o editorial (docs/ui/img/{tipo}/{slug}.svg); si no existe, iniciales */
const slug = s => s.normalize("NFD").replace(/[̀-ͯ]/g, "").toLowerCase().replace(/[^a-z0-9]+/g, "-");
function imgEntidad(nombre, tipo) {
  return `<img src="img/${tipo}/${slug(nombre)}.svg" alt="${tipo === "autor" ? "Retrato de " : ""}${nombre}" onerror="this.replaceWith(document.createTextNode('${iniciales(nombre)}'))">`;
}

/* ---------- Vista de resultados (ResultsView, UX-UI.md §6.7) ----------
   <div data-results='{"orden":true,"periodo":"registro","desde":0,"n":12,"cols":4,"ocultar":"categoria","quitar":false,"chips":true}'> */
/* Filtro de selección múltiple (CheckFilter, UX-UI.md §6.7):
   lista de casillas con buscador propio, 5 opciones visibles y "Ver más". */
const VISIBLES = 5;
function checkFilter(key, titulo, plural, opciones, marcadas = []) {
  const items = opciones.map((x, i) => `
    <div class="form-check bk-check ${i >= VISIBLES && !marcadas.includes(x) ? "is-extra" : ""}" data-nombre="${x.toLowerCase()}">
      <input class="form-check-input" type="checkbox" id="f-${key}-${i}" value="${x}" data-grupo="${key}" data-titulo="${titulo}" ${marcadas.includes(x) ? "checked" : ""}>
      <label class="form-check-label" for="f-${key}-${i}">${x}</label>
    </div>`).join("");
  const extra = opciones.length - VISIBLES;
  return `<fieldset class="bk-filter-group bk-check-group" data-grupo="${key}">
    <legend class="form-label d-flex align-items-center gap-2 w-100">${titulo}
      <span class="bk-count" data-count></span>
      <button type="button" class="btn btn-link btn-sm p-0 ms-auto fw-normal" data-limpiar-grupo hidden>Limpiar</button></legend>
    ${opciones.length > 8 ? `<div class="bk-search bk-search-sm mb-2"><i class="bi bi-search"></i><input class="form-control form-control-sm" type="search" placeholder="Buscar ${plural}" aria-label="Buscar ${plural}" data-buscar-en-grupo></div>` : ""}
    <div class="bk-check-list">${items}</div>
    <p class="small text-muted mb-0 mt-1" data-sin-coincidencias hidden>Sin coincidencias.</p>
    ${extra > 0 ? `<button type="button" class="btn btn-link btn-sm p-0 mt-1" data-ver-mas data-mas="Ver más (${extra})" data-menos="Ver menos">Ver más (${extra})</button>` : ""}
  </fieldset>`;
}

function resultsView(o) {
  const grupos = [
    ["categoria", "Categoría", "categorías", CATEGORIAS, o.chips ? ["Novela", "Fantasía"] : []],
    ["autor", "Autor", "autores", AUTORES, []],
    ["editorial", "Editorial", "editoriales", EDITORIALES, []]
  ].filter(([k]) => k !== o.ocultar).map(g => checkFilter(...g)).join("");
  const selects = grupos + `<div class="bk-filter-group"><label class="form-label" for="f-idioma">Idioma</label>
     <select class="form-select" id="f-idioma"><option>Todos</option><option>Español</option><option>Inglés</option></select></div>`;
  const ayudaPeriodo = o.periodo === "ventas" ? "Fecha de las ventas consideradas." : "Fecha de registro del libro.";
  const orden = o.orden
    ? `<label class="small text-muted text-nowrap" for="orden">Ordenar por</label>
       <select class="form-select form-select-sm" id="orden" style="width:auto"><option>Título A–Z</option><option>Título Z–A</option><option>Precio: menor a mayor</option><option>Precio: mayor a menor</option></select>`
    : `<span class="small text-muted"><i class="bi bi-info-circle me-1"></i>Ordenado por ${o.criterio || "relevancia"}</span>`;
  /* Los chips de casillas se generan desde las marcadas (ver actualizarChips) */
  const chips = `<span class="small text-muted bk-num">${o.total || 124} libros</span><span class="d-contents" data-chips></span>`;
  return `<div class="row g-5">
    <aside class="col-3">
      <form class="bk-filters" aria-label="Filtros">
        <div class="d-flex align-items-baseline mb-3"><p class="bk-caption mb-0">Filtros</p>
          <button type="button" class="btn btn-link btn-sm p-0 ms-auto" data-limpiar-todo data-limpiar-panel hidden>Limpiar todo</button></div>
        <div class="bk-filter-group"><label class="form-label" for="f-q">Título</label><input class="form-control" id="f-q" type="search" placeholder="Buscar por título"></div>
        ${selects}
        <fieldset class="bk-filter-group"><legend class="form-label">Precio</legend>
          <div class="d-flex gap-2 align-items-center">
            <div class="input-group input-group-sm"><span class="input-group-text">S/</span><input class="form-control" type="number" placeholder="Mín." aria-label="Precio mínimo"></div><span class="text-muted">–</span>
            <div class="input-group input-group-sm"><span class="input-group-text">S/</span><input class="form-control" type="number" placeholder="Máx." aria-label="Precio máximo"></div>
          </div></fieldset>
        <fieldset class="bk-filter-group border-0"><legend class="form-label">Período</legend>
          <div class="row g-2">
            <div class="col-6"><label class="small text-muted" for="f-des">Desde</label><input class="form-control form-control-sm" id="f-des" type="date"></div>
            <div class="col-6"><label class="small text-muted" for="f-has">Hasta</label><input class="form-control form-control-sm" id="f-has" type="date"></div>
          </div><div class="form-text mt-2">${ayudaPeriodo}</div></fieldset>
      </form>
    </aside>
    <section class="col-9" aria-label="Resultados">
      <div class="bk-toolbar"><div class="d-flex flex-wrap align-items-center gap-2">${chips}</div><div class="ms-auto d-flex align-items-center gap-2">${orden}</div></div>
      <div class="bk-grid-${o.cols || 4}" data-books="${o.desde || 0},${o.n || 12}${o.quitar ? ",quitar" : ""}"></div>
      <nav class="d-flex justify-content-center mt-5" aria-label="Páginas de resultados"><ul class="pagination mb-0">
        <li class="page-item disabled"><span class="page-link" aria-label="Anterior"><i class="bi bi-chevron-left"></i></span></li>
        <li class="page-item active"><span class="page-link" aria-current="page">1</span></li>
        <li class="page-item"><a class="page-link" href="#">2</a></li><li class="page-item"><a class="page-link" href="#">3</a></li>
        <li class="page-item disabled"><span class="page-link">…</span></li><li class="page-item"><a class="page-link" href="#">11</a></li>
        <li class="page-item"><a class="page-link" href="#" aria-label="Siguiente"><i class="bi bi-chevron-right"></i></a></li></ul></nav>
    </section></div>`;
}

/* ---------- Estado vacío (EmptyState) ---------- */
function emptyState(titulo, texto, accion, href) {
  return `<div class="bk-empty">
    <svg width="64" height="64" viewBox="0 0 64 64" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><path d="M8 14c8-3 16-3 24 2v36c-8-5-16-5-24-2V14Z"/><path d="M56 14c-8-3-16-3-24 2v36c8-5 16-5 24-2V14Z"/></svg>
    <h2 class="h3">${titulo}</h2><p class="text-muted mb-4">${texto}</p>${accion ? `<a class="btn btn-outline-primary" href="${href}">${accion}</a>` : ""}</div>`;
}

/* ---------- Página de administración de autores, categorías y editoriales ---------- */
/* Acciones de una fila de administración (UX-UI.md §6.12): Editar arriba y Desactivar/Activar debajo.
   attrsEditar: atributos del enlace o botón Editar (href o data-modal). g: "o"/"a" para concordar el aviso. */
function accionesFila(attrsEditar, activo, nombre, g = "o", extra = "") {
  const editar = attrsEditar.startsWith("href")
    ? `<a class="btn btn-sm btn-link text-decoration-none p-0" ${attrsEditar}>Editar</a>`
    : `<button type="button" class="btn btn-sm btn-link text-decoration-none p-0" ${attrsEditar}>Editar</button>`;
  const estado = activo
    ? `<button type="button" class="btn btn-sm btn-link text-decoration-none p-0 text-danger" data-confirm="¿Desactivar «${nombre}»?" data-msg="Dejará de mostrarse en la tienda. Lo ya registrado (pedidos, listas, reseñas) no cambia." data-accion="Sí, desactivar" data-ok="«${nombre}» desactivad${g}" data-peligro>Desactivar</button>`
    : `<button type="button" class="btn btn-sm btn-link text-decoration-none p-0" data-confirm="¿Activar «${nombre}»?" data-msg="Volverá a mostrarse en la tienda." data-accion="Sí, activar" data-ok="«${nombre}» activad${g}">Activar</button>`;
  return `<div class="d-inline-flex flex-column align-items-end gap-1">${editar}${extra}${estado}</div>`;
}

/* Diálogo "Agregar descuento" (lo usan la lista de libros y la consulta del libro).
   El botón que lo abre lleva data-libro con el título, que se muestra en el diálogo. */
function modalDescuento() {
  return `<div class="modal fade" id="m-desc" tabindex="-1" aria-labelledby="m-desc-t" aria-hidden="true"><div class="modal-dialog modal-dialog-centered"><form class="modal-content p-2">
    <div class="modal-header border-0 pb-0"><div><h2 class="modal-title h3" id="m-desc-t">Agregar descuento</h2><div class="small text-muted" id="m-desc-libro"></div></div><button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Cerrar"></button></div>
    <div class="modal-body">
      <label class="form-label" for="dp">Porcentaje</label><div class="input-group mb-3" style="max-width:9rem"><input class="form-control bk-num" id="dp" type="number" value="15"><span class="input-group-text">%</span></div>
      <div class="row g-3">
        <div class="col-6"><label class="form-label" for="dd">Desde</label><input class="form-control is-invalid" id="dd" type="date" value="2026-12-10" aria-describedby="dd-err"></div>
        <div class="col-6"><label class="form-label" for="dh">Hasta</label><input class="form-control is-invalid" id="dh" type="date" value="2026-12-20" aria-describedby="dd-err"></div>
        <div class="col-12 mt-1"><div class="text-danger small" id="dd-err">Se superpone con el descuento del 01/12 al 15/12.</div></div>
      </div>
    </div>
    <div class="modal-footer border-0"><button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal">Cancelar</button><button type="button" class="btn btn-primary" data-bs-dismiss="modal" data-toast="Descuento agregado">Agregar descuento</button></div>
  </form></div></div>`;
}
document.addEventListener("show.bs.modal", e => {
  if (e.target.id === "m-desc") document.getElementById("m-desc-libro").textContent = e.relatedTarget?.dataset.libro || "";
});
/* Pie de tabla de administración: rango mostrado y paginación (filas por página: porPagina) */
function pieTabla(mostrados, total, porPagina) {
  const paginas = Math.max(1, Math.ceil(total / porPagina));
  return `<div class="d-flex justify-content-between align-items-center mt-3">
    <span class="small text-muted bk-num">1–${mostrados} de ${total}</span>
    <ul class="pagination pagination-sm mb-0">
      <li class="page-item disabled"><span class="page-link"><i class="bi bi-chevron-left"></i></span></li>
      <li class="page-item active"><span class="page-link">1</span></li>
      ${Array.from({ length: Math.min(paginas, 3) - 1 }, (_, i) => `<li class="page-item"><a class="page-link" href="#">${i + 2}</a></li>`).join("")}
      <li class="page-item ${paginas === 1 ? "disabled" : ""}"><${paginas === 1 ? "span" : "a href=\"#\""} class="page-link"><i class="bi bi-chevron-right"></i></${paginas === 1 ? "span" : "a"}></li>
    </ul></div>`;
}

/* Vistas de consulta y edición en la misma pantalla (Perfil, Libro, Configuración).
   [data-vista="ver"|"editar"] se muestran según el modo; [data-cambiar-vista] cambia de modo; ?editar=1 abre en edición. */
function cambiarVista(modo) {
  document.querySelectorAll("[data-vista]").forEach(el => el.hidden = el.dataset.vista !== modo);
  if (modo === "editar") document.querySelector('[data-vista="editar"] input:not([readonly]):not([type=checkbox]), [data-vista="editar"] textarea')?.focus();
  window.scrollTo(0, 0);
}
document.addEventListener("click", e => {
  const b = e.target.closest("[data-cambiar-vista]");
  if (b) cambiarVista(b.dataset.cambiarVista);
});

function adminEntidadesPage(tipo) {
  const cfg = {
    autor: ["Autores", "autor", AUTORES, "Biografía", "Nuevo autor"],
    categoria: ["Categorías", "categoría", CATEGORIAS, "Descripción", "Nueva categoría"],
    editorial: ["Editoriales", "editorial", EDITORIALES, "Descripción", "Nueva editorial"]
  }[tipo];
  const [titulo, nombre, lista, campo, nuevo] = cfg;
  const round = tipo === "autor" ? "round" : "";
  document.querySelector("main").innerHTML = `
    <div class="bk-page-head"><div><h1>${titulo}</h1><p class="small text-muted mb-0 mt-1 bk-num">${lista.length} registrados</p></div>
      <button class="btn btn-primary" data-modal="#ent-form" data-titulo="${nuevo}"><i class="bi bi-plus-lg me-1"></i>${nuevo}</button></div>
    <div class="d-flex gap-2 mb-3">
      <div class="bk-search" style="max-width:20rem"><i class="bi bi-search"></i><input class="form-control" type="search" placeholder="Buscar por nombre" aria-label="Buscar por nombre" style="background:var(--bk-surface)"></div>
      <select class="form-select" style="width:auto" aria-label="Estado"><option>Todos</option><option>Activos</option><option>Inactivos</option></select>
    </div>
    <div class="bk-table-wrap"><table class="table table-hover bk-table">
      <thead><tr><th style="width:4rem"><span class="visually-hidden">Imagen</span></th><th>Nombre</th><th class="text-end">Libros</th><th>Estado</th><th class="text-end"><span class="visually-hidden">Acciones</span></th></tr></thead>
      <tbody>${lista.map((n, i) => `<tr>
        <td><div class="bk-entity-img ${round} m-0" style="width:2.5rem;font-size:.8rem">${imgEntidad(n, tipo)}</div></td>
        <td class="fw-semibold">${n}</td><td class="text-end bk-num">${(i * 7) % 13 + 2}</td>
        <td>${i === 9 ? '<span class="bk-status bk-status-neutral">Inactivo</span>' : '<span class="bk-status bk-status-ok">Activo</span>'}</td>
        <td class="text-end">${accionesFila(`data-modal="#ent-form" data-titulo="Editar ${nombre}" data-nombre="${n}"`, i !== 9, n, tipo === "categoria" || tipo === "editorial" ? "a" : "o")}</td></tr>`).join("")}</tbody></table></div>
    ${pieTabla(lista.length, lista.length, 20)}
    <div class="modal fade" id="ent-form" tabindex="-1" aria-labelledby="ent-form-t" aria-hidden="true"><div class="modal-dialog modal-dialog-centered"><form class="modal-content p-2">
      <div class="modal-header border-0 pb-0"><h2 class="modal-title h3" id="ent-form-t">${nuevo}</h2><button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Cerrar"></button></div>
      <div class="modal-body">
        <div class="d-flex align-items-center gap-3 mb-3"><div class="bk-entity-img ${round} m-0" style="width:4.5rem" id="ent-img"><i class="bi bi-image text-muted fs-4"></i></div><button type="button" class="btn btn-sm btn-outline-secondary">Elegir imagen</button></div>
        <label class="form-label" for="ent-n">Nombre</label><input class="form-control mb-3" id="ent-n">
        <label class="form-label" for="ent-d">${campo}</label><textarea class="form-control mb-3" id="ent-d" rows="4"></textarea>
        <div class="form-check form-switch" id="ent-activo"><input class="form-check-input" type="checkbox" id="ent-a" checked><label class="form-check-label" for="ent-a">Activo</label>
          <div class="form-text">Si está inactivo, no se muestra en la tienda ni se ofrece al registrar libros.</div></div>
      </div>
      <div class="modal-footer border-0"><button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal">Cancelar</button><button type="button" class="btn btn-primary" data-bs-dismiss="modal" data-toast="Cambios guardados">Guardar</button></div>
    </form></div></div>`;
  document.getElementById("ent-form").addEventListener("show.bs.modal", e => {
    const b = e.relatedTarget || document.activeElement;
    document.getElementById("ent-form-t").textContent = b?.dataset?.titulo || nuevo;
    document.getElementById("ent-n").value = b?.dataset?.nombre || "";
    document.getElementById("ent-activo").hidden = !!b?.dataset?.nombre;
    document.getElementById("ent-img").innerHTML = b?.dataset?.nombre ? imgEntidad(b.dataset.nombre, tipo) : '<i class="bi bi-image text-muted fs-4"></i>';
  });
}

/* ---------- Encabezado de la tienda ---------- */
const MENU_CUENTA = [
  ["Compras", [["Pedidos", "cuenta-pedidos.html"], ["Mis solicitudes", "cuenta-solicitudes.html"], ["Cupones", "cuenta-cupones.html"]]],
  ["Mi biblioteca", [["Mis listas", "listas.html"], ["Seguimientos", "cuenta-seguimientos.html"], ["Mis reseñas", "cuenta-resenas.html"]]],
  ["Cuenta", [["Perfil", "cuenta-datos.html"], ["Direcciones", "cuenta-direcciones.html"], ["Métodos de pago", "cuenta-metodos.html"]]]
];

function header(nav) {
  const cur = k => nav === k ? 'aria-current="page"' : "";
  let derecha;
  if (ROL === "visitante") {
    derecha = `<a class="btn btn-icon bk-cart-btn" href="carrito.html" data-auth aria-label="Carrito"><i class="bi bi-bag fs-5"></i></a>
      <a class="btn btn-sm btn-primary" href="login.html">Iniciar sesión</a>
      <a class="small fw-semibold text-decoration-none ms-1" href="registro.html">Registrarse</a>`;
  } else if (ROL === "admin") {
    derecha = `<a class="btn btn-sm btn-primary" href="admin-libros.html"><i class="bi bi-speedometer2 me-1"></i>Panel de administración</a>
      <a class="small text-decoration-none text-muted ms-2" href="inicio.html?rol=visitante">Cerrar sesión</a>`;
  } else {
    const grupos = MENU_CUENTA.map(([g, items], i) =>
      `${i ? '<li><hr class="dropdown-divider"></li>' : ""}<li><h6 class="dropdown-header">${g}</h6></li>` +
      items.filter(([n]) => g !== "Cuenta" || n === "Perfil").map(([n, h]) => `<li><a class="dropdown-item" href="${h}">${n}</a></li>`).join("")).join("");
    derecha = `<a class="btn btn-icon bk-cart-btn" href="carrito.html" aria-label="Carrito, 3 libros"><i class="bi bi-bag fs-5"></i><span class="bk-cart-count">3</span></a>
      <div class="dropdown">
        <button class="btn btn-sm d-flex align-items-center gap-2 border-0" data-bs-toggle="dropdown" aria-expanded="false">
          <span class="bk-avatar">MS</span><span class="fw-semibold">Mariana</span><i class="bi bi-chevron-down small"></i></button>
        <ul class="dropdown-menu dropdown-menu-end">${grupos}<li><hr class="dropdown-divider"></li><li><a class="dropdown-item" href="inicio.html?rol=visitante">Cerrar sesión</a></li></ul>
      </div>`;
  }
  return `<a class="bk-skip" href="#contenido">Saltar al contenido</a>
  <header class="bk-header"><div class="container-xl bk-header-inner">
    <a class="bk-logo" href="inicio.html">Book<span>Shop</span></a>
    <nav class="bk-nav d-flex gap-4" aria-label="Principal">
      <a href="catalogo.html" ${cur("catalogo")}>Catálogo</a>
      <div class="dropdown"><a href="#" ${cur("explorar")} data-bs-toggle="dropdown" aria-expanded="false">Explorar <i class="bi bi-chevron-down small"></i></a>
        <ul class="dropdown-menu"><li><a class="dropdown-item" href="explorar.html?tipo=autor">Autores</a></li><li><a class="dropdown-item" href="explorar.html?tipo=categoria">Categorías</a></li><li><a class="dropdown-item" href="explorar.html?tipo=editorial">Editoriales</a></li></ul></div>
    </nav>
    <form class="bk-search mx-auto" role="search" action="catalogo.html">
      <i class="bi bi-search"></i><input class="form-control" type="search" name="q" placeholder="Buscar libros por título" aria-label="Buscar libros por título">
    </form>
    <div class="d-flex align-items-center gap-2">${derecha}</div>
  </div></header>`;
}

function footer() {
  return `<footer class="bk-footer"><div class="container-xl d-flex justify-content-between gap-2">
    <span><span class="bk-logo fs-6">Book<span>Shop</span></span> · Trabajo académico, Programación 3</span>
    <span><a class="text-reset" href="catalogo.html">Catálogo</a> · <a class="text-reset" href="explorar.html?tipo=autor">Explorar</a> · <a class="text-reset" href="index.html">Mapa de pantallas</a></span></div></footer>`;
}

/* ---------- Menú lateral de Mi cuenta ---------- */
function accountNav(nav) {
  return `<nav class="bk-account-nav" aria-label="Mi cuenta">
    <div class="d-flex align-items-center gap-2 mb-2 px-2">
      <span class="bk-avatar" style="width:2.5rem;height:2.5rem">MS</span>
      <div class="lh-sm"><strong>Mariana Soto</strong><div class="small text-muted">mariana@correo.pe</div></div>
    </div>
    ${MENU_CUENTA.map(([g, items]) => `<p class="bk-caption">${g}</p>` +
      items.map(([n, h]) => `<a href="${h}" ${h.startsWith(nav) ? 'aria-current="page"' : ""}>${n}</a>`).join("")).join("")}
    <a href="inicio.html?rol=visitante" class="mt-3 text-muted">Cerrar sesión</a>
  </nav>`;
}

/* ---------- Administración ---------- */
const MENU_ADMIN = [
  ["Catálogo", [["Libros", "admin-libros", "bi-book"], ["Autores", "admin-autores", "bi-person"], ["Categorías", "admin-categorias", "bi-tags"], ["Editoriales", "admin-editoriales", "bi-building"]]],
  ["Moderación", [["Devoluciones", "admin-devoluciones", "bi-arrow-return-left"], ["Reportes de reseñas", "admin-reportes-resenas", "bi-flag"]]],
  ["Sistema", [["Configuración", "admin-configuracion", "bi-sliders"], ["Reportes PDF", "admin-reportes", "bi-file-earmark-bar-graph"]]]
];
function adminNav(nav) {
  return `<nav class="bk-admin-nav" aria-label="Administración">
    <a class="bk-logo d-block px-2 mb-1" href="admin-libros.html" style="font-size:1.35rem">Book<span>Shop</span></a>
    <div class="small text-muted px-2">Administración</div>
    ${MENU_ADMIN.map(([g, items]) => `<p class="bk-caption">${g}</p>` +
      items.map(([n, h, i]) => `<a href="${h}.html" ${nav === h ? 'aria-current="page"' : ""}><i class="bi ${i}"></i>${n}</a>`).join("")).join("")}
  </nav>`;
}
function adminTopbar() {
  return `<div class="bk-admin-top">
    <a class="small fw-semibold text-decoration-none" href="inicio.html?rol=admin"><i class="bi bi-box-arrow-up-right me-1"></i>Ver tienda</a>
    <span class="bk-avatar">AD</span><span class="small fw-semibold">Administrador</span>
    <a class="small text-decoration-none text-muted" href="inicio.html?rol=visitante">Cerrar sesión</a></div>`;
}

/* ---------- Avisos (ToastHost) ---------- */
function aviso(texto, enlace) {
  let host = document.getElementById("bk-toasts");
  if (!host) { host = document.createElement("div"); host.id = "bk-toasts"; host.className = "toast-container position-fixed bottom-0 end-0 p-3"; document.body.appendChild(host); }
  const el = document.createElement("div");
  el.className = "toast bk-toast"; el.setAttribute("role", "status");
  el.innerHTML = `<div class="d-flex align-items-center gap-3 px-3 py-2"><i class="bi bi-check2-circle"></i><span>${texto}</span>
    ${enlace ? `<a class="ms-auto fw-semibold" href="${enlace[1]}">${enlace[0]}</a>` : ""}
    <button type="button" class="btn-close btn-close-white ${enlace ? "" : "ms-auto"}" data-bs-dismiss="toast" aria-label="Cerrar"></button></div>`;
  host.appendChild(el);
  new bootstrap.Toast(el, { delay: 4000 }).show();
}

/* ---------- Confirmación (ConfirmModal) ----------
   <button data-confirm="¿Título?" data-msg="Consecuencia" data-accion="Sí, hacer"
           data-ok="Texto del aviso" data-href="destino.html" data-peligro> */
function confirmar(btn) {
  let m = document.getElementById("bk-confirm");
  if (!m) {
    document.body.insertAdjacentHTML("beforeend", `<div class="modal fade" id="bk-confirm" tabindex="-1" aria-labelledby="bk-confirm-t" aria-hidden="true">
      <div class="modal-dialog modal-dialog-centered"><div class="modal-content p-2">
        <div class="modal-header border-0 pb-0"><h2 class="modal-title h3" id="bk-confirm-t"></h2></div>
        <div class="modal-body" id="bk-confirm-m"></div>
        <div class="modal-footer border-0"><button class="btn btn-outline-secondary" data-bs-dismiss="modal">Volver</button><button class="btn" id="bk-confirm-ok"></button></div>
      </div></div></div>`);
    m = document.getElementById("bk-confirm");
  }
  m.querySelector("#bk-confirm-t").textContent = btn.dataset.confirm;
  m.querySelector("#bk-confirm-m").textContent = btn.dataset.msg || "";
  const ok = m.querySelector("#bk-confirm-ok");
  ok.textContent = btn.dataset.accion || "Confirmar";
  ok.className = "btn " + ("peligro" in btn.dataset ? "btn-danger" : "btn-primary");
  ok.onclick = () => {
    bootstrap.Modal.getInstance(m).hide();
    if (btn.dataset.href) { location.href = btn.dataset.href; return; }
    if (btn.dataset.ok) aviso(btn.dataset.ok);
    const fila = btn.closest("[data-removible]");
    if (fila) fila.remove();
  };
  bootstrap.Modal.getOrCreateInstance(m).show();
}

/* ---------- Comparación (CompareBar, en la sesión) ---------- */
const cmp = {
  get: () => JSON.parse(sessionStorage.getItem("bk-cmp") || "[]"),
  set: v => { sessionStorage.setItem("bk-cmp", JSON.stringify(v)); compareBar(); },
  add(id) {
    const v = cmp.get();
    if (v.includes(id)) return aviso("Este libro ya está en la comparación");
    if (v.length < 2) { cmp.set([...v, id]); return aviso("Agregado a la comparación"); }
    elegirReemplazo(id);
  }
};
function compareBar() {
  document.getElementById("bk-compare")?.remove();
  const v = cmp.get();
  if (!v.length || document.body.dataset.layout !== "tienda" || document.body.dataset.nav === "comparar") return;
  const slots = v.map(id => `<span class="slot"><span>${libro(id).t}</span><button data-cmp-del="${id}" aria-label="Quitar ${libro(id).t} de la comparación"><i class="bi bi-x"></i></button></span>`).join("");
  document.body.insertAdjacentHTML("beforeend", `<div class="bk-compare" id="bk-compare" role="region" aria-label="Comparación">
    <span class="fw-semibold">Comparar</span>${slots}${v.length < 2 ? '<span class="slot empty"><span>Elige otro libro</span></span>' : ""}
    <a class="btn btn-sm btn-light ${v.length < 2 ? "disabled" : ""}" href="comparar.html">Ver comparación</a></div>`);
}
function elegirReemplazo(nuevo) {
  const v = cmp.get();
  document.getElementById("bk-repl")?.remove();
  document.body.insertAdjacentHTML("beforeend", `<div class="modal fade" id="bk-repl" tabindex="-1" aria-labelledby="bk-repl-t" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered"><div class="modal-content p-2">
      <div class="modal-header border-0 pb-0"><h2 class="modal-title h3" id="bk-repl-t">¿Qué libro quieres reemplazar?</h2></div>
      <div class="modal-body"><p class="text-muted">Solo se pueden comparar dos libros. «${libro(nuevo).t}» ocupará el lugar del que elijas.</p>
        <div class="d-grid gap-2">${v.map(id => `<button class="btn btn-outline-primary text-start" data-repl="${id}">${libro(id).t}</button>`).join("")}</div></div>
      <div class="modal-footer border-0"><button class="btn btn-outline-secondary" data-bs-dismiss="modal">Volver</button></div>
    </div></div></div>`);
  const m = document.getElementById("bk-repl");
  m.querySelectorAll("[data-repl]").forEach(b => b.onclick = () => {
    cmp.set(v.map(id => id === +b.dataset.repl ? nuevo : id));
    bootstrap.Modal.getInstance(m).hide(); aviso("Comparación actualizada");
  });
  new bootstrap.Modal(m).show();
}

/* ---------- Filtros de selección múltiple: estado y chips ---------- */
function actualizarFiltros() {
  document.querySelectorAll(".bk-check-group").forEach(g => {
    const n = g.querySelectorAll("input[type=checkbox]:checked").length;
    g.querySelector("[data-count]").textContent = n ? n : "";
    g.querySelector("[data-limpiar-grupo]").hidden = !n;
  });
  document.querySelectorAll("[data-chips]").forEach(zona => {
    const vista = zona.closest(".row");
    const marcadas = [...vista.querySelectorAll(".bk-check-group input:checked")];
    const otros = [...vista.querySelectorAll(".bk-filters input:not([type=checkbox]):not([data-buscar-en-grupo])")].some(i => i.value) ||
      [...vista.querySelectorAll(".bk-filters select")].some(s => s.selectedIndex > 0);
    vista.querySelector("[data-limpiar-panel]").hidden = !(marcadas.length || otros);
    zona.innerHTML = marcadas.map(c =>
      `<span class="bk-chip">${c.dataset.titulo}: ${c.value} <button type="button" data-chip-quitar="${c.id}" aria-label="Quitar ${c.value} del filtro ${c.dataset.titulo.toLowerCase()}"><i class="bi bi-x"></i></button></span>`).join("") +
      (marcadas.length || otros ? `<button type="button" class="btn btn-sm btn-link text-decoration-none" data-limpiar-todo>Limpiar filtros</button>` : "");
  });
}
document.addEventListener("change", e => { if (e.target.closest(".bk-filters")) actualizarFiltros(); });
document.addEventListener("input", e => { if (e.target.closest(".bk-filters") && !e.target.matches("[data-buscar-en-grupo]")) actualizarFiltros(); });
document.addEventListener("input", e => {
  if (!e.target.matches("[data-buscar-en-grupo]")) return;
  const g = e.target.closest(".bk-check-group"), q = e.target.value.trim().toLowerCase();
  g.classList.toggle("is-searching", !!q);
  let hay = 0;
  g.querySelectorAll(".bk-check").forEach(i => { const m = !q || i.dataset.nombre.includes(q); i.classList.toggle("match", m); hay += m; });
  g.querySelector("[data-sin-coincidencias]").hidden = !!hay;
});
document.addEventListener("click", e => {
  const t = e.target.closest("[data-chip-quitar],[data-limpiar-grupo],[data-limpiar-todo],[data-ver-mas]");
  if (!t) return;
  if (t.dataset.chipQuitar) document.getElementById(t.dataset.chipQuitar).checked = false;
  if ("limpiarGrupo" in t.dataset) t.closest(".bk-check-group").querySelectorAll("input:checked").forEach(c => c.checked = false);
  if ("limpiarTodo" in t.dataset) {
    /* Quita todos los filtros: casillas, título, idioma, precio y período */
    const vista = t.closest(".row");
    vista.querySelectorAll(".bk-check-group input:checked").forEach(c => c.checked = false);
    vista.querySelectorAll(".bk-filters input:not([type=checkbox]):not([data-buscar-en-grupo])").forEach(i => i.value = "");
    vista.querySelectorAll(".bk-filters select").forEach(s => s.selectedIndex = 0);
  }
  if ("verMas" in t.dataset) {
    const g = t.closest(".bk-check-group"), abierto = g.classList.toggle("is-open");
    t.textContent = abierto ? t.dataset.menos : t.dataset.mas;
    t.setAttribute("aria-expanded", abierto);
    return;
  }
  actualizarFiltros();
});

/* ---------- Estilos propios del prototipo ---------- */
document.head.insertAdjacentHTML("beforeend", `<style>
  .solo-visitante, .solo-cliente, .solo-admin { display: none !important; }
  .rol-visitante .solo-visitante, .rol-cliente .solo-cliente, .rol-admin .solo-admin { display: revert !important; }
  .rol-visitante .no-visitante, .rol-admin .no-admin { display: none !important; }
  .bk-toast { background: var(--bk-ink); color: #F7F4EE; border: 0; border-radius: var(--bk-radius); box-shadow: var(--bk-shadow-pop); font-size: .9375rem; }
  .bk-toast a { color: #CFE3D7; }
  .bk-card-remove { position: absolute; top: .4rem; right: .4rem; width: 1.9rem; height: 1.9rem; padding: 0; border-radius: 50%; opacity: .92; }
</style>`);

/* ---------- Montaje ---------- */
document.addEventListener("DOMContentLoaded", () => {
  const body = document.body;
  body.classList.add("rol-" + ROL);
  const layout = body.dataset.layout, nav = body.dataset.nav || "";
  const main = document.querySelector("main");

  if (layout === "tienda" || layout === "cuenta") {
    main.insertAdjacentHTML("beforebegin", header(nav));
    main.insertAdjacentHTML("afterend", footer());
  }
  if (layout === "cuenta") {
    main.innerHTML = `<div class="row g-5"><div class="col-3">${accountNav(nav)}</div><div class="col-9">${main.innerHTML}</div></div>`;
  }
  if (layout === "admin") {
    const wrap = document.createElement("div");
    wrap.className = "bk-admin";
    wrap.innerHTML = adminNav(nav) + `<div class="bk-admin-col">${adminTopbar()}</div>`;
    main.before(wrap);
    wrap.querySelector(".bk-admin-col").appendChild(main);
    body.insertAdjacentHTML("afterbegin", '<a class="bk-skip" href="#contenido">Saltar al contenido</a>');
  }
  if (layout === "auth") {
    main.insertAdjacentHTML("beforebegin", `<a class="bk-skip" href="#contenido">Saltar al contenido</a><div class="text-center pt-5 pb-4"><a class="bk-logo fs-2" href="inicio.html">Book<span>Shop</span></a></div>`);
  }

  document.querySelectorAll("[data-results]").forEach(el => el.innerHTML = resultsView(JSON.parse(el.dataset.results)));
  renderBooks();
  actualizarFiltros();
  document.querySelectorAll("[data-entities]").forEach(el => {
    const [tipo, n] = el.dataset.entities.split(",");
    const lista = { autor: AUTORES, categoria: CATEGORIAS, editorial: EDITORIALES }[tipo];
    el.innerHTML = lista.slice(0, +n).map(x => `<div class="col">${entityCard(x, tipo)}</div>`).join("");
  });

  /* Conserva el rol forzado al navegar (visitante o admin) */
  if (new URLSearchParams(location.search).get("rol")) {
    document.querySelectorAll('a[href$=".html"], a[href*=".html?"]').forEach(a => {
      if (!/rol=/.test(a.getAttribute("href"))) a.href = a.getAttribute("href") + (a.getAttribute("href").includes("?") ? "&" : "?") + "rol=" + ROL;
    });
  }
  compareBar();
  if (document.querySelector("[data-vista]")) cambiarVista(new URLSearchParams(location.search).has("editar") ? "editar" : "ver");
});

/* Acciones genéricas por atributo */
document.addEventListener("click", e => {
  const t = e.target.closest("[data-auth],[data-confirm],[data-toast],[data-cmp-add],[data-cmp-del],[data-press],[data-modal]");
  if (!t) return;
  if ("auth" in t.dataset && ROL === "visitante") {
    e.preventDefault();
    location.href = "login.html?aviso=1&volver=" + encodeURIComponent(location.pathname.split("/").pop() + location.search.replace(/[?&]rol=visitante/, ""));
    return;
  }
  if (t.dataset.modal) {
    e.preventDefault();
    const m = bootstrap.Modal.getOrCreateInstance(document.querySelector(t.dataset.modal));
    document.querySelector(t.dataset.modal).dispatchEvent(Object.assign(new Event("bk-open"), { origen: t }));
    return m.show(t);
  }
  if (t.dataset.confirm) { e.preventDefault(); return confirmar(t); }
  if (t.dataset.cmpAdd) { e.preventDefault(); return cmp.add(+t.dataset.cmpAdd); }
  if (t.dataset.cmpDel) { e.preventDefault(); return cmp.set(cmp.get().filter(id => id !== +t.dataset.cmpDel)); }
  if ("press" in t.dataset) {
    const on = t.getAttribute("aria-pressed") !== "true";
    t.setAttribute("aria-pressed", on);
    const [a, b] = t.dataset.press.split("|");
    t.querySelector(".lbl") && (t.querySelector(".lbl").textContent = on ? a : b);
    const i = t.querySelector(".bi"); if (i) i.className = i.className.replace(on ? /(bi-[a-z-]+?)(?=\s|$)/ : /-fill/, on ? "$1-fill" : "");
  }
  if (t.dataset.toast) {
    if ("removeCard" in t.dataset) { e.preventDefault(); t.closest(".col")?.remove(); }
    const [txt, l, h] = t.dataset.toast.split("|");
    aviso(txt, l ? [l, h] : null);
  }
});
