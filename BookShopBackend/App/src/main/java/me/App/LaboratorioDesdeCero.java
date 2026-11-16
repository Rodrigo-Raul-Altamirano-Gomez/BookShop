package me.App;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import me.Business.catalogo.AutorBOImpl;
import me.Business.catalogo.CategoriaBOImpl;
import me.Business.catalogo.EditorialBOImpl;
import me.Business.catalogo.LibroBOImpl;
import me.Business.compra.CarritoBOImpl;
import me.Business.compra.PedidoBOImpl;
import me.Business.documentos.ComprobanteBOImpl;
import me.Business.documentos.ReporteBOImpl;
import me.Business.exceptions.NegocioException;
import me.Business.usuario.DireccionEnvioBOImpl;
import me.Business.usuario.MetodoPagoBOImpl;
import me.Business.usuario.UsuarioBOImpl;
import me.DAO.usuario.MetodoPagoDAOImpl;
import me.DAO.usuario.UsuarioDAOImpl;
import me.DBManager.DBManager;
import me.Model.dto.catalogo.ConsultaReporteLibrosDTO;
import me.Model.dto.catalogo.DatosLibroDTO;
import me.Model.dto.catalogo.DatosReferenciaDTO;
import me.Model.dto.catalogo.RegistroLibroDTO;
import me.Model.dto.catalogo.RegistroReferenciaDTO;
import me.Model.dto.compra.DatosDireccionEnvioDTO;
import me.Model.dto.compra.DatosMetodoPagoDTO;
import me.Model.dto.compra.DetallePedidoDTO;
import me.Model.dto.compra.DireccionEnvioDTO;
import me.Model.dto.compra.MetodoPagoDTO;
import me.Model.dto.compra.SeleccionCompraDTO;
import me.Model.dto.usuario.AccesoUsuarioDTO;
import me.Model.dto.usuario.CredencialesDTO;
import me.Model.dto.usuario.RegistroUsuarioDTO;
import me.Model.entities.compra.TarjetaBancaria;
import me.Model.entities.compra.TipoTarjeta;

/*
 * Laboratorio: construir la tienda desde cero con las mismas reglas de negocio que usa la web.
 * Reemplaza a compra-demo.sql, cuyos marcadores ${HASH} y ${CIFRADO} no se pueden escribir a mano.
 *
 * Requisitos: una base creada con DDL.sql (puede estar vacía) y db.properties configurado.
 * Usa la misma clave de cifrado que GlassFish: BookShopBackend/config/clave-datos.key.
 * Cada ejecución usa una "tanda" distinta (correos, nombres e ISBN), así que puede repetirse.
 */
public class LaboratorioDesdeCero {
    private static final String CLAVE_ADMIN = "Admin12345";
    private static final String CLAVE_CLIENTE = "Cliente123";

    public static void main(String[] args) throws Exception {
        configurarClave();
        String tanda = args.length > 0 ? args[0] : LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmss"));
        Path carpetaPdf = Path.of("laboratorio-pdf").toAbsolutePath();
        try {
            ejecutar(tanda, carpetaPdf);
        } catch (NegocioException e) {
            System.out.println("\nERROR de negocio: " + e.getMessage());
            if (e.getCause() != null) System.out.println("Causa: " + e.getCause());
        } finally {
            DBManager.close();
        }
    }

