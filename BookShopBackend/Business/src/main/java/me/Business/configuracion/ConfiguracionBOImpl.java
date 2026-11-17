package me.Business.configuracion;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Properties;
import me.Business.exceptions.NegocioException;
import me.Business.exceptions.ValidacionException;
import me.Model.dto.ConfiguracionDTO;

// Los parámetros del negocio se guardan en un archivo de propiedades junto al servidor.
public class ConfiguracionBOImpl implements ConfiguracionBO {

    private static final String ARCHIVO = "negocio.properties";

    @Override
    public ConfiguracionDTO obtener() throws NegocioException {
        File archivo = new File(ARCHIVO);
        if (!archivo.exists())
            return inicial();
        Properties valores = new Properties();
        try (FileInputStream entrada = new FileInputStream(archivo)) {
            valores.load(entrada);
            return new ConfiguracionDTO(
                new BigDecimal(valores.getProperty("porcentajeEnvio")),
                Integer.parseInt(valores.getProperty("plazoDiasDevolucion")),
                Integer.parseInt(valores.getProperty("maximoIntentosDevolucion")),
                Integer.parseInt(valores.getProperty("pedidosEntregadosPorCupon")),
                new BigDecimal(valores.getProperty("porcentajeCupon")),
                new BigDecimal(valores.getProperty("montoMinimoCupon")),
                Integer.parseInt(valores.getProperty("vigenciaDiasCupon")));
        } catch (IOException | RuntimeException e) {
            throw new NegocioException("No se pudo leer la configuración del negocio.", e);
        }
    }

    @Override
    public void guardar(ConfiguracionDTO datos) throws NegocioException {
        validar(datos);
        Properties valores = new Properties();
        valores.setProperty("porcentajeEnvio", datos.porcentajeEnvio().toPlainString());
        valores.setProperty("plazoDiasDevolucion", String.valueOf(datos.plazoDiasDevolucion()));
        valores.setProperty("maximoIntentosDevolucion", String.valueOf(datos.maximoIntentosDevolucion()));
        valores.setProperty("pedidosEntregadosPorCupon", String.valueOf(datos.pedidosEntregadosPorCupon()));
        valores.setProperty("porcentajeCupon", datos.porcentajeCupon().toPlainString());
        valores.setProperty("montoMinimoCupon", datos.montoMinimoCupon().toPlainString());
        valores.setProperty("vigenciaDiasCupon", String.valueOf(datos.vigenciaDiasCupon()));
        try (FileOutputStream salida = new FileOutputStream(ARCHIVO)) {
            valores.store(salida, "Configuracion de negocio BookShop");
        } catch (IOException e) {
            throw new NegocioException("No se pudo guardar la configuración.", e);
        }
    }

    // Valores usados mientras el administrador no guarde otros.
    private ConfiguracionDTO inicial() {
        return new ConfiguracionDTO(new BigDecimal("5.00"), 30, 3, 5, new BigDecimal("10.00"),
            new BigDecimal("100.00"), 30);
    }

    private void validar(ConfiguracionDTO datos) throws ValidacionException {
        BigDecimal cien = new BigDecimal("100");
        if (datos.porcentajeEnvio().signum() < 0 || datos.porcentajeEnvio().compareTo(cien) > 0)
            throw new ValidacionException("porcentajeEnvio", "El envío debe estar entre 0 y 100.");
        if (datos.porcentajeCupon().signum() <= 0 || datos.porcentajeCupon().compareTo(cien) > 0)
            throw new ValidacionException("porcentajeCupon", "El cupón debe estar entre 0 y 100.");
        if (datos.montoMinimoCupon().signum() < 0)
            throw new ValidacionException("montoMinimoCupon", "El monto mínimo no puede ser negativo.");
        if (datos.plazoDiasDevolucion() < 1 || datos.maximoIntentosDevolucion() < 1
            || datos.pedidosEntregadosPorCupon() < 1 || datos.vigenciaDiasCupon() < 1)
            throw new ValidacionException("configuracion", "Los plazos y cantidades deben ser positivos.");
    }
}
