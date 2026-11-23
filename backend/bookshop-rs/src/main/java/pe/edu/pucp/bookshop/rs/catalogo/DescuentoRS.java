package pe.edu.pucp.bookshop.rs.catalogo;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import pe.edu.pucp.bookshop.bl.catalogo.DescuentoBL;
import pe.edu.pucp.bookshop.bl.catalogo.impl.DescuentoBLImpl;
import pe.edu.pucp.bookshop.bl.exception.BusinessLogicException;
import pe.edu.pucp.bookshop.model.catalogo.Descuento;

import java.util.List;

@Path("descuentos")
@Produces("application/json")
@Consumes("application/json")
public class DescuentoRS {

    private DescuentoBL descuentoBL = new DescuentoBLImpl();

    @GET
    @Path("libro/{idLibro}")
    public List<Descuento> listarPorLibro(@PathParam("idLibro") int idLibro) throws BusinessLogicException {
        return descuentoBL.listarPorLibro(idLibro);
    }

    @POST
    public Descuento insertar(Descuento descuento) throws BusinessLogicException {
        return descuentoBL.insertar(descuento);
    }

    @DELETE
    @Path("{id}")
    public void eliminar(@PathParam("id") int id) throws BusinessLogicException {
        descuentoBL.eliminar(id);
    }
}
