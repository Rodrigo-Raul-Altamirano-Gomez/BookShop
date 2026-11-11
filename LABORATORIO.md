# Laboratorio: BookShop desde cero

Objetivo: construir la tienda desde una base de datos vacía y comprobar tres temas:

1. **Contraseñas con hash.** Se guardan como hash y no se pueden recuperar.
2. **Tarjetas cifradas.** Se guardan cifradas y solo se pueden leer con la clave del servidor.
3. **Documentos PDF.** Con esos datos se generan los PDF de RF17, RF18 y RF19 con JasperReports.

## 1. Conceptos

| Dato | Técnica | ¿Se puede recuperar? | Formato guardado |
|---|---|---|---|
| Contraseña | Hash PBKDF2-SHA256 con sal aleatoria y 120 000 iteraciones | No; solo se verifica | `PBKDF2$120000$<sal>$<huella>` |
| Tarjeta | Cifrado AES-GCM con una clave de 32 bytes | Sí, con la clave del servidor | `AESGCM$<nonce + cifrado>` |

- La **sal** y el **nonce** son aleatorios. Por eso el mismo dato produce un texto distinto cada vez.
- La **clave** está en `BookShopBackend/config/clave-datos.key`. No va en la base de datos ni en Git. Si se pierde, las tarjetas guardadas ya no se pueden leer.
- Esos textos los genera Java (`ProteccionDatos`). No se pueden escribir a mano en un `.sql`. Por eso `compra-demo.sql` (con `${HASH}` y `${CIFRADO}`) quedó reemplazado por `LaboratorioDesdeCero`.

Todos los comandos se ejecutan desde `BookShopBackend`.

## 2. Demostración sin base de datos

```powershell
mvn -q install -DskipTests
mvn -q dependency:build-classpath -pl App "-Dmdep.outputFile=cp.txt"
$cp = "App/target/classes;" + (Get-Content App/cp.txt)
java "-Dstdout.encoding=UTF-8" -cp $cp me.App.DemoProteccion
```

Observa lo siguiente:
- La misma contraseña produce dos hashes distintos, pero `verificar` acepta ambos.
- La misma tarjeta produce dos cifrados distintos, y los dos se descifran al mismo número.
- El descifrado falla en tres casos: con el texto `${CIFRADO}`, con un byte modificado y con otra clave.

## 3. Construir la tienda desde cero

1. Crea una base vacía con `DAO/src/main/resources/DDL.sql` y configura `DBManager/src/main/resources/db.properties`.
2. Ejecuta el laboratorio con el mismo `$cp` del paso anterior:
   ```powershell
   java "-Dstdout.encoding=UTF-8" -cp $cp me.App.LaboratorioDesdeCero
   ```
3. El programa muestra cada paso:

| Paso | Qué hace | Qué observar |
|---|---|---|
| 1 | Registra un administrador `adminXXXX@admin.bookshop` | El rol ADMIN sale del dominio del correo; la base guarda `PBKDF2$...` |
| 2 | El administrador crea categoría, autor, editorial y 2 libros | Solo un ADMIN puede hacerlo |
| 3 | Registra una clienta | Otro hash distinto |
| 4 | Inicia sesión con la contraseña correcta y con una incorrecta | Se comparan hashes |
| 5 | Registra dirección y tarjeta `4111 1111 1111 1111` | La base guarda `AESGCM$...` y la aplicación devuelve `**** 1111` |
| 6 | Carrito, preparación y pago | El pago descifra la tarjeta con la clave |
| 7 | Genera los PDF de RF17, RF18 y RF19 en `laboratorio-pdf/` | Abre los tres archivos |

Cada ejecución usa una tanda distinta: los correos, nombres e ISBN llevan la hora. Por eso puede repetirse sin choques. Al final se imprimen las cuentas para entrar a la web.

## 4. Verlo en la web

1. Despliega `Service/target/bookshop.war` en GlassFish. Su JVM debe tener `-Dbookshop.clave.file` apuntando a la misma clave.
2. Arranca el frontend:
   ```powershell
   dotnet run --project ../BookShopFrontend/Web --launch-profile http
   ```
3. Abre http://localhost:5188.
4. Prueba con las dos cuentas:
   - **Clienta:** compra y pulsa **Descargar comprobante (PDF)** (RF17).
   - **Administrador:** entra a **Cuenta → Reportes** (RF18 y RF19).

## 5. Ejercicios propuestos

1. Ejecuta `SELECT email, contrasenia FROM USUARIO;`. ¿Puedes saber la contraseña de alguien? ¿Por qué dos usuarios con la misma contraseña tienen hashes distintos?
2. Cambia a mano un carácter de `numero_tarjeta_cifrado` de un método de pago e intenta pagar. ¿Qué error aparece y por qué lo detecta AES-GCM?
3. Renombra `config/clave-datos.key`, reinicia GlassFish e intenta pagar con una tarjeta antigua. ¿Qué pasa? ¿Qué pasa con una tarjeta nueva?
4. Cambia `ITERACIONES` en `ProteccionDatos` a 1000 y mide cuánto tarda `hash()`. ¿Por qué conviene que sea lento?
5. Agrega una columna nueva a `ReporteUsuarios.jrxml`, por ejemplo la fecha de registro, y pásala desde `GeneradorPDF.usuarios`.
