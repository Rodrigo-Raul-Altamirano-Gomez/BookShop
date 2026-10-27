# Demostración de BookShopBackend

`me.App.Main` utiliza los DTO y las validaciones actuales de Business. Muestra un
filtro válido, su título normalizado, sus categorías sin duplicados y el error
producido por un rango de precios invertido. No necesita conectarse a MySQL.

App pertenece al reactor Maven junto con Model, DBManager, DAO y Business.
Desde la carpeta `BookShopBackend`, con JDK 21 o posterior y Maven:

```powershell
mvn test
java -cp "App/target/classes;Business/target/classes;Model/target/classes" me.App.Main
```

El comando Java anterior utiliza el separador de rutas de Windows (`;`). En Linux
y macOS se utiliza `:`. En IntelliJ IDEA también se puede ejecutar directamente
el método `main` de la clase `me.App.Main`.
