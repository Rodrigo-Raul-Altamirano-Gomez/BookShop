param(
    [string]$RepositorioMaven = (Join-Path $env:USERPROFILE '.m2/repository'),
    [string]$Jdk = $env:JAVA_HOME
)

& (Join-Path $PSScriptRoot 'pruebas-dao.ps1') -RepositorioMaven $RepositorioMaven -Jdk $Jdk -IncluirBusiness
