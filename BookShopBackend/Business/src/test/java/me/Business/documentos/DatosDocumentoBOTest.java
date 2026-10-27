package me.Business.documentos;

import java.sql.SQLException;
import java.time.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import me.Business.compra.PedidoBO;
import me.Business.exceptions.*;
import me.DAO.catalogo.LibroDAO;
import me.DAO.usuario.UsuarioDAO;
import me.Model.dto.catalogo.ConsultaReporteLibrosDTO;
import me.Model.entities.usuario.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DatosDocumentoBOTest {
    final Clock reloj = Clock.fixed(Instant.parse("2026-10-07T04:30:00Z"), ZoneOffset.UTC);
    Usuario usuario(int id, Rol rol) {
        Usuario u = new Usuario(); u.setId(id); u.setNombre("Ana"); u.setEmail("ana@mail.com"); u.setRol(rol); return u;
    }
    @Test void reporteMensualIncluyeMesCompletoYLimaSinGenerarDocumento() throws Exception {
        LibroDAO libros = mock(LibroDAO.class); UsuarioDAO usuarios = mock(UsuarioDAO.class);
        when(usuarios.leer(1)).thenReturn(usuario(1, Rol.ADMIN));
        when(usuarios.listarActividadUsuarios(LocalDate.of(2024,2,1), LocalDate.of(2024,2,29), null)).thenReturn(List.of());
        var dto = new ReporteBOImpl(libros, usuarios, reloj).prepararUsuarios(1, YearMonth.of(2024,2), null);
        assertEquals(LocalDateTime.of(2026,10,6,23,30), dto.fechaGeneracion());
        assertEquals(0, dto.cantidadResultados());
        verify(usuarios).listarActividadUsuarios(LocalDate.of(2024,2,1), LocalDate.of(2024,2,29), null);
    }
    @Test void validaPermisosYMaximoAntesDeConsultarDatos() throws Exception {
        LibroDAO libros = mock(LibroDAO.class); UsuarioDAO usuarios = mock(UsuarioDAO.class);
        when(usuarios.leer(1)).thenReturn(usuario(1, Rol.CLIENTE));
        var bo = new ReporteBOImpl(libros, usuarios, reloj);
        var consulta = new ConsultaReporteLibrosDTO(LocalDate.of(2026,1,1), LocalDate.of(2026,1,31), 0);
        assertThrows(AccesoDenegadoException.class, () -> bo.prepararLibros(1, consulta));
        verifyNoInteractions(libros);
        when(usuarios.leer(1)).thenReturn(usuario(1, Rol.ADMIN));
        assertThrows(ValidacionException.class, () -> bo.prepararLibros(1, consulta));
        verifyNoInteractions(libros);
    }
    @Test void errorSqlSePropagaComoExcepcionDeNegocio() throws Exception {
        LibroDAO libros = mock(LibroDAO.class); UsuarioDAO usuarios = mock(UsuarioDAO.class);
        when(usuarios.leer(1)).thenReturn(usuario(1, Rol.ADMIN));
        SQLException causa = new SQLException("Falla interna");
        var consulta = new ConsultaReporteLibrosDTO(LocalDate.of(2026,1,1), LocalDate.of(2026,1,31), 10);
        when(libros.listarVentasLibros(consulta)).thenThrow(causa);
        var error = assertThrows(PersistenciaException.class, () -> new ReporteBOImpl(libros, usuarios, reloj).prepararLibros(1, consulta));
        assertSame(causa, error.getCause());
        assertFalse(error.getMessage().contains("Falla interna"));
    }
    @Test void comprobanteSoloPreparaIdentidadSinFotoYConsultaPropia() throws Exception {
        PedidoBO pedidos = mock(PedidoBO.class); UsuarioDAO usuarios = mock(UsuarioDAO.class);
        when(usuarios.leer(1)).thenReturn(usuario(1, Rol.CLIENTE));
        var dto = new ComprobanteBOImpl(pedidos, usuarios, reloj).preparar(1, 40);
        verify(pedidos).consultar(1, 40);
        assertEquals("Ana", dto.comprador().nombre());
        assertNull(dto.comprador().imagen());
        assertEquals(LocalDateTime.of(2026,10,6,23,30), dto.fechaGeneracion());
    }
}
