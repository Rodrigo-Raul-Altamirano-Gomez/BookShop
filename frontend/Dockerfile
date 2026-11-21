# Frontend BookShop: Blazor Server (.NET 10)
FROM mcr.microsoft.com/dotnet/sdk:10.0 AS build
WORKDIR /src
COPY BookShopWeb/ BookShopWeb/
RUN dotnet publish BookShopWeb/BookShopWeb.csproj -c Release -o /app

FROM mcr.microsoft.com/dotnet/aspnet:10.0
WORKDIR /app
COPY --from=build /app .
ENV ASPNETCORE_URLS=http://0.0.0.0:8080
# Variable: ApiUrl (URL del backend terminada en /services/)
ENTRYPOINT ["dotnet", "BookShopWeb.dll"]
