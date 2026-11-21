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

-- Las reseñas se publican en el incremento 3; la tabla ya existe porque el detalle y la comparación muestran el promedio
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
