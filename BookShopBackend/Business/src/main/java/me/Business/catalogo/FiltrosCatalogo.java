package me.Business.catalogo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import me.Business.exceptions.ValidacionException;
import me.Model.dto.catalogo.FiltroLibroDTO;
import me.Model.dto.catalogo.OrdenLibro;

public final class FiltrosCatalogo {
    private FiltrosCatalogo() { }

    public static FiltroLibroDTO validar(FiltroLibroDTO filtro) throws ValidacionException {
        if (filtro == null) filtro = FiltroLibroDTO.porDefecto();
        if (filtro.pagina() < 1 || filtro.tamanioPagina() < 1)
            throw new ValidacionException("pagina", "La página y su tamaño deben ser positivos.");
        if (filtro.orden() == null || filtro.orden() == OrdenLibro.RECOMENDADOS_PRIMERO)
            throw new ValidacionException("orden", "Seleccione un orden de catalogo.");
        List<Integer> categorias = new ArrayList<>();
        for (Integer categoria : filtro.idsCategorias()) {
            if (categoria == null || categoria < 1)
                throw new ValidacionException("categorias", "Seleccione referencias válidas.");
            if (!categorias.contains(categoria)) categorias.add(categoria);
        }
        List<Integer> autores = new ArrayList<>();
        for (Integer autor : filtro.idsAutores()) {
            if (autor == null || autor < 1)
                throw new ValidacionException("autores", "Seleccione referencias válidas.");
            if (!autores.contains(autor)) autores.add(autor);
        }
        List<Integer> editoriales = new ArrayList<>();
        for (Integer editorial : filtro.idsEditoriales()) {
            if (editorial == null || editorial < 1)
                throw new ValidacionException("editoriales", "Seleccione referencias válidas.");
            if (!editoriales.contains(editorial)) editoriales.add(editorial);
        }
        List<String> idiomas = new ArrayList<>();
        for (String idioma : filtro.idiomas()) {
            if (idioma == null || idioma.isBlank() || idioma.length() > 100)
                throw new ValidacionException("idiomas", "Seleccione idiomas válidos.");
            if (!idiomas.contains(idioma.strip())) idiomas.add(idioma.strip());
        }
        if (filtro.precioMinimo() != null) importe(filtro.precioMinimo(), "precioMinimo", true);
        if (filtro.precioMaximo() != null) importe(filtro.precioMaximo(), "precioMaximo", true);
        if (filtro.precioMinimo() != null && filtro.precioMaximo() != null
                && filtro.precioMinimo().compareTo(filtro.precioMaximo()) > 0)
            throw new ValidacionException("precioMaximo", "El precio maximo no puede ser menor que el minimo.");
        String titulo = filtro.titulo() == null ? null : filtro.titulo().strip();
        if (titulo != null && titulo.isEmpty()) titulo = null;
        if (titulo != null && titulo.length() > 255)
            throw new ValidacionException("titulo", "El máximo es 255 caracteres.");
        return new FiltroLibroDTO(titulo, categorias, autores,
                editoriales, idiomas, filtro.precioMinimo(), filtro.precioMaximo(),
                filtro.soloActivos(), filtro.soloNuevos(), filtro.orden(), filtro.pagina(), filtro.tamanioPagina());
    }

    public static BigDecimal importe(BigDecimal importe, String campo, boolean permitirCero) throws ValidacionException {
        if (importe == null || (permitirCero ? importe.signum() < 0 : importe.signum() <= 0)
                || importe.compareTo(new BigDecimal("9999999999.99")) > 0 || importe.stripTrailingZeros().scale() > 2)
            throw new ValidacionException(campo, "Ingrese un importe valido con hasta dos decimales.");
        return importe.setScale(2);
    }
}
