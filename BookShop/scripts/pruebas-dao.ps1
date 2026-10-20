param(
    [string]$RepositorioMaven = (Join-Path $env:USERPROFILE '.m2/repository'),
    [string]$Jdk = $env:JAVA_HOME,
    [switch]$IncluirBusiness
)

$ErrorActionPreference = 'Stop'
$raizProyecto = Split-Path $PSScriptRoot -Parent
if (-not $Jdk) {
    $compilador = Get-Command javac -ErrorAction SilentlyContinue
    if ($compilador) {
        $Jdk = Split-Path (Split-Path $compilador.Source -Parent) -Parent
    }
    if (-not $Jdk -or -not (Test-Path (Join-Path $Jdk 'bin/javac.exe'))) {
        $instalaciones = Get-ChildItem 'C:/Program Files/Java' -Directory -ErrorAction SilentlyContinue
        $Jdk = $instalaciones | Where-Object { Test-Path (Join-Path $_.FullName 'bin/javac.exe') } |
            Sort-Object Name -Descending | Select-Object -First 1 -ExpandProperty FullName
    }
}
if (-not $Jdk) { throw 'Indica -Jdk con la ruta de un JDK 21 o posterior.' }

# Alternativa sin Maven instalado: usa las mismas dependencias ya descargadas.
$dependencias = @(
    'com/zaxxer/HikariCP/7.1.0/HikariCP-7.1.0.jar',
    'org/slf4j/slf4j-api/2.0.17/slf4j-api-2.0.17.jar',
    'org/slf4j/slf4j-jdk14/2.0.17/slf4j-jdk14-2.0.17.jar',
    'org/junit/jupiter/junit-jupiter-api/5.11.4/junit-jupiter-api-5.11.4.jar',
    'org/junit/jupiter/junit-jupiter-engine/5.11.4/junit-jupiter-engine-5.11.4.jar',
    'org/junit/jupiter/junit-jupiter-params/5.11.4/junit-jupiter-params-5.11.4.jar',
    'org/junit/platform/junit-platform-commons/1.11.4/junit-platform-commons-1.11.4.jar',
    'org/junit/platform/junit-platform-engine/1.11.4/junit-platform-engine-1.11.4.jar',
    'org/junit/platform/junit-platform-launcher/1.11.4/junit-platform-launcher-1.11.4.jar',
    'org/apiguardian/apiguardian-api/1.1.2/apiguardian-api-1.1.2.jar',
    'org/opentest4j/opentest4j/1.3.0/opentest4j-1.3.0.jar',
    'org/mockito/mockito-core/5.23.0/mockito-core-5.23.0.jar',
    'net/bytebuddy/byte-buddy/1.17.7/byte-buddy-1.17.7.jar',
    'net/bytebuddy/byte-buddy-agent/1.17.7/byte-buddy-agent-1.17.7.jar',
    'org/objenesis/objenesis/3.3/objenesis-3.3.jar',
    'com/h2database/h2/2.3.232/h2-2.3.232.jar'
)
$jars = $dependencias | ForEach-Object {
    $rutaJar = Join-Path $RepositorioMaven $_
    if (-not (Test-Path -LiteralPath $rutaJar)) { throw "Falta $rutaJar. Usa Maven para restaurar dependencias." }
    $rutaJar
}
$salida = Join-Path $raizProyecto ('DAO/target/verificacion-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $salida -Force | Out-Null
$classpath = [string]::Join([IO.Path]::PathSeparator, $jars)
$modulos = @('Model', 'DBManager', 'DAO')
if ($IncluirBusiness) { $modulos += 'Business' }
$fuentes = foreach ($modulo in $modulos) {
    Get-ChildItem (Join-Path $raizProyecto "$modulo/src/main/java") -Recurse -Filter '*.java' |
        Select-Object -ExpandProperty FullName
    Get-ChildItem (Join-Path $raizProyecto "$modulo/src/test/java") -Recurse -Filter '*.java' |
        Select-Object -ExpandProperty FullName
}
$ejecutor = Join-Path $salida 'PruebasDAO.java'
@'
import java.io.PrintWriter;
import java.nio.file.Path;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;

public class PruebasDAO {
    public static void main(String[] args) throws Exception {
        java.util.logging.Logger.getLogger("").setLevel(java.util.logging.Level.WARNING);
        var resumen = new SummaryGeneratingListener();
        var launcher = LauncherFactory.create();
        launcher.registerTestExecutionListeners(resumen);
        launcher.execute(LauncherDiscoveryRequestBuilder.request()
                .selectors(DiscoverySelectors.selectPackage("me")).build());
        var escritor = new PrintWriter(System.out, true);
        resumen.getSummary().printTo(escritor);
        resumen.getSummary().printFailuresTo(escritor);
        try (var archivo = new PrintWriter(Path.of(args[0]).toFile(), "UTF-8")) {
            resumen.getSummary().printTo(archivo);
            resumen.getSummary().printFailuresTo(archivo);
        }
        if (resumen.getSummary().getTestsFoundCount() == 0
                || resumen.getSummary().getTotalFailureCount() != 0) System.exit(1);
    }
}
'@ | Set-Content -LiteralPath $ejecutor -Encoding UTF8
& (Join-Path $Jdk 'bin/javac.exe') --release 21 -encoding UTF-8 -cp $classpath -d $salida @fuentes $ejecutor
if ($LASTEXITCODE -ne 0) { throw 'Falló la compilación de fuentes y pruebas.' }

# Los recursos de prueba preceden a cualquier configuración real de conexión.
$classpathPruebas = [string]::Join([IO.Path]::PathSeparator, @(
    $salida, (Join-Path $raizProyecto 'DBManager/src/test/resources'),
    (Join-Path $raizProyecto 'DAO/src/main/resources'), $classpath
))
Push-Location (Join-Path $raizProyecto 'DAO')
try {
    & (Join-Path $Jdk 'bin/java.exe') '-Dnet.bytebuddy.experimental=true' "-javaagent:$(Join-Path $RepositorioMaven 'org/mockito/mockito-core/5.23.0/mockito-core-5.23.0.jar')" -cp $classpathPruebas PruebasDAO (Join-Path $salida 'resultado.txt')
    if ($LASTEXITCODE -ne 0) { throw "Fallaron las pruebas. Resultado: $salida/resultado.txt" }
    Write-Output "Resultado: $salida/resultado.txt"
} finally {
    Pop-Location
}
