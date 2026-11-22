package pe.edu.pucp.bookshop.bl.hilo;

import pe.edu.pucp.bookshop.bl.CuponBL;
import pe.edu.pucp.bookshop.bl.PedidoBL;
import pe.edu.pucp.bookshop.bl.exception.BusinessLogicException;
import pe.edu.pucp.bookshop.bl.impl.CuponBLImpl;
import pe.edu.pucp.bookshop.bl.impl.PedidoBLImpl;

// Hilo del sistema (ERS RF03.4): cada minuto avanza el seguimiento de los pedidos y emite los cupones.
// Si una vuelta falla, se imprime el error y la siguiente lo vuelve a intentar.
public class SeguimientoHilo extends Thread {

    private PedidoBL pedidoBL = new PedidoBLImpl();
    private CuponBL cuponBL = new CuponBLImpl();

    @Override
    public void run() {
        while (!isInterrupted()) {
            try {
                pedidoBL.avanzarSeguimiento();
                cuponBL.emitirCupones();
            } catch (BusinessLogicException ex) {
                ex.printStackTrace();
            }
            try {
                Thread.sleep(60000);
            } catch (InterruptedException ex) {
                return;
            }
        }
    }
}
