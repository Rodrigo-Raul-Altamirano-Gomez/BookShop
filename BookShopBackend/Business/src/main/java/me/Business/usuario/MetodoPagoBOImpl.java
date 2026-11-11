package me.Business.usuario;

import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import me.Business.exceptions.AccesoDenegadoException;
import me.Business.exceptions.ConflictoException;
import me.Business.exceptions.NegocioException;
import me.Business.exceptions.PersistenciaException;
import me.Business.exceptions.RecursoNoEncontradoException;
import me.Business.exceptions.ValidacionException;
import me.Business.ProteccionDatos;
import me.DAO.usuario.MetodoPagoDAO;
import me.DAO.usuario.MetodoPagoDAOImpl;
import me.DAO.usuario.UsuarioDAO;
import me.DAO.usuario.UsuarioDAOImpl;
import me.DBManager.TransactionContext;
import me.Model.dto.ResultadoPaginadoDTO;
import me.Model.dto.compra.DatosMetodoPagoDTO;
import me.Model.dto.compra.MetodoPagoDTO;
import me.Model.entities.compra.BilleteraDigital;
import me.Model.entities.compra.MetodoPago;
import me.Model.entities.compra.TarjetaBancaria;
import me.Model.entities.usuario.Usuario;

public class MetodoPagoBOImpl implements MetodoPagoBO {

    private final MetodoPagoDAO metodoPagoDAO;
    private final UsuarioDAO usuarioDAO;
    private final Clock reloj;

    public MetodoPagoBOImpl() {
        this(new MetodoPagoDAOImpl(), new UsuarioDAOImpl(), Clock.system(ZoneId.of("America/Lima")));
    }

    public MetodoPagoBOImpl(MetodoPagoDAO metodoPagoDAO, UsuarioDAO usuarioDAO, Clock reloj) {
        this.metodoPagoDAO = metodoPagoDAO;
        this.usuarioDAO = usuarioDAO;
        this.reloj = reloj.withZone(ZoneId.of("America/Lima"));
    }

    @Override
    public MetodoPagoDTO registrar(int idActor, DatosMetodoPagoDTO datos) throws NegocioException {
        try {
            Usuario actor = usuarioDAO.leer(idActor);
            DatosMetodoPagoDTO validos = validarFormulario(datos, false);
            LocalDate hoy = LocalDate.now(reloj);
            MetodoPago recuperado = null;
            List<MetodoPago> existentes = metodoPagoDAO.listarParaComprobarNumeros(validos.tarjetaBancaria());
            for (MetodoPago existente : existentes) {
                String numeroExistente = numeroCompleto(existente);
                if (!numeroExistente.equals(validos.numero()))
                    continue;
                if (existente instanceof BilleteraDigital &&
                    !existente.getProveedor().equalsIgnoreCase(validos.proveedor()))
                    continue;
                if (existente.getUsuario().getId() != idActor || !existente.isEliminado()) {
                    throw new ConflictoException("Ese metodo de pago ya esta registrado.");
                }
                recuperado = existente;
            }
            MetodoPago metodo;
            if (recuperado == null) {
                metodo = validos.tarjetaBancaria() ? new TarjetaBancaria() : new BilleteraDigital();
                metodo.setUsuario(actor);
                metodo.setUsuarioCreacion(actor.getEmail());
                metodo.setFechaCreacion(LocalDateTime.now(reloj));
            } else {
                metodo = recuperado;
                metodo.setUsuarioActualizacion(actor.getEmail());
            }
            asignarDatos(metodo, validos, validos.numero(), hoy);
            if (recuperado == null) {
                metodoPagoDAO.insertar(metodo);
            } else if (!metodoPagoDAO.recuperar(metodo)) {
                throw new ConflictoException("El metodo cambio mientras se intentaba recuperarlo.");
            }
            if (validos.predeterminado()) {
                if (!metodoPagoDAO.establecerPredeterminado(idActor, metodo.getId(), hoy, actor.getEmail())) {
                    throw new ConflictoException("El metodo no puede elegirse como predeterminado.");
                }
                metodo.setPredeterminado(true);
            }
            MetodoPagoDTO respuesta = MetodosPago.aDTO(metodo, hoy);
            TransactionContext.commit();
            return respuesta;
        } catch (SQLException e) {
            TransactionContext.rollback();
            throw new PersistenciaException("No se pudo registrar el metodo de pago.", e);
        } catch (NegocioException e) {
            TransactionContext.rollback();
            throw e;
        } finally {
            TransactionContext.close();
        }
    }

