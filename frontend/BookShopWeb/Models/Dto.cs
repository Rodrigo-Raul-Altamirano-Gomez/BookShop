namespace BookShopWeb.Models;

// DTO que devuelve o recibe el backend (no son tablas)

public record ResultadoLibros
{
    public int Total { get; init; }
    public int Pagina { get; init; }
    public int TotalPaginas { get; init; }
    public List<Libro> Libros { get; init; } = new();
    public List<Entidad> Categorias { get; init; } = new();
    public List<Entidad> Autores { get; init; } = new();
    public List<Entidad> Editoriales { get; init; } = new();
    public List<string> Idiomas { get; init; } = new();
}

public record ResultadoPagina<T>
{
    public int Total { get; init; }
    public List<T> Elementos { get; init; } = new();
}

public record EntidadAdmin
{
    public Entidad Entidad { get; init; } = new();
    public int CantidadLibros { get; init; }
}

public record ConteoLibros
{
    public int Total { get; init; }
    public int Activos { get; init; }
}

public record Credenciales(string Correo, string Contrasena);

public record CambioContrasena(string Actual, string Nueva);
