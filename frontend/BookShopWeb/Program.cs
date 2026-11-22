using BookShopWeb.Code;
using BookShopWeb.Components;
using BookShopWeb.Security;
using BookShopWeb.Sesion;

var builder = WebApplication.CreateBuilder(args);

builder.Services.AddRazorComponents()
    .AddInteractiveServerComponents();

builder.Services.AddCookieAuth();

// Estado compartido de la sesión del usuario
builder.Services.AddScoped<AvisoService>();
builder.Services.AddScoped<ComparacionService>();
builder.Services.AddScoped<CarritoService>();
builder.Services.AddScoped<UsuarioActual>();

// URL del API: appsettings.json o la variable de entorno ApiUrl
Api.Url = builder.Configuration["ApiUrl"]!;

var app = builder.Build();

if (!app.Environment.IsDevelopment())
{
    app.UseExceptionHandler("/Error", createScopeForErrors: true);
}
app.UseStatusCodePagesWithReExecute("/not-found", createScopeForStatusCodePages: true);

app.UseAuthentication();
app.UseAuthorization();
app.MapAuthEndpoints();
app.MapDocumentosEndpoints();

app.UseAntiforgery();
// MapStaticAssets sirve lo que existe al compilar; UseStaticFiles, también las imágenes subidas después
app.UseStaticFiles();
app.MapStaticAssets();
app.MapRazorComponents<App>()
    .AddInteractiveServerRenderMode();

app.Run();
