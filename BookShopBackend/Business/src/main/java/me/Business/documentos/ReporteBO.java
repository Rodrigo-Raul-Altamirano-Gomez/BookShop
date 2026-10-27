package me.Business.documentos;

import java.time.YearMonth;
import me.Business.exceptions.NegocioException;
import me.Model.dto.catalogo.ConsultaReporteLibrosDTO;
import me.Model.dto.catalogo.ReporteVentasLibrosDTO;
import me.Model.dto.usuario.ReporteUsuariosDTO;

// Prepara datos; la generación con Jaspersoft Studio se incorpora en otra fase.
public interface ReporteBO {
    ReporteVentasLibrosDTO prepararLibros(int idActor, ConsultaReporteLibrosDTO consulta) throws NegocioException;
    ReporteUsuariosDTO prepararUsuarios(int idActor, YearMonth mes, Integer cantidadMaxima) throws NegocioException;
}
