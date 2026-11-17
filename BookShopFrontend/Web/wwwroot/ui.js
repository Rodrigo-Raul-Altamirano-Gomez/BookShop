// Descarga un PDF recibido desde el servidor Blazor (comprobantes y reportes).
window.bookshopDescargar = async (nombreArchivo, flujo) => {
    const datos = await flujo.arrayBuffer();
    const url = URL.createObjectURL(new Blob([datos], { type: 'application/pdf' }));
    const enlace = document.createElement('a');
    enlace.href = url;
    enlace.download = nombreArchivo;
    enlace.click();
    URL.revokeObjectURL(url);
};
