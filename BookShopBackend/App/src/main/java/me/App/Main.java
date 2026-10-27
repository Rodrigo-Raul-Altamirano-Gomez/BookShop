package me.App;

import java.math.BigDecimal;
import java.util.List;
import me.Business.catalogo.FiltrosCatalogo;
import me.Business.exceptions.ValidacionException;
import me.Model.dto.catalogo.FiltroLibroDTO;
import me.Model.dto.catalogo.OrdenLibro;

public class Main {
    public static void main(String[] args) throws ValidacionException {
        System.out.println("=== BookShopBackend: demostración de filtros ===");

        // Utiliza los contratos y las validaciones reales de Business, sin consultar la base.
        var entrada = new FiltroLibroDTO(
                "  Dune  ", List.of(2, 2), List.of(), List.of(), List.of("Español"),
                new BigDecimal("20.00"), new BigDecimal("80.00"),
                true, false, OrdenLibro.PRECIO_ASCENDENTE, 1, 5);
        var filtro = FiltrosCatalogo.validar(entrada);

        System.out.println("Título normalizado: " + filtro.titulo());
        System.out.println("Categorías sin duplicados: " + filtro.idsCategorias());
        System.out.println("Precio: S/ " + filtro.precioMinimo() + " a S/ " + filtro.precioMaximo());
        System.out.println("Orden: " + filtro.orden());
        System.out.println("Página: " + filtro.pagina() + ", resultados por página: " + filtro.tamanioPagina());

        // Un rango invertido muestra cómo se informa un error al consumidor de Business.
        var invalido = new FiltroLibroDTO(
                filtro.titulo(), filtro.idsCategorias(), filtro.idsAutores(), filtro.idsEditoriales(),
                filtro.idiomas(), new BigDecimal("80.00"), new BigDecimal("20.00"),
                true, false, filtro.orden(), 1, 5);
        try {
            FiltrosCatalogo.validar(invalido);
        } catch (ValidacionException e) {
            System.out.println("Validación de " + e.getCampo() + ": " + e.getMessage());
        }
    }
}
