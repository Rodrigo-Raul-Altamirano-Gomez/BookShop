package pe.edu.pucp.bookshop.dao.biblioteca.impl;

import pe.edu.pucp.bookshop.dao.biblioteca.ListaLibroDAO;
import pe.edu.pucp.bookshop.dbmanager.DBManager;
import pe.edu.pucp.bookshop.model.biblioteca.TipoLista;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ListaLibroDAOImpl implements ListaLibroDAO {

    @Override
    public List<Integer> listarLibros(int idUsuario, TipoLista lista) throws SQLException {
        String sql = "select fid_libro from lista_libro where fid_usuario = ? and lista = ?";
        List<Integer> libros = new ArrayList<>();
        try (Connection con = DBManager.getInstance().getConnection();
             PreparedStatement pstm = con.prepareStatement(sql)) {
            pstm.setInt(1, idUsuario);
            pstm.setString(2, lista.name());
            try (ResultSet rs = pstm.executeQuery()) {
                while (rs.next()) {
                    libros.add(rs.getInt("fid_libro"));
                }
            }
        }
        return libros;
    }

    @Override
    public List<TipoLista> listarListas(int idUsuario, int idLibro) throws SQLException {
        String sql = "select lista from lista_libro where fid_usuario = ? and fid_libro = ?";
        List<TipoLista> listas = new ArrayList<>();
        try (Connection con = DBManager.getInstance().getConnection();
             PreparedStatement pstm = con.prepareStatement(sql)) {
            pstm.setInt(1, idUsuario);
            pstm.setInt(2, idLibro);
            try (ResultSet rs = pstm.executeQuery()) {
                while (rs.next()) {
                    listas.add(TipoLista.valueOf(rs.getString("lista")));
                }
            }
        }
        return listas;
    }

    @Override
    public void insertar(int idUsuario, int idLibro, TipoLista lista) throws SQLException {
        String sql = "insert into lista_libro (fid_usuario, fid_libro, lista) values (?, ?, ?)";
        try (Connection con = DBManager.getInstance().getConnection();
             PreparedStatement pstm = con.prepareStatement(sql)) {
            pstm.setInt(1, idUsuario);
            pstm.setInt(2, idLibro);
            pstm.setString(3, lista.name());
            pstm.executeUpdate();
        }
    }

    @Override
    public void eliminar(int idUsuario, int idLibro, TipoLista lista) throws SQLException {
        String sql = "delete from lista_libro where fid_usuario = ? and fid_libro = ? and lista = ?";
        try (Connection con = DBManager.getInstance().getConnection();
             PreparedStatement pstm = con.prepareStatement(sql)) {
            pstm.setInt(1, idUsuario);
            pstm.setInt(2, idLibro);
            pstm.setString(3, lista.name());
            pstm.executeUpdate();
        }
    }
}
