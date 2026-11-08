package me.Business.recomendacion;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import me.Model.dto.compra.VentaLibroDTO;

final class AsociacionCompras {
    private AsociacionCompras() { }
    static Map<Integer, Double> puntuar(Set<Integer> carrito, List<VentaLibroDTO> ventas) {
        Map<Integer, Set<Integer>> pedidos = new HashMap<>();
        for (VentaLibroDTO venta : ventas) {
            if (!pedidos.containsKey(venta.idPedido())) pedidos.put(venta.idPedido(), new HashSet<>());
            pedidos.get(venta.idPedido()).add(venta.idLibro());
        }
        Map<Integer, Integer> frecuencias = new HashMap<>();
        for (Set<Integer> libros : pedidos.values())
            for (int id : libros) frecuencias.put(id, frecuencias.getOrDefault(id, 0) + 1);
        Map<Integer, Double> puntos = new HashMap<>();
        for (int origen : carrito) {
            int cantidadOrigen = frecuencias.getOrDefault(origen, 0);
            if (cantidadOrigen == 0) continue;
            Map<Integer, Integer> conjuntos = new HashMap<>();
            for (Set<Integer> libros : pedidos.values()) {
                if (!libros.contains(origen)) continue;
                for (int candidato : libros)
                    if (!carrito.contains(candidato)) conjuntos.put(candidato, conjuntos.getOrDefault(candidato, 0) + 1);
            }
            for (Map.Entry<Integer, Integer> par : conjuntos.entrySet()) {
                double confianza = (double) par.getValue() / cantidadOrigen;
                double lift = confianza / ((double) frecuencias.get(par.getKey()) / pedidos.size());
                if (lift >= 1.0) puntos.put(par.getKey(), puntos.getOrDefault(par.getKey(), 0.0) + confianza * lift);
            }
        }
        return puntos;
    }
}
