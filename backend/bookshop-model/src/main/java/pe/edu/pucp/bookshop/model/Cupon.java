package pe.edu.pucp.bookshop.model;

import java.time.LocalDate;

public class Cupon {

    private Integer id;
    private Usuario usuario;
    private String codigo;
    private Integer porcentaje;
    private LocalDate fechaEmision;
    private LocalDate fechaVencimiento;
    private Boolean activo;        // se desactiva al usarse en una compra
    private Boolean utilizable;    // lo calcula la lógica de negocio (ERS §2.2)
    private Pedido pedido;         // pedido en que se usó (solo el id); null si no se usó

    public Cupon() {
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public Integer getPorcentaje() { return porcentaje; }
    public void setPorcentaje(Integer porcentaje) { this.porcentaje = porcentaje; }

    public LocalDate getFechaEmision() { return fechaEmision; }
    public void setFechaEmision(LocalDate fechaEmision) { this.fechaEmision = fechaEmision; }

    public LocalDate getFechaVencimiento() { return fechaVencimiento; }
    public void setFechaVencimiento(LocalDate fechaVencimiento) { this.fechaVencimiento = fechaVencimiento; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }

    public Boolean getUtilizable() { return utilizable; }
    public void setUtilizable(Boolean utilizable) { this.utilizable = utilizable; }

    public Pedido getPedido() { return pedido; }
    public void setPedido(Pedido pedido) { this.pedido = pedido; }
}
