package me.Service.compra;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import me.Business.exceptions.NegocioException;
import me.Model.dto.ResultadoPaginadoDTO;
import me.Model.dto.compra.CuponDTO;
import me.Service.Servicios;
import me.Service.Sesion;

@Path("cuenta/cupones")
@Produces(MediaType.APPLICATION_JSON)
@RequestScoped
public class CuponRS {
    @Inject
    private Servicios servicios;
    @Context
    private HttpServletRequest peticion;

    @GET
    public ResultadoPaginadoDTO<CuponDTO> listar(
            @DefaultValue("1") @QueryParam("pagina") int pagina,
            @DefaultValue("10") @QueryParam("tamanioPagina") int tamanioPagina) throws NegocioException {
        return servicios.getCuponBO().listar(Sesion.exigirIdUsuario(peticion), pagina, tamanioPagina);
    }
}
