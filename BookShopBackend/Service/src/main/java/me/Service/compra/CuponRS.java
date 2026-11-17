package me.Service.compra;

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
import me.Business.compra.CuponBO;
import me.Business.compra.CuponBOImpl;
import me.Service.Sesion;

@Path("cuenta/cupones")
@Produces(MediaType.APPLICATION_JSON)
public class CuponRS {

    private final CuponBO cuponBO = new CuponBOImpl();

    @Context
    private HttpServletRequest peticion;

    @GET
    public ResultadoPaginadoDTO<CuponDTO> listar(@DefaultValue("1") @QueryParam("pagina") int pagina,
        @DefaultValue("10") @QueryParam("tamanioPagina") int tamanioPagina) throws NegocioException {
        return cuponBO.listar(Sesion.exigirIdUsuario(peticion), pagina, tamanioPagina);
    }
}
