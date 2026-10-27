package me.Business.recomendacion;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import me.Model.dto.compra.VentaLibroDTO;

final class FiltradoColaborativo {
    private FiltradoColaborativo() { }
    static Set<Integer> pedidosUsuario(int idUsuario, List<VentaLibroDTO> ventas) {
        Set<Integer> pedidos = new HashSet<>();
        for (var venta : ventas) if (venta.idUsuario() == idUsuario) pedidos.add(venta.idPedido());
        return pedidos;
    }
    static Set<Integer> librosUsuario(int idUsuario, List<VentaLibroDTO> ventas) {
        Set<Integer> libros = new HashSet<>();
        for (var venta : ventas) if (venta.idUsuario() == idUsuario) libros.add(venta.idLibro());
        return libros;
    }
    static Map<Integer, Double> puntuar(int idUsuario, List<VentaLibroDTO> ventas) {
        Map<Integer, Set<Integer>> porUsuario = new HashMap<>();
        for (var venta : ventas)
            porUsuario.computeIfAbsent(venta.idUsuario(), id -> new HashSet<>()).add(venta.idLibro());
        Set<Integer> propios = porUsuario.getOrDefault(idUsuario, Set.of());
        Map<Integer, Double> puntos = new HashMap<>();
        for (var entrada : porUsuario.entrySet()) {
            if (entrada.getKey() == idUsuario) continue;
            Set<Integer> comunes = new HashSet<>(entrada.getValue());
            comunes.retainAll(propios);
            Set<Integer> union = new HashSet<>(entrada.getValue());
            union.addAll(propios);
            if (comunes.isEmpty()) continue;
            double jaccard = (double) comunes.size() / union.size();
            for (int candidato : entrada.getValue())
                if (!propios.contains(candidato)) puntos.merge(candidato, jaccard, Double::sum);
        }
        return puntos;
    }
}
