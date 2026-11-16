namespace Web.Servicios;

public class CuentaServicio
{
    private readonly HttpClientUtils api;
    public List<DireccionEnvioDTO> Direcciones { get; } = [];
    public List<MetodoPagoDTO> Metodos { get; } = [];
    public List<CuponDTO> Cupones { get; } = [];

    public CuentaServicio(HttpClientUtils api, SesionUsuario sesion)
    {
        this.api = api;
        sesion.Cerrada += Limpiar;
    }

    private void Limpiar()
    {
        Direcciones.Clear();
        Metodos.Clear();
        Cupones.Clear();
    }

    public async Task CargarDatosCompra()
    {
        await CargarDirecciones();
        await CargarMetodos();
        await CargarCupones();
    }

    public async Task CargarDirecciones()
    {
        var datos = await api.Get<ResultadoPaginadoDTO<DireccionEnvioDTO>>(
            $"cuenta/direcciones?pagina=1&tamanioPagina={int.MaxValue}");
        Direcciones.Clear();
        Direcciones.AddRange(datos.Items);
    }

    public async Task CargarMetodos()
    {
        var datos = await api.Get<ResultadoPaginadoDTO<MetodoPagoDTO>>(
            $"cuenta/metodos?pagina=1&tamanioPagina={int.MaxValue}");
        Metodos.Clear();
        Metodos.AddRange(datos.Items);
    }

    public async Task CargarCupones()
    {
        var datos = await api.Get<ResultadoPaginadoDTO<CuponDTO>>(
            $"cuenta/cupones?pagina=1&tamanioPagina={int.MaxValue}");
        Cupones.Clear();
        Cupones.AddRange(datos.Items);
    }

    public async Task<int> GuardarDireccion(DatosDireccionEnvioDTO datos, int id = 0)
    {
        var guardada = id == 0
            ? await api.Post<DireccionEnvioDTO>("cuenta/direcciones", datos)
            : await api.Put<DireccionEnvioDTO>($"cuenta/direcciones/{id}", datos);
        await CargarDirecciones();
        return guardada.Id;
    }

    public async Task<int> GuardarMetodo(DatosMetodoPagoDTO datos, int id = 0)
    {
        var guardado = id == 0
            ? await api.Post<MetodoPagoDTO>("cuenta/metodos", datos)
            : await api.Put<MetodoPagoDTO>($"cuenta/metodos/{id}", datos);
        await CargarMetodos();
        return guardado.Id;
    }

    public async Task CambiarGuardado(int id, bool favorito, bool guardar)
    {
        string ruta = $"cuenta/libros-guardados/{id}?favorito={favorito.ToString().ToLowerInvariant()}";
        if (guardar)
            await api.Put(ruta);
        else
            await api.Delete(ruta);
    }

    public static string TipoSeguimiento(string tipo)
    {
        if (tipo == "autores")
            return "AUTOR";
        if (tipo == "categorias")
            return "CATEGORIA";
        return "EDITORIAL";
    }

    public async Task AlternarSeguimiento(string tipo, int id, bool seguido)
    {
        string ruta = $"cuenta/seguimientos/{TipoSeguimiento(tipo)}/{id}";
        if (seguido)
            await api.Delete(ruta);
        else
            await api.Put(ruta);
    }
}
