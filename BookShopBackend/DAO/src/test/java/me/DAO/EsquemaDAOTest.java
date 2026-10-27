package me.DAO;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EsquemaDAOTest {
    @Test
    void cadaTablaTieneSuInterfazYSuImplementacion() throws Exception {
        Map<String, String> contratos = Map.ofEntries(
                Map.entry("USUARIO", "usuario.Usuario"),
                Map.entry("DIRECCION_ENVIO", "usuario.DireccionEnvio"),
                Map.entry("METODO_PAGO", "usuario.MetodoPago"),
                Map.entry("LIBRO_GUARDADO", "usuario.LibroGuardado"),
                Map.entry("SEGUIMIENTO", "usuario.Seguimiento"),
                Map.entry("AUTOR", "catalogo.Autor"),
                Map.entry("CATEGORIA", "catalogo.Categoria"),
                Map.entry("EDITORIAL", "catalogo.Editorial"),
                Map.entry("LIBRO", "catalogo.Libro"),
                Map.entry("DESCUENTO", "catalogo.Descuento"),
                Map.entry("RESENA", "catalogo.Resena"),
                Map.entry("VOTO_UTIL", "catalogo.VotoUtil"),
                Map.entry("CUPON", "venta.Cupon"),
                Map.entry("PEDIDO", "venta.Pedido"),
                Map.entry("ITEM_PEDIDO", "venta.ItemPedido"),
                Map.entry("SOLICITUD", "solicitud.Solicitud")
        );
        var matcher = Pattern.compile("CREATE TABLE (\\w+)")
                .matcher(Files.readString(Path.of("src/main/resources/DDL.sql")));
        Set<String> tablas = new HashSet<>();
        while (matcher.find()) tablas.add(matcher.group(1));
        assertEquals(tablas, contratos.keySet(), "No omitir tablas ni crear DAO para subtipos sin tabla.");
        for (var contrato : contratos.values()) {
            Class<?> interfaz = Class.forName("me.DAO." + contrato + "DAO");
            Class<?> implementacion = Class.forName("me.DAO." + contrato + "DAOImpl");
            assertTrue(interfaz.isInterface());
            assertTrue(interfaz.isAssignableFrom(implementacion));
            assertNotNull(implementacion.getConstructor().newInstance());
        }
    }
}
