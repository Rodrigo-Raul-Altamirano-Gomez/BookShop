using Microsoft.AspNetCore.Components;
using Web.Demostracion;
using Web.Modelos;

namespace Web.Componentes.Paginas.Admin;

public partial class Administracion : IDisposable
{
    [Inject]
    private EstadoDemostracion Estado { get; set; } = null!;

    private bool ConfiguracionAbierta, ReportesAbiertos, VistaReporte;
    private string ErrorFormulario = "", PestanaReporte = "Libros";
    private ConfiguracionVisual Configuracion = new(), BorradorConfiguracion = new();
    private DateTime InicioReporte = new(2026, 10, 1), FinReporte = new(2026, 10, 7);
    private int MesReporte = 10, AnioReporte = 2026;
    private int? LimiteReporte;
    private readonly string[] NombresMeses = ["Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
         "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"];
    private IEnumerable<Libro> LibrosReporte => Estado.Libros.OrderByDescending(b => b.Precio).Take(
        LimiteReporte ?? Estado.Libros.Count);

    private readonly (string Nombre, string Correo, string Activo, string Gasto, string Tiempo,
         string Categoria, string Autor, string Editorial)[] UsuariosEjemplo = [("María López",
              "maria@correo.pe", "Sí", "S/ 285.70", "2 h 35 min", "Literatura", "Gabriel García Márquez",
                   "Debolsillo"), ("Ana Torres", "ana@correo.pe", "Sí", "S/ 124.90", "1 h 10 min",
                        "Ciencia ficción", "Frank Herbert", "Debolsillo"), ("Carlos Ruiz",
                         "carlos@correo.pe", "No", "S/ 0.00", "20 min", "Sin compras", "Sin compras",
                         "Sin compras")];
    private IEnumerable<(string Nombre, string Correo, string Activo, string Gasto, string Tiempo,
         string Categoria, string Autor, string Editorial)> UsuariosReporte => UsuariosEjemplo.Take(
             LimiteReporte ?? UsuariosEjemplo.Length);

    protected override void OnInitialized()
    {
        if (Estado.EstadoVistas.TryGetValue("administracion:configuracion", out var Guardado)
            && Guardado is ConfiguracionVisual Valores)
            Configuracion = Valores.Copiar();
        Estado.Cambiado += ActualizarVista;
    }

    private void ActualizarVista() => _ = InvokeAsync(StateHasChanged);
    public void Dispose() => Estado.Cambiado -= ActualizarVista;
    private void AbrirConfiguracion()
    {
        BorradorConfiguracion = Configuracion.Copiar();
        ErrorFormulario = "";
        ConfiguracionAbierta = true;
    }

    private void CerrarConfiguracion() => ConfiguracionAbierta = false;
    private void GuardarConfiguracion()
    {
        if (BorradorConfiguracion.PorcentajeEnvio < 0 || BorradorConfiguracion.PorcentajeEnvio > 100
            || BorradorConfiguracion.PorcentajeCupon <= 0 || BorradorConfiguracion.PorcentajeCupon > 100
                || BorradorConfiguracion.DiasDevolucion < 1 || BorradorConfiguracion
                    .IntentosDevolucion < 1 || BorradorConfiguracion.PedidosEntregados < 1
                        || BorradorConfiguracion.DiasCupon < 1 || BorradorConfiguracion.MontoMinimoCupon < 0)
        {
            ErrorFormulario = "Introduce porcentajes y cantidades válidos en todos los campos.";
            return;
        }

        Configuracion = BorradorConfiguracion.Copiar();
        Estado.EstadoVistas["administracion:configuracion"] = Configuracion.Copiar();
        ConfiguracionAbierta = false;
        Estado.Avisar("Configuración guardada");
    }

    private void AbrirReportes()
    {
        ReportesAbiertos = true;
        VistaReporte = false;
        ErrorFormulario = "";
    }

    private void CerrarReportes() => ReportesAbiertos = false;
    private void ElegirLibros()
    {
        PestanaReporte = "Libros";
        ErrorFormulario = "";
    }

    private void ElegirUsuarios()
    {
        PestanaReporte = "Usuarios";
        ErrorFormulario = "";
    }

    private void GenerarReporte()
    {
        if (LimiteReporte is <= 0 || (PestanaReporte == "Libros" && FinReporte < InicioReporte)
            || AnioReporte < 1)
        {
            ErrorFormulario = "Revisa el período y la cantidad máxima.";
            return;
        }

        ReportesAbiertos = false;
        VistaReporte = true;
    }

    private void CerrarVistaReporte() => VistaReporte = false;
    private class ConfiguracionVisual
    {
        public decimal PorcentajeEnvio { get; set; } = 5;
        public int DiasDevolucion { get; set; } = 30;
        public int IntentosDevolucion { get; set; } = 3;
        public int PedidosEntregados { get; set; } = 5;
        public decimal PorcentajeCupon { get; set; } = 10;
        public decimal MontoMinimoCupon { get; set; } = 100;
        public int DiasCupon { get; set; } = 30;

        public ConfiguracionVisual Copiar() => (ConfiguracionVisual)MemberwiseClone();
    }
}

