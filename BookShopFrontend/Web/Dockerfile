FROM mcr.microsoft.com/dotnet/sdk:10.0 AS compilacion
WORKDIR /src
COPY . ./
# Blazor detecta los componentes durante restore e incluye sus scripts de framework.
RUN dotnet restore Web.csproj
RUN dotnet publish Web.csproj --no-restore -c Release -o /app/publicacion /p:UseAppHost=false

FROM mcr.microsoft.com/dotnet/aspnet:10.0 AS ejecucion
WORKDIR /app
ENV ASPNETCORE_ENVIRONMENT=Production
ENV ASPNETCORE_HTTP_PORTS=10000
EXPOSE 10000
COPY --from=compilacion /app/publicacion ./
USER $APP_UID
ENTRYPOINT ["dotnet", "Web.dll"]
