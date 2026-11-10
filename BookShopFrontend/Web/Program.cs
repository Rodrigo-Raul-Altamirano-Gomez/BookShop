using Web.Componentes;
using Web.Servicios;

var builder = WebApplication.CreateBuilder(args);
builder.Services.AddScoped<HttpClientUtils>();
builder.Services.AddScoped<SesionUsuario>();
builder.Services.AddScoped<CatalogoServicio>();
builder.Services.AddScoped<CuentaServicio>();
builder.Services.AddScoped<CompraServicio>();
// Componentes interactivos y estado temporal de la interfaz.
builder.Services.AddRazorComponents().AddInteractiveServerComponents();
var app = builder.Build();
// Manejo de errores fuera del entorno de desarrollo.
if (!app.Environment.IsDevelopment())
{
    app.UseExceptionHandler("/Error", createScopeForErrors: true);
    app.UseHsts();
}

app.UseStatusCodePagesWithReExecute("/not-found", createScopeForStatusCodePages: true);
app.UseHttpsRedirection();
app.UseAntiforgery();
// Comprobación de disponibilidad para el alojamiento.
app.MapGet("/salud", () => Results.Text("ok"));
app.MapStaticAssets();
app.MapRazorComponents<Aplicacion>().AddInteractiveServerRenderMode();
app.Run();
