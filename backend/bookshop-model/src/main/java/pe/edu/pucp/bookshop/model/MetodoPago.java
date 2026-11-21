package pe.edu.pucp.bookshop.model;

public class MetodoPago {

    private Integer id;
    private Usuario usuario;
    private TipoMetodoPago tipo;
    private String titular;
    private String numero;
    private String fechaVencimiento;   // MM/AA, solo tarjeta
    private String proveedor;          // Yape o Plin, solo billetera
    private Boolean predeterminado;
    private Boolean activo;

    public MetodoPago() {
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public TipoMetodoPago getTipo() { return tipo; }
    public void setTipo(TipoMetodoPago tipo) { this.tipo = tipo; }

    public String getTitular() { return titular; }
    public void setTitular(String titular) { this.titular = titular; }

    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }

    public String getFechaVencimiento() { return fechaVencimiento; }
    public void setFechaVencimiento(String fechaVencimiento) { this.fechaVencimiento = fechaVencimiento; }

    public String getProveedor() { return proveedor; }
    public void setProveedor(String proveedor) { this.proveedor = proveedor; }

    public Boolean getPredeterminado() { return predeterminado; }
    public void setPredeterminado(Boolean predeterminado) { this.predeterminado = predeterminado; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
}
