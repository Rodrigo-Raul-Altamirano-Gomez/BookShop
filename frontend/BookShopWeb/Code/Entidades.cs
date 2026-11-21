using BookShopWeb.Models;

namespace BookShopWeb.Code;

// Textos y rutas de cada tipo de entidad (autor, categoría, editorial)
public static class Entidades
{
    // Segmento de la URL: /autores/5, /explorar/autores, /admin/autores
    public static string Ruta(TipoEntidad tipo) => tipo switch
    {
        TipoEntidad.AUTOR => "autores",
        TipoEntidad.CATEGORIA => "categorias",
        _ => "editoriales"
    };

    public static TipoEntidad DesdeRuta(string ruta) => ruta switch
    {
        "autores" => TipoEntidad.AUTOR,
        "categorias" => TipoEntidad.CATEGORIA,
        _ => TipoEntidad.EDITORIAL
    };

    public static string Plural(TipoEntidad tipo) => tipo switch
    {
        TipoEntidad.AUTOR => "Autores",
        TipoEntidad.CATEGORIA => "Categorías",
        _ => "Editoriales"
    };

    public static string Singular(TipoEntidad tipo) => tipo switch
    {
        TipoEntidad.AUTOR => "Autor",
        TipoEntidad.CATEGORIA => "Categoría",
        _ => "Editorial"
    };

    // "Nuevo autor", "Nueva categoría", "Nueva editorial"
    public static string Nuevo(TipoEntidad tipo) => tipo == TipoEntidad.AUTOR ? "Nuevo autor" : "Nueva " + Singular(tipo).ToLower();

    // Concordancia del aviso: "desactivado" o "desactivada"
    public static string Genero(TipoEntidad tipo) => tipo == TipoEntidad.AUTOR ? "o" : "a";

    // Parámetro del filtro de libros que corresponde al tipo
    public static string Filtro(TipoEntidad tipo) => tipo switch
    {
        TipoEntidad.AUTOR => "autor",
        TipoEntidad.CATEGORIA => "categoria",
        _ => "editorial"
    };
}
