package me.Model.dto.opinion;

// Distribucion de todas las resenas disponibles del libro, independiente de la pagina visible.
// Cantidad, suma para recomendaciones y promedio salen de los mismos recuentos, sin duplicar datos.
public record ValoracionLibroDTO(
        int idLibro,
        long cantidadUnaEstrella,
        long cantidadDosEstrellas,
        long cantidadTresEstrellas,
        long cantidadCuatroEstrellas,
        long cantidadCincoEstrellas
) {
    public long cantidadResenas() {
        return cantidadUnaEstrella + cantidadDosEstrellas + cantidadTresEstrellas
                + cantidadCuatroEstrellas + cantidadCincoEstrellas;
    }

    public long sumaPuntuaciones() {
        return cantidadUnaEstrella + 2L * cantidadDosEstrellas + 3L * cantidadTresEstrellas
                + 4L * cantidadCuatroEstrellas + 5L * cantidadCincoEstrellas;
    }

    public Double promedio() {
        long cantidad = cantidadResenas();
        return cantidad == 0 ? null : (double) sumaPuntuaciones() / cantidad;
    }
}
