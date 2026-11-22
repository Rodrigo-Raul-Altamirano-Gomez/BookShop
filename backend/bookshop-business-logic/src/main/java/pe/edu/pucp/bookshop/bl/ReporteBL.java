package pe.edu.pucp.bookshop.bl;

import pe.edu.pucp.bookshop.bl.exception.BusinessLogicException;

import java.time.LocalDate;

// Reportes PDF del administrador. limite: cantidad máxima de filas (null: todas).
public interface ReporteBL {
    byte[] generarReporteLibros(LocalDate desde, LocalDate hasta, Integer limite) throws BusinessLogicException;
    byte[] generarReporteClientes(LocalDate desde, LocalDate hasta, Integer limite) throws BusinessLogicException;
}