    @Override
    public MetodoPagoDTO modificar(int idActor, int idMetodo, DatosMetodoPagoDTO datos) throws NegocioException {
        try {
            Usuario actor = usuarioDAO.leer(idActor);
            DatosMetodoPagoDTO validos = validarFormulario(datos, true);
            LocalDate hoy = LocalDate.now(reloj);
            MetodoPago metodo = metodoPagoDAO.leer(idMetodo);
            comprobarPropiedad(metodo, actor);
            boolean tarjeta = metodo instanceof TarjetaBancaria;
            if (tarjeta != validos.tarjetaBancaria()) {
                throw new ValidacionException("tarjetaBancaria", "El subtipo del metodo no puede cambiarse.");
            }
            String numero = validos.numero() == null ? numeroCompleto(metodo) : validos.numero();
            for (MetodoPago existente : metodoPagoDAO.listarParaComprobarNumeros(tarjeta)) {
                if (existente.getId() == idMetodo)
                    continue;
                String numeroExistente = numeroCompleto(existente);
                if (!numeroExistente.equals(numero))
                    continue;
                if (existente instanceof BilleteraDigital &&
                    !existente.getProveedor().equalsIgnoreCase(validos.proveedor()))
                    continue;
                throw new ConflictoException("Ese metodo de pago ya esta registrado.");
            }
            boolean eraPredeterminado = metodo.isPredeterminado();
            asignarDatos(metodo, validos, numero, hoy);
            metodo.setUsuarioActualizacion(actor.getEmail());
            metodoPagoDAO.modificar(metodo);
            if (validos.predeterminado()) {
                if (!metodoPagoDAO.establecerPredeterminado(idActor, idMetodo, hoy, actor.getEmail())) {
                    throw new ConflictoException("El metodo no puede elegirse como predeterminado.");
                }
                metodo.setPredeterminado(true);
            } else if (eraPredeterminado) {
                if (!metodoPagoDAO.quitarPredeterminado(idMetodo, actor.getEmail())) {
                    throw new ConflictoException("El metodo predeterminado cambio mientras se editaba.");
                }
                metodo.setPredeterminado(false);
            }
            MetodoPagoDTO respuesta = MetodosPago.aDTO(metodo, hoy);
            TransactionContext.commit();
            return respuesta;
        } catch (SQLException e) {
            TransactionContext.rollback();
            throw new PersistenciaException("No se pudo guardar el metodo de pago.", e);
        } catch (NegocioException e) {
            TransactionContext.rollback();
            throw e;
        } finally {
            TransactionContext.close();
        }
    }

    @Override
    public MetodoPagoDTO consultar(int idActor, int idMetodo) throws NegocioException {
        try {
            Usuario actor = usuarioDAO.leer(idActor);
            MetodoPago metodo = metodoPagoDAO.leer(idMetodo);
            comprobarPropiedad(metodo, actor);
            return MetodosPago.aDTO(metodo, LocalDate.now(reloj));
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo consultar el metodo de pago.", e);
        }
    }

    @Override
    public ResultadoPaginadoDTO<MetodoPagoDTO> listar(int idActor, int pagina, int tamanioPagina) throws
        NegocioException {
        try {
            Usuario actor = usuarioDAO.leer(idActor);
            if (pagina < 1 || tamanioPagina < 1)
                throw new ValidacionException("pagina", "La página y su tamaño deben ser positivos.");
            LocalDate hoy = LocalDate.now(reloj);
            // Entrar a Datos de compra es un evento explicito que actualiza las vigencias.
            metodoPagoDAO.desactivarVencidos(hoy, actor.getEmail());
            ResultadoPaginadoDTO<MetodoPago> registros = metodoPagoDAO.listarPorUsuario(idActor, pagina,
                tamanioPagina);
            List<MetodoPagoDTO> respuestas = new ArrayList<>();
            for (MetodoPago metodo : registros.items()) respuestas.add(MetodosPago.aDTO(metodo, hoy));
            return new ResultadoPaginadoDTO<>(respuestas, registros.totalResultados(), pagina, tamanioPagina);
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudieron consultar los metodos de pago.", e);
        }
    }

    @Override
    public void eliminar(int idActor, int idMetodo) throws NegocioException {
        try {
            Usuario actor = usuarioDAO.leer(idActor);
            comprobarPropiedad(metodoPagoDAO.leer(idMetodo), actor);
            if (!metodoPagoDAO.eliminar(idMetodo, actor.getEmail())) {
                throw new ConflictoException("El metodo ya se habia eliminado.");
            }
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo eliminar el metodo de pago.", e);
        }
    }

    @Override
    public void establecerPredeterminado(int idActor, int idMetodo) throws NegocioException {
        try {
            Usuario actor = usuarioDAO.leer(idActor);
            LocalDate hoy = LocalDate.now(reloj);
            MetodoPago metodo = metodoPagoDAO.leer(idMetodo);
            comprobarPropiedad(metodo, actor);
            if (!metodo.validarEstado(hoy)) {
                throw new ConflictoException("Un metodo vencido no puede elegirse como predeterminado.");
            }
            if (!metodoPagoDAO.establecerPredeterminado(idActor, idMetodo, hoy, actor.getEmail())) {
                throw new ConflictoException("El metodo no puede elegirse como predeterminado.");
            }
            TransactionContext.commit();
        } catch (SQLException e) {
            TransactionContext.rollback();
            throw new PersistenciaException("No se pudo elegir el metodo predeterminado.", e);
        } catch (NegocioException e) {
            TransactionContext.rollback();
            throw e;
        } finally {
            TransactionContext.close();
        }
    }

