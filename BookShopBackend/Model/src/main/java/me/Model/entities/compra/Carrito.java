package me.Model.entities.compra;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import me.Model.entities.catalogo.Libro;
import me.Model.entities.usuario.Usuario;

public class Carrito {
    private int id;
    private Usuario usuario;
    private List<Item> items;

    public Carrito() {
        this.items = new ArrayList<>();
    }

    public Carrito(Usuario usuario, List<Item> items) {
        this(0, usuario, items);
    }

    public Carrito(int id, Usuario usuario, List<Item> items) {
        this();
        this.id = id;
        this.usuario = usuario;
        this.items = new ArrayList<>(items);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public List<Item> getItems() {
        return new ArrayList<>(items);
    }

    public void setItems(List<Item> items) {
        this.items = new ArrayList<>(items);
    }

    public BigDecimal getTotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (Item item : items) {
            if (!item.isEliminado()) total = total.add(item.getSubTotal());
        }
        return total;
    }

    public int getCantidad() {
        int cantidad = 0;
        for (Item item : items) {
            if (!item.isEliminado()) cantidad = Math.addExact(cantidad, item.getCantidad());
        }
        return cantidad;
    }

    public void agregarItem(Libro libro, int cantidad) {
        if (cantidad <= 0) return;
        for (Item item : items) {
            if (item.getLibro().getId() == libro.getId()) {
                if (item.isEliminado()) {
                    item.setCantidad(cantidad);
                    item.setEliminado(false);
                } else {
                    item.setCantidad(Math.addExact(item.getCantidad(), cantidad));
                }
                return;
            }
        }
        items.add(new Item(0, libro, cantidad));
    }

    public void modificarCantidad(int idLibro, int nuevaCantidad) {
        if (nuevaCantidad <= 0) {
            eliminarItem(idLibro);
            return;
        }
        for (Item item : items) {
            if (item.getLibro().getId() == idLibro && !item.isEliminado()) {
                item.setCantidad(nuevaCantidad);
                return;
            }
        }
    }

    public void eliminarItem(int idLibro) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getLibro().getId() == idLibro) {
                items.remove(i);
                return;
            }
        }
    }

    public void vaciar() {
        items.clear();
    }
}
