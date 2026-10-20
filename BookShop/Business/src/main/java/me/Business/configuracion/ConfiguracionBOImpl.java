package me.Business.configuracion;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;
import me.Business.exceptions.ConfiguracionException;
import me.Business.exceptions.NegocioException;
import me.Business.exceptions.ValidacionException;
import me.Business.Autorizacion;
import me.DAO.usuario.UsuarioDAOImpl;
import me.Model.dto.ConfiguracionDTO;

public class ConfiguracionBOImpl implements ConfiguracionBO {
    private final Path archivo;
    private final Autorizacion autorizacion;
    private ConfiguracionDTO configuracion;

    public ConfiguracionBOImpl() throws NegocioException {
        this(Path.of(System.getProperty("bookshop.config.file", "config/negocio.properties")),
                new Autorizacion(new UsuarioDAOImpl()));
    }

    public ConfiguracionBOImpl(Path archivo, Autorizacion autorizacion) throws NegocioException {
        this.archivo = archivo.toAbsolutePath();
        this.autorizacion = autorizacion;
        this.configuracion = cargar();
    }

    public static synchronized ConfiguracionDTO actual() throws NegocioException {
        return new ConfiguracionBOImpl().configuracion;
    }

    @Override
    public synchronized ConfiguracionDTO obtener(int idActor) throws NegocioException {
        autorizacion.administrador(idActor);
        configuracion = cargar();
        return configuracion;
    }

    @Override
    public synchronized void guardar(int idActor, ConfiguracionDTO datos) throws NegocioException {
        autorizacion.administrador(idActor);
        ConfiguracionDTO nueva = validar(datos);
        Properties valores = new Properties();
        valores.setProperty("porcentajeEnvio", nueva.porcentajeEnvio().toPlainString());
        valores.setProperty("plazoDiasDevolucion", Integer.toString(nueva.plazoDiasDevolucion()));
        valores.setProperty("maximoIntentosDevolucion", Integer.toString(nueva.maximoIntentosDevolucion()));
        valores.setProperty("pedidosEntregadosPorCupon", Integer.toString(nueva.pedidosEntregadosPorCupon()));
        valores.setProperty("porcentajeCupon", nueva.porcentajeCupon().toPlainString());
        valores.setProperty("montoMinimoCupon", nueva.montoMinimoCupon().toPlainString());
        valores.setProperty("vigenciaDiasCupon", Integer.toString(nueva.vigenciaDiasCupon()));
        Path temporal = null;
        try {
            Files.createDirectories(archivo.getParent());
            temporal = Files.createTempFile(archivo.getParent(), "negocio-", ".tmp");
            try (Writer salida = Files.newBufferedWriter(temporal, StandardCharsets.UTF_8)) {
                valores.store(salida, "Configuracion de negocio BookShop");
            }
            Files.move(temporal, archivo, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            configuracion = nueva;
        } catch (IOException | SecurityException e) {
            throw new ConfiguracionException("No se pudo guardar la configuración; se conserva la anterior.", e);
        } finally {
            if (temporal != null) {
                try { Files.deleteIfExists(temporal); }
                catch (IOException e) { /* La configuración vigente no depende del temporal. */ }
            }
        }
    }

    private ConfiguracionDTO cargar() throws NegocioException {
        if (!Files.exists(archivo)) return inicial();
        Properties p = new Properties();
        try (Reader entrada = Files.newBufferedReader(archivo, StandardCharsets.UTF_8)) {
            p.load(entrada);
            return validar(new ConfiguracionDTO(new BigDecimal(p.getProperty("porcentajeEnvio")),
                    Integer.parseInt(p.getProperty("plazoDiasDevolucion")),
                    Integer.parseInt(p.getProperty("maximoIntentosDevolucion")),
                    Integer.parseInt(p.getProperty("pedidosEntregadosPorCupon")),
                    new BigDecimal(p.getProperty("porcentajeCupon")),
                    new BigDecimal(p.getProperty("montoMinimoCupon")),
                    Integer.parseInt(p.getProperty("vigenciaDiasCupon"))));
        } catch (IOException | IllegalArgumentException | NullPointerException | ValidacionException e) {
            throw new ConfiguracionException("El archivo de configuración no es válido; no se sobrescribió.", e);
        }
    }

    public static ConfiguracionDTO inicial() {
        return new ConfiguracionDTO(new BigDecimal("5.00"), 30, 3, 5,
                new BigDecimal("10.00"), new BigDecimal("100.00"), 30);
    }

    private static ConfiguracionDTO validar(ConfiguracionDTO d) throws ValidacionException {
        if (d == null) throw new ValidacionException("configuracion", "Faltan los parámetros.");
        porcentaje(d.porcentajeEnvio(), "porcentajeEnvio", true);
        porcentaje(d.porcentajeCupon(), "porcentajeCupon", false);
        if (d.plazoDiasDevolucion() < 1) throw new ValidacionException("plazoDiasDevolucion", "Debe ser positivo.");
        if (d.maximoIntentosDevolucion() < 1) throw new ValidacionException("maximoIntentosDevolucion", "Debe ser positivo.");
        if (d.pedidosEntregadosPorCupon() < 1) throw new ValidacionException("pedidosEntregadosPorCupon", "Debe ser positivo.");
        if (d.vigenciaDiasCupon() < 1) throw new ValidacionException("vigenciaDiasCupon", "Debe ser positivo.");
        if (d.montoMinimoCupon() == null || d.montoMinimoCupon().signum() < 0
                || d.montoMinimoCupon().compareTo(new BigDecimal("999999999999.99")) > 0)
            throw new ValidacionException("montoMinimoCupon", "El importe debe ser no negativo y caber en el esquema.");
        return new ConfiguracionDTO(d.porcentajeEnvio().setScale(2, RoundingMode.HALF_UP),
                d.plazoDiasDevolucion(), d.maximoIntentosDevolucion(), d.pedidosEntregadosPorCupon(),
                d.porcentajeCupon().setScale(2, RoundingMode.HALF_UP),
                d.montoMinimoCupon().setScale(2, RoundingMode.HALF_UP), d.vigenciaDiasCupon());
    }

    private static void porcentaje(BigDecimal valor, String campo, boolean permiteCero) throws ValidacionException {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) < (permiteCero ? 0 : 1)
                || valor.compareTo(new BigDecimal("100")) > 0
                || !permiteCero && valor.setScale(2, RoundingMode.HALF_UP).signum() == 0)
            throw new ValidacionException(campo, "El porcentaje debe estar " + (permiteCero ? "entre 0" : "por encima de 0") + " y 100.");
    }
}
