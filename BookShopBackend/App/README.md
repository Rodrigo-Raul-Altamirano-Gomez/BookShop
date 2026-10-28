# Prueba del flujo de compra

`me.App.Main` ejecuta una compra con los servicios reales de Business y sus DAO.
Todo el recorrido está dentro de `main`, sin ingreso por consola ni métodos auxiliares.
Los IDs del cliente, libro, dirección, método de pago, cupón y la cantidad se definen
al inicio del método. Ajustarlos a los registros de la base MySQL de desarrollo.
Los valores incluidos son ejemplos y no garantizan que esos registros existan.

El flujo consulta el libro, agrega un ejemplar al carrito, modifica la cantidad,
verifica que una cantidad negativa sea rechazada sin alterar el carrito, prepara
la compra y selecciona los datos de envío y pago. Luego ejecuta el pago simulado
de Business, consulta el pedido persistido y comprueba sus ejemplares, importe y
que el carrito haya quedado vacío. Si una comprobación falla, lanza una excepción.

## Preparación y ejecución

Requiere una base MySQL de desarrollo con el esquema actual, un cliente activo,
un libro activo con stock suficiente y una dirección y un método de pago vigente
pertenecientes al cliente. El cupón 0 significa no utilizar cupón.
Configura `db.properties` y la protección de datos según el README principal.
Las claves deben permitir leer el método de pago almacenado.

Desde `BookShopBackend`, con JDK 21 o posterior y Maven:

```powershell
mvn -pl App -am compile
```

En IntelliJ IDEA ejecuta `me.App.Main` dentro del módulo App.
El pago simulado puede ser rechazado según las reglas de Business. Si tiene éxito,
registra el pedido y descuenta stock en la base configurada, sin pedir confirmación.
No hay limpieza automática de la compra confirmada. La aplicación no crea los
registros necesarios para el escenario ni sustituye las pruebas automatizadas.
