package me.Business.configuracion;

import me.Business.exceptions.NegocioException;
import me.Model.dto.ConfiguracionDTO;

public interface ConfiguracionBO {

    ConfiguracionDTO obtener() throws NegocioException;

    void guardar(ConfiguracionDTO datos) throws NegocioException;
}
