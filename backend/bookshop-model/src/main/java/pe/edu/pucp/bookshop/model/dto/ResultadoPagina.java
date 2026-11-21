package pe.edu.pucp.bookshop.model.dto;

import java.util.List;

// Una página de un listado y el total de registros que cumplen el filtro
public record ResultadoPagina<T>(int total, List<T> elementos) {
}