    private DatosMetodoPagoDTO validarFormulario(DatosMetodoPagoDTO datos, boolean edicion) throws
        ValidacionException {
        if (datos == null)
            throw new ValidacionException("metodo", "Los datos del metodo de pago son obligatorios.");
        String numero = datos.numero();
        if (edicion && (numero == null || numero.isBlank())) {
            numero = null;
        } else {
            if (numero == null)
                throw new ValidacionException("numero", "El numero es obligatorio.");
            numero = numero.replaceAll("[\\s().-]", "");
            String formato = datos.tarjetaBancaria() ? "[0-9]{16}" : "9[0-9]{8}";
            if (!numero.matches(formato)) {
                throw new ValidacionException("numero", datos.tarjetaBancaria() ?
                    "La tarjeta debe tener dieciseis digitos." :
                    "El celular debe tener nueve digitos y comenzar por 9.");
            }
        }
        if (datos.tarjetaBancaria()) {
            if (datos.tipoTarjeta() == null)
                throw new ValidacionException("tipoTarjeta", "Selecciona credito o debito.");
            if (datos.fechaVencimiento() == null || datos.fechaVencimiento().isBefore(LocalDate.now(reloj))) {
                throw new ValidacionException("fechaVencimiento", "La tarjeta debe tener un vencimiento vigente.");
            }
        } else if (datos.tipoTarjeta() != null || datos.fechaVencimiento() != null) {
            throw new ValidacionException("fechaVencimiento",
                "La vigencia de la billetera se determina automaticamente.");
        }
        String titular = datos.titular() == null ? "" : datos.titular().strip();
        if (titular.isEmpty())
            throw new ValidacionException("titular", "Este campo es obligatorio.");
        if (titular.length() > 150)
            throw new ValidacionException("titular", "El máximo es 150 caracteres.");
        String proveedor = datos.proveedor() == null ? "" : datos.proveedor().strip();
        if (proveedor.isEmpty())
            throw new ValidacionException("proveedor", "Este campo es obligatorio.");
        if (proveedor.length() > 100)
            throw new ValidacionException("proveedor", "El máximo es 100 caracteres.");
        String alias = datos.alias() == null ? null : datos.alias().strip();
        if (alias != null && alias.isEmpty())
            alias = null;
        if (alias != null && alias.length() > 100)
            throw new ValidacionException("alias", "El máximo es 100 caracteres.");
        return new DatosMetodoPagoDTO(datos.tarjetaBancaria(), titular, proveedor, alias, numero,
            datos.tipoTarjeta(), datos.fechaVencimiento(), datos.predeterminado());
    }

    private String numeroCompleto(MetodoPago metodo) throws NegocioException {
        String cifrado;
        if (metodo instanceof TarjetaBancaria) {
            TarjetaBancaria tarjeta = (TarjetaBancaria) metodo;
            cifrado = tarjeta.getNumeroTarjetaCifrado();
        } else {
            BilleteraDigital billetera = (BilleteraDigital) metodo;
            cifrado = billetera.getNumeroTelefonoCifrado();
        }
        return ProteccionDatos.descifrar(cifrado);
    }

    private void asignarDatos(MetodoPago metodo, DatosMetodoPagoDTO datos, String numero, LocalDate hoy)
        throws NegocioException {
        metodo.setTitular(datos.titular());
        metodo.setProveedor(datos.proveedor());
        metodo.setAlias(datos.alias());
        if (metodo instanceof TarjetaBancaria) {
            TarjetaBancaria tarjeta = (TarjetaBancaria) metodo;
            tarjeta.setNumeroTarjetaCifrado(ProteccionDatos.cifrar(numero));
            tarjeta.setTipoTarjeta(datos.tipoTarjeta());
            tarjeta.setFechaVencimiento(datos.fechaVencimiento());
        } else {
            BilleteraDigital billetera = (BilleteraDigital) metodo;
            billetera.setNumeroTelefonoCifrado(ProteccionDatos.cifrar(numero));
            if (billetera.getFechaVencimiento() == null || billetera.getFechaVencimiento().isBefore(hoy)) {
                billetera.setFechaVencimiento(hoy.plusDays(365));
            }
        }
        metodo.setActivo(!metodo.getFechaVencimiento().isBefore(hoy));
    }

    private void comprobarPropiedad(MetodoPago metodo, Usuario actor) throws NegocioException {
        if (metodo == null || metodo.isEliminado())
            throw new RecursoNoEncontradoException("El metodo de pago no existe.");
        if (metodo.getUsuario().getId() != actor.getId()) {
            throw new AccesoDenegadoException("El metodo de pago pertenece a otro usuario.");
        }
    }
}
