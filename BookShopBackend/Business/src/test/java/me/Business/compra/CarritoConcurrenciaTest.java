package me.Business.compra;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import me.Business.configuracion.ConfiguracionBOImpl;
import me.DAO.catalogo.DescuentoDAO;
import me.DAO.catalogo.LibroDAO;
import me.Model.entities.catalogo.Libro;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CarritoConcurrenciaTest {
    @Test
    @Timeout(10)
    void dosHilosAgreganAlMismoCarritoSinPerderCantidades() throws Exception {
        LibroDAO libros = mock(LibroDAO.class);
        Libro libro = new Libro();
        libro.setId(1);
        libro.setPrecioBase(BigDecimal.TEN);
        libro.setStockFisico(500);
        when(libros.leer(1)).thenReturn(libro);
        CarritoBOImpl carrito = new CarritoBOImpl(libros, mock(DescuentoDAO.class),
            Clock.systemUTC(), ConfiguracionBOImpl::inicial);
        CountDownLatch inicio = new CountDownLatch(1);
        AtomicReference<Throwable> fallo = new AtomicReference<>();
        Runnable agregar = () -> {
            try {
                inicio.await();
                for (int i = 0; i < 100; i++)
                    carrito.agregar(1, 1, 1);
            } catch (Exception e) {
                fallo.set(e);
            }
        };
        Thread primero = new Thread(agregar);
        Thread segundo = new Thread(agregar);
        primero.start();
        segundo.start();
        inicio.countDown();
        primero.join();
        segundo.join();
        assertNull(fallo.get());
        assertEquals(200, carrito.consultar(1).cantidadLibros());
    }
}
