// Un solo diálogo de la aplicación: foco al abrir, Escape y tabulación dentro de él.
let dialogoAbierto = null;
let focoAnterior = null;
const controles = 'button:not([disabled]), a[href], input:not([disabled]), select:not([disabled]), textarea:not([disabled])';

new MutationObserver(() => {
    const dialogos = document.querySelectorAll('[role="dialog"]');
    const actual = dialogos[dialogos.length - 1] || null;
    if (actual !== dialogoAbierto) {
        if (actual) {
            focoAnterior = document.activeElement;
            actual.querySelector('input, button')?.focus();
        } else {
            focoAnterior?.focus();
        }
        dialogoAbierto = actual;
    }
    document.body.classList.toggle('dialog-open', actual !== null);
}).observe(document.body, { childList: true, subtree: true });

document.addEventListener('keydown', event => {
    if (!dialogoAbierto) return;
    if (event.key === 'Escape') {
        event.preventDefault();
        dialogoAbierto.querySelector('.modal-close')?.click();
    }
    if (event.key === 'Tab') {
        const elementos = dialogoAbierto.querySelectorAll(controles);
        const primero = elementos[0], ultimo = elementos[elementos.length - 1];
        if (event.shiftKey && document.activeElement === primero) {
            event.preventDefault();
            ultimo?.focus();
        } else if (!event.shiftKey && document.activeElement === ultimo) {
            event.preventDefault();
            primero?.focus();
        }
    }
});

// Descarga de PDF recibidos desde el servidor Blazor (comprobantes y reportes).
window.bookshopDescargar = async (nombreArchivo, flujo) => {
    const datos = await flujo.arrayBuffer();
    const url = URL.createObjectURL(new Blob([datos], { type: 'application/pdf' }));
    const enlace = document.createElement('a');
    enlace.href = url;
    enlace.download = nombreArchivo;
    document.body.appendChild(enlace);
    enlace.click();
    enlace.remove();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
};

// Visor de PDF dentro de la aplicación: convierte el flujo en una URL local para un <iframe>.
window.bookshopUrlPdf = async (flujo) => {
    const datos = await flujo.arrayBuffer();
    return URL.createObjectURL(new Blob([datos], { type: 'application/pdf' }));
};
window.bookshopLiberarUrl = (url) => URL.revokeObjectURL(url);
