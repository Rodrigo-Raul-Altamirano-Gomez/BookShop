using Web.Modelos;
using Web.Modelos.DTO;

namespace Web.Servicios;

public class CuentaServicio
{
    private readonly HttpClientUtils api;
    private readonly SesionUsuario Sesion;
    private readonly CatalogoServicio Catalogo;
    public List<Direccion> Direcciones { get; } = [];
    public List<MetodoPago> Metodos { get; } = [];
    public List<Cupon> Cupones { get; } = [];
    public HashSet<int> Deseados { get; } = [];
    public HashSet<int> Favoritos { get; } = [];
    public HashSet<string> Seguimientos { get; } = [];
    public Dictionary<string, int> OrdenSeguimientos { get; } = [];

    public CuentaServicio(HttpClientUtils api, SesionUsuario sesion, CatalogoServicio catalogo)
    {
        this.api = api;
        Sesion = sesion;
        Catalogo = catalogo;
        Sesion.Cerrada += Limpiar;
    }

    private void Limpiar()
    {
        Direcciones.Clear();
        Metodos.Clear();
        Cupones.Clear();
        Deseados.Clear();
        Favoritos.Clear();
        Seguimientos.Clear();
        OrdenSeguimientos.Clear();

    }

    public async Task CargarDatosCompra()
    {
        await CargarDirecciones();
        await CargarMetodos();
        await CargarCupones();
    }

    public async Task CargarDirecciones()
    {
        Direcciones.Clear();
        int pagina = 1;
        long total;
        do
        {
            var datos = await api.Get<ResultadoPaginadoDTO<DireccionEnvioDTO>>($"cuenta/direcciones?pagina={pagina}&tamanioPagina=10");
            Direcciones.AddRange(datos.Items.Select(Presentacion.Direccion));
            total = datos.TotalResultados;
            pagina++;
        } while ((pagina - 1) * 10 < total);
    }

    public async Task CargarMetodos()
    {
        Metodos.Clear();
        int pagina = 1;
        long total;
        do
        {
            var datos = await api.Get<ResultadoPaginadoDTO<MetodoPagoDTO>>($"cuenta/metodos?pagina={pagina}&tamanioPagina=10");
            Metodos.AddRange(datos.Items.Select(Presentacion.Metodo));
            total = datos.TotalResultados;
            pagina++;
        } while ((pagina - 1) * 10 < total);
    }

    public async Task CargarCupones()
    {
        Cupones.Clear();
        int pagina = 1;
        long total;
        do
        {
            var datos = await api.Get<ResultadoPaginadoDTO<CuponDTO>>($"cuenta/cupones?pagina={pagina}&tamanioPagina=10");
            foreach (var cupon in datos.Items)
            {
                string estado = "Usado";
                if (cupon.Estado == "DISPONIBLE") estado = "Disponible";
                else if (cupon.Estado == "VENCIDO") estado = "Vencido";
                Cupones.Add(new Cupon
                {
                    Id = cupon.Id,
                    Porcentaje = cupon.Porcentaje,
                    Minimo = cupon.MontoMinimo,
                    Vencimiento = DateOnly.FromDateTime(cupon.FechaFin),
                    Estado = estado
                });
            }
            total = datos.TotalResultados;
            pagina++;
        } while ((pagina - 1) * 10 < total);
    }

    public async Task GuardarDireccion(Direccion direccion, int id = 0)
    {
        var datos = new DatosDireccionEnvioDTO
        {
            Alias = direccion.Alias,
            Calle = direccion.Calle,
            Distrito = direccion.Distrito,
            Ciudad = direccion.Ciudad,
            Referencia = direccion.ReferenciaAdicional,
            Predeterminada = direccion.Predeterminada
        };
        if (id == 0)
            await api.Post("cuenta/direcciones", datos);
        else
            await api.Put($"cuenta/direcciones/{id}", datos);
        await CargarDirecciones();
    }

    public async Task GuardarMetodo(MetodoPago metodo, int id = 0)
    {
        var datos = new DatosMetodoPagoDTO
        {
            TarjetaBancaria = metodo.Tipo == "Tarjeta",
            Titular = metodo.Titular,
            Proveedor = metodo.Proveedor,
            Alias = metodo.Alias,
            Numero = metodo.Numero,
            TipoTarjeta = null,
            FechaVencimiento = metodo.Tipo == "Tarjeta" ? metodo.Vencimiento : null,
            Predeterminado = metodo.Predeterminada
        };
        if (metodo.Tipo == "Tarjeta")
            datos.TipoTarjeta = metodo.TipoTarjeta == "Crédito" ? "CREDITO" : "DEBITO";
        try
        {
            if (id == 0)
                await api.Post("cuenta/metodos", datos);
            else
                await api.Put($"cuenta/metodos/{id}", datos);
            await CargarMetodos();
        }
        finally
        {
            metodo.Numero = null;
        }
    }

    public async Task AlternarGuardado(int id, bool favorito)
    {
        if (!Sesion.RequerirAcceso())
            return;
        var lista = favorito ? Favoritos : Deseados;
        string ruta = $"cuenta/libros-guardados/{id}?favorito={favorito.ToString().ToLowerInvariant()}";
        if (lista.Contains(id))
            await api.Delete(ruta);
        else
            await api.Put(ruta);
        if (lista.Contains(id))
            lista.Remove(id);
        else
            lista.Add(id);
    }

    public static string TipoSeguimiento(string tipo)
    {
        if (tipo == "autores") return "AUTOR";
        if (tipo == "categorias") return "CATEGORIA";
        return "EDITORIAL";
    }

    public async Task CargarSeguimientos()
    {
        Seguimientos.Clear();
        OrdenSeguimientos.Clear();
        foreach (var tipo in new[] { "autores", "categorias", "editoriales" })
        {
            int pagina = 1;
            long total;
            do
            {
                var datos = await api.Post<ResultadoPaginadoDTO<TarjetaReferenciaDTO>>("cuenta/seguimientos/busqueda", new ConsultaSeguimientosDTO
                {
                    Tipo = TipoSeguimiento(tipo),
                    Orden = "MAS_RECIENTES",
                    Pagina = pagina,
                    TamanioPagina = 12
                });
                foreach (var dto in datos.Items)
                {
                    var clave = $"{tipo}:{dto.Id}";
                    Seguimientos.Add(clave);
                    OrdenSeguimientos[clave] = -(pagina - 1) * 12 - datos.Items.IndexOf(dto);
                    if (!Catalogo.Referencias.Any(r => r.Tipo == tipo && r.Id == dto.Id))
                        Catalogo.Referencias.Add(Presentacion.Referencia(dto, tipo));
                }
                total = datos.TotalResultados;
                pagina++;
            } while ((pagina - 1) * 12 < total);
        }
    }

    public async Task AlternarSeguimiento(string tipo, int id)
    {
        if (!Sesion.RequerirAcceso())
            return;
        string ruta = $"cuenta/seguimientos/{TipoSeguimiento(tipo)}/{id}";
        if (Seguimientos.Contains($"{tipo}:{id}"))
            await api.Delete(ruta);
        else
            await api.Put(ruta);
        await CargarSeguimientos();
    }
}
