package me.Model.dto.catalogo;

// El algoritmo determina el orden; esta consulta solo indica la página.
public record ConsultaRecomendacionesDTO(int pagina, int tamanioPagina) {

    public static ConsultaRecomendacionesDTO porDefecto() {
        return new ConsultaRecomendacionesDTO(1, 12);
    }
}
