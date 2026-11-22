# Guion de la demostración

Recorrido de unos 15 minutos por las capacidades de los tres incrementos. Usa los datos de prueba de `database/02_datos.sql`; todas las cuentas tienen la contraseña `123456`.

## Antes de empezar

- Cargar `01_esquema.sql` y `02_datos.sql` poco antes de la demostración: las fechas del historial son relativas a la carga.
- Arrancar el backend y esperar un minuto: en su primera vuelta, el hilo emite los cupones que corresponden al historial.
- Tener abiertas dos ventanas del navegador: una normal (Ana y luego el administrador) y otra privada (Luis).

## 1. La tienda como visitante (2 min)

1. **Inicio:** portada, *Novedades*, *Más vendidos* y *Más populares* (sin sesión no aparece *Para ti*), y el bloque *Explorar*.
2. **Catálogo:** buscar «la», filtrar por una categoría y un idioma, ordenar por precio y pasar de página. La URL cambia con cada filtro: copiarla en otra pestaña muestra lo mismo, y *Atrás* deshace el último cambio.
3. **Detalle y comparación:** abrir un libro con descuento (precio tachado y porcentaje), *Comparar* con otro y abrir la comparación.
4. **Explorar:** buscar una editorial y abrir su página, con sus libros en la misma vista de resultados.
5. Pulsar *Agregar al carrito*: un visitante va a iniciar sesión.

## 2. Cuentas (1 min)

1. Iniciar sesión con `rosa@correo.com`: la cuenta está desactivada y el diálogo ofrece reactivarla.
2. Cerrar sesión e iniciar con `ana@correo.com`. Mostrar el perfil y sus direcciones y métodos de pago.

## 3. Compra (3 min) — Ana

1. Agregar dos libros al carrito, cambiar una cantidad y mostrar el resumen (subtotal, envío y total). Debajo, *También suelen llevarse* sugiere libros que otros clientes compraron junto con ellos.
2. *Continuar*: en el resumen de compra elegir dirección y método de pago, y aplicar el cupón `BK7Q2` (10 %).
3. *Pagar*. El pago simulado se rechaza 1 de cada 5 veces; si sale, se muestra el aviso y se puede reintentar.
4. En la confirmación, *Descargar comprobante* abre el PDF en una pestaña nueva.
5. **Pedidos:** el pedido nuevo está *En almacén*. El hilo lo pasa a *En camino* y luego a *Entregado* en uno o dos minutos por paso; se vuelve a él al final de la demostración.

## 4. Posventa (2 min)

1. **Cancelar:** hacer una segunda compra rápida de un libro, sin cupón, y cancelarla desde su detalle mientras sigue *En almacén*. La confirmación indica cuántas cancelaciones le quedan este mes, y el stock se repone.
2. **Devolver:** abrir el N.º 1010 (entregado, dentro del plazo) y pedir la devolución con un motivo. Aparece en *Mis solicitudes*, donde se puede editar o retirar.
3. **Mis cupones:** el cupón usado y los demás con su vigencia.

## 5. Biblioteca y reseñas (3 min)

1. **Listas:** en el detalle de un libro, pulsar *Deseados* y *Favoritos*. En *Mis listas* aparecen las dos secciones; quitar un libro de una de ellas.
2. **Seguimientos:** en *Explorar → Editoriales*, abrir *Casa del Río* y pulsar *Seguir*. En *Seguimientos*, pestaña *Editoriales*, aparece primero con el orden *Seguido recientemente*.
3. **Reseñar:** abrir *Diccionario de lo invisible* (Ana lo tiene entregado y aún no lo reseña), puntuar con 2 estrellas y escribir un comentario. El promedio y la distribución se actualizan, y la reseña propia queda primero. Mostrarla en *Mis reseñas*.
4. **Votar y reportar (Luis, ventana privada):** en el mismo libro, marcar la reseña de Ana como *Útil* (se ve el contador) y *Reportar* con el motivo *Ofensiva*. Intentar reportarla otra vez: no se permite mientras el primer reporte esté pendiente. El reporte se ve en *Mis solicitudes → Reportes de reseñas*.

## 6. Administración (3 min) — `admin@admin.bookshop`

1. **Catálogo:** editar un libro y agregarle un descuento; en la tienda se ve el precio nuevo. Desactivar y volver a activar un autor.
2. **Devoluciones:** aceptar la devolución de Ana (o la pendiente de Luis, N.º 1013): el pedido queda *Devuelto* y el stock se repone.
3. **Reportes de reseñas:** la tarjeta del reporte de Luis muestra la reseña tal como estaba al reportarla. *Ocultar reseña*: deja de verse y de contar en el promedio. *Mantener visible* en otro reporte lo rechaza.
4. **Detalle del libro como administrador:** en *Diccionario de lo invisible* la reseña aparece con la marca *Oculta* y el botón *Mostrar*; en *El faro de los nombres* está la reseña de spam ocultada en los datos de prueba.
5. **Reportes PDF:** generar *Libros por monto vendido* y *Clientes por monto gastado* para el mes actual; cada uno se abre en una pestaña nueva con sus totales. En el de clientes también aparecen los que no compraron en el período, como Andrés, con monto 0.

## 7. Recomendaciones y cierre (1 min) — Ana

1. **Inicio:** *Para ti* recomienda libros que compraron clientes con gustos parecidos a los de Ana. *Ver todos* abre la vista completa, con filtros y búsqueda pero con el orden fijo del algoritmo.
2. **Más vendidos → Ver todos:** poner en *Período* las fechas del último mes y ver cómo cambia el conjunto: ahora cuenta solo las ventas de esas fechas.
3. **Similares:** en el detalle de un libro, los parecidos por categoría, autor, editorial, idioma, precio, páginas y año.
4. Volver a *Pedidos*: el pedido de la compra ya está *Entregado*, con su seguimiento completo.
