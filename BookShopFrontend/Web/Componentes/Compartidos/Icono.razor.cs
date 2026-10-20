using Microsoft.AspNetCore.Components;

namespace Web.Componentes.Compartidos;

public partial class Icono
{
    [Parameter]
    public string Nombre { get; set; } = "book";
    private string Trazo => Nombre switch
    {
        "search" => "M21 21l-5-5M18 10a8 8 0 1 1-16 0 8 8 0 0 1 16 0",
        "cart" => "M2 3h3l3 13h11l3-9H6M9 21h.01M18 21h.01",
        "user" => "M20 21v-2a8 8 0 0 0-16 0v2M16 6a4 4 0 1 1-8 0 4 4 0 0 1 8 0",
        "heart" => ("M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.7l-1.1-1.1a5.5 5.5 0 0 0-7.8 "
            + "7.8L12 21l8.8-8.6a5.5 5.5 0 0 0 0-7.8"),
        "star" => "M12 2l3 6.5 7 1-5 5 1.2 7-6.2-3.3-6.2 3.3 1.2-7-5-5 7-1z",
        "compare" => "M4 7h16l-4-4M20 17H4l4 4",
        "close" => "M6 6l12 12M18 6L6 18",
        "arrow" => "M4 12h16m-6-6 6 6-6 6",
        "check" => "M5 12l4 4L19 6",
        "plus" => "M12 5v14M5 12h14",
        "edit" => "M16 3l5 5L9 20l-6 1 1-6zM14 5l5 5",
        "trash" => "M3 6h18M9 6V3h6v3M5 6l1 15h12l1-15M10 10v7M14 10v7",
        "flag" => "M5 22V3c5-4 9 4 14 0v11c-5 4-9-4-14 0",
        "thumb" => "M7 10l4-8c3 0 3 3 1 8h7c2 0 2 2 1 4l-2 7H7zM3 10h4v11H3z",
        "pin" => "M20 9c0 6-8 13-8 13S4 15 4 9a8 8 0 1 1 16 0M15 9a3 3 0 1 1-6 0 3 3 0 0 1 6 0",
        "card" => "M3 4h18v16H3zM3 9h18M7 15h4",
        "wallet" => "M4 6V3h15v3M3 6h18v15H3zM16 11h5v5h-5z",
        "grid" => "M3 3h7v7H3zM14 3h7v7h-7zM3 14h7v7H3zM14 14h7v7h-7z",
        "building" => "M5 21V3h14v18M2 21h20M9 7h1M14 7h1M9 11h1M14 11h1M10 21v-6h4v6",
        "settings" => "M3 6h18M3 12h18M3 18h18M7 3v6M16 9v6M9 15v6",
        "report" => "M5 3h14v18H5zM9 16v-3M12 16V8M15 16v-5",
        "follow" => "M4 5h10M4 10h7M4 15h7M17 11v10M12 16h10",
        "shield" => "M12 2l9 4v6c0 6-9 10-9 10S3 18 3 12V6zM8 12l3 3 5-6",
        "key" => "M14 5a5 5 0 1 1-7 7L2 17v4h4v-3h3l3-4",
        "power" => "M12 2v10M6 5a9 9 0 1 0 12 0",
        "eye" => ("M2 12s4-7 10-7 10 7 10 7-4 7-10 7-10-7-10-7M15 12a3 3 0 1 1-6 0 3 3 " + "0 0 1 6 0"),
        "tag" => "M3 3h9l10 10-9 9L3 12zM7 7h.01",
        _ => "M12 5C9 2 4 2 2 3v17c3-1 7-1 10 2 3-3 7-3 10-2V3c-2-1-7-1-10 2v17"
    };
}

