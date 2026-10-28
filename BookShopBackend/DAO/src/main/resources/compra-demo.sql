-- Datos para el flujo de compra de App/Main sobre el DDL actual.
-- Ejecutar solo en una base de desarrollo recién creada.
-- El ejecutor reemplaza los marcadores con ProteccionDatos.hash y cifrar.
INSERT INTO USUARIO (id,email,contrasenia,nombre,fecha_nacimiento,usuario_creacion)
VALUES (1,'ana@example.test','${HASH}','Ana García','1995-04-15','demo');
INSERT INTO CATEGORIA (id,nombre,usuario_creacion) VALUES (1,'Ficción','demo');
INSERT INTO AUTOR (id,nombre,usuario_creacion) VALUES (1,'Autor de demostración','demo');
INSERT INTO EDITORIAL (id,nombre,usuario_creacion) VALUES (1,'Editorial de demostración','demo');
INSERT INTO LIBRO (id,id_categoria,id_autor,id_editorial,titulo,isbn,sinopsis,idioma,
    proveedor,anio_edicion,numero_paginas,precio_base,stock_fisico,usuario_creacion)
VALUES (3,1,1,1,'Libro de demostración','9780000000003','Libro para probar una compra.',
    'Español','Proveedor de demostración',2026,200,50.00,20,'demo');
INSERT INTO DIRECCION_ENVIO (id,id_usuario,alias,predeterminada,calle,distrito,ciudad,usuario_creacion)
VALUES (1,1,'Casa',1,'Calle de prueba 123','San Miguel','Lima','demo');
INSERT INTO METODO_PAGO (id,id_usuario,titular,proveedor,alias,tipo_metodo,numero_tarjeta_cifrado,
    fecha_vencimiento,predeterminado,tipo_tarjeta,usuario_creacion)
VALUES (1,1,'Ana García','Banco de prueba','Tarjeta de prueba','TARJETA_BANCARIA','${CIFRADO}',
    '2030-12-31',1,'DEBITO','demo');
