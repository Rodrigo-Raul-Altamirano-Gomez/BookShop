-- BookShop: esquema de la base de datos
DROP DATABASE IF EXISTS bookshop;
CREATE DATABASE bookshop CHARACTER SET utf8mb4 COLLATE utf8mb4_spanish_ci;
USE bookshop;

CREATE TABLE usuario (
  id_usuario        INT AUTO_INCREMENT PRIMARY KEY,
  correo            VARCHAR(100) NOT NULL UNIQUE,
  nombre_completo   VARCHAR(100) NOT NULL,
  fecha_nacimiento  DATE NOT NULL,
  contrasena        CHAR(64) NOT NULL,          -- SHA-256 en hexadecimal
  foto              VARCHAR(200),
  rol               VARCHAR(20) NOT NULL,       -- CLIENTE, ADMINISTRADOR
  activo            TINYINT(1) NOT NULL,
  fecha_registro    DATETIME NOT NULL
);

CREATE TABLE direccion (
  id_direccion    INT AUTO_INCREMENT PRIMARY KEY,
  fid_usuario     INT NOT NULL,
  alias           VARCHAR(50) NOT NULL,
  direccion       VARCHAR(200) NOT NULL,
  distrito        VARCHAR(100) NOT NULL,
  ciudad          VARCHAR(100) NOT NULL,
  predeterminada  TINYINT(1) NOT NULL,
  activa          TINYINT(1) NOT NULL,
  FOREIGN KEY (fid_usuario) REFERENCES usuario(id_usuario)
);

CREATE TABLE metodo_pago (
  id_metodo_pago     INT AUTO_INCREMENT PRIMARY KEY,
  fid_usuario        INT NOT NULL,
  tipo               VARCHAR(20) NOT NULL,      -- TARJETA, BILLETERA
  titular            VARCHAR(100) NOT NULL,
  numero             VARCHAR(16) NOT NULL,
  fecha_vencimiento  VARCHAR(5),                -- MM/AA, solo tarjeta
  proveedor          VARCHAR(20),               -- Yape o Plin, solo billetera
  predeterminado     TINYINT(1) NOT NULL,
  activo             TINYINT(1) NOT NULL,
  FOREIGN KEY (fid_usuario) REFERENCES usuario(id_usuario)
);

-- Autores, categorías y editoriales: tienen los mismos datos, así que van en una sola tabla
CREATE TABLE entidad (
  id_entidad   INT AUTO_INCREMENT PRIMARY KEY,
  tipo         VARCHAR(20) NOT NULL,            -- AUTOR, CATEGORIA, EDITORIAL
  nombre       VARCHAR(100) NOT NULL,
  descripcion  TEXT,                            -- biografía en el caso del autor
  imagen       VARCHAR(200),
  activo       TINYINT(1) NOT NULL
);

CREATE TABLE libro (
  id_libro          INT AUTO_INCREMENT PRIMARY KEY,
  isbn              VARCHAR(20) NOT NULL,
  titulo            VARCHAR(200) NOT NULL,
  sinopsis          TEXT,
  idioma            VARCHAR(30) NOT NULL,
  paginas           INT NOT NULL,
  anio_publicacion  INT NOT NULL,
  precio            DECIMAL(10,2) NOT NULL,
  stock             INT NOT NULL,
  portada           VARCHAR(200),
  activo            TINYINT(1) NOT NULL,
  fecha_registro    DATETIME NOT NULL,
  fid_autor         INT NOT NULL,
  fid_categoria     INT NOT NULL,
  fid_editorial     INT NOT NULL,
  FOREIGN KEY (fid_autor) REFERENCES entidad(id_entidad),
  FOREIGN KEY (fid_categoria) REFERENCES entidad(id_entidad),
  FOREIGN KEY (fid_editorial) REFERENCES entidad(id_entidad)
);

CREATE TABLE descuento (
  id_descuento  INT AUTO_INCREMENT PRIMARY KEY,
  fid_libro     INT NOT NULL,
  porcentaje    INT NOT NULL,
  fecha_inicio  DATE NOT NULL,
  fecha_fin     DATE NOT NULL,
  FOREIGN KEY (fid_libro) REFERENCES libro(id_libro)
);

-- activa = 0: oculta por moderación (no se muestra ni cuenta en el promedio)
CREATE TABLE resena (
  id_resena    INT AUTO_INCREMENT PRIMARY KEY,
  fid_usuario  INT NOT NULL,
  fid_libro    INT NOT NULL,
  puntuacion   INT NOT NULL,
  comentario   TEXT NOT NULL,
  fecha        DATETIME NOT NULL,
  activa       TINYINT(1) NOT NULL,
  UNIQUE (fid_usuario, fid_libro),
  FOREIGN KEY (fid_usuario) REFERENCES usuario(id_usuario),
  FOREIGN KEY (fid_libro) REFERENCES libro(id_libro)
);

CREATE TABLE voto_util (
  fid_usuario  INT NOT NULL,
  fid_resena   INT NOT NULL,
  PRIMARY KEY (fid_usuario, fid_resena),
  FOREIGN KEY (fid_usuario) REFERENCES usuario(id_usuario),
  FOREIGN KEY (fid_resena) REFERENCES resena(id_resena)
);

-- Guarda una copia del texto y la puntuación de la reseña al momento de reportarla (RF10.7)
CREATE TABLE reporte_resena (
  id_reporte_resena     INT AUTO_INCREMENT PRIMARY KEY,
  fid_usuario           INT NOT NULL,
  fid_resena            INT NOT NULL,
  motivo                VARCHAR(30) NOT NULL,   -- OFENSIVA, SPAM, NO_RELACIONADA, OTRO
  comentario            TEXT NOT NULL,
  contenido_reportado   TEXT NOT NULL,
  puntuacion_reportada  INT NOT NULL,
  estado                VARCHAR(20) NOT NULL,   -- PENDIENTE, ACEPTADA, RECHAZADA, RETIRADA
  fecha                 DATETIME NOT NULL,
  FOREIGN KEY (fid_usuario) REFERENCES usuario(id_usuario),
  FOREIGN KEY (fid_resena) REFERENCES resena(id_resena)
);

