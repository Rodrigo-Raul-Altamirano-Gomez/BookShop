-- BookShop: esquema de creacion para MySQL 8.0.16 o posterior.
-- Las columnas generadas refuerzan restricciones; no son atributos adicionales de Model.
SET NAMES utf8mb4;
SET SESSION time_zone = '-05:00';
-- Este archivo recrea las tablas; no es una migración de datos existentes.
-- EN_ESPERA pertenece solo al pedido temporal en memoria; no se persiste.
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS SOLICITUD_REPORTE_RESENA;
DROP TABLE IF EXISTS SOLICITUD_DEVOLUCION;
DROP TABLE IF EXISTS SOLICITUD;
DROP TABLE IF EXISTS VOTO_UTIL;
DROP TABLE IF EXISTS SEGUIMIENTO_AUTOR;
DROP TABLE IF EXISTS SEGUIMIENTO_CATEGORIA;
DROP TABLE IF EXISTS SEGUIMIENTO_EDITORIAL;
DROP TABLE IF EXISTS EDITORIAL;
DROP TABLE IF EXISTS PAGO;
DROP TABLE IF EXISTS ITEM_PEDIDO;
DROP TABLE IF EXISTS PEDIDO;
DROP TABLE IF EXISTS CUPON;
DROP TABLE IF EXISTS METODO_PAGO;
DROP TABLE IF EXISTS RESENA;
DROP TABLE IF EXISTS LIBRO_GUARDADO;
DROP TABLE IF EXISTS DESCUENTO;
DROP TABLE IF EXISTS LIBRO;
DROP TABLE IF EXISTS AUTOR;
DROP TABLE IF EXISTS CATEGORIA;
DROP TABLE IF EXISTS DIRECCION_ENVIO;
DROP TABLE IF EXISTS SEGUIMIENTO;
DROP TABLE IF EXISTS USUARIO;

SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE USUARIO (
    id INT NOT NULL AUTO_INCREMENT,
    email VARCHAR(254) COLLATE utf8mb4_0900_as_ci NOT NULL,
    contrasenia VARCHAR(255) NOT NULL COMMENT 'Guardar solamente el hash de la contrasenia',
    nombre VARCHAR(250) NOT NULL COMMENT 'Nombre completo del usuario',
    imagen MEDIUMBLOB NULL,
    tipo_imagen VARCHAR(30) NULL,
    fecha_nacimiento DATETIME NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    rol ENUM('ADMIN', 'CLIENTE') NOT NULL DEFAULT 'CLIENTE',
    tiempo_total BIGINT NOT NULL DEFAULT 0 COMMENT 'Tiempo acumulado en la app, en segundos',
    cantidad_pedidos_entregados INT NOT NULL DEFAULT 0,
    usuario_creacion VARCHAR(254) NOT NULL,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    usuario_actualizacion VARCHAR(254) NULL,
    fecha_actualizacion DATETIME NULL,
    eliminado BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT ck_usuario_imagen CHECK (
        (imagen IS NULL AND tipo_imagen IS NULL)
        OR (imagen IS NOT NULL AND tipo_imagen IS NOT NULL
            AND tipo_imagen IN ('image/jpeg', 'image/png')
            AND OCTET_LENGTH(imagen) BETWEEN 1 AND 5242880)
    ),
    PRIMARY KEY (id),
    UNIQUE KEY uk_usuario_email (email),
    CONSTRAINT ck_usuario_datos CHECK (
        CHAR_LENGTH(TRIM(nombre)) > 0 AND CHAR_LENGTH(TRIM(contrasenia)) > 0
        AND email = TRIM(email) AND CHAR_LENGTH(SUBSTRING_INDEX(email, '@', 1)) > 0
        AND CHAR_LENGTH(email) - CHAR_LENGTH(REPLACE(email, '@', '')) = 1
        AND CHAR_LENGTH(SUBSTRING_INDEX(email, '@', -1)) > 0
    ),
    CONSTRAINT ck_usuario_rol CHECK (
        (rol = 'ADMIN' AND LOWER(SUBSTRING_INDEX(email, '@', -1)) = 'admin.bookshop')
        OR (rol = 'CLIENTE' AND LOWER(SUBSTRING_INDEX(email, '@', -1)) <> 'admin.bookshop')
    ),
    CONSTRAINT ck_usuario_tiempo_total CHECK (tiempo_total >= 0),
    CONSTRAINT ck_usuario_pedidos_entregados CHECK (cantidad_pedidos_entregados >= 0),
    CONSTRAINT ck_usuario_banderas CHECK (activo IN (0, 1) AND eliminado IN (0, 1)),
    CONSTRAINT ck_usuario_auditoria CHECK (
        CHAR_LENGTH(TRIM(usuario_creacion)) > 0
        AND ((usuario_actualizacion IS NULL AND fecha_actualizacion IS NULL)
            OR (usuario_actualizacion IS NOT NULL AND CHAR_LENGTH(TRIM(usuario_actualizacion)) > 0
                AND fecha_actualizacion IS NOT NULL AND fecha_actualizacion >= fecha_creacion))
    )
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE DIRECCION_ENVIO (
    id INT NOT NULL AUTO_INCREMENT,
    id_usuario INT NOT NULL,
    alias VARCHAR(100) NOT NULL,
    predeterminada BOOLEAN NOT NULL DEFAULT FALSE,
    calle VARCHAR(255) NOT NULL,
    distrito VARCHAR(100) NOT NULL,
    ciudad VARCHAR(100) NOT NULL,
    referencia VARCHAR(255) NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    usuario_creacion VARCHAR(254) NOT NULL,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    usuario_actualizacion VARCHAR(254) NULL,
    fecha_actualizacion DATETIME NULL,
    eliminado BOOLEAN NOT NULL DEFAULT FALSE,
    marca_predeterminada TINYINT GENERATED ALWAYS AS (
        CASE WHEN predeterminada = TRUE THEN 1 ELSE NULL END
    ) VIRTUAL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_direccion_propietario (id, id_usuario),
    UNIQUE KEY uk_direccion_predeterminada (id_usuario, marca_predeterminada),
    KEY idx_direccion_usuario (id_usuario),
    CONSTRAINT fk_direccion_usuario FOREIGN KEY (id_usuario)
        REFERENCES USUARIO (id),
    CONSTRAINT ck_direccion_datos CHECK (
        CHAR_LENGTH(TRIM(alias)) > 0 AND CHAR_LENGTH(TRIM(calle)) > 0
        AND CHAR_LENGTH(TRIM(distrito)) > 0 AND CHAR_LENGTH(TRIM(ciudad)) > 0
    ),
    CONSTRAINT ck_direccion_predeterminada CHECK (NOT predeterminada OR (activo AND NOT eliminado)),
    CONSTRAINT ck_direccion_envio_banderas CHECK (
        predeterminada IN (0, 1) AND activo IN (0, 1) AND eliminado IN (0, 1)
    ),
    CONSTRAINT ck_direccion_envio_auditoria CHECK (
        CHAR_LENGTH(TRIM(usuario_creacion)) > 0
        AND ((usuario_actualizacion IS NULL AND fecha_actualizacion IS NULL)
            OR (usuario_actualizacion IS NOT NULL AND CHAR_LENGTH(TRIM(usuario_actualizacion)) > 0
                AND fecha_actualizacion IS NOT NULL AND fecha_actualizacion >= fecha_creacion))
    )
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE CATEGORIA (
    id INT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(100) NOT NULL,
    descripcion TEXT NULL,
    imagen MEDIUMBLOB NULL,
    tipo_imagen VARCHAR(30) NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    usuario_creacion VARCHAR(254) NOT NULL,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    usuario_actualizacion VARCHAR(254) NULL,
    fecha_actualizacion DATETIME NULL,
    eliminado BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT ck_categoria_imagen CHECK (
        (imagen IS NULL AND tipo_imagen IS NULL)
        OR (imagen IS NOT NULL AND tipo_imagen IS NOT NULL
            AND tipo_imagen IN ('image/jpeg', 'image/png')
            AND OCTET_LENGTH(imagen) BETWEEN 1 AND 5242880)
    ),
    PRIMARY KEY (id),
    UNIQUE KEY uk_categoria_nombre (nombre),
    CONSTRAINT ck_categoria_nombre CHECK (CHAR_LENGTH(TRIM(nombre)) > 0),
    CONSTRAINT ck_categoria_banderas CHECK (activo IN (0, 1) AND eliminado IN (0, 1)),
    CONSTRAINT ck_categoria_auditoria CHECK (
        CHAR_LENGTH(TRIM(usuario_creacion)) > 0
        AND ((usuario_actualizacion IS NULL AND fecha_actualizacion IS NULL)
            OR (usuario_actualizacion IS NOT NULL AND CHAR_LENGTH(TRIM(usuario_actualizacion)) > 0
                AND fecha_actualizacion IS NOT NULL AND fecha_actualizacion >= fecha_creacion))
    )
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE AUTOR (
    id INT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(200) NOT NULL,
    biografia TEXT NULL,
    imagen MEDIUMBLOB NULL,
    tipo_imagen VARCHAR(30) NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    usuario_creacion VARCHAR(254) NOT NULL,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    usuario_actualizacion VARCHAR(254) NULL,
    fecha_actualizacion DATETIME NULL,
    eliminado BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT ck_autor_imagen CHECK (
        (imagen IS NULL AND tipo_imagen IS NULL)
        OR (imagen IS NOT NULL AND tipo_imagen IS NOT NULL
            AND tipo_imagen IN ('image/jpeg', 'image/png')
            AND OCTET_LENGTH(imagen) BETWEEN 1 AND 5242880)
    ),
    PRIMARY KEY (id),
    KEY idx_autor_nombre (nombre),
    CONSTRAINT ck_autor_nombre CHECK (CHAR_LENGTH(TRIM(nombre)) > 0),
    CONSTRAINT ck_autor_banderas CHECK (activo IN (0, 1) AND eliminado IN (0, 1)),
    CONSTRAINT ck_autor_auditoria CHECK (
        CHAR_LENGTH(TRIM(usuario_creacion)) > 0
        AND ((usuario_actualizacion IS NULL AND fecha_actualizacion IS NULL)
            OR (usuario_actualizacion IS NOT NULL AND CHAR_LENGTH(TRIM(usuario_actualizacion)) > 0
                AND fecha_actualizacion IS NOT NULL AND fecha_actualizacion >= fecha_creacion))
    )
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE EDITORIAL (
    id INT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(150) NOT NULL,
    descripcion TEXT NULL,
    imagen MEDIUMBLOB NULL,
    tipo_imagen VARCHAR(30) NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    usuario_creacion VARCHAR(254) NOT NULL,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    usuario_actualizacion VARCHAR(254) NULL,
    fecha_actualizacion DATETIME NULL,
    eliminado BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT ck_editorial_imagen CHECK (
        (imagen IS NULL AND tipo_imagen IS NULL)
        OR (imagen IS NOT NULL AND tipo_imagen IS NOT NULL
            AND tipo_imagen IN ('image/jpeg', 'image/png')
            AND OCTET_LENGTH(imagen) BETWEEN 1 AND 5242880)
    ),
    PRIMARY KEY (id),
    UNIQUE KEY uk_editorial_nombre (nombre),
    CONSTRAINT ck_editorial_nombre CHECK (CHAR_LENGTH(TRIM(nombre)) > 0),
    CONSTRAINT ck_editorial_banderas CHECK (activo IN (0, 1) AND eliminado IN (0, 1)),
    CONSTRAINT ck_editorial_auditoria CHECK (
        CHAR_LENGTH(TRIM(usuario_creacion)) > 0
        AND ((usuario_actualizacion IS NULL AND fecha_actualizacion IS NULL)
            OR (usuario_actualizacion IS NOT NULL AND CHAR_LENGTH(TRIM(usuario_actualizacion)) > 0
                AND fecha_actualizacion IS NOT NULL AND fecha_actualizacion >= fecha_creacion))
    )
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE LIBRO (
    id INT NOT NULL AUTO_INCREMENT,
    id_categoria INT NOT NULL,
    titulo VARCHAR(255) NOT NULL,
    id_autor INT NOT NULL,
    portada MEDIUMBLOB NULL,
    tipo_portada VARCHAR(30) NULL,
    isbn VARCHAR(20) NOT NULL,
    sinopsis TEXT NOT NULL,
    id_editorial INT NOT NULL,
    idioma VARCHAR(100) NOT NULL,
    proveedor VARCHAR(150) NOT NULL,
    anio_edicion INT NOT NULL,
    numero_paginas INT NOT NULL,
    peso DECIMAL(10, 3) NULL,
    alto DECIMAL(10, 2) NULL,
    ancho DECIMAL(10, 2) NULL,
    saga VARCHAR(150) NULL,
    numero_tomo INT NULL,
    precio_base DECIMAL(12, 2) NOT NULL,
    stock_fisico INT NOT NULL DEFAULT 0,
    numero_vetos INT NOT NULL DEFAULT 0,
    vetado_directamente BOOLEAN NOT NULL DEFAULT FALSE,
    activo BOOLEAN GENERATED ALWAYS AS (
        NOT eliminado AND NOT vetado_directamente AND numero_vetos = 0
    ) VIRTUAL,
    usuario_creacion VARCHAR(254) NOT NULL,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    usuario_actualizacion VARCHAR(254) NULL,
    fecha_actualizacion DATETIME NULL,
    eliminado BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT ck_libro_imagen CHECK (
        (portada IS NULL AND tipo_portada IS NULL)
        OR (portada IS NOT NULL AND tipo_portada IS NOT NULL
            AND tipo_portada IN ('image/jpeg', 'image/png')
            AND OCTET_LENGTH(portada) BETWEEN 1 AND 5242880)
    ),
    PRIMARY KEY (id),
    UNIQUE KEY uk_libro_isbn (isbn),
    KEY idx_libro_categoria (id_categoria),
    KEY idx_libro_titulo (titulo),
    KEY idx_libro_autor (id_autor),
    KEY idx_libro_precio (precio_base),
    KEY idx_libro_editorial (id_editorial),
    KEY idx_libro_idioma (idioma),
    KEY idx_libro_creacion (fecha_creacion),
    CONSTRAINT fk_libro_editorial FOREIGN KEY (id_editorial) REFERENCES EDITORIAL (id),
    CONSTRAINT fk_libro_categoria FOREIGN KEY (id_categoria)
        REFERENCES CATEGORIA (id),
    CONSTRAINT fk_libro_autor FOREIGN KEY (id_autor)
        REFERENCES AUTOR (id),
    CONSTRAINT ck_libro_datos CHECK (
        CHAR_LENGTH(TRIM(titulo)) > 0 AND CHAR_LENGTH(TRIM(idioma)) > 0
        AND CHAR_LENGTH(TRIM(sinopsis)) > 0 AND CHAR_LENGTH(TRIM(proveedor)) > 0
        AND CHAR_LENGTH(TRIM(isbn)) > 0
    ),
    CONSTRAINT ck_libro_ficha CHECK (
        anio_edicion BETWEEN 1 AND 9999 AND numero_paginas > 0
        AND (peso IS NULL OR peso > 0) AND (alto IS NULL OR alto > 0)
        AND (ancho IS NULL OR ancho > 0)
        AND (numero_tomo IS NULL OR (numero_tomo > 0 AND saga IS NOT NULL AND CHAR_LENGTH(TRIM(saga)) > 0))
    ),
    CONSTRAINT ck_libro_vetos CHECK (
        numero_vetos BETWEEN 0 AND 4 AND (NOT vetado_directamente OR numero_vetos >= 1)
    ),
    CONSTRAINT ck_libro_stock CHECK (stock_fisico >= 0),
    CONSTRAINT ck_libro_precio CHECK (precio_base > 0),
    CONSTRAINT ck_libro_banderas CHECK (vetado_directamente IN (0, 1) AND eliminado IN (0, 1)),
    CONSTRAINT ck_libro_auditoria CHECK (
        CHAR_LENGTH(TRIM(usuario_creacion)) > 0
        AND ((usuario_actualizacion IS NULL AND fecha_actualizacion IS NULL)
            OR (usuario_actualizacion IS NOT NULL AND CHAR_LENGTH(TRIM(usuario_actualizacion)) > 0
                AND fecha_actualizacion IS NOT NULL AND fecha_actualizacion >= fecha_creacion))
    )
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE LIBRO_GUARDADO (
    id INT NOT NULL AUTO_INCREMENT,
    id_usuario INT NOT NULL,
    id_libro INT NOT NULL,
    favorito BOOLEAN NOT NULL COMMENT 'TRUE: favorito; FALSE: deseado',
    usuario_creacion VARCHAR(254) NOT NULL,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    usuario_actualizacion VARCHAR(254) NULL,
    fecha_actualizacion DATETIME NULL,
    eliminado BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    UNIQUE KEY uk_libro_guardado_usuario_libro_favorito (id_usuario, id_libro, favorito),
    KEY idx_libro_guardado_usuario_favorito (id_usuario, favorito, fecha_creacion),
    KEY idx_libro_guardado_libro (id_libro),
    CONSTRAINT fk_libro_guardado_usuario FOREIGN KEY (id_usuario)
        REFERENCES USUARIO (id),
    CONSTRAINT fk_libro_guardado_libro FOREIGN KEY (id_libro)
        REFERENCES LIBRO (id),
    CONSTRAINT ck_libro_guardado_banderas CHECK (favorito IN (0, 1) AND eliminado IN (0, 1)),
    CONSTRAINT ck_libro_guardado_auditoria CHECK (
        CHAR_LENGTH(TRIM(usuario_creacion)) > 0
        AND ((usuario_actualizacion IS NULL AND fecha_actualizacion IS NULL)
            OR (usuario_actualizacion IS NOT NULL AND CHAR_LENGTH(TRIM(usuario_actualizacion)) > 0
                AND fecha_actualizacion IS NOT NULL AND fecha_actualizacion >= fecha_creacion))
    )
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE RESENA (
    id INT NOT NULL AUTO_INCREMENT,
    puntuacion TINYINT NOT NULL,
    comentario TEXT NOT NULL,
    fecha DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_libro INT NOT NULL,
    id_usuario INT NOT NULL,
    usuario_creacion VARCHAR(254) NOT NULL,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    usuario_actualizacion VARCHAR(254) NULL,
    fecha_actualizacion DATETIME NULL,
    eliminado BOOLEAN NOT NULL DEFAULT FALSE,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    cantidad_votos_utiles INT NOT NULL DEFAULT 0,
    marca_publicada TINYINT GENERATED ALWAYS AS (
        CASE WHEN activo AND NOT eliminado THEN 1 ELSE NULL END
    ) VIRTUAL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_resena_publicada (id_libro, id_usuario, marca_publicada),
    KEY idx_resena_usuario (id_usuario),
    CONSTRAINT fk_resena_libro FOREIGN KEY (id_libro)
        REFERENCES LIBRO (id),
    CONSTRAINT fk_resena_usuario FOREIGN KEY (id_usuario)
        REFERENCES USUARIO (id),
    CONSTRAINT ck_resena_comentario CHECK (CHAR_LENGTH(TRIM(comentario)) > 0),
    CONSTRAINT ck_resena_votos CHECK (cantidad_votos_utiles >= 0),
    CONSTRAINT ck_resena_puntuacion CHECK (puntuacion BETWEEN 1 AND 5),
    CONSTRAINT ck_resena_banderas CHECK (eliminado IN (0, 1) AND activo IN (0, 1)),
    CONSTRAINT ck_resena_auditoria CHECK (
        CHAR_LENGTH(TRIM(usuario_creacion)) > 0
        AND ((usuario_actualizacion IS NULL AND fecha_actualizacion IS NULL)
            OR (usuario_actualizacion IS NOT NULL AND CHAR_LENGTH(TRIM(usuario_actualizacion)) > 0
                AND fecha_actualizacion IS NOT NULL AND fecha_actualizacion >= fecha_creacion))
    )
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE METODO_PAGO (
    id INT NOT NULL AUTO_INCREMENT,
    id_usuario INT NOT NULL,
    titular VARCHAR(150) NOT NULL,
    proveedor VARCHAR(100) NOT NULL,
    alias VARCHAR(100) NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    tipo_metodo ENUM('TARJETA_BANCARIA', 'BILLETERA_DIGITAL') NOT NULL,
    numero_tarjeta_cifrado VARCHAR(512) NULL COMMENT 'Numero completo cifrado; solo TARJETA_BANCARIA',
    fecha_vencimiento DATE NOT NULL,
    predeterminado BOOLEAN NOT NULL DEFAULT FALSE,
    tipo_tarjeta ENUM('CREDITO', 'DEBITO') NULL COMMENT 'Solo TARJETA_BANCARIA',
    numero_telefono_cifrado VARCHAR(512) NULL COMMENT 'Numero completo cifrado; solo BILLETERA_DIGITAL',
    usuario_creacion VARCHAR(254) NOT NULL,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    usuario_actualizacion VARCHAR(254) NULL,
    fecha_actualizacion DATETIME NULL,
    eliminado BOOLEAN NOT NULL DEFAULT FALSE,
    marca_predeterminado TINYINT GENERATED ALWAYS AS (
        CASE WHEN predeterminado = TRUE THEN 1 ELSE NULL END
    ) VIRTUAL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_metodo_propietario (id, id_usuario),
    UNIQUE KEY uk_metodo_predeterminado (id_usuario, marca_predeterminado),
    KEY idx_metodo_usuario_activo (id_usuario, activo),
    CONSTRAINT fk_metodo_usuario FOREIGN KEY (id_usuario)
        REFERENCES USUARIO (id),
    CONSTRAINT ck_metodo_datos CHECK (CHAR_LENGTH(TRIM(titular)) > 0 AND CHAR_LENGTH(TRIM(proveedor)) > 0),
    CONSTRAINT ck_metodo_predeterminado CHECK (NOT predeterminado OR (activo AND NOT eliminado)),
    CONSTRAINT ck_metodo_subtipo CHECK (
        (tipo_metodo = 'TARJETA_BANCARIA' AND numero_tarjeta_cifrado IS NOT NULL
            AND CHAR_LENGTH(TRIM(numero_tarjeta_cifrado)) > 0
            AND tipo_tarjeta IS NOT NULL AND numero_telefono_cifrado IS NULL)
        OR (tipo_metodo = 'BILLETERA_DIGITAL' AND numero_telefono_cifrado IS NOT NULL
            AND CHAR_LENGTH(TRIM(numero_telefono_cifrado)) > 0
            AND numero_tarjeta_cifrado IS NULL AND tipo_tarjeta IS NULL)
    ),
    CONSTRAINT ck_metodo_pago_banderas CHECK (
        activo IN (0, 1) AND predeterminado IN (0, 1) AND eliminado IN (0, 1)
    ),
    CONSTRAINT ck_metodo_pago_auditoria CHECK (
        CHAR_LENGTH(TRIM(usuario_creacion)) > 0
        AND ((usuario_actualizacion IS NULL AND fecha_actualizacion IS NULL)
            OR (usuario_actualizacion IS NOT NULL AND CHAR_LENGTH(TRIM(usuario_actualizacion)) > 0
                AND fecha_actualizacion IS NOT NULL AND fecha_actualizacion >= fecha_creacion))
    )
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE DESCUENTO (
    id INT NOT NULL AUTO_INCREMENT,
    id_libro INT NOT NULL,
    porcentaje DECIMAL(5, 2) NOT NULL,
    fecha_inicio DATETIME NOT NULL,
    fecha_fin DATETIME NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    usuario_creacion VARCHAR(254) NOT NULL,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    usuario_actualizacion VARCHAR(254) NULL,
    fecha_actualizacion DATETIME NULL,
    eliminado BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    KEY idx_descuento_libro_vigencia (id_libro, activo, fecha_inicio, fecha_fin),
    CONSTRAINT fk_descuento_libro FOREIGN KEY (id_libro)
        REFERENCES LIBRO (id),
    CONSTRAINT ck_descuento_porcentaje CHECK (porcentaje > 0 AND porcentaje <= 100),
    CONSTRAINT ck_descuento_vigencia CHECK (fecha_fin > fecha_inicio),
    CONSTRAINT ck_descuento_banderas CHECK (activo IN (0, 1) AND eliminado IN (0, 1)),
    CONSTRAINT ck_descuento_auditoria CHECK (
        CHAR_LENGTH(TRIM(usuario_creacion)) > 0
        AND ((usuario_actualizacion IS NULL AND fecha_actualizacion IS NULL)
            OR (usuario_actualizacion IS NOT NULL AND CHAR_LENGTH(TRIM(usuario_actualizacion)) > 0
                AND fecha_actualizacion IS NOT NULL AND fecha_actualizacion >= fecha_creacion))
    )
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE CUPON (
    id INT NOT NULL AUTO_INCREMENT,
    id_usuario INT NOT NULL,
    porcentaje DECIMAL(5, 2) NOT NULL,
    monto_minimo DECIMAL(14, 2) NOT NULL,
    fecha_inicio DATETIME NOT NULL,
    fecha_fin DATETIME NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    usuario_creacion VARCHAR(254) NOT NULL,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    usuario_actualizacion VARCHAR(254) NULL,
    fecha_actualizacion DATETIME NULL,
    eliminado BOOLEAN NOT NULL DEFAULT FALSE,
    consumido BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    UNIQUE KEY uk_cupon_propietario (id, id_usuario),
    KEY idx_cupon_usuario_vigencia (id_usuario, activo, fecha_inicio, fecha_fin),
    CONSTRAINT fk_cupon_usuario FOREIGN KEY (id_usuario)
        REFERENCES USUARIO (id),
    CONSTRAINT ck_cupon_porcentaje CHECK (porcentaje > 0 AND porcentaje <= 100),
    CONSTRAINT ck_cupon_minimo CHECK (monto_minimo >= 0),
    CONSTRAINT ck_cupon_vigencia CHECK (fecha_fin > fecha_inicio),
    CONSTRAINT ck_cupon_banderas CHECK (activo IN (0, 1) AND eliminado IN (0, 1) AND consumido IN (0, 1)),
    CONSTRAINT ck_cupon_auditoria CHECK (
        CHAR_LENGTH(TRIM(usuario_creacion)) > 0
        AND ((usuario_actualizacion IS NULL AND fecha_actualizacion IS NULL)
            OR (usuario_actualizacion IS NOT NULL AND CHAR_LENGTH(TRIM(usuario_actualizacion)) > 0
                AND fecha_actualizacion IS NOT NULL AND fecha_actualizacion >= fecha_creacion))
    )
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE PEDIDO (
    id INT NOT NULL AUTO_INCREMENT,
    id_usuario INT NOT NULL,
    subtotal_productos DECIMAL(14, 2) NOT NULL,
    descuento_libros DECIMAL(14, 2) NOT NULL DEFAULT 0.00,
    id_cupon INT NULL,
    porcentaje_cupon DECIMAL(5, 2) NOT NULL DEFAULT 0.00,
    descuento_cupon DECIMAL(14, 2) NOT NULL DEFAULT 0.00,
    porcentaje_envio DECIMAL(5, 2) NOT NULL DEFAULT 0.00,
    costo_envio DECIMAL(14, 2) NOT NULL DEFAULT 0.00,
    total DECIMAL(14, 2) NOT NULL,
    cantidad_total_libros INT NOT NULL,
    id_direccion_envio INT NOT NULL,
    id_metodo_pago INT NOT NULL,
    identificador_transaccion VARCHAR(100) NOT NULL,
    fecha_pago DATETIME NOT NULL,
    estado ENUM('EN_ALMACEN', 'EN_CAMINO', 'ENTREGADO', 'CANCELADO', 'DEVUELTO')
        NOT NULL DEFAULT 'EN_ALMACEN',
    codigo_seguimiento VARCHAR(100) NOT NULL,
    fecha_entrega_estimada DATETIME NOT NULL,
    fecha_salida DATETIME NULL,
    fecha_entrega DATETIME NULL,
    numero_intentos INT NOT NULL DEFAULT 0,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    usuario_creacion VARCHAR(254) NOT NULL,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    usuario_actualizacion VARCHAR(254) NULL,
    fecha_actualizacion DATETIME NULL,
    eliminado BOOLEAN NOT NULL DEFAULT FALSE,
    cupon_en_uso INT GENERATED ALWAYS AS (
        CASE WHEN estado IN ('EN_ALMACEN', 'EN_CAMINO', 'ENTREGADO') THEN id_cupon ELSE NULL END
    ) VIRTUAL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_pedido_propietario (id, id_usuario),
    UNIQUE KEY uk_pedido_cupon_en_uso (cupon_en_uso),
    UNIQUE KEY uk_pedido_seguimiento (codigo_seguimiento),
    UNIQUE KEY uk_pedido_transaccion (identificador_transaccion),
    KEY idx_pedido_usuario_fecha (id_usuario, fecha_pago),
    KEY idx_pedido_direccion (id_direccion_envio, id_usuario),
    KEY idx_pedido_metodo (id_metodo_pago, id_usuario),
    KEY idx_pedido_cupon (id_cupon, id_usuario),
    KEY idx_pedido_fecha (fecha_pago, estado),
    CONSTRAINT fk_pedido_usuario FOREIGN KEY (id_usuario)
        REFERENCES USUARIO (id),
    CONSTRAINT fk_pedido_direccion FOREIGN KEY (id_direccion_envio, id_usuario)
        REFERENCES DIRECCION_ENVIO (id, id_usuario),
    CONSTRAINT fk_pedido_metodo FOREIGN KEY (id_metodo_pago, id_usuario)
        REFERENCES METODO_PAGO (id, id_usuario),
    CONSTRAINT fk_pedido_cupon FOREIGN KEY (id_cupon, id_usuario)
        REFERENCES CUPON (id, id_usuario),
    CONSTRAINT ck_pedido_importes CHECK (
        subtotal_productos > 0 AND descuento_libros >= 0
        AND descuento_libros <= subtotal_productos AND descuento_cupon >= 0
        AND descuento_cupon <= subtotal_productos - descuento_libros
        AND costo_envio >= 0 AND total >= 0
        AND total = subtotal_productos - descuento_libros - descuento_cupon + costo_envio
    ),
    CONSTRAINT ck_pedido_porcentajes CHECK (
        porcentaje_cupon BETWEEN 0 AND 100 AND porcentaje_envio BETWEEN 0 AND 100
        AND descuento_cupon = ROUND((subtotal_productos - descuento_libros) * porcentaje_cupon / 100, 2)
        AND costo_envio = ROUND((subtotal_productos - descuento_libros) * porcentaje_envio / 100, 2)
    ),
    CONSTRAINT ck_pedido_cupon CHECK (
        (id_cupon IS NULL AND porcentaje_cupon = 0 AND descuento_cupon = 0)
        OR (id_cupon IS NOT NULL AND porcentaje_cupon > 0)
    ),
    CONSTRAINT ck_pedido_identificadores CHECK (
        CHAR_LENGTH(TRIM(identificador_transaccion)) > 0 AND CHAR_LENGTH(TRIM(codigo_seguimiento)) > 0
    ),
    CONSTRAINT ck_pedido_fechas CHECK (
        fecha_entrega_estimada >= fecha_pago
        AND (fecha_salida IS NULL OR fecha_salida >= fecha_pago)
        AND (fecha_entrega IS NULL OR (fecha_salida IS NOT NULL AND fecha_entrega >= fecha_salida))
    ),
    CONSTRAINT ck_pedido_estado_fechas CHECK (
        (estado IN ('EN_ALMACEN', 'CANCELADO') AND fecha_salida IS NULL AND fecha_entrega IS NULL)
        OR (estado = 'EN_CAMINO' AND fecha_salida IS NOT NULL AND fecha_entrega IS NULL)
        OR (estado IN ('ENTREGADO', 'DEVUELTO') AND fecha_salida IS NOT NULL AND fecha_entrega IS NOT NULL)
    ),
    CONSTRAINT ck_pedido_contadores CHECK (cantidad_total_libros > 0 AND numero_intentos >= 0),
    CONSTRAINT ck_pedido_banderas CHECK (activo IN (0, 1) AND eliminado IN (0, 1)),
    CONSTRAINT ck_pedido_auditoria CHECK (
        CHAR_LENGTH(TRIM(usuario_creacion)) > 0
        AND ((usuario_actualizacion IS NULL AND fecha_actualizacion IS NULL)
            OR (usuario_actualizacion IS NOT NULL AND CHAR_LENGTH(TRIM(usuario_actualizacion)) > 0
                AND fecha_actualizacion IS NOT NULL AND fecha_actualizacion >= fecha_creacion))
    )
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE ITEM_PEDIDO (
    id INT NOT NULL AUTO_INCREMENT,
    id_pedido INT NOT NULL,
    id_libro INT NOT NULL,
    cantidad INT NOT NULL,
    precio_base DECIMAL(12, 2) NOT NULL,
    precio_unitario DECIMAL(12, 2) NOT NULL,
    porcentaje_descuento DECIMAL(5, 2) NOT NULL DEFAULT 0.00,
    sub_total DECIMAL(14, 2) NOT NULL,
    usuario_creacion VARCHAR(254) NOT NULL,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    usuario_actualizacion VARCHAR(254) NULL,
    fecha_actualizacion DATETIME NULL,
    eliminado BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    UNIQUE KEY uk_item_pedido_libro (id_pedido, id_libro),
    KEY idx_item_pedido_libro (id_libro),
    CONSTRAINT fk_item_pedido FOREIGN KEY (id_pedido)
        REFERENCES PEDIDO (id),
    CONSTRAINT fk_item_pedido_libro FOREIGN KEY (id_libro)
        REFERENCES LIBRO (id),
    CONSTRAINT ck_cantidad CHECK (cantidad > 0),
    CONSTRAINT ck_sub_total CHECK (sub_total = precio_unitario * cantidad),
    CONSTRAINT ck_precio_item CHECK (precio_base > 0 AND precio_unitario >= 0 AND precio_unitario <= precio_base),
    CONSTRAINT ck_porcentaje_item CHECK (
        porcentaje_descuento BETWEEN 0 AND 100
        AND precio_unitario = ROUND(precio_base * (100 - porcentaje_descuento) / 100, 2)
    ),
    CONSTRAINT ck_item_pedido_banderas CHECK (eliminado IN (0, 1)),
    CONSTRAINT ck_item_pedido_auditoria CHECK (
        CHAR_LENGTH(TRIM(usuario_creacion)) > 0
        AND ((usuario_actualizacion IS NULL AND fecha_actualizacion IS NULL)
            OR (usuario_actualizacion IS NOT NULL AND CHAR_LENGTH(TRIM(usuario_actualizacion)) > 0
                AND fecha_actualizacion IS NOT NULL AND fecha_actualizacion >= fecha_creacion))
    )
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- Herencia en una tabla, igual que METODO_PAGO: comunes, tipo y campos de cada subtipo.
CREATE TABLE SEGUIMIENTO (
    id INT NOT NULL AUTO_INCREMENT,
    id_usuario INT NOT NULL,
    tipo_seguimiento ENUM('AUTOR', 'CATEGORIA', 'EDITORIAL') NOT NULL,
    id_autor INT NULL,
    id_categoria INT NULL,
    id_editorial INT NULL,
    eliminado BOOLEAN NOT NULL DEFAULT FALSE,
    usuario_creacion VARCHAR(254) NOT NULL,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    usuario_actualizacion VARCHAR(254) NULL,
    fecha_actualizacion DATETIME NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_seguimiento_autor (id_usuario, id_autor),
    UNIQUE KEY uk_seguimiento_categoria (id_usuario, id_categoria),
    UNIQUE KEY uk_seguimiento_editorial (id_usuario, id_editorial),
    KEY idx_seguimiento_usuario_tipo_fecha (id_usuario, tipo_seguimiento, eliminado, fecha_creacion),
    KEY idx_seguimiento_autor_referencia (id_autor),
    KEY idx_seguimiento_categoria_referencia (id_categoria),
    KEY idx_seguimiento_editorial_referencia (id_editorial),
    CONSTRAINT fk_seguimiento_usuario FOREIGN KEY (id_usuario) REFERENCES USUARIO (id),
    CONSTRAINT fk_seguimiento_autor FOREIGN KEY (id_autor) REFERENCES AUTOR (id),
    CONSTRAINT fk_seguimiento_categoria FOREIGN KEY (id_categoria) REFERENCES CATEGORIA (id),
    CONSTRAINT fk_seguimiento_editorial FOREIGN KEY (id_editorial) REFERENCES EDITORIAL (id),
    CONSTRAINT ck_seguimiento_subtipo CHECK (
        (tipo_seguimiento = 'AUTOR' AND id_autor IS NOT NULL
            AND id_categoria IS NULL AND id_editorial IS NULL)
        OR (tipo_seguimiento = 'CATEGORIA' AND id_categoria IS NOT NULL
            AND id_autor IS NULL AND id_editorial IS NULL)
        OR (tipo_seguimiento = 'EDITORIAL' AND id_editorial IS NOT NULL
            AND id_autor IS NULL AND id_categoria IS NULL)
    ),
    CONSTRAINT ck_seguimiento_banderas CHECK (eliminado IN (0, 1)),
    CONSTRAINT ck_seguimiento_auditoria CHECK (
        CHAR_LENGTH(TRIM(usuario_creacion)) > 0
        AND ((usuario_actualizacion IS NULL AND fecha_actualizacion IS NULL)
            OR (usuario_actualizacion IS NOT NULL AND CHAR_LENGTH(TRIM(usuario_actualizacion)) > 0
                AND fecha_actualizacion IS NOT NULL AND fecha_actualizacion >= fecha_creacion))
    )
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE VOTO_UTIL (
    id INT NOT NULL AUTO_INCREMENT,
    id_usuario INT NOT NULL,
    id_resena INT NOT NULL,
    eliminado BOOLEAN NOT NULL DEFAULT FALSE,
    usuario_creacion VARCHAR(254) NOT NULL,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    usuario_actualizacion VARCHAR(254) NULL,
    fecha_actualizacion DATETIME NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_voto_util (id_usuario, id_resena),
    KEY idx_voto_resena (id_resena, eliminado),
    CONSTRAINT fk_voto_usuario FOREIGN KEY (id_usuario) REFERENCES USUARIO (id),
    CONSTRAINT fk_voto_resena FOREIGN KEY (id_resena) REFERENCES RESENA (id),
    CONSTRAINT ck_voto_util_banderas CHECK (eliminado IN (0, 1)),
    CONSTRAINT ck_voto_util_auditoria CHECK (
        CHAR_LENGTH(TRIM(usuario_creacion)) > 0
        AND ((usuario_actualizacion IS NULL AND fecha_actualizacion IS NULL)
            OR (usuario_actualizacion IS NOT NULL AND CHAR_LENGTH(TRIM(usuario_actualizacion)) > 0
                AND fecha_actualizacion IS NOT NULL AND fecha_actualizacion >= fecha_creacion))
    )
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE SOLICITUD (
    id INT NOT NULL AUTO_INCREMENT,
    id_usuario INT NOT NULL,
    tipo_solicitud ENUM('DEVOLUCION', 'REPORTE_RESENA') NOT NULL,
    id_pedido INT NULL,
    motivo_devolucion ENUM('LIBRO_DANADO', 'LIBRO_INCORRECTO', 'PEDIDO_INCOMPLETO',
        'NO_CUMPLE_EXPECTATIVAS', 'OTRO') NULL,
    id_resena INT NULL,
    motivo_reporte ENUM('CONTENIDO_OFENSIVO', 'PUBLICIDAD_O_SPAM', 'CONTENIDO_AJENO_AL_LIBRO', 'OTRO') NULL,
    puntuacion_original TINYINT NULL,
    comentario_original TEXT NULL,
    comentario TEXT NULL,
    estado ENUM('PENDIENTE', 'ACEPTADA', 'RECHAZADA', 'CERRADA') NOT NULL DEFAULT 'PENDIENTE',
    fecha_resolucion DATETIME NULL,
    eliminado BOOLEAN NOT NULL DEFAULT FALSE,
    marca_pendiente TINYINT GENERATED ALWAYS AS (
        CASE WHEN estado = 'PENDIENTE' AND NOT eliminado THEN 1 ELSE NULL END
    ) VIRTUAL,
    usuario_creacion VARCHAR(254) NOT NULL,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    usuario_actualizacion VARCHAR(254) NULL,
    fecha_actualizacion DATETIME NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_devolucion_pendiente (id_pedido, marca_pendiente),
    UNIQUE KEY uk_reporte_pendiente (id_usuario, id_resena, marca_pendiente),
    KEY idx_solicitud_usuario_tipo_fecha (id_usuario, tipo_solicitud, eliminado, fecha_creacion),
    KEY idx_solicitud_tipo_estado_fecha (tipo_solicitud, estado, eliminado, fecha_creacion),
    KEY idx_solicitud_pedido_usuario (id_pedido, id_usuario),
    KEY idx_solicitud_resena (id_resena),
    CONSTRAINT fk_solicitud_usuario FOREIGN KEY (id_usuario) REFERENCES USUARIO (id),
    CONSTRAINT fk_solicitud_pedido FOREIGN KEY (id_pedido, id_usuario) REFERENCES PEDIDO (id, id_usuario),
    CONSTRAINT fk_solicitud_resena FOREIGN KEY (id_resena) REFERENCES RESENA (id),
    CONSTRAINT ck_solicitud_subtipo CHECK (
        (tipo_solicitud = 'DEVOLUCION' AND id_pedido IS NOT NULL AND motivo_devolucion IS NOT NULL
            AND id_resena IS NULL AND motivo_reporte IS NULL
            AND puntuacion_original IS NULL AND comentario_original IS NULL AND estado <> 'CERRADA')
        OR (tipo_solicitud = 'REPORTE_RESENA' AND id_resena IS NOT NULL AND motivo_reporte IS NOT NULL
            AND id_pedido IS NULL AND motivo_devolucion IS NULL
            AND puntuacion_original IS NOT NULL AND puntuacion_original BETWEEN 1 AND 5
            AND comentario_original IS NOT NULL AND CHAR_LENGTH(TRIM(comentario_original)) > 0)
    ),
    CONSTRAINT ck_solicitud_resolucion CHECK (
        (estado = 'PENDIENTE' AND fecha_resolucion IS NULL)
        OR (estado <> 'PENDIENTE' AND fecha_resolucion IS NOT NULL AND fecha_resolucion >= fecha_creacion)
    ),
    CONSTRAINT ck_solicitud_retiro CHECK (NOT eliminado OR estado = 'PENDIENTE'),
    CONSTRAINT ck_solicitud_banderas CHECK (eliminado IN (0, 1)),
    CONSTRAINT ck_solicitud_auditoria CHECK (
        CHAR_LENGTH(TRIM(usuario_creacion)) > 0
        AND ((usuario_actualizacion IS NULL AND fecha_actualizacion IS NULL)
            OR (usuario_actualizacion IS NOT NULL AND CHAR_LENGTH(TRIM(usuario_actualizacion)) > 0
                AND fecha_actualizacion IS NOT NULL AND fecha_actualizacion >= fecha_creacion))
    )
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- Las FK usan RESTRICT por defecto: el borrado logico conserva relaciones e historia.
-- Business/DAO: permisos, referencias inmutables, propagacion/recuento de vetos,
-- elegibilidad de resenas/solicitudes, plazos/intentos configurables y transiciones de estado.
-- Tambien validan vigencia por reloj, ausencia de descuentos solapados, unicidad de numeros
-- descifrados, suma de items frente a cabecera y coherencia de votos/consumo/contador de entregas.
-- Estas operaciones se coordinan en transacciones; no se duplican aqui mediante triggers.
-- Carrito, comparacion, EN_ESPERA, reportes PDF y configuracion no tienen tablas.
