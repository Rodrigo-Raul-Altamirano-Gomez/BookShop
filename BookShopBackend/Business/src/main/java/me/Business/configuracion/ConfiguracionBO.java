package me.Business.configuracion;

import me.Business.exceptions.NegocioException;
import me.Model.dto.ConfiguracionDTO;

public interface ConfiguracionBO {
    ConfiguracionDTO obtener(int idActor) throws NegocioException;
    void guardar(int idActor, ConfiguracionDTO datos) throws NegocioException;
}
