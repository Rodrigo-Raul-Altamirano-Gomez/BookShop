package me.Business.compra;

import me.Business.exceptions.NegocioException;
import me.Model.dto.ConfiguracionDTO;

@FunctionalInterface
public interface ProveedorConfiguracion {
    ConfiguracionDTO obtener() throws NegocioException;
}

