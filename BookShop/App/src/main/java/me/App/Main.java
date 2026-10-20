package me.App;

import me.Business.ConfiguracionBusiness;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import me.Business.excepcion.BusinessLogicException;
import me.Business.catalogo.CategoriaBO;
import me.Business.catalogo.LibroBO;
import me.Business.catalogo.ResenaBO;
import me.Business.usuario.DireccionEnvioBO;
import me.Business.usuario.MetodoPagoBO;
import me.Business.usuario.UsuarioBO;
import me.Model.entities.catalogo.Categoria;
import me.Model.entities.catalogo.Libro;
import me.Model.entities.catalogo.Resena;
import me.Model.entities.usuario.DireccionEnvio;
import me.Model.entities.usuario.MetodoPago;
import me.Model.entities.usuario.Rol;
import me.Model.entities.usuario.TarjetaBancaria;
import me.Model.entities.usuario.Usuario;
import me.Model.entities.venta.TipoTarjeta;

public class Main {
    public static void main(String[] args) {
        int idAdministrador = 5;
        String identificadorEjecucion = String.valueOf(System.currentTimeMillis());
        String nombre = "Rodrigo";
        String apellidos = "Raul";
        String contrasenia = "RodrigoBookShop2026!";
        String email = "rodrigo.raul." + identificadorEjecucion + "@bookshop.test";
        ConfiguracionBusiness business = new ConfiguracionBusiness();
        UsuarioBO usuarioBO = business.getUsuarioBO();
        CategoriaBO categoriaBO = business.getCategoriaBO();
        LibroBO libroBO = business.getLibroBO();
        DireccionEnvioBO direccionBO = business.getDireccionEnvioBO();
        MetodoPagoBO metodoPagoBO = business.getMetodoPagoBO();
        ResenaBO resenaBO = business.getResenaBO();
        try {
            System.out.println("=== CRUD DE 6 ENTIDADES ===");
            Usuario usuario = new Usuario(0, email, contrasenia, nombre, apellidos,
                    LocalDateTime.of(2000, 1, 1, 0, 0), Rol.CLIENTE);
            usuarioBO.registrar(usuario);
            verificar(usuario.getId() > 0, "No se generó el ID del usuario.");
            Usuario usuarioGuardado = usuarioBO.leer(usuario.getId());
            verificar(usuarioGuardado != null && email.equals(usuarioGuardado.getEmail())
                    && nombre.equals(usuarioGuardado.getNombre())
                    && apellidos.equals(usuarioGuardado.getApellidos()), "El usuario no fue almacenado correctamente.");
            System.out.println("USUARIO CREATE: verificado, id=" + usuario.getId());
            System.out.println("USUARIO READ: verificado, nombre=" + usuarioGuardado.getNombre());
            usuarioGuardado.setNombre("Rodrigo Actualizado");
            usuarioBO.modificarPerfil(usuario.getId(), usuarioGuardado);
            Usuario usuarioActualizado = usuarioBO.leer(usuario.getId());
            verificar(usuarioActualizado != null && "Rodrigo Actualizado".equals(usuarioActualizado.getNombre()),
                    "El nombre actualizado del usuario no fue almacenado.");
            System.out.println("USUARIO UPDATE: verificado, nombre=" + usuarioActualizado.getNombre());
            Usuario administrador = usuarioBO.leer(idAdministrador);
            verificar(administrador != null && administrador.isActivo() && administrador.getRol() == Rol.ADMIN,
                    "El usuario administrador no existe, está inactivo o no tiene rol ADMIN.");
            List<Usuario> usuarios = usuarioBO.listarTodos(idAdministrador, 10, 1);
            verificar(!usuarios.isEmpty(), "El listado de usuarios está vacío.");
            System.out.println("USUARIO LIST TODOS (página 1):");
            for (Usuario actual : usuarios) {
                System.out.println("  id=" + actual.getId() + ", nombre=" + actual.getNombre()
                        + ", email=" + actual.getEmail() + ", activo=" + actual.isActivo());
            }
            String nombreCategoria = "Ciencia Ficción " + identificadorEjecucion;
            Categoria categoria = new Categoria(0, nombreCategoria,
                    "Categoría de ciencia ficción para el laboratorio");
            categoriaBO.insertar(idAdministrador, categoria);
            verificar(categoria.getId() > 0, "No se generó el ID de la categoría.");
            Categoria categoriaGuardada = categoriaBO.leer(categoria.getId());
            verificar(categoriaGuardada != null && nombreCategoria.equals(categoriaGuardada.getNombre()),
                    "La categoría no fue almacenada correctamente.");
            System.out.println("CATEGORÍA CREATE: verificado, id=" + categoria.getId());
            System.out.println("CATEGORÍA READ: verificado, nombre=" + categoriaGuardada.getNombre());
            categoriaGuardada.setDescripcion("Categoría actualizada para el laboratorio");
            categoriaBO.modificar(idAdministrador, categoriaGuardada);
            Categoria categoriaActualizada = categoriaBO.leer(categoria.getId());
            verificar(categoriaActualizada != null
                    && "Categoría actualizada para el laboratorio".equals(categoriaActualizada.getDescripcion()),
                    "La descripción actualizada de la categoría no fue almacenada.");
            System.out.println("CATEGORÍA UPDATE: verificado, descripción=" + categoriaActualizada.getDescripcion());
            List<Categoria> categorias = categoriaBO.listarTodos(idAdministrador, 10, 1);
            verificar(!categorias.isEmpty(), "El listado de categorías está vacío.");
            System.out.println("CATEGORÍA LIST TODOS (página 1):");
            for (Categoria actual : categorias) {
                System.out.println("  id=" + actual.getId() + ", nombre=" + actual.getNombre()
                        + ", activo=" + actual.isActivo());
            }
            String tituloLibro = "Dune " + identificadorEjecucion;
            String isbn = "LAB06" + identificadorEjecucion;
            Libro libro = new Libro(0, tituloLibro, "Frank Herbert", new BigDecimal("49.90"), 6, categoriaActualizada,
                    "Una historia de ciencia ficción en el planeta Arrakis", "Editorial Laboratorio",
                    "Proveedor Laboratorio", isbn, LocalDate.now().getYear(), 240,
                    0.350, 21.0, 14.0, "Dune Chronicles", 1);
            libroBO.insertar(idAdministrador, libro);
            verificar(libro.getId() > 0, "No se generó el ID del libro.");
            Libro libroGuardado = libroBO.leer(libro.getId());
            verificar(libroGuardado != null && tituloLibro.equals(libroGuardado.getTitulo())
                    && isbn.equals(libroGuardado.getIsbn()) && libroGuardado.getStockFisico() == 6
                    && libroGuardado.getPrecioBase().compareTo(new BigDecimal("49.90")) == 0,
                    "El libro no fue almacenado correctamente.");
            System.out.println("LIBRO CREATE: verificado, id=" + libro.getId());
            System.out.println("LIBRO READ: verificado, título=" + libroGuardado.getTitulo());
            String tituloActualizado = "God Emperor of Dune " + identificadorEjecucion;
            libroGuardado.setTitulo(tituloActualizado);
            libroBO.modificar(idAdministrador, libroGuardado);
            Libro libroActualizado = libroBO.leer(libro.getId());
            verificar(libroActualizado != null && tituloActualizado.equals(libroActualizado.getTitulo())
                    && libroActualizado.getPrecioBase().compareTo(new BigDecimal("49.90")) == 0
                    && libroActualizado.getStockFisico() == 6,
                    "Los cambios del libro no fueron almacenados correctamente.");
            System.out.println("LIBRO UPDATE: verificado, título=" + libroActualizado.getTitulo()
                    + ", precio=S/ " + libroActualizado.getPrecioBase()
                    + ", stock=" + libroActualizado.getStockFisico());
            List<Libro> libros = libroBO.listarTodos(idAdministrador, 10, 1);
            verificar(!libros.isEmpty(), "El listado de libros está vacío.");
            System.out.println("LIBRO LIST TODOS (página 1):");
            for (Libro actual : libros) {
                System.out.println("  id=" + actual.getId() + ", título=" + actual.getTitulo()
                        + ", activo=" + actual.isActivo());
            }
            DireccionEnvio direccion = new DireccionEnvio(0, usuario, "Av. Principal 123",
                    "Miraflores", "Lima", null);
            direccionBO.insertar(usuario.getId(), direccion);
            verificar(direccion.getId() > 0, "No se generó el ID de la dirección.");
            DireccionEnvio direccionGuardada = direccionBO.leer(usuario.getId(), direccion.getId());
            verificar(direccionGuardada != null && "Av. Principal 123".equals(direccionGuardada.getCalle())
                    && "Miraflores".equals(direccionGuardada.getDistrito()),
                    "La dirección no fue almacenada correctamente.");
            System.out.println("DIRECCIÓN CREATE: verificado, id=" + direccion.getId());
            System.out.println("DIRECCIÓN READ: verificado, calle=" + direccionGuardada.getCalle());
            direccionGuardada.setCalle("Av. Principal 456");
            direccionBO.modificar(usuario.getId(), direccion.getId(), direccionGuardada);
            DireccionEnvio direccionActualizada = direccionBO.leer(usuario.getId(), direccion.getId());
            verificar(direccionActualizada != null && "Av. Principal 456".equals(direccionActualizada.getCalle()),
                    "La calle actualizada no fue almacenada.");
            System.out.println("DIRECCIÓN UPDATE: verificado, calle=" + direccionActualizada.getCalle());
            List<DireccionEnvio> direcciones = direccionBO.listarTodos(idAdministrador, 10, 1);
            verificar(!direcciones.isEmpty(), "El listado de direcciones está vacío.");
            System.out.println("DIRECCIÓN LIST TODOS (página 1):");
            for (DireccionEnvio actual : direcciones) {
                System.out.println("  id=" + actual.getId() + ", calle=" + actual.getCalle()
                        + ", ciudad=" + actual.getCiudad() + ", activo=" + actual.isActivo());
            }
            MetodoPago metodoPago = new TarjetaBancaria(0, "Rodrigo Raúl", "Visa", usuario,
                    "Tarjeta Rodrigo", "**** **** **** 4581", LocalDate.now().plusYears(3), TipoTarjeta.DEBITO);
            metodoPagoBO.insertar(usuario.getId(), metodoPago);
            verificar(metodoPago.getId() > 0, "No se generó el ID del método de pago.");
            MetodoPago metodoPagoGuardado = metodoPagoBO.leer(usuario.getId(), metodoPago.getId());
            verificar(metodoPagoGuardado != null && "Rodrigo Raúl".equals(metodoPagoGuardado.getTitular())
                    && "Tarjeta Rodrigo".equals(metodoPagoGuardado.getAlias()),
                    "El método de pago no fue almacenado correctamente.");
            System.out.println("MÉTODO DE PAGO CREATE: verificado, id=" + metodoPago.getId());
            System.out.println("MÉTODO DE PAGO READ: verificado, alias=" + metodoPagoGuardado.getAlias());
            metodoPagoGuardado.setAlias("Tarjeta Rodrigo Actualizada");
            metodoPagoBO.modificar(usuario.getId(), metodoPago.getId(), metodoPagoGuardado);
            MetodoPago metodoPagoActualizado = metodoPagoBO.leer(usuario.getId(), metodoPago.getId());
            verificar(metodoPagoActualizado != null
                    && "Tarjeta Rodrigo Actualizada".equals(metodoPagoActualizado.getAlias()),
                    "El alias actualizado no fue almacenado.");
            System.out.println("MÉTODO DE PAGO UPDATE: verificado, alias=" + metodoPagoActualizado.getAlias());
            List<MetodoPago> metodos = metodoPagoBO.listarTodos(idAdministrador, 10, 1);
            verificar(!metodos.isEmpty(), "El listado de métodos de pago está vacío.");
            System.out.println("MÉTODO DE PAGO LIST TODOS (página 1):");
            for (MetodoPago actual : metodos) {
                System.out.println("  id=" + actual.getId() + ", alias=" + actual.getAlias()
                        + ", activo=" + actual.isActivo());
            }
            Resena resena = new Resena(0, 5, "¡Una historia increíble!", libro, usuario);
            resenaBO.insertar(usuario.getId(), resena);
            verificar(resena.getId() > 0, "No se generó el ID de la reseña.");
            Resena resenaGuardada = resenaBO.leer(resena.getId());
            verificar(resenaGuardada != null && resenaGuardada.getPuntuacion() == 5
                    && "¡Una historia increíble!".equals(resenaGuardada.getComentario()),
                    "La reseña no fue almacenada correctamente.");
            System.out.println("RESEÑA CREATE: verificado, id=" + resena.getId());
            System.out.println("RESEÑA READ: verificado, comentario=" + resenaGuardada.getComentario());
            resenaGuardada.setPuntuacion(4);
            resenaGuardada.setComentario("Leí el libro de nuevo y me gustó aún más.");
            resenaBO.modificar(usuario.getId(), resenaGuardada);
            Resena resenaActualizada = resenaBO.leer(resena.getId());
            verificar(resenaActualizada != null && resenaActualizada.getPuntuacion() == 4
                    && "Leí el libro de nuevo y me gustó aún más.".equals(resenaActualizada.getComentario()),
                    "Los cambios de la reseña no fueron almacenados correctamente.");
            System.out.println("RESEÑA UPDATE: verificado, comentario=" + resenaActualizada.getComentario());
            List<Resena> resenas = resenaBO.listarTodos(idAdministrador, 10, 1);
            verificar(!resenas.isEmpty(), "El listado de reseñas está vacío.");
            System.out.println("RESEÑA LIST TODOS (página 1):");
            for (Resena actual : resenas) {
                System.out.println("  id=" + actual.getId() + ", puntuación=" + actual.getPuntuacion()
                        + ", comentario=" + actual.getComentario());
            }
            resenaBO.eliminar(usuario.getId(), resena.getId());
            verificar(resenaBO.leer(resena.getId()) == null,
                    "La reseña aún existe después de eliminarla.");
            System.out.println("RESEÑA DELETE: ausente por ID, id=" + resena.getId());
            metodoPagoBO.eliminar(usuario.getId(), metodoPago.getId());
            MetodoPago metodoEliminado = metodoPagoBO.leer(usuario.getId(), metodoPago.getId());
            verificar(metodoEliminado != null && !metodoEliminado.isActivo(),
                    "El método de pago no quedó inactivo después de eliminarlo.");
            System.out.println("MÉTODO DE PAGO DELETE: inactivo por ID, id=" + metodoPago.getId());
            direccionBO.eliminar(usuario.getId(), direccion.getId());
            DireccionEnvio direccionEliminada = direccionBO.leer(usuario.getId(), direccion.getId());
            verificar(direccionEliminada != null && !direccionEliminada.isActivo(),
                    "La dirección no quedó inactiva después de eliminarla.");
            System.out.println("DIRECCIÓN DELETE: inactiva por ID, id=" + direccion.getId());
            libroBO.eliminar(idAdministrador, libro.getId());
            Libro libroEliminado = libroBO.leer(libro.getId());
            verificar(libroEliminado != null && !libroEliminado.isActivo(),
                    "El libro no quedó inactivo después de eliminarlo.");
            System.out.println("LIBRO DELETE: inactivo por ID, id=" + libro.getId());
            categoriaBO.eliminar(idAdministrador, categoria.getId());
            Categoria categoriaEliminada = categoriaBO.leer(categoria.getId());
            verificar(categoriaEliminada != null && !categoriaEliminada.isActivo(),
                    "La categoría no quedó inactiva después de eliminarla.");
            System.out.println("CATEGORÍA DELETE: inactiva por ID, id=" + categoria.getId());
            usuarioBO.eliminar(usuario.getId(), contrasenia);
            Usuario usuarioEliminado = usuarioBO.leer(usuario.getId());
            verificar(usuarioEliminado != null && !usuarioEliminado.isActivo(),
                    "El usuario no quedó inactivo después de eliminarlo.");
            System.out.println("USUARIO DELETE: inactivo por ID, id=" + usuario.getId());
            System.out.println("=== DEMOSTRACIÓN COMPLETADA ===");
        } catch (BusinessLogicException e) {
            System.err.println("ERROR DE NEGOCIO: " + e.getMessage());
            throw e;
        } catch (RuntimeException e) {
            System.err.println("ERROR INESPERADO: " + e.getMessage());
            throw e;
        }
    }

    private static void verificar(boolean condicion, String mensaje) {
        if (!condicion) throw new IllegalStateException(mensaje);
    }
}
