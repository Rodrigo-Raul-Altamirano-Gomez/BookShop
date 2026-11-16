using Microsoft.AspNetCore.Components;

namespace Web.Componentes.Paginas.Cuenta;

public partial class Solicitudes
{
    [Inject] private HttpClientUtils Api { get; set; } = default!;
    private FiltroSolicitudDTO consulta = new();
    private ResultadoPaginadoDTO<ResumenSolicitudDTO> resultado = new();
    private ResumenSolicitudDTO? seleccionada => devolucion?.Solicitud ?? reporte?.Solicitud;
    private DetalleDevolucionDTO? devolucion;
    private DetalleReporteResenaDTO? reporte;
    private DetallePedidoDTO? pedidoReferencia;
    private bool editando, verResena, procesando;
    private string? errorOperacion;
    private string rutaCargada = "";
    private string Ruta => Navegacion.ToBaseRelativePath(Navegacion.Uri).Split('?')[0];
    private bool EsModeracion => Ruta == "moderacion";
    private string Titulo => EsModeracion ? "Moderación" : "Mis solicitudes";
    private int TotalPaginas => Math.Max(1, (int)((resultado.TotalResultados + 9) / 10));
    private readonly string[] estados = ["PENDIENTE", "ACEPTADA", "RECHAZADA", "CERRADA"];

    protected override async Task CargarDatos()
    {
        rutaCargada = Ruta;
        resultado = new();
        CerrarDetalle();
        if (Sesion.Usuario is null || (EsModeracion && !Sesion.EsAdministrador))
            return;
        string ambito = EsModeracion ? "moderacion" : "propias";
        resultado = await Api.Post<ResultadoPaginadoDTO<ResumenSolicitudDTO>>(
            $"solicitudes/{ambito}/busqueda", consulta);
    }

    protected override async Task OnParametersSetAsync()
    {
        if (rutaCargada != Ruta)
        {
            consulta = new();
            await ActualizarDatos();
        }
    }

    private async Task MostrarTipo(string tipo)
    {
        consulta = new FiltroSolicitudDTO { Tipo = tipo };
        await ActualizarDatos();
    }

    private async Task CambiarEstado(string estado)
    {
        if (consulta.Estados.Contains(estado))
            consulta.Estados.Remove(estado);
        else
            consulta.Estados.Add(estado);
        consulta.Pagina = 1;
        await ActualizarDatos();
    }

    private async Task CambiarPagina(int pagina)
    {
        consulta.Pagina = pagina;
        await ActualizarDatos();
    }

    private async Task Consultar(ResumenSolicitudDTO solicitud)
    {
        CerrarDetalle();
        procesando = true;
        try
        {
            if (solicitud.Tipo == "DEVOLUCION")
            {
                devolucion = await Api.Get<DetalleDevolucionDTO>($"solicitudes/devoluciones/{solicitud.Id}");
            }
            else
            {
                reporte = await Api.Get<DetalleReporteResenaDTO>($"solicitudes/reportes/{solicitud.Id}");
            }
            errorOperacion = null;
        }
        catch (InvalidOperationException e)
        {
            Sesion.Avisar(e.Message);
        }
        procesando = false;
    }

    private async Task Editar(ResumenSolicitudDTO solicitud)
    {
        await Consultar(solicitud);
        editando = seleccionada?.Estado == "PENDIENTE";
    }

    private void CerrarDetalle()
    {
        devolucion = null;
        reporte = null;
        pedidoReferencia = null;
        editando = verResena = false;
        errorOperacion = null;
    }

    private async Task GuardarDevolucion(DatosDevolucionDTO datos)
    {
        await Api.Put($"solicitudes/devoluciones/{seleccionada!.Id}", datos);
        consulta.Pagina = 1;
        await ActualizarDatos();
    }

    private async Task GuardarReporte(DatosReporteResenaDTO datos)
    {
        await Api.Put($"solicitudes/reportes/{seleccionada!.Id}", datos);
        consulta.Pagina = 1;
        await ActualizarDatos();
    }

    private async Task Retirar(ResumenSolicitudDTO solicitud)
    {
        procesando = true;
        try
        {
            await Api.Delete($"solicitudes/{solicitud.Id}");
            consulta.Pagina = 1;
            await ActualizarDatos();
        }
        catch (InvalidOperationException e)
        {
            Sesion.Avisar(e.Message);
        }
        procesando = false;
    }

    private async Task VerPedido()
    {
        procesando = true;
        errorOperacion = null;
        try
        {
            pedidoReferencia = await Api.Get<DetallePedidoDTO>($"solicitudes/{seleccionada!.Id}/pedido");
        }
        catch (InvalidOperationException e)
        {
            errorOperacion = e.Message;
        }
        procesando = false;
    }

    private async Task Resolver(string estado)
    {
        procesando = true;
        errorOperacion = null;
        try
        {
            if (devolucion is not null)
            {
                var resuelta = await Api.Put<DetalleDevolucionDTO>(
                    $"solicitudes/devoluciones/{seleccionada!.Id}/resolucion?resultado={estado}");
                if (estado == "ACEPTADA")
                    Sesion.Avisar($"Devolución aceptada. Reembolso simulado: {Presentacion.Dinero(resuelta.Pedido.Total)}.");
                else
                    Sesion.Avisar("Devolución rechazada.");
            }
            else
            {
                await Api.Put<DetalleReporteResenaDTO>(
                    $"solicitudes/reportes/{seleccionada!.Id}/resolucion?resultado={estado}");
                Sesion.Avisar("Reporte resuelto.");
            }
            consulta.Pagina = 1;
            await ActualizarDatos();
        }
        catch (InvalidOperationException e)
        {
            errorOperacion = e.Message;
        }
        procesando = false;
    }

    private static string TextoEstado(string estado) => estado switch
    {
        "PENDIENTE" => "Pendiente",
        "ACEPTADA" => "Aceptada",
        "RECHAZADA" => "Rechazada",
        "CERRADA" => "Cerrada",
        _ => estado
    };
}
