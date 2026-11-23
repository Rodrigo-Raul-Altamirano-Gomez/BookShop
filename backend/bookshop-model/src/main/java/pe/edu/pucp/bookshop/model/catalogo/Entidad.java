package pe.edu.pucp.bookshop.model.catalogo;

// Autor, categoría o editorial: tienen los mismos datos y se distinguen por el tipo
public class Entidad {

    private Integer id;
    private TipoEntidad tipo;
    private String nombre;
    private String descripcion;   // biografía en el caso del autor
    private String imagen;
    private Boolean activo;

    public Entidad() {
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public TipoEntidad getTipo() { return tipo; }
    public void setTipo(TipoEntidad tipo) { this.tipo = tipo; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getImagen() { return imagen; }
    public void setImagen(String imagen) { this.imagen = imagen; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
}
