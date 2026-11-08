package me.Service;

import java.util.Set;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import me.Service.catalogo.AutorRS;
import me.Service.catalogo.CategoriaRS;
import me.Service.catalogo.EditorialRS;
import me.Service.catalogo.LibroRS;
import me.Service.compra.CarritoRS;
import me.Service.compra.CuponRS;
import me.Service.compra.PedidoRS;
import me.Service.opinion.ResenaRS;
import me.Service.recomendacion.RecomendacionRS;
import me.Service.usuario.DireccionEnvioRS;
import me.Service.usuario.LibroGuardadoRS;
import me.Service.usuario.MetodoPagoRS;
import me.Service.usuario.SeguimientoRS;
import me.Service.usuario.UsuarioRS;

@ApplicationPath("/api")
public class AplicacionRest extends Application {
    @Override
    public Set<Class<?>> getClasses() {
        return Set.of(
                AutorRS.class,
                CategoriaRS.class,
                EditorialRS.class,
                LibroRS.class,
                DireccionEnvioRS.class,
                MetodoPagoRS.class,
                LibroGuardadoRS.class,
                SeguimientoRS.class,
                CarritoRS.class,
                CuponRS.class,
                PedidoRS.class,
                ResenaRS.class,
                RecomendacionRS.class,
                UsuarioRS.class,
                Errores.class);
    }
}
