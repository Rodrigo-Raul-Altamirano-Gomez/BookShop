package me.Model.entities.usuario;

import me.Model.entities.catalogo.Autor;

public class SeguimientoAutor extends Seguimiento {
    private Autor autor;

    public SeguimientoAutor() {
    }

    public SeguimientoAutor(int id, Usuario usuario, Autor autor) {
        super(id, usuario);
        this.autor = autor;
    }

    public Autor getAutor() {
        return autor;
    }

    public void setAutor(Autor autor) {
        this.autor = autor;
    }
}
