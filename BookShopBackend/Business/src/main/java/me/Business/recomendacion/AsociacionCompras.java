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
        for (VentaLibroDTO venta : ventas)
            pedidos.computeIfAbsent(venta.idPedido(), id -> new HashSet<>()).add(venta.idLibro());
        Map<Integer, Integer> frecuencias = new HashMap<>();
        for (Set<Integer> libros : pedidos.values())
            for (int id : libros) frecuencias.merge(id, 1, Integer::sum);
        Map<Integer, Double> puntos = new HashMap<>();
        for (int origen : carrito) {
            int cantidadOrigen = frecuencias.getOrDefault(origen, 0);
            if (cantidadOrigen == 0) continue;
            Map<Integer, Integer> conjuntos = new HashMap<>();
            for (Set<Integer> libros : pedidos.values()) {
                if (!libros.contains(origen)) continue;
                for (int candidato : libros)
                    if (!carrito.contains(candidato)) conjuntos.merge(candidato, 1, Integer::sum);
            }
            for (Map.Entry<Integer, Integer> par : conjuntos.entrySet()) {
                double confianza = (double) par.getValue() / cantidadOrigen;
                double lift = confianza / ((double) frecuencias.get(par.getKey()) / pedidos.size());
                if (lift >= 1.0) puntos.merge(par.getKey(), confianza * lift, Double::sum);
            }
        }
        return puntos;
    }
}
