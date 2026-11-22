package pe.edu.pucp.bookshop.rs;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import pe.edu.pucp.bookshop.bl.hilo.SeguimientoHilo;

// Arranca el hilo de seguimiento al desplegar la aplicación y lo detiene al apagarla
@WebListener
public class SeguimientoListener implements ServletContextListener {

    private SeguimientoHilo hilo;

    @Override
    public void contextInitialized(ServletContextEvent evento) {
        hilo = new SeguimientoHilo();
        hilo.start();
    }

    @Override
    public void contextDestroyed(ServletContextEvent evento) {
        hilo.interrupt();
    }
}
