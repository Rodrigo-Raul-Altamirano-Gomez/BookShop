using System.Globalization;

namespace BookShopWeb.Code;

// Formatos de UX-UI §12.1
public static class Formato
{
    private static readonly CultureInfo numeros = CultureInfo.GetCultureInfo("en-US");

    // S/ 1,234.50
    public static string Soles(decimal monto) => "S/ " + monto.ToString("N2", numeros);

    // dd/mm/aaaa
    public static string Fecha(DateOnly fecha) => fecha.ToString("dd/MM/yyyy");
    public static string Fecha(DateTime fecha) => fecha.ToString("dd/MM/yyyy");

    // **** **** **** 1234 (tarjeta) o ••••• 4321 (celular)
    public static string Enmascarar(string numero)
    {
        string ultimos = numero.Length > 4 ? numero[^4..] : numero;
        return numero.Length == 16 ? "•••• •••• •••• " + ultimos : "••••• " + ultimos;
    }

    // Iniciales de un nombre (avatar, imagen de entidad sin foto)
    public static string Iniciales(string nombre)
    {
        string iniciales = "";
        foreach (string palabra in nombre.Split(' ', StringSplitOptions.RemoveEmptyEntries))
        {
            if (palabra.Length > 2 && iniciales.Length < 2)
            {
                iniciales += char.ToUpper(palabra[0]);
            }
        }
        return iniciales == "" && nombre.Length > 0 ? nombre[..1].ToUpper() : iniciales;
    }
}
