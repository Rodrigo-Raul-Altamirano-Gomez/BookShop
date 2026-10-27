package me.Model.entities.usuario;

import me.Model.entities.catalogo.Editorial;

public class SeguimientoEditorial extends Seguimiento {
    private Editorial editorial;

    public SeguimientoEditorial() {
    }

    public SeguimientoEditorial(int id, Usuario usuario, Editorial editorial) {
        super(id, usuario);
        this.editorial = editorial;
    }

    public Editorial getEditorial() {
        return editorial;
    }

    public void setEditorial(Editorial editorial) {
        this.editorial = editorial;
    }
}
