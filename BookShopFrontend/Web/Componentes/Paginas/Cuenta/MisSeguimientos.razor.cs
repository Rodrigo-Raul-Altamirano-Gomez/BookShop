using Web.Modelos;

namespace Web.Componentes.Paginas.Cuenta;

public partial class MisSeguimientos
{
    [Microsoft.AspNetCore.Components.Inject] protected CuentaServicio Cuenta { get; set; } = default!;

    [Microsoft.AspNetCore.Components.Inject] protected CatalogoServicio Catalogo { get; set; } = default!;

    protected override async Task CargarDatos()
    {
        if (Sesion.Usuario is not null)
            try
            {
                await Cuenta.CargarSeguimientos();
                Sesion.Notificar();
            }
            catch (InvalidOperationException e)
            {
                Sesion.Error = e.Message;
                Sesion.Avisar(e.Message);
            }
    }
    private const string ClaveVista = "cuenta:mis-seguimientos";
    private string Tipo { get; set; } = "autores";
    private string Orden { get; set; } = "name";
    private int Pagina { get; set; } = 1;

    private const int TamanoPagina = 12;
    private int TotalPaginas => Math.Max(1, (int)Math.Ceiling(Referencias.Count / (double)TamanoPagina));

    private List<Referencia> Referencias
    {
        get
        {
            if (Sesion.Usuario is null)
                return [];
            var referencias = Catalogo.Referencias.Where(referencia => referencia.Tipo == Tipo && Cuenta.Seguimientos.Contains($"{referencia.Tipo}:{referencia.Id}"));
            return (Orden == "recent" ? referencias.OrderByDescending(referencia => Cuenta.OrdenSeguimientos.GetValueOrDefault($"{referencia.Tipo}:{referencia.Id}")) : referencias
                    .OrderBy(referencia => referencia.Nombre)).ToList();
        }
    }

    private IEnumerable<Referencia> ReferenciasPagina => Referencias.Skip((Pagina - 1) * TamanoPagina)
        .Take(TamanoPagina);

    protected override void OnInitialized()
    {
        base.OnInitialized();
        if (Sesion.EstadoVistas.TryGetValue(ClaveVista, out var guardado)
            && guardado is VistaSeguimientos vista)
        {
            Tipo = vista.Tipo;
            Orden = vista.Orden;
            Pagina = Sesion.Usuario is null ? vista.Pagina : Math.Min(vista.Pagina, TotalPaginas);
        }
    }

    private void GuardarVista() => Sesion.EstadoVistas[ClaveVista] = new VistaSeguimientos
    {

        Tipo = Tipo,

        Orden = Orden,

        Pagina = Pagina

    };
    private void MostrarAutores()
    {
        Tipo = "autores";
        Pagina = 1;
        GuardarVista();
    }

    private void MostrarCategorias()
    {
        Tipo = "categorias";
        Pagina = 1;
        GuardarVista();
    }

    private void MostrarEditoriales()
    {
        Tipo = "editoriales";
        Pagina = 1;
        GuardarVista();
    }

    private void ReiniciarPagina()
    {
        Pagina = 1;
        GuardarVista();
    }

    private void CambiarPagina(int pagina)
    {
        Pagina = pagina;
        GuardarVista();
    }

    private class VistaSeguimientos
    {
        public string Tipo { get; set; } = "autores";
        public string Orden { get; set; } = "name";
        public int Pagina { get; set; } = 1;
    }
}