-- La entidad ya tiene su tipo (autor, categoría o editorial)
CREATE TABLE seguimiento_entidad (
  fid_usuario  INT NOT NULL,
  fid_entidad  INT NOT NULL,
  fecha        DATETIME NOT NULL,
  PRIMARY KEY (fid_usuario, fid_entidad),
  FOREIGN KEY (fid_usuario) REFERENCES usuario(id_usuario),
  FOREIGN KEY (fid_entidad) REFERENCES entidad(id_entidad)
);

-- Deseados y Favoritos: un libro puede estar en las dos, una vez en cada una (RF06.2)
CREATE TABLE lista_libro (
  fid_usuario  INT NOT NULL,
  fid_libro    INT NOT NULL,
  lista        VARCHAR(20) NOT NULL,            -- DESEADOS, FAVORITOS
  PRIMARY KEY (fid_usuario, fid_libro, lista),
  FOREIGN KEY (fid_usuario) REFERENCES usuario(id_usuario),
  FOREIGN KEY (fid_libro) REFERENCES libro(id_libro)
);

-- Se desactiva al usarse en una compra; es utilizable si está activo y no venció (ERS §2.2)
CREATE TABLE cupon (
  id_cupon           INT AUTO_INCREMENT PRIMARY KEY,
  fid_usuario        INT NOT NULL,
  codigo             VARCHAR(10) NOT NULL,
  porcentaje         INT NOT NULL,
  fecha_emision      DATE NOT NULL,
  fecha_vencimiento  DATE NOT NULL,
  activo             TINYINT(1) NOT NULL,
  FOREIGN KEY (fid_usuario) REFERENCES usuario(id_usuario)
);

-- El número del pedido es su id; empieza en 1001 (N.º 1001)
CREATE TABLE pedido (
  id_pedido           INT AUTO_INCREMENT PRIMARY KEY,
  fid_usuario         INT NOT NULL,
  fid_direccion       INT NOT NULL,
  fid_metodo_pago     INT NOT NULL,
  fid_cupon           INT,                         -- opcional
  estado              VARCHAR(20) NOT NULL,        -- EN_ALMACEN, EN_CAMINO, ENTREGADO, CANCELADO, DEVUELTO
  fecha_compra        DATETIME NOT NULL,
  fecha_salida        DATETIME,
  fecha_entrega       DATETIME,
  fecha_cancelacion   DATETIME,
  fecha_devolucion    DATETIME,
  codigo_transaccion  VARCHAR(30) NOT NULL,
  codigo_seguimiento  VARCHAR(30) NOT NULL,
  subtotal            DECIMAL(10,2) NOT NULL,
  descuento_cupon     DECIMAL(10,2) NOT NULL,
  costo_envio         DECIMAL(10,2) NOT NULL,
  total               DECIMAL(10,2) NOT NULL,
  contado_para_cupon  TINYINT(1) NOT NULL,
  FOREIGN KEY (fid_usuario) REFERENCES usuario(id_usuario),
  FOREIGN KEY (fid_direccion) REFERENCES direccion(id_direccion),
  FOREIGN KEY (fid_metodo_pago) REFERENCES metodo_pago(id_metodo_pago),
  FOREIGN KEY (fid_cupon) REFERENCES cupon(id_cupon)
) AUTO_INCREMENT = 1001;

-- Precio y descuento del momento de la compra (importes históricos)
CREATE TABLE detalle_pedido (
  id_detalle_pedido   INT AUTO_INCREMENT PRIMARY KEY,
  fid_pedido          INT NOT NULL,
  fid_libro           INT NOT NULL,
  cantidad            INT NOT NULL,
  precio_unitario     DECIMAL(10,2) NOT NULL,      -- precio sin descuento
  descuento_unitario  DECIMAL(10,2) NOT NULL,      -- rebaja por unidad del descuento vigente
  subtotal            DECIMAL(10,2) NOT NULL,
  FOREIGN KEY (fid_pedido) REFERENCES pedido(id_pedido),
  FOREIGN KEY (fid_libro) REFERENCES libro(id_libro)
);

CREATE TABLE solicitud_devolucion (
  id_solicitud_devolucion  INT AUTO_INCREMENT PRIMARY KEY,
  fid_pedido               INT NOT NULL,
  motivo                   VARCHAR(30) NOT NULL,   -- PRODUCTO_DANADO, PRODUCTO_EQUIVOCADO, YA_NO_LO_QUIERO, OTRO
  comentario               TEXT NOT NULL,
  estado                   VARCHAR(20) NOT NULL,   -- PENDIENTE, ACEPTADA, RECHAZADA, RETIRADA
  fecha                    DATETIME NOT NULL,
  FOREIGN KEY (fid_pedido) REFERENCES pedido(id_pedido)
);

-- Una sola fila (id_configuracion = 1)
CREATE TABLE configuracion (
  id_configuracion            INT PRIMARY KEY,
  costo_envio                 DECIMAL(10,2) NOT NULL,
  dias_devolucion             INT NOT NULL,
  max_solicitudes_devolucion  INT NOT NULL,
  max_cancelaciones_mes       INT NOT NULL,
  cupon_monto_minimo          DECIMAL(10,2) NOT NULL,
  cupon_cada_pedidos          INT NOT NULL,
  cupon_porcentaje            INT NOT NULL,
  cupon_dias_vigencia         INT NOT NULL
);
