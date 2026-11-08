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
    private final UsuarioBO usuario;
    private final AutorBO autor;
    private final CategoriaBO categoria;
    private final EditorialBO editorial;
    private final LibroBO libro;
    private final DireccionEnvioBO direccionEnvio;
    private final MetodoPagoBO metodoPago;
    private final LibroGuardadoBO libroGuardado;
    private final SeguimientoBO seguimiento;
    private final CarritoBO carrito;
    private final CuponBO cupon;
    private final PedidoBO pedido;
    private final ResenaBO resena;
    private final RecomendacionBO recomendacion;

    public Servicios() {
        usuario = new UsuarioBOImpl();
        autor = new AutorBOImpl();
        categoria = new CategoriaBOImpl();
        editorial = new EditorialBOImpl();
        libro = new LibroBOImpl();
        direccionEnvio = new DireccionEnvioBOImpl();
        metodoPago = new MetodoPagoBOImpl();
        libroGuardado = new LibroGuardadoBOImpl();
        seguimiento = new SeguimientoBOImpl();
        carrito = new CarritoBOImpl();
        cupon = new CuponBOImpl();
        pedido = new PedidoBOImpl();
        resena = new ResenaBOImpl();
        recomendacion = new RecomendacionBOImpl();
    }

    public UsuarioBO usuario() {
        return usuario;
    }

    public AutorBO autor() {
        return autor;
    }

    public CategoriaBO categoria() {
        return categoria;
    }

    public EditorialBO editorial() {
        return editorial;
    }

    public LibroBO libro() {
        return libro;
    }

    public DireccionEnvioBO direccionEnvio() {
        return direccionEnvio;
    }

    public MetodoPagoBO metodoPago() {
        return metodoPago;
    }

    public LibroGuardadoBO libroGuardado() {
        return libroGuardado;
    }

    public SeguimientoBO seguimiento() {
        return seguimiento;
    }

    public CarritoBO carrito() {
        return carrito;
    }

    public CuponBO cupon() {
        return cupon;
    }

    public PedidoBO pedido() {
        return pedido;
    }

    public ResenaBO resena() {
        return resena;
    }

    public RecomendacionBO recomendacion() {
        return recomendacion;
    }

    @PreDestroy
    public void cerrar() {
        DBManager.close();
    }
}
