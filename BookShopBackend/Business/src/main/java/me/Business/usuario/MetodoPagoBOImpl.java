package me.Business.usuario;

import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import me.Business.exceptions.AccesoDenegadoException;
import me.Business.exceptions.ConflictoException;
import me.Business.exceptions.CuentaInactivaException;
import me.Business.exceptions.NegocioException;
import me.Business.exceptions.PersistenciaException;
import me.Business.exceptions.RecursoNoEncontradoException;
import me.Business.exceptions.ValidacionException;
import me.Business.Autorizacion;
import me.Business.ProteccionDatos;
import me.Business.Validaciones;
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
    private final MetodoPagoDAO metodos;
    private final UsuarioDAO usuarios;
    private final Clock reloj;
    private final Autorizacion autorizacion;

    public MetodoPagoBOImpl() {
        this(new MetodoPagoDAOImpl(), new UsuarioDAOImpl(), Clock.system(ZoneId.of("America/Lima")));
    }

    public MetodoPagoBOImpl(MetodoPagoDAO metodos, UsuarioDAO usuarios, Clock reloj) {
        this.metodos = Objects.requireNonNull(metodos);
        this.usuarios = Objects.requireNonNull(usuarios);
        this.reloj = Objects.requireNonNull(reloj).withZone(ZoneId.of("America/Lima"));
        this.autorizacion = new Autorizacion(usuarios);
    }

    @Override
    public MetodoPagoDTO registrar(int idActor, DatosMetodoPagoDTO datos) throws NegocioException {
        Usuario actor = autorizacion.usuarioActivo(idActor);
        DatosMetodoPagoDTO validos = validar(datos, false);
        LocalDate hoy = LocalDate.now(reloj);
        try {
            bloquearUsuario(idActor);
            MetodoPago recuperado = null;
            List<MetodoPago> existentes = metodos.listarParaComprobarNumeros(validos.tarjetaBancaria());
            for (MetodoPago existente : existentes) {
                if (!mismoNumero(existente, validos.numero(), validos.proveedor())) continue;
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
                metodos.insertar(metodo);
            } else if (!metodos.recuperar(metodo)) {
                throw new ConflictoException("El metodo cambio mientras se intentaba recuperarlo.");
            }
            if (validos.predeterminado()) {
                if (!metodos.establecerPredeterminado(idActor, metodo.getId(), hoy, actor.getEmail())) {
                    throw new ConflictoException("El metodo no puede elegirse como predeterminado.");
                }
                metodo.setPredeterminado(true);
            }
            MetodoPagoDTO respuesta = MetodosPago.aDTO(metodo, hoy);
            TransactionContext.commit();
            return respuesta;
        } catch (SQLException e) {
            TransactionContext.rollback();
            if (e.getErrorCode() == 1062 || "23505".equals(e.getSQLState())) {
                throw new ConflictoException("El metodo de pago ya esta registrado o cambio su predeterminado.");
            }
            throw new PersistenciaException("No se pudo registrar el metodo de pago.", e);
        } catch (NegocioException | RuntimeException e) {
            TransactionContext.rollback();
            throw e;
        } finally {
            TransactionContext.close();
        }
    }

    @Override
    public MetodoPagoDTO modificar(int idActor, int idMetodo, DatosMetodoPagoDTO datos) throws NegocioException {
        Usuario actor = autorizacion.usuarioActivo(idActor);
        DatosMetodoPagoDTO validos = validar(datos, true);
        LocalDate hoy = LocalDate.now(reloj);
        try {
            bloquearUsuario(idActor);
            MetodoPago metodo = metodos.leerParaActualizar(idMetodo);
            comprobarPropiedad(metodo, actor);
            boolean tarjeta = metodo instanceof TarjetaBancaria;
            if (tarjeta != validos.tarjetaBancaria()) {
                throw new ValidacionException("tarjetaBancaria", "El subtipo del metodo no puede cambiarse.");
            }
            String numero = validos.numero() == null ? numeroCompleto(metodo) : validos.numero();
            for (MetodoPago existente : metodos.listarParaComprobarNumeros(tarjeta)) {
                if (existente.getId() != idMetodo && mismoNumero(existente, numero, validos.proveedor())) {
                    throw new ConflictoException("Ese metodo de pago ya esta registrado.");
                }
            }
            boolean eraPredeterminado = metodo.isPredeterminado();
            asignarDatos(metodo, validos, numero, hoy);
            metodo.setUsuarioActualizacion(actor.getEmail());
            metodos.modificar(metodo);
            if (validos.predeterminado()) {
                if (!metodos.establecerPredeterminado(idActor, idMetodo, hoy, actor.getEmail())) {
                    throw new ConflictoException("El metodo no puede elegirse como predeterminado.");
                }
                metodo.setPredeterminado(true);
            } else if (eraPredeterminado) {
                if (!metodos.quitarPredeterminado(idMetodo, actor.getEmail())) {
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
        } catch (NegocioException | RuntimeException e) {
            TransactionContext.rollback();
            throw e;
        } finally {
            TransactionContext.close();
        }
    }

    @Override
    public MetodoPagoDTO consultar(int idActor, int idMetodo) throws NegocioException {
        Usuario actor = autorizacion.usuarioActivo(idActor);
        try {
            MetodoPago metodo = metodos.leer(idMetodo);
            comprobarPropiedad(metodo, actor);
            return MetodosPago.aDTO(metodo, LocalDate.now(reloj));
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo consultar el metodo de pago.", e);
        }
    }

    @Override
    public ResultadoPaginadoDTO<MetodoPagoDTO> listar(int idActor, int pagina, int tamanioPagina)
            throws NegocioException {
        Usuario actor = autorizacion.usuarioActivo(idActor);
        Validaciones.pagina(pagina, tamanioPagina);
        LocalDate hoy = LocalDate.now(reloj);
        try {
            // Entrar a Datos de compra es un evento explicito que actualiza las vigencias.
            metodos.desactivarVencidos(hoy, actor.getEmail());
            ResultadoPaginadoDTO<MetodoPago> registros = metodos.listarPorUsuario(idActor, pagina, tamanioPagina);
            List<MetodoPagoDTO> respuestas = new ArrayList<>();
            for (MetodoPago metodo : registros.items()) respuestas.add(MetodosPago.aDTO(metodo, hoy));
            return new ResultadoPaginadoDTO<>(respuestas, registros.totalResultados(), pagina, tamanioPagina);
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudieron consultar los metodos de pago.", e);
        }
    }

    @Override
    public void eliminar(int idActor, int idMetodo) throws NegocioException {
        Usuario actor = autorizacion.usuarioActivo(idActor);
        try {
            comprobarPropiedad(metodos.leer(idMetodo), actor);
            if (!metodos.eliminar(idMetodo, actor.getEmail())) {
                throw new ConflictoException("El metodo ya se habia eliminado.");
            }
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo eliminar el metodo de pago.", e);
        }
    }

    @Override
    public void establecerPredeterminado(int idActor, int idMetodo) throws NegocioException {
        Usuario actor = autorizacion.usuarioActivo(idActor);
        LocalDate hoy = LocalDate.now(reloj);
        try {
            bloquearUsuario(idActor);
            MetodoPago metodo = metodos.leerParaActualizar(idMetodo);
            comprobarPropiedad(metodo, actor);
            if (!metodo.validarEstado(hoy)) {
                throw new ConflictoException("Un metodo vencido no puede elegirse como predeterminado.");
            }
            if (!metodos.establecerPredeterminado(idActor, idMetodo, hoy, actor.getEmail())) {
                throw new ConflictoException("El metodo no puede elegirse como predeterminado.");
            }
            TransactionContext.commit();
        } catch (SQLException e) {
            TransactionContext.rollback();
            throw new PersistenciaException("No se pudo elegir el metodo predeterminado.", e);
        } catch (NegocioException | RuntimeException e) {
            TransactionContext.rollback();
            throw e;
        } finally {
            TransactionContext.close();
        }
    }

    private DatosMetodoPagoDTO validar(DatosMetodoPagoDTO datos, boolean edicion) throws ValidacionException {
        if (datos == null) throw new ValidacionException("metodo", "Los datos del metodo de pago son obligatorios.");
        String numero = datos.numero();
        if (edicion && (numero == null || numero.isBlank())) {
            numero = null;
        } else {
            if (numero == null) throw new ValidacionException("numero", "El numero es obligatorio.");
            numero = numero.replaceAll("[\\s().-]", "");
            String formato = datos.tarjetaBancaria() ? "[0-9]{16}" : "9[0-9]{8}";
            if (!numero.matches(formato)) {
                throw new ValidacionException("numero", datos.tarjetaBancaria()
                        ? "La tarjeta debe tener dieciseis digitos." : "El celular debe tener nueve digitos y comenzar por 9.");
            }
        }
        if (datos.tarjetaBancaria()) {
            if (datos.tipoTarjeta() == null) throw new ValidacionException("tipoTarjeta", "Selecciona credito o debito.");
            if (datos.fechaVencimiento() == null || datos.fechaVencimiento().isBefore(LocalDate.now(reloj))) {
                throw new ValidacionException("fechaVencimiento", "La tarjeta debe tener un vencimiento vigente.");
            }
        } else if (datos.tipoTarjeta() != null || datos.fechaVencimiento() != null) {
            throw new ValidacionException("fechaVencimiento", "La vigencia de la billetera se determina automaticamente.");
        }
        return new DatosMetodoPagoDTO(datos.tarjetaBancaria(), Validaciones.texto(datos.titular(), "titular", 150),
                Validaciones.texto(datos.proveedor(), "proveedor", 100), Validaciones.opcional(datos.alias(), "alias", 100),
                numero, datos.tipoTarjeta(), datos.fechaVencimiento(), datos.predeterminado());
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

    private boolean mismoNumero(MetodoPago existente, String numero, String proveedor) throws NegocioException {
        if (!numero.equals(numeroCompleto(existente))) return false;
        return existente instanceof TarjetaBancaria || existente.getProveedor().trim().equalsIgnoreCase(proveedor);
    }

    private void asignarDatos(MetodoPago metodo, DatosMetodoPagoDTO datos, String numero, LocalDate hoy)
            throws NegocioException {
        metodo.setTitular(datos.titular());
        metodo.setProveedor(datos.proveedor());
        metodo.setAlias(datos.alias());
        if (metodo instanceof TarjetaBancaria) {
            TarjetaBancaria tarjeta = (TarjetaBancaria) metodo;
            if (tarjeta.getNumeroTarjetaCifrado() == null || !numero.equals(numeroCompleto(tarjeta))) {
                tarjeta.setNumeroTarjetaCifrado(ProteccionDatos.cifrar(numero));
            }
            tarjeta.setTipoTarjeta(datos.tipoTarjeta());
            tarjeta.setFechaVencimiento(datos.fechaVencimiento());
        } else {
            BilleteraDigital billetera = (BilleteraDigital) metodo;
            if (billetera.getNumeroTelefonoCifrado() == null || !numero.equals(numeroCompleto(billetera))) {
                billetera.setNumeroTelefonoCifrado(ProteccionDatos.cifrar(numero));
            }
            if (billetera.getFechaVencimiento() == null || billetera.getFechaVencimiento().isBefore(hoy)) {
                billetera.setFechaVencimiento(hoy.plusDays(365));
            }
        }
        metodo.setActivo(!metodo.getFechaVencimiento().isBefore(hoy));
    }

    private void bloquearUsuario(int idActor) throws SQLException, NegocioException {
        Usuario usuario = usuarios.leerParaActualizar(idActor);
        if (usuario == null || !usuario.estaDisponible()) throw new CuentaInactivaException("La cuenta ya no esta activa.");
    }

    private void comprobarPropiedad(MetodoPago metodo, Usuario actor) throws NegocioException {
        if (metodo == null || metodo.isEliminado()) throw new RecursoNoEncontradoException("El metodo de pago no existe.");
        if (metodo.getUsuario().getId() != actor.getId()) {
            throw new AccesoDenegadoException("El metodo de pago pertenece a otro usuario.");
        }
    }
}
