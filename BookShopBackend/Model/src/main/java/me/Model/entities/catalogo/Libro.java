package me.Model.entities.catalogo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Libro {
    private int id;
    private byte[] portada;
    private String tipoPortada;
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

    public Libro(int id, String titulo, Autor autor, Editorial editorial, Categoria categoria,
                 BigDecimal precioBase, int stockFisico) {
        this();
        this.id = id;
        this.titulo = titulo;
        this.autor = autor;
        this.editorial = editorial;
        this.categoria = categoria;
        this.precioBase = precioBase;
        this.stockFisico = stockFisico;
        recalcularVetos();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public byte[] getPortada() {
        return portada == null ? null : portada.clone();
    }

    public void setPortada(byte[] portada) {
        this.portada = portada == null ? null : portada.clone();
    }

    public String getTipoPortada() {
        return tipoPortada;
    }

    public void setTipoPortada(String tipoPortada) {
        this.tipoPortada = tipoPortada;
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
        if (this.autor != null && (autor == null || this.autor.getId() != autor.getId())) {
            throw new IllegalStateException("No se puede cambiar el autor del libro.");
        }
        this.autor = autor;
    }

    public Editorial getEditorial() {
        return editorial;
    }

    public void setEditorial(Editorial editorial) {
        if (this.editorial != null && (editorial == null || this.editorial.getId() != editorial.getId())) {
            throw new IllegalStateException("No se puede cambiar la editorial del libro.");
        }
        this.editorial = editorial;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public void setCategoria(Categoria categoria) {
        if (this.categoria != null && (categoria == null || this.categoria.getId() != categoria.getId())) {
            throw new IllegalStateException("No se puede cambiar la categoria del libro.");
        }
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
        if (stockFisico < 0) {
            throw new IllegalArgumentException("El stock no puede ser negativo.");
        }
        this.stockFisico = stockFisico;
    }

    public int getNumeroVetos() {
        return numeroVetos;
    }

    public void setNumeroVetos(int numeroVetos) {
        if (numeroVetos < 0 || numeroVetos > 4) {
            throw new IllegalArgumentException("El numero de vetos debe estar entre cero y cuatro.");
        }
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
        if (activo) {
            retirarVetoDirecto();
        } else {
            aplicarVetoDirecto();
        }
    }

    public void aplicarVetoDirecto() {
        if (vetadoDirectamente) return;
        if (numeroVetos >= 4) {
            throw new IllegalStateException("El contador de vetos es inconsistente.");
        }
        vetadoDirectamente = true;
        numeroVetos++;
    }

    public void retirarVetoDirecto() {
        if (!vetadoDirectamente) return;
        if (numeroVetos == 0) {
            throw new IllegalStateException("El contador de vetos es inconsistente.");
        }
        vetadoDirectamente = false;
        numeroVetos--;
    }

    public void recalcularVetos() {
        numeroVetos = vetadoDirectamente ? 1 : 0;
        if (autor != null && !autor.estaDisponible()) numeroVetos++;
        if (editorial != null && !editorial.estaDisponible()) numeroVetos++;
        if (categoria != null && !categoria.estaDisponible()) numeroVetos++;
    }

    public boolean tieneStockSuficiente(int cantidad) {
        return isActivo() && cantidad > 0 && stockFisico >= cantidad;
    }

    public void descontarStock(int cantidad) {
        if (!tieneStockSuficiente(cantidad)) {
            throw new IllegalArgumentException("El libro no dispone del stock solicitado.");
        }
        stockFisico -= cantidad;
    }

    public void reponerStock(int cantidad) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser positiva.");
        }
        stockFisico = Math.addExact(stockFisico, cantidad);
    }
}
