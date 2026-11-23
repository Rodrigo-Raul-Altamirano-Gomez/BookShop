package pe.edu.pucp.bookshop.model.catalogo;

import java.time.LocalDateTime;

public class Libro {

    private Integer id;
    private String isbn;
    private String titulo;
    private String sinopsis;
    private String idioma;
    private Integer paginas;
    private Integer anioPublicacion;
    private Double precio;
    private Integer stock;
    private String portada;
    private Boolean activo;
    private LocalDateTime fechaRegistro;
    private Entidad autor;
    private Entidad categoria;
    private Entidad editorial;
    private Descuento descuentoVigente;   // null si hoy no tiene descuento
    private Double puntuacionPromedio;    // de las reseñas visibles
    private Integer cantidadResenas;

    public Libro() {
    }

    // ERS §2.3: precio × (1 − porcentaje del descuento vigente / 100)
    public Double getPrecioFinal() {
        if (precio == null) {
            return null;
        }
        if (descuentoVigente == null) {
            return precio;
        }
        double precioFinal = precio * (1 - descuentoVigente.getPorcentaje() / 100.0);
        return Math.round(precioFinal * 100) / 100.0;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getSinopsis() { return sinopsis; }
    public void setSinopsis(String sinopsis) { this.sinopsis = sinopsis; }

    public String getIdioma() { return idioma; }
    public void setIdioma(String idioma) { this.idioma = idioma; }

    public Integer getPaginas() { return paginas; }
    public void setPaginas(Integer paginas) { this.paginas = paginas; }

    public Integer getAnioPublicacion() { return anioPublicacion; }
    public void setAnioPublicacion(Integer anioPublicacion) { this.anioPublicacion = anioPublicacion; }

    public Double getPrecio() { return precio; }
    public void setPrecio(Double precio) { this.precio = precio; }

    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }

    public String getPortada() { return portada; }
    public void setPortada(String portada) { this.portada = portada; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }

    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }

    public Entidad getAutor() { return autor; }
    public void setAutor(Entidad autor) { this.autor = autor; }

    public Entidad getCategoria() { return categoria; }
    public void setCategoria(Entidad categoria) { this.categoria = categoria; }

    public Entidad getEditorial() { return editorial; }
    public void setEditorial(Entidad editorial) { this.editorial = editorial; }

    public Descuento getDescuentoVigente() { return descuentoVigente; }
    public void setDescuentoVigente(Descuento descuentoVigente) { this.descuentoVigente = descuentoVigente; }

    public Double getPuntuacionPromedio() { return puntuacionPromedio; }
    public void setPuntuacionPromedio(Double puntuacionPromedio) { this.puntuacionPromedio = puntuacionPromedio; }

    public Integer getCantidadResenas() { return cantidadResenas; }
    public void setCantidadResenas(Integer cantidadResenas) { this.cantidadResenas = cantidadResenas; }
}
