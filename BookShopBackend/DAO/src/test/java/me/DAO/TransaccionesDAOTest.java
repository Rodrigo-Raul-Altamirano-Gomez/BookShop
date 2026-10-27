package me.DAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import me.DAO.catalogo.AutorDAOImpl;
import me.DAO.catalogo.LibroDAOImpl;
import me.DAO.catalogo.VotoUtilDAOImpl;
import me.DAO.usuario.DireccionEnvioDAOImpl;
import me.DAO.usuario.UsuarioDAOImpl;
import me.DBManager.DBManager;
import me.DBManager.TransactionContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TransaccionesDAOTest {
    private Connection conexion;
    private PreparedStatement statement;
    private MockedStatic<DBManager> manager;

    @BeforeEach
    void prepararConexion() throws Exception {
        conexion = mock(Connection.class);
        statement = mock(PreparedStatement.class);
        when(conexion.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeUpdate()).thenReturn(1);
        manager = mockStatic(DBManager.class);
        manager.when(DBManager::getConnection).thenReturn(conexion);
    }

    @AfterEach
    void cerrarContexto() {
        TransactionContext.close();
        manager.close();
    }

    @Test
    void variosDaoUsanElContextoSinConfirmarloNiCerrarlo() throws Exception {
        TransactionContext.getConnection();
        assertTrue(new LibroDAOImpl().descontarStock(5, 2, "ana@test"));
        assertTrue(new UsuarioDAOImpl().incrementarPedidosEntregados(1, "ana@test"));
        verify(statement).setInt(4, 2);
        verify(statement, times(2)).close();
        verify(conexion, never()).close();
        verify(conexion, never()).commit();
        verify(conexion, never()).rollback();
        assertSame(conexion, TransactionContext.getConnection());
    }

    @Test
    void operacionIndependienteDevuelveElPrestamoAlPool() throws Exception {
        assertTrue(new LibroDAOImpl().cambiarImagen(5, null, "admin@admin.bookshop"));
        verify(statement).close();
        verify(conexion).close();
        verify(conexion, never()).setAutoCommit(false);
        manager.verify(DBManager::getConnection, times(1));
    }

    @Test
    void lecturaBloqueadaAbreYConservaLaTransaccion() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(statement.executeQuery()).thenReturn(rs);
        assertNull(new LibroDAOImpl().leerParaActualizar(1));
        verify(conexion).setAutoCommit(false);
        verify(conexion, never()).close();
        assertSame(conexion, TransactionContext.getConnection());
        manager.verify(DBManager::getConnection, times(1));
    }

    @Test
    void errorJdbcSePropagaYPermiteRevertirLaTransaccion() throws Exception {
        TransactionContext.getConnection();
        when(statement.executeUpdate()).thenThrow(new SQLException("Fallo simulado"));
        assertThrows(SQLException.class, () -> new LibroDAOImpl().descontarStock(1, 2, "ana@test"));
        verify(statement).close();
        verify(conexion, never()).close();
        verify(conexion, never()).commit();
        TransactionContext.rollback();
        verify(conexion).rollback();
    }

    @Test
    void errorFueraDeTransaccionTambienLiberaLaConexion() throws Exception {
        when(statement.executeUpdate()).thenThrow(new SQLException("Fallo simulado"));
        assertThrows(SQLException.class, () -> new LibroDAOImpl().cambiarImagen(1, null, "admin"));
        verify(statement).close();
        verify(conexion).close();
    }

    @Test
    void recalculoDeVetosIncluyeLasTresReferenciasYElVetoDirecto() throws Exception {
        TransactionContext.getConnection();
        ResultSet rs = mock(ResultSet.class);
        when(statement.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        assertTrue(new AutorDAOImpl().cambiarActivo(1, false, "admin"));
        var sql = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(conexion, times(2)).prepareStatement(sql.capture());
        String recalculo = sql.getAllValues().getLast();
        assertTrue(recalculo.contains("vetado_directamente"));
        assertTrue(recalculo.contains("a.eliminado"));
        assertTrue(recalculo.contains("c.eliminado"));
        assertTrue(recalculo.contains("e.eliminado"));
        verify(conexion, never()).commit();
        verify(conexion, never()).close();
    }
}
