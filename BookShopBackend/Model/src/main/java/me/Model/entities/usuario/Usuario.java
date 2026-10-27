package me.Model.entities.usuario;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;

public class Usuario {
    private int id;
    private String email;
    private String contrasenia;
    private String nombre;
    private byte[] imagen;
    private String tipoImagen;
    private LocalDateTime fechaNacimiento;
    private boolean activo;
    private Rol rol;
    private Duration tiempoTotal;
    private int cantidadPedidosEntregados;
    private boolean eliminado;
    private String usuarioCreacion;
    private LocalDateTime fechaCreacion;
    private String usuarioActualizacion;
    private LocalDateTime fechaActualizacion;

    public Usuario() {
        this.activo = true;
        this.rol = Rol.CLIENTE;
        this.tiempoTotal = Duration.ZERO;
        this.fechaCreacion = LocalDateTime.now();
    }

    public Usuario(int id, String email, String contrasenia, String nombre,
                   LocalDateTime fechaNacimiento, Rol rol) {
        this();
        this.id = id;
        this.email = email;
        this.contrasenia = contrasenia;
        this.nombre = nombre;
        this.fechaNacimiento = fechaNacimiento;
        this.rol = rol;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getContrasenia() {
        return contrasenia;
    }

    public void setContrasenia(String contrasenia) {
        this.contrasenia = contrasenia;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public byte[] getImagen() {
        return imagen == null ? null : imagen.clone();
    }

    public void setImagen(byte[] imagen) {
        this.imagen = imagen == null ? null : imagen.clone();
    }

    public String getTipoImagen() {
        return tipoImagen;
    }

    public void setTipoImagen(String tipoImagen) {
        this.tipoImagen = tipoImagen;
    }

    public LocalDateTime getFechaNacimiento() {
        return fechaNacimiento;
    }

    public void setFechaNacimiento(LocalDateTime fechaNacimiento) {
        this.fechaNacimiento = fechaNacimiento;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public Rol getRol() {
        return rol;
    }

    public void setRol(Rol rol) {
        this.rol = rol;
    }

    public Duration getTiempoTotal() {
        return tiempoTotal;
    }

    public void setTiempoTotal(Duration tiempoTotal) {
        this.tiempoTotal = tiempoTotal;
    }

    public int getCantidadPedidosEntregados() {
        return cantidadPedidosEntregados;
    }

    public void setCantidadPedidosEntregados(int cantidadPedidosEntregados) {
        if (cantidadPedidosEntregados < 0) {
            throw new IllegalArgumentException("El contador de pedidos no puede ser negativo.");
        }
        this.cantidadPedidosEntregados = cantidadPedidosEntregados;
    }

    public boolean isEliminado() {
        return eliminado;
    }

    public void setEliminado(boolean eliminado) {
        this.eliminado = eliminado;
    }

    public String getUsuarioCreacion() {
        return usuarioCreacion;
    }

    public void setUsuarioCreacion(String usuarioCreacion) {
        this.usuarioCreacion = usuarioCreacion;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public String getUsuarioActualizacion() {
        return usuarioActualizacion;
    }

    public void setUsuarioActualizacion(String usuarioActualizacion) {
        this.usuarioActualizacion = usuarioActualizacion;
    }

    public LocalDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }

    public void setFechaActualizacion(LocalDateTime fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }

    public boolean estaDisponible() {
        return activo && !eliminado;
    }

    public boolean tieneCorreoAdministrador() {
        if (email == null) return false;
        String correo = email.trim().toLowerCase(Locale.ROOT);
        int separador = correo.indexOf('@');
        return separador > 0 && separador == correo.lastIndexOf('@')
                && correo.substring(separador + 1).equals("admin.bookshop");
    }

    public void asignarRolPorCorreo() {
        rol = tieneCorreoAdministrador() ? Rol.ADMIN : Rol.CLIENTE;
    }

    public void registrarPedidoEntregado() {
        cantidadPedidosEntregados = Math.incrementExact(cantidadPedidosEntregados);
    }
}
