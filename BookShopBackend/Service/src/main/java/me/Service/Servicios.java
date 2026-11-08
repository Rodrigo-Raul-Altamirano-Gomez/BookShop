package me.Service;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import me.Business.catalogo.AutorBO;
import me.Business.catalogo.AutorBOImpl;
import me.Business.catalogo.CategoriaBO;
import me.Business.catalogo.CategoriaBOImpl;
import me.Business.catalogo.EditorialBO;
import me.Business.catalogo.EditorialBOImpl;
import me.Business.catalogo.LibroBO;
import me.Business.catalogo.LibroBOImpl;
import me.Business.compra.CarritoBO;
import me.Business.compra.CarritoBOImpl;
import me.Business.compra.CuponBO;
import me.Business.compra.CuponBOImpl;
import me.Business.compra.PedidoBO;
import me.Business.compra.PedidoBOImpl;
import me.Business.opinion.ResenaBO;
import me.Business.opinion.ResenaBOImpl;
import me.Business.recomendacion.RecomendacionBO;
import me.Business.recomendacion.RecomendacionBOImpl;
import me.Business.usuario.DireccionEnvioBO;
import me.Business.usuario.DireccionEnvioBOImpl;
import me.Business.usuario.LibroGuardadoBO;
import me.Business.usuario.LibroGuardadoBOImpl;
import me.Business.usuario.MetodoPagoBO;
import me.Business.usuario.MetodoPagoBOImpl;
import me.Business.usuario.SeguimientoBO;
import me.Business.usuario.SeguimientoBOImpl;
import me.Business.usuario.UsuarioBO;
import me.Business.usuario.UsuarioBOImpl;
import me.DBManager.DBManager;

// Conserva los BO con estado durante la vida de la aplicación.
@ApplicationScoped
public class Servicios {
    private final UsuarioBO usuarioBO;
    private final AutorBO autorBO;
    private final CategoriaBO categoriaBO;
    private final EditorialBO editorialBO;
    private final LibroBO libroBO;
    private final DireccionEnvioBO direccionEnvioBO;
    private final MetodoPagoBO metodoPagoBO;
    private final LibroGuardadoBO libroGuardadoBO;
    private final SeguimientoBO seguimientoBO;
    private final CarritoBO carritoBO;
    private final CuponBO cuponBO;
    private final PedidoBO pedidoBO;
    private final ResenaBO resenaBO;
    private final RecomendacionBO recomendacionBO;

    public Servicios() {
        usuarioBO = new UsuarioBOImpl();
        autorBO = new AutorBOImpl();
        categoriaBO = new CategoriaBOImpl();
        editorialBO = new EditorialBOImpl();
        libroBO = new LibroBOImpl();
        direccionEnvioBO = new DireccionEnvioBOImpl();
        metodoPagoBO = new MetodoPagoBOImpl();
        libroGuardadoBO = new LibroGuardadoBOImpl();
        seguimientoBO = new SeguimientoBOImpl();
        carritoBO = new CarritoBOImpl();
        cuponBO = new CuponBOImpl();
        pedidoBO = new PedidoBOImpl();
        resenaBO = new ResenaBOImpl();
        recomendacionBO = new RecomendacionBOImpl();
    }

    public UsuarioBO getUsuarioBO() {
        return usuarioBO;
    }

    public AutorBO getAutorBO() {
        return autorBO;
    }

    public CategoriaBO getCategoriaBO() {
        return categoriaBO;
    }

    public EditorialBO getEditorialBO() {
        return editorialBO;
    }

    public LibroBO getLibroBO() {
        return libroBO;
    }

    public DireccionEnvioBO getDireccionEnvioBO() {
        return direccionEnvioBO;
    }

    public MetodoPagoBO getMetodoPagoBO() {
        return metodoPagoBO;
    }

    public LibroGuardadoBO getLibroGuardadoBO() {
        return libroGuardadoBO;
    }

    public SeguimientoBO getSeguimientoBO() {
        return seguimientoBO;
    }

    public CarritoBO getCarritoBO() {
        return carritoBO;
    }

    public CuponBO getCuponBO() {
        return cuponBO;
    }

    public PedidoBO getPedidoBO() {
        return pedidoBO;
    }

    public ResenaBO getResenaBO() {
        return resenaBO;
    }

    public RecomendacionBO getRecomendacionBO() {
        return recomendacionBO;
    }

    @PreDestroy
    public void cerrar() {
        DBManager.close();
    }
}
