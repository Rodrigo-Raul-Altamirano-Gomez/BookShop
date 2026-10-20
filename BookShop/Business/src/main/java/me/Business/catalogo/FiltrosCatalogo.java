package me.Business.catalogo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import me.Business.exceptions.ValidacionException;
import me.Business.Validaciones;
import me.Model.dto.catalogo.FiltroLibroDTO;
import me.Model.dto.catalogo.OrdenLibro;

public final class FiltrosCatalogo {
    private FiltrosCatalogo() { }

    public static FiltroLibroDTO validar(FiltroLibroDTO filtro) throws ValidacionException {
        if (filtro == null) filtro = new FiltroLibroDTO();
        Validaciones.pagina(filtro.pagina(), filtro.tamanioPagina());
        if (filtro.orden() == null || filtro.orden() == OrdenLibro.RECOMENDADOS_PRIMERO)
            throw new ValidacionException("orden", "Seleccione un orden de catalogo.");
        List<Integer> categorias = ids(filtro.idsCategorias(), "categorias");
        List<Integer> autores = ids(filtro.idsAutores(), "autores");
        List<Integer> editoriales = ids(filtro.idsEditoriales(), "editoriales");
        List<String> idiomas = new ArrayList<>();
        for (String idioma : filtro.idiomas()) idiomas.add(Validaciones.texto(idioma, "idiomas", 100));
        if (filtro.precioMinimo() != null) importe(filtro.precioMinimo(), "precioMinimo", true);
        if (filtro.precioMaximo() != null) importe(filtro.precioMaximo(), "precioMaximo", true);
        if (filtro.precioMinimo() != null && filtro.precioMaximo() != null
                && filtro.precioMinimo().compareTo(filtro.precioMaximo()) > 0)
            throw new ValidacionException("precioMaximo", "El precio maximo no puede ser menor que el minimo.");
        return new FiltroLibroDTO(Validaciones.opcional(filtro.titulo(), "titulo", 255), categorias, autores,
                editoriales, new ArrayList<>(new LinkedHashSet<>(idiomas)), filtro.precioMinimo(), filtro.precioMaximo(),
                filtro.soloActivos(), filtro.soloNuevos(), filtro.orden(), filtro.pagina(), filtro.tamanioPagina());
    }

    private static List<Integer> ids(List<Integer> ids, String campo) throws ValidacionException {
        for (Integer id : ids) if (id == null || id < 1)
            throw new ValidacionException(campo, "Seleccione referencias validas.");
        return new ArrayList<>(new LinkedHashSet<>(ids));
    }

    public static BigDecimal importe(BigDecimal importe, String campo, boolean permitirCero) throws ValidacionException {
        if (importe == null || (permitirCero ? importe.signum() < 0 : importe.signum() <= 0)
                || importe.compareTo(new BigDecimal("9999999999.99")) > 0 || importe.stripTrailingZeros().scale() > 2)
            throw new ValidacionException(campo, "Ingrese un importe valido con hasta dos decimales.");
        return importe.setScale(2);
    }
}

