-- BookShop: datos de prueba
USE bookshop;

-- Configuración inicial (ERS §3.1)
INSERT INTO configuracion VALUES (1, 10.00, 7, 2, 3, 100.00, 3, 10, 30);

-- Usuarios. Contraseña de todos: 123456 (guardada como SHA-256)
-- Rosa está desactivada para mostrar la reactivación.
INSERT INTO usuario (id_usuario, correo, nombre_completo, fecha_nacimiento, contrasena, foto, rol, activo, fecha_registro) VALUES
(1, 'admin@admin.bookshop', 'Administrador BookShop', '1990-01-15', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', NULL, 'ADMINISTRADOR', 1, '2025-01-10 09:00:00'),
(2, 'ana@correo.com',  'Ana Torres Vega',     '1996-04-12', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', NULL, 'CLIENTE', 1, '2025-02-03 10:30:00'),
(3, 'luis@correo.com', 'Luis Paredes Ríos',   '1989-09-30', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', NULL, 'CLIENTE', 1, '2025-05-21 18:10:00'),
(4, 'rosa@correo.com', 'Rosa Medina Salas',   '2000-12-02', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', NULL, 'CLIENTE', 0, '2025-08-14 12:45:00'),
(5, 'carla@correo.com',  'Carla Mendoza Ruiz',     '1994-07-21', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', NULL, 'CLIENTE', 1, '2025-03-11 09:20:00'),
(6, 'jorge@correo.com',  'Jorge Salinas Paz',      '1985-02-14', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', NULL, 'CLIENTE', 1, '2025-04-02 16:05:00'),
(7, 'lucia@correo.com',  'Lucía Romero Díaz',      '1999-11-03', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', NULL, 'CLIENTE', 1, '2025-06-18 11:40:00'),
(8, 'martin@correo.com', 'Martín Vega Soto',       '1991-05-30', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', NULL, 'CLIENTE', 1, '2025-09-09 20:15:00'),
(9, 'sofia@correo.com',  'Sofía Herrera León',     '2001-08-08', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', NULL, 'CLIENTE', 1, '2026-01-20 13:30:00'),
(10, 'andres@correo.com', 'Andrés Castillo Ugarte', '1997-03-17', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', NULL, 'CLIENTE', 1, '2026-10-01 10:00:00');

INSERT INTO direccion (fid_usuario, alias, direccion, distrito, ciudad, predeterminada, activa) VALUES
(2, 'Casa',    'Jr. Domeyer 214',            'Barranco',   'Lima', 1, 1),
(2, 'Oficina', 'Av. Javier Prado Este 1580', 'San Isidro', 'Lima', 0, 1),
(3, 'Casa',    'Av. Brasil 1250',            'Jesús María', 'Lima', 1, 1),
(4, 'Casa',    'Calle Los Pinos 340',        'Surco',       'Lima', 1, 1),
(5, 'Casa',    'Av. Arequipa 2890',          'Lince',       'Lima', 1, 1),
(6, 'Casa',    'Jr. Huallaga 512',           'Cercado',     'Lima', 1, 1),
(7, 'Casa',    'Calle Tarata 160',           'Miraflores',  'Lima', 1, 1),
(8, 'Casa',    'Av. La Marina 2355',         'San Miguel',  'Lima', 1, 1),
(9, 'Casa',    'Av. Universitaria 1801',     'San Martín de Porres', 'Lima', 1, 1);

INSERT INTO metodo_pago (fid_usuario, tipo, titular, numero, fecha_vencimiento, proveedor, predeterminado, activo) VALUES
(2, 'TARJETA',   'Ana Torres',   '4111222233334821', '08/28', NULL,   1, 1),
(2, 'BILLETERA', 'Ana Torres',   '987654321',        NULL,    'Yape', 0, 1),
(3, 'TARJETA',   'Luis Paredes', '5500111122223344', '11/27', NULL,   1, 1),
(4, 'TARJETA',   'Rosa Medina',    '4000123412341111', '03/28', NULL,   1, 1),
(5, 'TARJETA',   'Carla Mendoza',  '4000123412342222', '06/29', NULL,   1, 1),
(6, 'BILLETERA', 'Jorge Salinas',  '912345678',        NULL,    'Plin', 1, 1),
(7, 'TARJETA',   'Lucía Romero',   '4000123412343333', '01/28', NULL,   1, 1),
(8, 'BILLETERA', 'Martín Vega',    '923456789',        NULL,    'Yape', 1, 1),
(9, 'TARJETA',   'Sofía Herrera',  '4000123412344444', '10/30', NULL,   1, 1);

-- Autores
INSERT INTO entidad (id_entidad, tipo, nombre, descripcion, imagen, activo) VALUES
(1,  'AUTOR', 'Inés Valdivia',   'Escritora limeña (1978). Su obra mezcla la memoria familiar con lo fantástico, siempre en casas y barrios de la costa peruana.', 'img/autor/ines-valdivia.svg', 1),
(2,  'AUTOR', 'Tomás Arguedas',  'Narrador ayacuchano. Escribe sobre el mar, los puertos y las familias que se quedan esperando.', 'img/autor/tomas-arguedas.svg', 1),
(3,  'AUTOR', 'Lucía Ferrán',    'Autora de fantasía y novela. Sus mundos nacen de mapas imposibles y viajes interiores.', 'img/autor/lucia-ferran.svg', 1),
(4,  'AUTOR', 'Marco Salazar',   'Ensayista y profesor de filosofía. Le interesa lo cotidiano que nadie mira.', 'img/autor/marco-salazar.svg', 1),
(5,  'AUTOR', 'Ana Lucía Pardo', 'Cuentista. Retrata la noche limeña y a quienes la habitan.', 'img/autor/ana-lucia-pardo.svg', 1),
(6,  'AUTOR', 'Julián Ortega',   'Historiador. Divulga la historia andina con un estilo cercano.', 'img/autor/julian-ortega.svg', 1),
(7,  'AUTOR', 'Carmen Rivas',    'Poeta arequipeña. Ha publicado cinco poemarios sobre el desierto y la sal.', 'img/autor/carmen-rivas.svg', 1),
(8,  'AUTOR', 'Diego Montalvo',  'Novelista amazónico. Sus historias siguen el curso de los ríos.', 'img/autor/diego-montalvo.svg', 1),
(9,  'AUTOR', 'Rosa Quiroga',    'Cronista de viajes. Recorrió Sudamérica en bus durante dos años.', 'img/autor/rosa-quiroga.svg', 1),
(10, 'AUTOR', 'Hugo Benavides',  'Autor de fantasía épica y lector incansable de bibliotecas antiguas.', 'img/autor/hugo-benavides.svg', 1),
(11, 'AUTOR', 'Elena Castañeda', 'Novelista. Escribe cartas, diarios y ciudades que no duermen.', 'img/autor/elena-castaneda.svg', 1),
(12, 'AUTOR', 'Pablo Iturbe',    'Físico y divulgador científico. Explica el mundo con metáforas sencillas.', 'img/autor/pablo-iturbe.svg', 1),
(13, 'AUTOR', 'Mariana Soto',    'Historiadora de la vida cotidiana en el siglo XIX.', 'img/autor/mariana-soto.svg', 1);

-- Categorías
INSERT INTO entidad (id_entidad, tipo, nombre, descripcion, imagen, activo) VALUES
(14, 'CATEGORIA', 'Novela',    'Historias largas de ficción: de la novela histórica a la contemporánea.', 'img/categoria/novela.svg', 1),
(15, 'CATEGORIA', 'Ensayo',    'Textos de reflexión y pensamiento sobre los temas más variados.', 'img/categoria/ensayo.svg', 1),
(16, 'CATEGORIA', 'Poesía',    'Poemarios de autores clásicos y contemporáneos.', 'img/categoria/poesia.svg', 1),
(17, 'CATEGORIA', 'Cuento',    'Colecciones de relatos breves.', 'img/categoria/cuento.svg', 1),
(18, 'CATEGORIA', 'Historia',  'Libros para entender el pasado y el presente.', 'img/categoria/historia.svg', 1),
(19, 'CATEGORIA', 'Fantasía',  'Mundos imaginarios, magia y aventuras.', 'img/categoria/fantasia.svg', 1),
(20, 'CATEGORIA', 'Viajes',    'Crónicas y relatos de viaje.', 'img/categoria/viajes.svg', 1),
(21, 'CATEGORIA', 'Ciencia',   'Divulgación científica para todos.', 'img/categoria/ciencia.svg', 1),
(22, 'CATEGORIA', 'Biografía', 'Vidas contadas por sus protagonistas o por otros.', 'img/categoria/biografia.svg', 1),
(23, 'CATEGORIA', 'Infantil',  'Libros para los más pequeños.', 'img/categoria/infantil.svg', 1),
(24, 'CATEGORIA', 'Teatro',    'Obras de teatro para leer y representar.', 'img/categoria/teatro.svg', 1),
(25, 'CATEGORIA', 'Arte',      'Pintura, fotografía, arquitectura y diseño.', 'img/categoria/arte.svg', 1);

-- Editoriales
INSERT INTO entidad (id_entidad, tipo, nombre, descripcion, imagen, activo) VALUES
(26, 'EDITORIAL', 'Ediciones Faro', 'Editorial independiente fundada en 2009. Publica narrativa y ensayo latinoamericano.', 'img/editorial/ediciones-faro.svg', 1),
(27, 'EDITORIAL', 'Casa del Río',   'Sello dedicado a la literatura amazónica y de frontera.', 'img/editorial/casa-del-rio.svg', 1),
(28, 'EDITORIAL', 'Lumbre',         'Fantasía, novela y clásicos reeditados.', 'img/editorial/lumbre.svg', 1),
(29, 'EDITORIAL', 'Papel Vivo',     'Ensayo y cuento breve.', 'img/editorial/papel-vivo.svg', 1),
(30, 'EDITORIAL', 'Grulla',         'Poesía en ediciones cuidadas.', 'img/editorial/grulla.svg', 1),
(31, 'EDITORIAL', 'Altamar',        'Aventura, viajes y fantasía.', 'img/editorial/altamar.svg', 1),
(32, 'EDITORIAL', 'Orilla',         'Cuento e historia del Perú.', 'img/editorial/orilla.svg', 1),
(33, 'EDITORIAL', 'Sextante',       'Divulgación científica e histórica.', 'img/editorial/sextante.svg', 1),
(34, 'EDITORIAL', 'Bruma',          'Narrativa contemporánea.', 'img/editorial/bruma.svg', 1),
(35, 'EDITORIAL', 'Tinta Sur',      'Biografías y crónicas.', 'img/editorial/tinta-sur.svg', 1),
(36, 'EDITORIAL', 'El Telar',       'Libros infantiles y teatro.', 'img/editorial/el-telar.svg', 1),
(37, 'EDITORIAL', 'Mirador',        'Libros de arte y fotografía.', 'img/editorial/mirador.svg', 1);

-- Libros (sin portada: se muestra la portada tipográfica). Fechas de registro escalonadas en el último año.
INSERT INTO libro (id_libro, isbn, titulo, sinopsis, idioma, paginas, anio_publicacion, precio, stock, portada, activo, fecha_registro, fid_autor, fid_categoria, fid_editorial) VALUES
(1,  '978-612-4567-01-3', 'La casa de los relojes', 'En una casa de Barranco donde ningún reloj marca la misma hora, tres generaciones de mujeres guardan un secreto que solo puede contarse a medianoche.', 'Español', 312, 2024, 60.00, 12, NULL, 1, NOW() - INTERVAL 3 DAY, 1, 14, 26),
(2,  '978-612-4567-02-3', 'Mareas del sur', 'Un pescador de Pucusana espera durante años el regreso de su hermano, mientras el pueblo cambia a su alrededor.', 'Español', 268, 2023, 54.90, 7, NULL, 1, NOW() - INTERVAL 10 DAY, 2, 14, 28),
(3,  '978-612-4567-03-3', 'El cartógrafo ciego', 'Un cartógrafo que perdió la vista dibuja mapas de lugares que aún no existen, y alguien empieza a construirlos.', 'Español', 420, 2025, 69.00, 3, NULL, 1, NOW() - INTERVAL 18 DAY, 3, 19, 31),
(4,  '978-612-4567-04-3', 'Diccionario de lo invisible', 'Ensayos breves sobre las cosas que usamos a diario y nunca nombramos.', 'Español', 196, 2022, 45.00, 20, NULL, 1, NOW() - INTERVAL 25 DAY, 4, 15, 29),
(5,  '978-612-4567-05-3', 'Noches en Barranco', 'Doce cuentos que transcurren entre la medianoche y el amanecer en un barrio bohemio.', 'Español', 154, 2021, 39.90, 0, NULL, 1, NOW() - INTERVAL 33 DAY, 5, 17, 32),
(6,  '978-612-4567-06-3', 'Breve historia del silencio', 'Cómo distintas culturas andinas entendieron y usaron el silencio a lo largo de los siglos.', 'Español', 344, 2020, 72.00, 9, NULL, 1, NOW() - INTERVAL 41 DAY, 6, 18, 33),
(7,  '978-612-4567-07-3', 'El jardín de sal', 'Poemas escritos en el desierto, donde la sal florece y el agua es un recuerdo.', 'Español', 98, 2024, 35.00, 15, NULL, 1, NOW() - INTERVAL 48 DAY, 7, 16, 30),
(8,  '978-612-4567-08-3', 'Todo lo que cabe en un río', 'Una familia remonta el Ucayali para devolver algo que no les pertenece.', 'Español', 288, 2025, 58.00, 4, NULL, 1, NOW() - INTERVAL 55 DAY, 8, 14, 27),
(9,  '978-612-4567-09-3', 'Manual para perderse', 'Crónicas de viaje sin mapa ni destino por los caminos de Sudamérica.', 'Español', 210, 2023, 49.00, 11, NULL, 1, NOW() - INTERVAL 63 DAY, 9, 20, 31),
(10, '978-612-4567-10-3', 'La última biblioteca', 'Cuando los libros empiezan a desaparecer, una aprendiz de bibliotecaria debe proteger el último ejemplar de cada historia.', 'Español', 512, 2025, 64.50, 6, NULL, 1, NOW() - INTERVAL 70 DAY, 10, 19, 28),
(11, '978-612-4567-11-3', 'Cartas a una ciudad dormida', 'Una mujer escribe cartas a Lima durante un año entero, y la ciudad le responde.', 'Español', 236, 2022, 52.00, 8, NULL, 1, NOW() - INTERVAL 78 DAY, 11, 14, 26),
(12, '978-612-4567-12-3', 'Geometría de la lluvia', 'An accessible journey through the physics of water, clouds and the shape of rain.', 'Inglés', 380, 2021, 78.00, 5, NULL, 1, NOW() - INTERVAL 86 DAY, 12, 21, 33),
(13, '978-612-4567-13-3', 'Los años de ceniza', 'La vida cotidiana de Lima durante las décadas más difíciles del siglo XIX.', 'Español', 402, 2019, 66.00, 2, NULL, 1, NOW() - INTERVAL 94 DAY, 13, 18, 32),
(14, '978-612-4567-14-3', 'Un idioma para los pájaros', 'Poemas para leer en voz alta, al aire libre.', 'Español', 112, 2023, 32.00, 14, NULL, 1, NOW() - INTERVAL 101 DAY, 1, 16, 30),
(15, '978-612-4567-15-3', 'El hombre que contaba puertas', 'Cuentos sobre obsesiones pequeñas que cambian vidas enteras.', 'Español', 176, 2024, 41.00, 10, NULL, 1, NOW() - INTERVAL 109 DAY, 2, 17, 29),
(16, '978-612-4567-16-3', 'Desierto adentro', 'Una geóloga vuelve al desierto de su infancia para buscar a su padre.', 'Español', 298, 2025, 57.00, 7, NULL, 1, NOW() - INTERVAL 116 DAY, 3, 14, 27),
(17, '978-612-4567-17-3', 'El faro de los nombres', 'El guardián de un faro anota el nombre de cada barco que pasa, hasta que uno no tiene nombre.', 'Español', 254, 2024, 55.00, 9, NULL, 1, NOW() - INTERVAL 124 DAY, 2, 14, 26),
(18, '978-612-4567-18-3', 'Mapas de agua', 'Segunda parte de las crónicas del cartógrafo: ahora los mapas se dibujan sobre el mar.', 'Español', 446, 2025, 71.00, 5, NULL, 1, NOW() - INTERVAL 131 DAY, 3, 19, 31),
(19, '978-612-4567-19-3', 'Elogio de la espera', 'Ensayos sobre las colas, las salas de espera y el arte de no hacer nada.', 'Español', 168, 2023, 42.00, 13, NULL, 1, NOW() - INTERVAL 139 DAY, 4, 15, 29),
(20, '978-612-4567-20-3', 'Lima de madrugada', 'Nuevos cuentos nocturnos: taxistas, panaderos y vigilantes.', 'Español', 182, 2024, 44.00, 6, NULL, 1, NOW() - INTERVAL 147 DAY, 5, 17, 32),
(21, '978-612-4567-21-3', 'Los caminos del inca', 'Historia de la red de caminos que unió los Andes.', 'Español', 390, 2022, 79.00, 4, NULL, 1, NOW() - INTERVAL 154 DAY, 6, 18, 33),
(22, '978-612-4567-22-3', 'Sal y viento', 'Poemario sobre la costa sur y sus salinas.', 'Español', 104, 2025, 34.00, 0, NULL, 1, NOW() - INTERVAL 162 DAY, 7, 16, 30),
(23, '978-612-4567-23-3', 'La canoa de papel', 'Un niño construye una canoa con hojas de cuaderno para cruzar el río.', 'Español', 64, 2024, 29.00, 18, NULL, 1, NOW() - INTERVAL 170 DAY, 8, 23, 36),
(24, '978-612-4567-24-3', 'Trenes del altiplano', 'Crónica de un viaje en tren de Cusco a Puno, con escalas inesperadas.', 'Español', 224, 2022, 47.00, 7, NULL, 1, NOW() - INTERVAL 178 DAY, 9, 20, 31),
(25, '978-612-4567-25-3', 'El guardián de los índices', 'Precuela de La última biblioteca: el primer bibliotecario y el primer libro perdido.', 'Español', 476, 2023, 62.00, 8, NULL, 1, NOW() - INTERVAL 186 DAY, 10, 19, 28),
(26, '978-612-4567-26-3', 'Diario de una calle', 'Una calle de Lima cuenta su propia historia durante cien años.', 'Español', 270, 2021, 50.00, 5, NULL, 1, NOW() - INTERVAL 194 DAY, 11, 14, 34),
(27, '978-612-4567-27-3', 'The Shape of Time', 'Clocks, calendars and the physics behind measuring time.', 'Inglés', 310, 2023, 82.00, 6, NULL, 1, NOW() - INTERVAL 202 DAY, 12, 21, 33),
(28, '978-612-4567-28-3', 'Retrato de una viajera', 'Biografía de Flora Tristán contada a partir de sus cartas.', 'Español', 336, 2022, 68.00, 4, NULL, 1, NOW() - INTERVAL 210 DAY, 13, 22, 35),
(29, '978-612-4567-29-3', 'El teatro de las sombras', 'Tres obras breves para teatro de títeres.', 'Español', 120, 2024, 36.00, 9, NULL, 1, NOW() - INTERVAL 218 DAY, 5, 24, 36),
(30, '978-612-4567-30-3', 'Colores del desierto', 'Fotografía y pintura inspiradas en el desierto de Nazca.', 'Español', 180, 2023, 95.00, 3, NULL, 1, NOW() - INTERVAL 226 DAY, 7, 25, 37),
(31, '978-612-4567-31-3', 'Memorias de un cartero', 'La vida de un cartero rural de Cajamarca, contada por él mismo.', 'Español', 240, 2020, 46.00, 6, NULL, 1, NOW() - INTERVAL 234 DAY, 6, 22, 35),
(32, '978-612-4567-32-3', 'Cuentos para antes de dormir', 'Diez cuentos tranquilos para leer en familia.', 'Español', 88, 2025, 31.00, 20, NULL, 1, NOW() - INTERVAL 242 DAY, 1, 23, 36),
(33, '978-612-4567-33-3', 'La ciudad de los espejos', 'En una ciudad donde cada espejo muestra otra vida, una joven busca la suya.', 'Español', 360, 2024, 59.00, 0, NULL, 1, NOW() - INTERVAL 250 DAY, 10, 19, 28),
(34, '978-612-4567-34-3', 'Pequeña historia del café', 'Cómo una semilla cambió la economía y las costumbres del Perú.', 'Español', 208, 2023, 48.00, 10, NULL, 1, NOW() - INTERVAL 258 DAY, 4, 18, 33),
(35, '978-612-4567-35-3', 'Islas flotantes', 'Crónicas del lago Titicaca y sus habitantes.', 'Español', 196, 2021, 43.00, 7, NULL, 1, NOW() - INTERVAL 266 DAY, 9, 20, 34),
(36, '978-612-4567-36-3', 'El último verano', 'Cuatro amigos pasan su último verano juntos antes de separarse.', 'Español', 248, 2025, 53.00, 9, NULL, 1, NOW() - INTERVAL 274 DAY, 11, 14, 34),
(37, '978-612-4567-37-3', 'Arquitectura de barro', 'Las construcciones de adobe del Perú, en fotografías y planos.', 'Español', 220, 2022, 88.00, 2, NULL, 1, NOW() - INTERVAL 282 DAY, 12, 25, 37),
(38, '978-612-4567-38-3', 'Voces del puerto', 'Monólogos teatrales de los trabajadores del Callao.', 'Español', 132, 2023, 38.00, 5, NULL, 1, NOW() - INTERVAL 290 DAY, 2, 24, 36),
(39, '978-612-4567-39-3', 'Selva de palabras', 'Mitos y leyendas amazónicas recogidas en diez comunidades.', 'Español', 274, 2024, 51.00, 8, NULL, 1, NOW() - INTERVAL 298 DAY, 8, 17, 27),
(40, '978-612-4567-40-3', 'Un año sin relojes', 'Novela descatalogada: se conserva desactivada para mostrar los libros inactivos.', 'Español', 230, 2018, 40.00, 3, NULL, 0, NOW() - INTERVAL 330 DAY, 1, 14, 26);

-- Descuentos: vigentes, programados y finalizados, relativos a la fecha de carga
INSERT INTO descuento (fid_libro, porcentaje, fecha_inicio, fecha_fin) VALUES
(1,  20, CURDATE() - INTERVAL 5 DAY,   CURDATE() + INTERVAL 10 DAY),
(1,  10, CURDATE() + INTERVAL 15 DAY,  CURDATE() + INTERVAL 25 DAY),
(1,  15, CURDATE() + INTERVAL 30 DAY,  CURDATE() + INTERVAL 40 DAY),
(1,  15, CURDATE() - INTERVAL 45 DAY,  CURDATE() - INTERVAL 30 DAY),
(1,  25, CURDATE() - INTERVAL 80 DAY,  CURDATE() - INTERVAL 70 DAY),
(1,  10, CURDATE() - INTERVAL 100 DAY, CURDATE() - INTERVAL 90 DAY),
(1,  20, CURDATE() - INTERVAL 140 DAY, CURDATE() - INTERVAL 125 DAY),
(4,  15, CURDATE() - INTERVAL 2 DAY,   CURDATE() + INTERVAL 20 DAY),
(7,  10, CURDATE() - INTERVAL 1 DAY,   CURDATE() + INTERVAL 14 DAY),
(10, 25, CURDATE() - INTERVAL 7 DAY,   CURDATE() + INTERVAL 7 DAY),
(15, 20, CURDATE(),                    CURDATE() + INTERVAL 30 DAY),
(21, 10, CURDATE() + INTERVAL 5 DAY,   CURDATE() + INTERVAL 20 DAY),
(30, 30, CURDATE() - INTERVAL 3 DAY,   CURDATE() + INTERVAL 12 DAY);

-- Historial de pedidos, relativo a la fecha de carga. Cada pedido salió del almacén a los 2 minutos y se entregó a los 4.
-- Ana tiene 8 pedidos que ya cuentan para cupón: recibió un cupón con el 3.º (BK3M8, usado en el N.º 1004)
-- y otro con el 6.º (BK7Q2, utilizable). Con un pedido más que cuente, recibirá el siguiente.
-- Ana: N.º 1006 devuelto, N.º 1009 con una devolución rechazada, N.º 1010 dentro del plazo de devolución y N.º 1011 cancelado este mes.
-- Luis: N.º 1013 con una devolución pendiente de revisión.
INSERT INTO cupon (id_cupon, fid_usuario, codigo, porcentaje, fecha_emision, fecha_vencimiento, activo) VALUES
(1, 2, 'BK3M8', 10, CURDATE() - INTERVAL 93 DAY, CURDATE() - INTERVAL 63 DAY, 0),
(2, 2, 'BK7Q2', 10, CURDATE() - INTERVAL 13 DAY, CURDATE() + INTERVAL 17 DAY, 1);

INSERT INTO pedido (id_pedido, fid_usuario, fid_direccion, fid_metodo_pago, fid_cupon, estado, fecha_compra, fecha_salida, fecha_entrega,
                    fecha_cancelacion, fecha_devolucion, codigo_transaccion, codigo_seguimiento, subtotal, descuento_cupon, costo_envio, total, contado_para_cupon) VALUES
(1001, 2, 1, 1, NULL, 'ENTREGADO', NOW() - INTERVAL 120 DAY, NOW() - INTERVAL 120 DAY + INTERVAL 2 MINUTE, NOW() - INTERVAL 120 DAY + INTERVAL 4 MINUTE,
 NULL, NULL, CONCAT('TX-', DATE_FORMAT(NOW() - INTERVAL 120 DAY, '%Y%m%d'), '-4F1A'), 'SG-K2M8-7QX1', 121.00, 0.00, 10.00, 131.00, 1),
(1002, 2, 1, 2, NULL, 'ENTREGADO', NOW() - INTERVAL 110 DAY, NOW() - INTERVAL 110 DAY + INTERVAL 2 MINUTE, NOW() - INTERVAL 110 DAY + INTERVAL 4 MINUTE,
 NULL, NULL, CONCAT('TX-', DATE_FORMAT(NOW() - INTERVAL 110 DAY, '%Y%m%d'), '-9B07'), 'SG-P4T1-3WZ9', 111.00, 0.00, 10.00, 121.00, 1),
(1003, 2, 2, 1, NULL, 'ENTREGADO', NOW() - INTERVAL 100 DAY, NOW() - INTERVAL 100 DAY + INTERVAL 2 MINUTE, NOW() - INTERVAL 100 DAY + INTERVAL 4 MINUTE,
 NULL, NULL, CONCAT('TX-', DATE_FORMAT(NOW() - INTERVAL 100 DAY, '%Y%m%d'), '-C3E5'), 'SG-H7N2-8KD4', 109.80, 0.00, 10.00, 119.80, 1),
(1004, 2, 1, 1, 1,    'ENTREGADO', NOW() - INTERVAL 80 DAY,  NOW() - INTERVAL 80 DAY + INTERVAL 2 MINUTE,  NOW() - INTERVAL 80 DAY + INTERVAL 4 MINUTE,
 NULL, NULL, CONCAT('TX-', DATE_FORMAT(NOW() - INTERVAL 80 DAY, '%Y%m%d'), '-71AD'), 'SG-R9F3-2LM6', 104.00, 10.40, 10.00, 103.60, 1),
(1005, 2, 1, 1, NULL, 'ENTREGADO', NOW() - INTERVAL 60 DAY,  NOW() - INTERVAL 60 DAY + INTERVAL 2 MINUTE,  NOW() - INTERVAL 60 DAY + INTERVAL 4 MINUTE,
 NULL, NULL, CONCAT('TX-', DATE_FORMAT(NOW() - INTERVAL 60 DAY, '%Y%m%d'), '-E28B'), 'SG-W5Y8-1QC7', 116.00, 0.00, 10.00, 126.00, 1),
(1006, 2, 1, 2, NULL, 'DEVUELTO',  NOW() - INTERVAL 45 DAY,  NOW() - INTERVAL 45 DAY + INTERVAL 2 MINUTE,  NOW() - INTERVAL 45 DAY + INTERVAL 4 MINUTE,
 NULL, NOW() - INTERVAL 41 DAY, CONCAT('TX-', DATE_FORMAT(NOW() - INTERVAL 45 DAY, '%Y%m%d'), '-5D90'), 'SG-B3V6-9TN2', 93.00, 0.00, 10.00, 103.00, 0),
(1007, 2, 1, 1, NULL, 'ENTREGADO', NOW() - INTERVAL 20 DAY,  NOW() - INTERVAL 20 DAY + INTERVAL 2 MINUTE,  NOW() - INTERVAL 20 DAY + INTERVAL 4 MINUTE,
 NULL, NULL, CONCAT('TX-', DATE_FORMAT(NOW() - INTERVAL 20 DAY, '%Y%m%d'), '-A64F'), 'SG-M1K7-4ZR3', 119.50, 0.00, 10.00, 129.50, 1),
(1008, 2, 2, 1, NULL, 'ENTREGADO', NOW() - INTERVAL 15 DAY,  NOW() - INTERVAL 15 DAY + INTERVAL 2 MINUTE,  NOW() - INTERVAL 15 DAY + INTERVAL 4 MINUTE,
 NULL, NULL, CONCAT('TX-', DATE_FORMAT(NOW() - INTERVAL 15 DAY, '%Y%m%d'), '-3C1E'), 'SG-T6D2-7PH5', 123.00, 0.00, 10.00, 133.00, 1),
(1009, 2, 1, 1, NULL, 'ENTREGADO', NOW() - INTERVAL 10 DAY,  NOW() - INTERVAL 10 DAY + INTERVAL 2 MINUTE,  NOW() - INTERVAL 10 DAY + INTERVAL 4 MINUTE,
 NULL, NULL, CONCAT('TX-', DATE_FORMAT(NOW() - INTERVAL 10 DAY, '%Y%m%d'), '-F8B2'), 'SG-Z4Q9-6JS1', 107.00, 0.00, 10.00, 117.00, 1),
(1010, 2, 1, 1, NULL, 'ENTREGADO', NOW() - INTERVAL 3 DAY,   NOW() - INTERVAL 3 DAY + INTERVAL 2 MINUTE,   NOW() - INTERVAL 3 DAY + INTERVAL 4 MINUTE,
 NULL, NULL, CONCAT('TX-', DATE_FORMAT(NOW() - INTERVAL 3 DAY, '%Y%m%d'), '-0D7C'), 'SG-C8L5-3NA6', 105.00, 0.00, 10.00, 115.00, 0),
(1011, 2, 1, 2, NULL, 'CANCELADO', NOW() - INTERVAL 1 DAY,   NULL, NULL,
 NOW() - INTERVAL 1 DAY + INTERVAL 1 MINUTE, NULL, CONCAT('TX-', DATE_FORMAT(NOW() - INTERVAL 1 DAY, '%Y%m%d'), '-96E3'), 'SG-G2X7-5VB8', 41.00, 0.00, 10.00, 51.00, 0),
(1012, 3, 3, 3, NULL, 'ENTREGADO', NOW() - INTERVAL 30 DAY,  NOW() - INTERVAL 30 DAY + INTERVAL 2 MINUTE,  NOW() - INTERVAL 30 DAY + INTERVAL 4 MINUTE,
 NULL, NULL, CONCAT('TX-', DATE_FORMAT(NOW() - INTERVAL 30 DAY, '%Y%m%d'), '-2A5F'), 'SG-N9R4-8EW2', 112.00, 0.00, 10.00, 122.00, 1),
(1013, 3, 3, 3, NULL, 'ENTREGADO', NOW() - INTERVAL 2 DAY,   NOW() - INTERVAL 2 DAY + INTERVAL 2 MINUTE,   NOW() - INTERVAL 2 DAY + INTERVAL 4 MINUTE,
 NULL, NULL, CONCAT('TX-', DATE_FORMAT(NOW() - INTERVAL 2 DAY, '%Y%m%d'), '-B71D'), 'SG-F3H6-1YK9', 116.00, 0.00, 10.00, 126.00, 0);

-- El N.º 1006 llevó «La casa de los relojes» con su descuento del 15 % de ese momento
INSERT INTO detalle_pedido (fid_pedido, fid_libro, cantidad, precio_unitario, descuento_unitario, subtotal) VALUES
(1001, 6,  1, 72.00, 0.00, 72.00), (1001, 9,  1, 49.00, 0.00, 49.00),
(1002, 13, 1, 66.00, 0.00, 66.00), (1002, 4,  1, 45.00, 0.00, 45.00),
(1003, 2,  2, 54.90, 0.00, 109.80),
(1004, 3,  1, 69.00, 0.00, 69.00), (1004, 7,  1, 35.00, 0.00, 35.00),
(1005, 11, 1, 52.00, 0.00, 52.00), (1005, 14, 2, 32.00, 0.00, 64.00),
(1006, 1,  1, 60.00, 9.00, 51.00), (1006, 19, 1, 42.00, 0.00, 42.00),
(1007, 10, 1, 64.50, 0.00, 64.50), (1007, 17, 1, 55.00, 0.00, 55.00),
(1008, 21, 1, 79.00, 0.00, 79.00), (1008, 20, 1, 44.00, 0.00, 44.00),
(1009, 12, 1, 78.00, 0.00, 78.00), (1009, 23, 1, 29.00, 0.00, 29.00),
(1010, 8,  1, 58.00, 0.00, 58.00), (1010, 24, 1, 47.00, 0.00, 47.00),
(1011, 15, 1, 41.00, 0.00, 41.00),
(1012, 25, 1, 62.00, 0.00, 62.00), (1012, 26, 1, 50.00, 0.00, 50.00),
(1013, 28, 1, 68.00, 0.00, 68.00), (1013, 34, 1, 48.00, 0.00, 48.00);

INSERT INTO solicitud_devolucion (fid_pedido, motivo, comentario, estado, fecha) VALUES
(1006, 'PRODUCTO_DANADO',     'El libro llegó con la tapa doblada y varias páginas manchadas.', 'ACEPTADA',  NOW() - INTERVAL 43 DAY),
(1009, 'YA_NO_LO_QUIERO',     'Me regalaron el mismo libro antes de que llegara el pedido.',    'RECHAZADA', NOW() - INTERVAL 8 DAY),
(1013, 'PRODUCTO_EQUIVOCADO', 'Recibí otra edición distinta a la que pedí.',                     'PENDIENTE', NOW() - INTERVAL 1 DAY);

-- Historial de otros clientes, para que las recomendaciones y los reportes muestren resultados.
-- Cada uno tiene sus gustos (fantasía, historia, poesía, viajes) y comparten libros con Ana.
-- Los importes se calculan al final con los precios de los libros. Ninguno está contado para cupón:
-- al arrancar, el hilo cuenta los que corresponden y emite los cupones (RN10).
INSERT INTO pedido (id_pedido, fid_usuario, fid_direccion, fid_metodo_pago, fid_cupon, estado, fecha_compra, fecha_salida, fecha_entrega,
                    fecha_cancelacion, fecha_devolucion, codigo_transaccion, codigo_seguimiento, subtotal, descuento_cupon, costo_envio, total, contado_para_cupon) VALUES
(1014, 5, 5, 5, NULL, 'ENTREGADO', NOW() - INTERVAL 80 DAY, NOW() - INTERVAL 80 DAY + INTERVAL 2 MINUTE, NOW() - INTERVAL 80 DAY + INTERVAL 4 MINUTE, NULL, NULL, CONCAT('TX-', DATE_FORMAT(NOW() - INTERVAL 80 DAY, '%Y%m%d'), '-5A14'), 'SG-C5A1-1014', 0, 0, 10.00, 0, 0),
(1015, 5, 5, 5, NULL, 'ENTREGADO', NOW() - INTERVAL 50 DAY, NOW() - INTERVAL 50 DAY + INTERVAL 2 MINUTE, NOW() - INTERVAL 50 DAY + INTERVAL 4 MINUTE, NULL, NULL, CONCAT('TX-', DATE_FORMAT(NOW() - INTERVAL 50 DAY, '%Y%m%d'), '-5A15'), 'SG-C5A2-1015', 0, 0, 10.00, 0, 0),
(1016, 5, 5, 5, NULL, 'ENTREGADO', NOW() - INTERVAL 25 DAY, NOW() - INTERVAL 25 DAY + INTERVAL 2 MINUTE, NOW() - INTERVAL 25 DAY + INTERVAL 4 MINUTE, NULL, NULL, CONCAT('TX-', DATE_FORMAT(NOW() - INTERVAL 25 DAY, '%Y%m%d'), '-5A16'), 'SG-C5A3-1016', 0, 0, 10.00, 0, 0),
(1017, 5, 5, 5, NULL, 'ENTREGADO', NOW() - INTERVAL 8 DAY,  NOW() - INTERVAL 8 DAY + INTERVAL 2 MINUTE,  NOW() - INTERVAL 8 DAY + INTERVAL 4 MINUTE,  NULL, NULL, CONCAT('TX-', DATE_FORMAT(NOW() - INTERVAL 8 DAY, '%Y%m%d'), '-5A17'),  'SG-C5A4-1017', 0, 0, 10.00, 0, 0),
(1018, 6, 6, 6, NULL, 'ENTREGADO', NOW() - INTERVAL 90 DAY, NOW() - INTERVAL 90 DAY + INTERVAL 2 MINUTE, NOW() - INTERVAL 90 DAY + INTERVAL 4 MINUTE, NULL, NULL, CONCAT('TX-', DATE_FORMAT(NOW() - INTERVAL 90 DAY, '%Y%m%d'), '-6B18'), 'SG-J6B1-1018', 0, 0, 10.00, 0, 0),
(1019, 6, 6, 6, NULL, 'ENTREGADO', NOW() - INTERVAL 60 DAY, NOW() - INTERVAL 60 DAY + INTERVAL 2 MINUTE, NOW() - INTERVAL 60 DAY + INTERVAL 4 MINUTE, NULL, NULL, CONCAT('TX-', DATE_FORMAT(NOW() - INTERVAL 60 DAY, '%Y%m%d'), '-6B19'), 'SG-J6B2-1019', 0, 0, 10.00, 0, 0),
(1020, 6, 6, 6, NULL, 'ENTREGADO', NOW() - INTERVAL 35 DAY, NOW() - INTERVAL 35 DAY + INTERVAL 2 MINUTE, NOW() - INTERVAL 35 DAY + INTERVAL 4 MINUTE, NULL, NULL, CONCAT('TX-', DATE_FORMAT(NOW() - INTERVAL 35 DAY, '%Y%m%d'), '-6B20'), 'SG-J6B3-1020', 0, 0, 10.00, 0, 0),
(1021, 6, 6, 6, NULL, 'ENTREGADO', NOW() - INTERVAL 12 DAY, NOW() - INTERVAL 12 DAY + INTERVAL 2 MINUTE, NOW() - INTERVAL 12 DAY + INTERVAL 4 MINUTE, NULL, NULL, CONCAT('TX-', DATE_FORMAT(NOW() - INTERVAL 12 DAY, '%Y%m%d'), '-6B21'), 'SG-J6B4-1021', 0, 0, 10.00, 0, 0),
(1022, 6, 6, 6, NULL, 'CANCELADO', NOW() - INTERVAL 20 DAY, NULL, NULL, NOW() - INTERVAL 20 DAY + INTERVAL 1 MINUTE, NULL, CONCAT('TX-', DATE_FORMAT(NOW() - INTERVAL 20 DAY, '%Y%m%d'), '-6B22'), 'SG-J6B5-1022', 0, 0, 10.00, 0, 0),
(1023, 7, 7, 7, NULL, 'ENTREGADO', NOW() - INTERVAL 70 DAY, NOW() - INTERVAL 70 DAY + INTERVAL 2 MINUTE, NOW() - INTERVAL 70 DAY + INTERVAL 4 MINUTE, NULL, NULL, CONCAT('TX-', DATE_FORMAT(NOW() - INTERVAL 70 DAY, '%Y%m%d'), '-7C23'), 'SG-L7C1-1023', 0, 0, 10.00, 0, 0),
(1024, 7, 7, 7, NULL, 'ENTREGADO', NOW() - INTERVAL 45 DAY, NOW() - INTERVAL 45 DAY + INTERVAL 2 MINUTE, NOW() - INTERVAL 45 DAY + INTERVAL 4 MINUTE, NULL, NULL, CONCAT('TX-', DATE_FORMAT(NOW() - INTERVAL 45 DAY, '%Y%m%d'), '-7C24'), 'SG-L7C2-1024', 0, 0, 10.00, 0, 0),
(1025, 7, 7, 7, NULL, 'ENTREGADO', NOW() - INTERVAL 20 DAY, NOW() - INTERVAL 20 DAY + INTERVAL 2 MINUTE, NOW() - INTERVAL 20 DAY + INTERVAL 4 MINUTE, NULL, NULL, CONCAT('TX-', DATE_FORMAT(NOW() - INTERVAL 20 DAY, '%Y%m%d'), '-7C25'), 'SG-L7C3-1025', 0, 0, 10.00, 0, 0),
(1026, 7, 7, 7, NULL, 'ENTREGADO', NOW() - INTERVAL 6 DAY,  NOW() - INTERVAL 6 DAY + INTERVAL 2 MINUTE,  NOW() - INTERVAL 6 DAY + INTERVAL 4 MINUTE,  NULL, NULL, CONCAT('TX-', DATE_FORMAT(NOW() - INTERVAL 6 DAY, '%Y%m%d'), '-7C26'),  'SG-L7C4-1026', 0, 0, 10.00, 0, 0),
(1027, 8, 8, 8, NULL, 'ENTREGADO', NOW() - INTERVAL 75 DAY, NOW() - INTERVAL 75 DAY + INTERVAL 2 MINUTE, NOW() - INTERVAL 75 DAY + INTERVAL 4 MINUTE, NULL, NULL, CONCAT('TX-', DATE_FORMAT(NOW() - INTERVAL 75 DAY, '%Y%m%d'), '-8D27'), 'SG-M8D1-1027', 0, 0, 10.00, 0, 0),
(1028, 8, 8, 8, NULL, 'ENTREGADO', NOW() - INTERVAL 40 DAY, NOW() - INTERVAL 40 DAY + INTERVAL 2 MINUTE, NOW() - INTERVAL 40 DAY + INTERVAL 4 MINUTE, NULL, NULL, CONCAT('TX-', DATE_FORMAT(NOW() - INTERVAL 40 DAY, '%Y%m%d'), '-8D28'), 'SG-M8D2-1028', 0, 0, 10.00, 0, 0),
(1029, 8, 8, 8, NULL, 'ENTREGADO', NOW() - INTERVAL 18 DAY, NOW() - INTERVAL 18 DAY + INTERVAL 2 MINUTE, NOW() - INTERVAL 18 DAY + INTERVAL 4 MINUTE, NULL, NULL, CONCAT('TX-', DATE_FORMAT(NOW() - INTERVAL 18 DAY, '%Y%m%d'), '-8D29'), 'SG-M8D3-1029', 0, 0, 10.00, 0, 0),
(1030, 8, 8, 8, NULL, 'ENTREGADO', NOW() - INTERVAL 4 DAY,  NOW() - INTERVAL 4 DAY + INTERVAL 2 MINUTE,  NOW() - INTERVAL 4 DAY + INTERVAL 4 MINUTE,  NULL, NULL, CONCAT('TX-', DATE_FORMAT(NOW() - INTERVAL 4 DAY, '%Y%m%d'), '-8D30'),  'SG-M8D4-1030', 0, 0, 10.00, 0, 0),
(1031, 9, 9, 9, NULL, 'ENTREGADO', NOW() - INTERVAL 55 DAY, NOW() - INTERVAL 55 DAY + INTERVAL 2 MINUTE, NOW() - INTERVAL 55 DAY + INTERVAL 4 MINUTE, NULL, NULL, CONCAT('TX-', DATE_FORMAT(NOW() - INTERVAL 55 DAY, '%Y%m%d'), '-9E31'), 'SG-S9E1-1031', 0, 0, 10.00, 0, 0),
(1032, 9, 9, 9, NULL, 'ENTREGADO', NOW() - INTERVAL 30 DAY, NOW() - INTERVAL 30 DAY + INTERVAL 2 MINUTE, NOW() - INTERVAL 30 DAY + INTERVAL 4 MINUTE, NULL, NULL, CONCAT('TX-', DATE_FORMAT(NOW() - INTERVAL 30 DAY, '%Y%m%d'), '-9E32'), 'SG-S9E2-1032', 0, 0, 10.00, 0, 0),
(1033, 9, 9, 9, NULL, 'ENTREGADO', NOW() - INTERVAL 10 DAY, NOW() - INTERVAL 10 DAY + INTERVAL 2 MINUTE, NOW() - INTERVAL 10 DAY + INTERVAL 4 MINUTE, NULL, NULL, CONCAT('TX-', DATE_FORMAT(NOW() - INTERVAL 10 DAY, '%Y%m%d'), '-9E33'), 'SG-S9E3-1033', 0, 0, 10.00, 0, 0),
(1034, 9, 9, 9, NULL, 'ENTREGADO', NOW() - INTERVAL 2 DAY,  NOW() - INTERVAL 2 DAY + INTERVAL 2 MINUTE,  NOW() - INTERVAL 2 DAY + INTERVAL 4 MINUTE,  NULL, NULL, CONCAT('TX-', DATE_FORMAT(NOW() - INTERVAL 2 DAY, '%Y%m%d'), '-9E34'),  'SG-S9E4-1034', 0, 0, 10.00, 0, 0),
(1035, 4, 4, 4, NULL, 'ENTREGADO', NOW() - INTERVAL 150 DAY, NOW() - INTERVAL 150 DAY + INTERVAL 2 MINUTE, NOW() - INTERVAL 150 DAY + INTERVAL 4 MINUTE, NULL, NULL, CONCAT('TX-', DATE_FORMAT(NOW() - INTERVAL 150 DAY, '%Y%m%d'), '-4F35'), 'SG-R4F1-1035', 0, 0, 10.00, 0, 0);

-- Una unidad de cada libro, a su precio actual y sin descuento
INSERT INTO detalle_pedido (fid_pedido, fid_libro, cantidad, precio_unitario, descuento_unitario, subtotal)
SELECT p.id, l.id_libro, 1, l.precio, 0, l.precio
FROM (SELECT 1014 AS id, 3 AS libro UNION ALL SELECT 1014, 10 UNION ALL SELECT 1015, 18 UNION ALL SELECT 1015, 25
      UNION ALL SELECT 1016, 1 UNION ALL SELECT 1016, 16 UNION ALL SELECT 1017, 36 UNION ALL SELECT 1017, 2
      UNION ALL SELECT 1018, 6 UNION ALL SELECT 1018, 21 UNION ALL SELECT 1019, 13 UNION ALL SELECT 1019, 34
      UNION ALL SELECT 1020, 12 UNION ALL SELECT 1020, 27 UNION ALL SELECT 1021, 4 UNION ALL SELECT 1021, 19
      UNION ALL SELECT 1022, 37 UNION ALL SELECT 1023, 7 UNION ALL SELECT 1023, 14 UNION ALL SELECT 1024, 15
      UNION ALL SELECT 1024, 20 UNION ALL SELECT 1025, 5 UNION ALL SELECT 1025, 29 UNION ALL SELECT 1026, 32
      UNION ALL SELECT 1026, 23 UNION ALL SELECT 1027, 1 UNION ALL SELECT 1027, 2 UNION ALL SELECT 1028, 3
      UNION ALL SELECT 1028, 18 UNION ALL SELECT 1029, 11 UNION ALL SELECT 1029, 17 UNION ALL SELECT 1030, 8
      UNION ALL SELECT 1030, 26 UNION ALL SELECT 1031, 9 UNION ALL SELECT 1031, 24 UNION ALL SELECT 1032, 35
      UNION ALL SELECT 1032, 1 UNION ALL SELECT 1033, 2 UNION ALL SELECT 1033, 11 UNION ALL SELECT 1034, 16
      UNION ALL SELECT 1035, 28 UNION ALL SELECT 1035, 31) p
JOIN libro l ON l.id_libro = p.libro;

UPDATE pedido p
JOIN (SELECT fid_pedido, SUM(subtotal) AS suma FROM detalle_pedido GROUP BY fid_pedido) d ON d.fid_pedido = p.id_pedido
SET p.subtotal = d.suma, p.total = d.suma + p.costo_envio
WHERE p.id_pedido >= 1014;

-- Reseñas de libros entregados. La 27 está oculta por moderación (su reporte fue aceptado).
INSERT INTO resena (id_resena, fid_usuario, fid_libro, puntuacion, comentario, fecha, activa) VALUES
(1,  2, 2,  5, 'Una novela luminosa sobre la costa y la familia. El último capítulo es perfecto.', NOW() - INTERVAL 95 DAY, 1),
(2,  2, 3,  4, 'Mapas imposibles y una prosa muy cuidada. El ritmo decae un poco a la mitad.', NOW() - INTERVAL 70 DAY, 1),
(3,  2, 6,  4, 'Divulgación cercana y bien documentada. Se lee como una novela.', NOW() - INTERVAL 110 DAY, 1),
(4,  2, 13, 3, 'Interesante, aunque esperaba más profundidad en los capítulos finales.', NOW() - INTERVAL 100 DAY, 1),
(5,  2, 10, 5, 'La mejor fantasía que leí este año. La biblioteca es un personaje más.', NOW() - INTERVAL 15 DAY, 1),
(6,  3, 25, 4, 'Buena precuela: explica muy bien el origen de la biblioteca.', NOW() - INTERVAL 25 DAY, 1),
(7,  3, 26, 3, 'La idea es original, pero algunos capítulos se repiten.', NOW() - INTERVAL 24 DAY, 1),
(8,  5, 3,  5, 'Me atrapó desde el primer capítulo. Quiero leer la segunda parte ya.', NOW() - INTERVAL 75 DAY, 1),
(9,  5, 10, 5, 'Emocionante y tierna a la vez. Ideal para quien ama los libros.', NOW() - INTERVAL 74 DAY, 1),
(10, 5, 18, 4, 'Continúa bien la historia del cartógrafo, aunque el final queda abierto.', NOW() - INTERVAL 45 DAY, 1),
(11, 5, 1,  5, 'Una casa llena de secretos. La estructura por habitaciones funciona muy bien.', NOW() - INTERVAL 20 DAY, 1),
(12, 5, 16, 4, 'Muy bien escrita: el desierto se siente en cada página.', NOW() - INTERVAL 19 DAY, 1),
(13, 6, 6,  5, 'Un ensayo histórico excelente sobre un tema poco tratado.', NOW() - INTERVAL 85 DAY, 1),
(14, 6, 21, 4, 'Mucha información y buenos mapas. Algo denso en la segunda mitad.', NOW() - INTERVAL 84 DAY, 1),
(15, 6, 13, 4, 'Retrata muy bien la vida cotidiana de la época.', NOW() - INTERVAL 55 DAY, 1),
(16, 6, 12, 5, 'Explica la física del agua con metáforas sencillas. Muy recomendable.', NOW() - INTERVAL 30 DAY, 1),
(17, 6, 27, 3, 'Interesante, aunque algunos capítulos son demasiado técnicos.', NOW() - INTERVAL 29 DAY, 1),
(18, 7, 7,  5, 'Poemas breves y precisos. Algunos se quedan contigo varios días.', NOW() - INTERVAL 65 DAY, 1),
(19, 7, 14, 4, 'Bonitos para leer en voz alta. La edición es muy cuidada.', NOW() - INTERVAL 64 DAY, 1),
(20, 7, 15, 3, 'Cuentos entretenidos, aunque esperaba finales más sorprendentes.', NOW() - INTERVAL 40 DAY, 1),
(21, 7, 20, 4, 'La noche limeña contada con mucho cariño.', NOW() - INTERVAL 39 DAY, 1),
(22, 8, 1,  2, 'Aburrido. La autora no sabe escribir y se nota en cada página.', NOW() - INTERVAL 70 DAY, 1),
(23, 8, 2,  5, 'Lo terminé en dos noches. Los personajes se sienten reales.', NOW() - INTERVAL 69 DAY, 1),
(24, 8, 3,  3, 'No me gustó el final: el cartógrafo descubre que los mapas eran de su hermano.', NOW() - INTERVAL 35 DAY, 1),
(25, 8, 18, 5, 'Mejor que la primera parte. El mar le sienta bien a la historia.', NOW() - INTERVAL 34 DAY, 1),
(26, 8, 11, 4, 'Las cartas a Lima son preciosas. Un libro para releer.', NOW() - INTERVAL 15 DAY, 1),
(27, 8, 17, 1, 'No compren aquí, mejor visiten tiendalibros-barata.xyz donde todo está a mitad de precio.', NOW() - INTERVAL 14 DAY, 0),
(28, 9, 9,  5, 'Dan ganas de tomar un bus y perderse por Sudamérica.', NOW() - INTERVAL 50 DAY, 1),
(29, 9, 24, 4, 'Crónicas muy amenas. El tramo hacia Puno es mi favorito.', NOW() - INTERVAL 49 DAY, 1),
(30, 9, 35, 4, 'Una mirada respetuosa a las islas y a su gente.', NOW() - INTERVAL 25 DAY, 1),
(31, 9, 2,  4, 'Una historia de espera que conmueve. Muy recomendable.', NOW() - INTERVAL 8 DAY, 1);

INSERT INTO voto_util (fid_usuario, fid_resena) VALUES
(5, 1), (8, 1), (9, 1), (6, 2), (7, 2), (2, 8), (8, 8), (9, 8), (3, 9), (2, 11),
(9, 11), (2, 13), (5, 13), (2, 16), (9, 23), (5, 23), (2, 28), (5, 10);

-- Reportes: dos pendientes para la moderación, uno aceptado (la reseña 27 quedó oculta) y uno rechazado
INSERT INTO reporte_resena (fid_usuario, fid_resena, motivo, comentario, contenido_reportado, puntuacion_reportada, estado, fecha) VALUES
(2, 27, 'SPAM',     'La reseña solo tiene un enlace a otra tienda.', 'No compren aquí, mejor visiten tiendalibros-barata.xyz donde todo está a mitad de precio.', 1, 'ACEPTADA', NOW() - INTERVAL 13 DAY),
(2, 24, 'OTRO',     'Cuenta el final sin avisar.', 'No me gustó el final: el cartógrafo descubre que los mapas eran de su hermano.', 3, 'RECHAZADA', NOW() - INTERVAL 30 DAY),
(9, 22, 'OFENSIVA', 'Ataca a la autora en lugar de opinar sobre el libro.', 'Aburrido. La autora no sabe escribir y se nota en cada página.', 2, 'PENDIENTE', NOW() - INTERVAL 5 DAY),
(3, 24, 'OTRO',     'Revela el final del libro.', 'No me gustó el final: el cartógrafo descubre que los mapas eran de su hermano.', 3, 'PENDIENTE', NOW() - INTERVAL 3 DAY);

-- Seguimientos y listas de Ana
INSERT INTO seguimiento_entidad (fid_usuario, fid_entidad, fecha) VALUES
(2, 1,  NOW() - INTERVAL 60 DAY), (2, 3,  NOW() - INTERVAL 20 DAY), (2, 10, NOW() - INTERVAL 5 DAY),
(2, 14, NOW() - INTERVAL 90 DAY), (2, 19, NOW() - INTERVAL 15 DAY), (2, 26, NOW() - INTERVAL 40 DAY);

INSERT INTO lista_libro (fid_usuario, fid_libro, lista) VALUES
(2, 5, 'DESEADOS'), (2, 30, 'DESEADOS'), (2, 18, 'DESEADOS'), (2, 25, 'DESEADOS'),
(2, 2, 'FAVORITOS'), (2, 10, 'FAVORITOS'), (2, 3, 'FAVORITOS');
