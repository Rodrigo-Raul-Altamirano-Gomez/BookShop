package me.Model.entities.catalogo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Libro {

    private int id;
    private String portada;
    private String titulo;
    private Autor autor;
    private Editorial editorial;
    private Categoria categoria;
    private String idioma;
    private BigDecimal precioBase;
    private String sinopsis;
    private String proveedor;
    private String isbn;
    private int anioEdicion;
    private int numeroPaginas;
    private Double peso;
    private Double alto;
    private Double ancho;
    private String saga;
    private Integer numeroTomo;
    private int stockFisico;
    private int numeroVetos;
    private boolean vetadoDirectamente;
    private boolean eliminado;
    private String usuarioCreacion;
    private LocalDateTime fechaCreacion;
    private String usuarioActualizacion;
    private LocalDateTime fechaActualizacion;

    public Libro() {
        this.precioBase = BigDecimal.ZERO;
        this.fechaCreacion = LocalDateTime.now();
    }

    public Libro(int id, String titulo, Autor autor, Editorial editorial, Categoria categoria, BigDecimal
        precioBase, int stockFisico) {
        this();
        this.id = id;
        this.titulo = titulo;
        this.autor = autor;
        this.editorial = editorial;
        this.categoria = categoria;
        this.precioBase = precioBase;
        this.stockFisico = stockFisico;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPortada() {
        return portada;
    }

    public void setPortada(String portada) {
        this.portada = portada;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public Autor getAutor() {
        return autor;
    }

    public void setAutor(Autor autor) {
        this.autor = autor;
    }

    public Editorial getEditorial() {
        return editorial;
    }

    public void setEditorial(Editorial editorial) {
        this.editorial = editorial;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public void setCategoria(Categoria categoria) {
        this.categoria = categoria;
    }

    public String getIdioma() {
        return idioma;
    }

    public void setIdioma(String idioma) {
        this.idioma = idioma;
    }

    public BigDecimal getPrecioBase() {
        return precioBase;
    }

    public void setPrecioBase(BigDecimal precioBase) {
        this.precioBase = precioBase;
    }

    public String getSinopsis() {
        return sinopsis;
    }

    public void setSinopsis(String sinopsis) {
        this.sinopsis = sinopsis;
    }

    public String getProveedor() {
        return proveedor;
    }

    public void setProveedor(String proveedor) {
        this.proveedor = proveedor;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public int getAnioEdicion() {
        return anioEdicion;
    }

    public void setAnioEdicion(int anioEdicion) {
        this.anioEdicion = anioEdicion;
    }

    public int getNumeroPaginas() {
        return numeroPaginas;
    }

    public void setNumeroPaginas(int numeroPaginas) {
        this.numeroPaginas = numeroPaginas;
    }

    public Double getPeso() {
        return peso;
    }

    public void setPeso(Double peso) {
        this.peso = peso;
    }

    public Double getAlto() {
        return alto;
    }

    public void setAlto(Double alto) {
        this.alto = alto;
    }

    public Double getAncho() {
        return ancho;
    }

    public void setAncho(Double ancho) {
        this.ancho = ancho;
    }

    public String getSaga() {
        return saga;
    }

    public void setSaga(String saga) {
        this.saga = saga;
    }

    public Integer getNumeroTomo() {
        return numeroTomo;
    }

    public void setNumeroTomo(Integer numeroTomo) {
        this.numeroTomo = numeroTomo;
    }

    public int getStockFisico() {
        return stockFisico;
    }

    public void setStockFisico(int stockFisico) {
        this.stockFisico = stockFisico;
    }

    public int getNumeroVetos() {
        return numeroVetos;
    }

    public void setNumeroVetos(int numeroVetos) {
        this.numeroVetos = numeroVetos;
    }

    public boolean isVetadoDirectamente() {
        return vetadoDirectamente;
    }

    public void setVetadoDirectamente(boolean vetadoDirectamente) {
        this.vetadoDirectamente = vetadoDirectamente;
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

    public boolean isActivo() {
        return !eliminado && !vetadoDirectamente && numeroVetos == 0;
    }

    public void setActivo(boolean activo) {
        vetadoDirectamente = !activo;
    }

}
