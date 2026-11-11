package me.Business.configuracion;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import me.Business.exceptions.ConfiguracionException;
import me.Business.exceptions.NegocioException;
import me.Business.exceptions.ValidacionException;
import me.Model.dto.ConfiguracionDTO;

public class ConfiguracionBOImpl implements ConfiguracionBO {

    private final Path archivo;
    private ConfiguracionDTO configuracion;

    public ConfiguracionBOImpl() throws NegocioException {
        this(Path.of(System.getProperty("bookshop.config.file", "config/negocio.properties")));
    }

    public ConfiguracionBOImpl(Path archivo) throws NegocioException {
        this.archivo = archivo.toAbsolutePath();
        this.configuracion = cargar();
    }

    public static ConfiguracionDTO actual() throws NegocioException {
        return new ConfiguracionBOImpl().configuracion;
    }

    @Override
    public ConfiguracionDTO obtener(int idActor) throws NegocioException {
        configuracion = cargar();
        return configuracion;
    }

    @Override
    public void guardar(int idActor, ConfiguracionDTO datos) throws NegocioException {
        ConfiguracionDTO nueva = validar(datos);
        Properties valores = new Properties();
        valores.setProperty("porcentajeEnvio", nueva.porcentajeEnvio().toPlainString());
        valores.setProperty("plazoDiasDevolucion", Integer.toString(nueva.plazoDiasDevolucion()));
        valores.setProperty("maximoIntentosDevolucion", Integer.toString(nueva.maximoIntentosDevolucion()));
        valores.setProperty("pedidosEntregadosPorCupon", Integer.toString(nueva.pedidosEntregadosPorCupon()));
        valores.setProperty("porcentajeCupon", nueva.porcentajeCupon().toPlainString());
        valores.setProperty("montoMinimoCupon", nueva.montoMinimoCupon().toPlainString());
        valores.setProperty("vigenciaDiasCupon", Integer.toString(nueva.vigenciaDiasCupon()));
        try {
            Files.createDirectories(archivo.getParent());
            try (Writer salida = Files.newBufferedWriter(archivo, StandardCharsets.UTF_8)) {
                valores.store(salida, "Configuracion de negocio BookShop");
            }
            configuracion = nueva;
        } catch (IOException | SecurityException e) {
            throw new ConfiguracionException("No se pudo guardar la configuración.", e);
        }
    }

    private ConfiguracionDTO cargar() throws NegocioException {
        if (!Files.exists(archivo))
            return inicial();
        Properties valores = new Properties();
        try (Reader entrada = Files.newBufferedReader(archivo, StandardCharsets.UTF_8)) {
            valores.load(entrada);
            return validar(new ConfiguracionDTO(new BigDecimal(valores.getProperty("porcentajeEnvio")),
                Integer.parseInt(valores.getProperty("plazoDiasDevolucion")),
                Integer.parseInt(valores.getProperty("maximoIntentosDevolucion")),
                Integer.parseInt(valores.getProperty("pedidosEntregadosPorCupon")), new
                BigDecimal(valores.getProperty("porcentajeCupon")), new
                BigDecimal(valores.getProperty("montoMinimoCupon")),
                Integer.parseInt(valores.getProperty("vigenciaDiasCupon"))));
        } catch (IOException | IllegalArgumentException | NullPointerException | ValidacionException e) {
            throw new ConfiguracionException("El archivo de configuración no es válido; no se sobrescribió.", e);
        }
    }

    public static ConfiguracionDTO inicial() {
        return new ConfiguracionDTO(new BigDecimal("5.00"), 30, 3, 5, new BigDecimal("10.00"), new
            BigDecimal("100.00"), 30);
    }

    private static ConfiguracionDTO validar(ConfiguracionDTO datos) throws ValidacionException {
        if (datos == null)
            throw new ValidacionException("configuracion", "Faltan los parámetros.");
        if (datos.porcentajeEnvio() == null || datos.porcentajeEnvio().signum() < 0 ||
            datos.porcentajeEnvio().compareTo(new BigDecimal("100")) > 0) {
            throw new ValidacionException("porcentajeEnvio", "El envío debe estar entre 0 y 100.");
        }
        if (datos.porcentajeCupon() == null || datos.porcentajeCupon().compareTo(new BigDecimal("100")) > 0) {
            throw new ValidacionException("porcentajeCupon",
                "El porcentaje del cupón debe estar por encima de 0 y hasta 100.");
        }
        BigDecimal porcentajeCupon = datos.porcentajeCupon().setScale(2, RoundingMode.HALF_UP);
        if (porcentajeCupon.signum() <= 0) {
            throw new ValidacionException("porcentajeCupon", "El porcentaje del cupón debe ser positivo.");
        }
        if (datos.plazoDiasDevolucion() < 1)
            throw new ValidacionException("plazoDiasDevolucion", "El plazo debe ser positivo.");
        if (datos.maximoIntentosDevolucion() < 1)
            throw new ValidacionException("maximoIntentosDevolucion", "El máximo de intentos debe ser positivo.");
        if (datos.pedidosEntregadosPorCupon() < 1)
            throw new ValidacionException("pedidosEntregadosPorCupon",
                "La cantidad de pedidos debe ser positiva.");
        if (datos.vigenciaDiasCupon() < 1)
            throw new ValidacionException("vigenciaDiasCupon", "La vigencia debe ser positiva.");
        if (datos.montoMinimoCupon() == null || datos.montoMinimoCupon().signum() < 0)
            throw new ValidacionException("montoMinimoCupon", "El monto mínimo no puede ser negativo.");
        return new ConfiguracionDTO(datos.porcentajeEnvio().setScale(2, RoundingMode.HALF_UP),
            datos.plazoDiasDevolucion(), datos.maximoIntentosDevolucion(), datos.pedidosEntregadosPorCupon(),
            porcentajeCupon, datos.montoMinimoCupon().setScale(2, RoundingMode.HALF_UP),
            datos.vigenciaDiasCupon());
    }
}
