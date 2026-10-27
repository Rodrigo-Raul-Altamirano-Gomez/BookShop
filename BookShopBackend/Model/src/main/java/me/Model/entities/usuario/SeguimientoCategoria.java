package me.Model.entities.usuario;

import me.Model.entities.catalogo.Categoria;

public class SeguimientoCategoria extends Seguimiento {
    private Categoria categoria;

    public SeguimientoCategoria() {
    }

    public SeguimientoCategoria(int id, Usuario usuario, Categoria categoria) {
        super(id, usuario);
        this.categoria = categoria;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public void setCategoria(Categoria categoria) {
        this.categoria = categoria;
    }
}