    private static void ejecutar(String tanda, Path carpetaPdf) throws Exception {
        var usuarios = new UsuarioBOImpl();

        paso(1, "Registrar un ADMINISTRADOR (el rol lo decide el dominio @admin.bookshop)");
        String correoAdmin = "admin" + tanda + "@admin.bookshop";
        AccesoUsuarioDTO admin = usuarios.registrar(new RegistroUsuarioDTO("Administrador Laboratorio",
                correoAdmin, LocalDate.of(1990, 1, 15), CLAVE_ADMIN, CLAVE_ADMIN, null));
        System.out.println("Cuenta creada: id=" + admin.id() + ", rol=" + admin.rol() + ", correo=" + correoAdmin);
        mostrarContraseniaGuardada(admin.id(), CLAVE_ADMIN);

        paso(2, "El administrador arma el catálogo: categoría, autor, editorial y libros");
        int categoria = new CategoriaBOImpl().registrar(admin.id(), referencia("Novela " + tanda,
                "Narrativa de ficción."));
        int autor = new AutorBOImpl().registrar(admin.id(), referencia("Autora de Laboratorio " + tanda,
                "Autora creada para el laboratorio."));
        int editorial = new EditorialBOImpl().registrar(admin.id(), referencia("Editorial PUCP " + tanda,
                "Editorial del laboratorio."));
        var libros = new LibroBOImpl();
        int libroA = libros.registrar(admin.id(), libro("El jardín de los datos", "978" + tanda + "001",
                autor, categoria, editorial, "45.00", 30));
        int libroB = libros.registrar(admin.id(), libro("Cifrar para proteger", "978" + tanda + "002",
                autor, categoria, editorial, "60.00", 15));
        System.out.println("Categoría=" + categoria + ", autor=" + autor + ", editorial=" + editorial
                + ", libros=" + libroA + " y " + libroB);

        paso(3, "Registrar un CLIENTE (la contraseña se guarda como hash PBKDF2)");
        String correoCliente = "cliente" + tanda + "@correo.pe";
        AccesoUsuarioDTO cliente = usuarios.registrar(new RegistroUsuarioDTO("Lucía Torres",
                correoCliente, LocalDate.of(2002, 6, 20), CLAVE_CLIENTE, CLAVE_CLIENTE, null));
        System.out.println("Cuenta creada: id=" + cliente.id() + ", rol=" + cliente.rol() + ", correo=" + correoCliente);
        mostrarContraseniaGuardada(cliente.id(), CLAVE_CLIENTE);

        paso(4, "Iniciar sesión: el sistema compara hashes, nunca la contraseña guardada");
        System.out.println("Con la contraseña correcta  -> id " + usuarios.iniciarSesion(
                new CredencialesDTO(correoCliente, CLAVE_CLIENTE)).id());
        try {
            usuarios.iniciarSesion(new CredencialesDTO(correoCliente, "cliente123"));
            System.out.println("Con una contraseña incorrecta -> entró (no debería pasar)");
        } catch (NegocioException e) {
            System.out.println("Con una contraseña incorrecta -> " + e.getMessage());
        }

        paso(5, "El cliente registra dirección y tarjeta (la tarjeta se guarda cifrada con AES-GCM)");
        DireccionEnvioDTO direccion = new DireccionEnvioBOImpl().registrar(cliente.id(), new DatosDireccionEnvioDTO(
                "Casa", "Av. Universitaria 1801", "San Miguel", "Lima", "Frente a la PUCP", true));
        MetodoPagoDTO tarjeta = new MetodoPagoBOImpl().registrar(cliente.id(), new DatosMetodoPagoDTO(
                true, "Lucía Torres", "Visa", "Mi Visa", "4111 1111 1111 1111", TipoTarjeta.CREDITO,
                LocalDate.now().plusYears(3), true));
        System.out.println("Número que escribió el cliente : 4111 1111 1111 1111");
        System.out.println("Lo que guarda la base de datos : " + tarjetaGuardada(tarjeta.id()));
        System.out.println("Lo que devuelve la aplicación  : " + tarjeta.numeroEnmascarado());

        paso(6, "Comprar: carrito -> preparación -> pago (el pago descifra la tarjeta)");
        var carrito = new CarritoBOImpl();
        carrito.agregar(cliente.id(), libroA, 2);
        carrito.agregar(cliente.id(), libroB, 1);
        var pedidos = new PedidoBOImpl();
        pedidos.preparar(cliente.id());
        pedidos.seleccionar(cliente.id(), new SeleccionCompraDTO(direccion.id(), tarjeta.id(), 0));
        DetallePedidoDTO pedido = pedidos.pagar(cliente.id());
        System.out.println("Pedido " + pedido.pedido().id() + " " + pedido.pedido().estado()
                + ", total S/ " + pedido.pedido().total() + ", transacción " + pedido.identificadorTransaccion());

        paso(7, "Generar los PDF de RF17, RF18 y RF19");
        Files.createDirectories(carpetaPdf);
        guardar(carpetaPdf, "RF17-comprobante-pedido-" + pedido.pedido().id() + ".pdf",
                new ComprobanteBOImpl().generarPDF(cliente.id(), pedido.pedido().id()));
        var reportes = new ReporteBOImpl();
        LocalDate hoy = LocalDate.now();
        guardar(carpetaPdf, "RF18-ventas-libros.pdf", reportes.generarLibrosPDF(admin.id(),
                new ConsultaReporteLibrosDTO(hoy.withDayOfMonth(1), hoy, 10)));
        guardar(carpetaPdf, "RF19-usuarios.pdf", reportes.generarUsuariosPDF(admin.id(), YearMonth.from(hoy), 10));

        System.out.println("\n=== Listo. Puedes entrar a la web con estas cuentas ===");
        System.out.println("Administrador: " + correoAdmin + " / " + CLAVE_ADMIN + "  (Cuenta -> Reportes)");
        System.out.println("Cliente      : " + correoCliente + " / " + CLAVE_CLIENTE);
        System.out.println("PDF generados en: " + carpetaPdf);
    }

