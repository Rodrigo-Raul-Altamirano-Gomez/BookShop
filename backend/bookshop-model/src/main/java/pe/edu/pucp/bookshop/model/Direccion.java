package pe.edu.pucp.bookshop.model;

public class Direccion {

    private Integer id;
    private Usuario usuario;
    private String alias;
    private String direccion;
    private String distrito;
    private String ciudad;
    private Boolean predeterminada;
    private Boolean activa;

    public Direccion() {
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public String getAlias() { return alias; }
    public void setAlias(String alias) { this.alias = alias; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public String getDistrito() { return distrito; }
    public void setDistrito(String distrito) { this.distrito = distrito; }

    public String getCiudad() { return ciudad; }
    public void setCiudad(String ciudad) { this.ciudad = ciudad; }

    public Boolean getPredeterminada() { return predeterminada; }
    public void setPredeterminada(Boolean predeterminada) { this.predeterminada = predeterminada; }

    public Boolean getActiva() { return activa; }
    public void setActiva(Boolean activa) { this.activa = activa; }
}
