# Interfaz de BookShop

Implementación de escritorio en Blazor / .NET 10, limitada al proyecto **Web**.
Demostración pública: [BookShop en Render](https://bookshop-demo.onrender.com).
Basada en los cinco documentos de `../../docs`, sus referencias visuales y los
campos de los DTO de Java. El texto del diseño prevalece sobre las variaciones
de las imágenes.

## Ejecutar

Desde esta carpeta:

```powershell
dotnet run --project Web.csproj --launch-profile http
```

Abre `http://localhost:5188`. También está disponible el perfil `https` del proyecto.

## Organización

- `Componentes/Estructura`: marco común, búsqueda, panel de cuenta y avisos.
- `Componentes/Compartidos`: tarjetas, iconos, paginación, modales, formularios,
  gestión de libro/referencia y resumen de pedido.
- `Componentes/Paginas`: Inicio, Catálogo, Cuenta, Compra y Administración.
- `Modelos`: modelos de presentación utilizados por las pantallas.
- `Demostracion`: ejemplos y estado temporal del circuito Blazor.
- `wwwroot`: estilos, ilustraciones SVG locales y soporte de teclado para modales.

Las páginas y componentes con interacción separan su marcado `.razor` del estado,
eventos y operaciones de pantalla en `.razor.cs`. Los componentes que solo muestran
texto no necesitan code-behind. El CSS compartido está en `wwwroot/app.css`; el CSS
particular acompaña a su componente en `.razor.css`.

## Recorridos

Inicio, búsqueda por título, catálogo con filtros aplicados explícitamente, orden,
paginación, listados y páginas de autores/categorías/editoriales; detalle de libro,
ficha expandible, reseñas, comparación temporal y vistas de recomendaciones.

Cuenta incluye acceso/registro/reactivación, perfil y formularios de seguridad,
deseados/favoritos, seguimientos, reseñas, direcciones/métodos/cupones y solicitudes
con consulta de referencias. Compra incluye carrito, selecciones navegables,
confirmación, historial, seguimiento y formularios de cancelación/devolución.

Administración incluye portal, listados y altas, edición e imágenes, gestión de
precio/stock/descuentos, moderación, Todas las reseñas y modales de configuración
y reportes. Reportes y comprobantes muestran vistas previas HTML; **no generan PDF**.

## Datos y escenarios de ejemplo

- Cliente: inicia sesión con cualquier correo válido y contraseña no vacía.
- Administrador: un correo con dominio `@admin.bookshop`, por ejemplo
  `demo@admin.bookshop`, con cualquier contraseña no vacía.
- Reactivación: `inactiva@correo.pe`.
- Error de acceso: `error@correo.pe`; muestra el error y vacía las credenciales.
- En compra, el segundo cupón ilustra la selección rechazada y conserva la anterior.
- El tercer método de pago ilustra un rechazo en el primer intento; volver a
  intentarlo muestra la confirmación.
- En pedidos, Actualizar seguimiento avanza una etapa de presentación por pulsación.

Son escenarios fijos, sin autenticación, autorización ni validaciones reales.
No utilices credenciales ni datos financieros reales. La información de ejemplo
es ficticia. La selección de archivos solo produce una vista previa en memoria.

Los cambios se conservan durante la navegación dentro de una pestaña; recargar o
abrir otra pestaña inicia otros ejemplos. Filtros/orden/página del catálogo e historial
se conservan al volver de sus detalles. Cancelar formularios descarta el borrador.

## Límite de esta fase

No hay llamadas HTTP al backend, acceso a MySQL, persistencia de datos de la tienda,
hash/cifrado, reservas de stock, algoritmos reales, probabilidades de pago,
elegibilidad de reseñas/devoluciones, transacciones ni generación de documentos.
Los filtros y sumas sirven para mostrar la interfaz sobre ejemplos. Envío y cupón
utilizan importes ilustrativos, no las fórmulas/configuración de Java.

Web no depende de Data, Service ni Integration. Esos proyectos y el backend
permanecen sin cambios de código. Las imágenes locales son ilustraciones de prueba,
no portadas comerciales ni fotografías de autores.

## Verificación

```powershell
dotnet build Web.csproj --no-restore
```

La revisión de interacción se realiza en el navegador con datos de ejemplo;
no equivale a verificar las reglas del backend ni una compra real.

Comprobado en escritorio: búsqueda con Enter, aplicación y conservación de filtros,
paginación, comparación, error/acceso correcto, cantidades del carrito, cancelación
de formularios, alta de dirección, rechazo de cupón/pago y confirmación. También se
comprobaron seguimiento, vista previa del comprobante, acceso administrativo,
edición de stock, retirada de portada, configuración conservada al volver y resolución
de una solicitud. La compilación terminó con cero errores y cero advertencias.