    // Lee directamente la fila para mostrar que la base nunca contiene la contraseña original.
    private static void mostrarContraseniaGuardada(int idUsuario, String original) throws Exception {
        String guardada = new UsuarioDAOImpl().leer(idUsuario).getContrasenia();
        System.out.println("Contraseña escrita : " + original);
        System.out.println("Guardada en la base: " + guardada);
    }

    private static String tarjetaGuardada(int idMetodo) throws Exception {
        return ((TarjetaBancaria) new MetodoPagoDAOImpl().leer(idMetodo)).getNumeroTarjetaCifrado();
    }

    private static RegistroReferenciaDTO referencia(String nombre, String introduccion) {
        return new RegistroReferenciaDTO(new DatosReferenciaDTO(nombre, introduccion), null);
    }

    private static RegistroLibroDTO libro(String titulo, String isbn, int autor, int categoria, int editorial,
            String precio, int stock) {
        var datos = new DatosLibroDTO(titulo, "Español", "Libro creado en el laboratorio de BookShop.",
                "Proveedor del laboratorio", isbn, 2026, 180, null, null, null, null, null);
        return new RegistroLibroDTO(datos, autor, categoria, editorial, new BigDecimal(precio), stock, null);
    }

    private static void guardar(Path carpeta, String nombre, byte[] pdf) throws Exception {
        Path archivo = carpeta.resolve(nombre);
        Files.write(archivo, pdf);
        System.out.println("PDF: " + archivo.getFileName() + " (" + pdf.length / 1024 + " KB)");
    }

    // Misma clave que GlassFish, tanto si se ejecuta desde BookShopBackend como desde el módulo App.
    private static void configurarClave() {
        if (System.getProperty("bookshop.clave.file") == null) {
            Path clave = Files.isDirectory(Path.of("App")) ? Path.of("config", "clave-datos.key")
                    : Path.of("..", "config", "clave-datos.key");
            System.setProperty("bookshop.clave.file", clave.toAbsolutePath().normalize().toString());
        }
        System.out.println("Clave de cifrado: " + System.getProperty("bookshop.clave.file"));
    }

    private static void paso(int numero, String texto) {
        System.out.println("\n--- Paso " + numero + ": " + texto + " ---");
    }
}
