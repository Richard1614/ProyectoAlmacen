CREATE DATABASE IF NOT EXISTS austral_almacen
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE austral_almacen;

-- Seguridad
CREATE TABLE rol (
  id_rol BIGINT AUTO_INCREMENT PRIMARY KEY,
  nombre VARCHAR(40) NOT NULL UNIQUE,
  descripcion VARCHAR(150) NOT NULL
) ENGINE=InnoDB;

CREATE TABLE permiso (
  id_permiso BIGINT AUTO_INCREMENT PRIMARY KEY,
  codigo VARCHAR(60) NOT NULL UNIQUE,
  descripcion VARCHAR(150) NOT NULL
) ENGINE=InnoDB;

CREATE TABLE rol_permiso (
  id_rol BIGINT NOT NULL,
  id_permiso BIGINT NOT NULL,
  PRIMARY KEY (id_rol, id_permiso),
  CONSTRAINT fk_rp_rol FOREIGN KEY (id_rol) REFERENCES rol(id_rol),
  CONSTRAINT fk_rp_permiso FOREIGN KEY (id_permiso) REFERENCES permiso(id_permiso)
) ENGINE=InnoDB;

CREATE TABLE usuario (
  id_usuario BIGINT AUTO_INCREMENT PRIMARY KEY,
  id_rol BIGINT NOT NULL,
  nombre VARCHAR(120) NOT NULL,
  correo VARCHAR(150) NOT NULL UNIQUE,
  telefono VARCHAR(25),
  password_hash VARCHAR(100) NOT NULL,
  activo BOOLEAN NOT NULL DEFAULT TRUE,
  creado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  actualizado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_usuario_rol FOREIGN KEY (id_rol) REFERENCES rol(id_rol)
) ENGINE=InnoDB;

-- Catalogo e inventario
CREATE TABLE proveedor (
  id_proveedor BIGINT AUTO_INCREMENT PRIMARY KEY,
  ruc CHAR(11) NOT NULL UNIQUE,
  razon_social VARCHAR(160) NOT NULL,
  contacto VARCHAR(120),
  correo VARCHAR(150),
  telefono VARCHAR(25),
  direccion VARCHAR(220),
  activo BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE=InnoDB;

CREATE TABLE almacen (
  id_almacen BIGINT AUTO_INCREMENT PRIMARY KEY,
  codigo VARCHAR(20) NOT NULL UNIQUE,
  nombre VARCHAR(100) NOT NULL,
  ubicacion VARCHAR(180),
  activo BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE=InnoDB;

CREATE TABLE producto (
  id_producto BIGINT AUTO_INCREMENT PRIMARY KEY,
  codigo VARCHAR(30) NOT NULL UNIQUE,
  nombre VARCHAR(160) NOT NULL,
  descripcion VARCHAR(300),
  unidad_medida VARCHAR(30) NOT NULL,
  stock_minimo DECIMAL(14,3) NOT NULL DEFAULT 0,
  activo BOOLEAN NOT NULL DEFAULT TRUE,
  creado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT ck_producto_stock_min CHECK (stock_minimo >= 0)
) ENGINE=InnoDB;

CREATE TABLE inventario (
  id_inventario BIGINT AUTO_INCREMENT PRIMARY KEY,
  id_producto BIGINT NOT NULL,
  id_almacen BIGINT NOT NULL,
  stock_actual DECIMAL(14,3) NOT NULL DEFAULT 0,
  actualizado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_inventario_producto_almacen (id_producto, id_almacen),
  CONSTRAINT fk_inv_producto FOREIGN KEY (id_producto) REFERENCES producto(id_producto),
  CONSTRAINT fk_inv_almacen FOREIGN KEY (id_almacen) REFERENCES almacen(id_almacen),
  CONSTRAINT ck_inventario_no_negativo CHECK (stock_actual >= 0)
) ENGINE=InnoDB;

-- Compras, aprobacion y recepcion
CREATE TABLE orden_compra (
  id_orden_compra BIGINT AUTO_INCREMENT PRIMARY KEY,
  numero VARCHAR(30) NOT NULL UNIQUE,
  id_proveedor BIGINT NOT NULL,
  creada_por BIGINT NOT NULL,
  estado ENUM('BORRADOR','PENDIENTE_APROBACION','APROBADA','RECHAZADA','REABIERTA','PARCIAL','RECIBIDA','ANULADA') NOT NULL DEFAULT 'BORRADOR',
  fecha_emision DATE NOT NULL,
  moneda CHAR(3) NOT NULL DEFAULT 'PEN',
  total DECIMAL(14,2) NOT NULL DEFAULT 0,
  observacion VARCHAR(500),
  version INT NOT NULL DEFAULT 1,
  creado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  actualizado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_oc_proveedor FOREIGN KEY (id_proveedor) REFERENCES proveedor(id_proveedor),
  CONSTRAINT fk_oc_usuario FOREIGN KEY (creada_por) REFERENCES usuario(id_usuario),
  CONSTRAINT ck_oc_total CHECK (total >= 0)
) ENGINE=InnoDB;

CREATE TABLE detalle_orden_compra (
  id_detalle_oc BIGINT AUTO_INCREMENT PRIMARY KEY,
  id_orden_compra BIGINT NOT NULL,
  id_producto BIGINT NOT NULL,
  cantidad DECIMAL(14,3) NOT NULL,
  precio_unitario DECIMAL(14,2) NOT NULL,
  subtotal DECIMAL(14,2) GENERATED ALWAYS AS (ROUND(cantidad * precio_unitario, 2)) STORED,
  UNIQUE KEY uk_doc_producto (id_orden_compra, id_producto),
  CONSTRAINT fk_doc_oc FOREIGN KEY (id_orden_compra) REFERENCES orden_compra(id_orden_compra) ON DELETE CASCADE,
  CONSTRAINT fk_doc_producto FOREIGN KEY (id_producto) REFERENCES producto(id_producto),
  CONSTRAINT ck_doc_cantidad CHECK (cantidad > 0),
  CONSTRAINT ck_doc_precio CHECK (precio_unitario >= 0)
) ENGINE=InnoDB;

CREATE TABLE autorizacion_oc (
  id_autorizacion BIGINT AUTO_INCREMENT PRIMARY KEY,
  id_orden_compra BIGINT NOT NULL,
  id_usuario BIGINT NOT NULL,
  decision ENUM('APROBADA','RECHAZADA') NOT NULL,
  comentario VARCHAR(500),
  fecha DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_aut_oc FOREIGN KEY (id_orden_compra) REFERENCES orden_compra(id_orden_compra),
  CONSTRAINT fk_aut_usuario FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario)
) ENGINE=InnoDB;

CREATE TABLE recepcion (
  id_recepcion BIGINT AUTO_INCREMENT PRIMARY KEY,
  numero VARCHAR(30) NOT NULL UNIQUE,
  id_orden_compra BIGINT NOT NULL,
  id_almacen BIGINT NOT NULL,
  guia_remision VARCHAR(50) NOT NULL,
  recibida_por BIGINT NOT NULL,
  estado ENUM('BORRADOR','CONFIRMADA','OBSERVADA','ANULADA') NOT NULL DEFAULT 'BORRADOR',
  fecha_recepcion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  observacion VARCHAR(500),
  CONSTRAINT fk_rec_oc FOREIGN KEY (id_orden_compra) REFERENCES orden_compra(id_orden_compra),
  CONSTRAINT fk_rec_almacen FOREIGN KEY (id_almacen) REFERENCES almacen(id_almacen),
  CONSTRAINT fk_rec_usuario FOREIGN KEY (recibida_por) REFERENCES usuario(id_usuario),
  UNIQUE KEY uk_recepcion_guia_proveedor (id_orden_compra, guia_remision)
) ENGINE=InnoDB;

CREATE TABLE detalle_recepcion (
  id_detalle_recepcion BIGINT AUTO_INCREMENT PRIMARY KEY,
  id_recepcion BIGINT NOT NULL,
  id_producto BIGINT NOT NULL,
  cantidad_recibida DECIMAL(14,3) NOT NULL,
  cantidad_conforme DECIMAL(14,3) NOT NULL,
  cantidad_rechazada DECIMAL(14,3) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_drec_producto (id_recepcion, id_producto),
  CONSTRAINT fk_drec_recepcion FOREIGN KEY (id_recepcion) REFERENCES recepcion(id_recepcion) ON DELETE CASCADE,
  CONSTRAINT fk_drec_producto FOREIGN KEY (id_producto) REFERENCES producto(id_producto),
  CONSTRAINT ck_drec_cantidades CHECK (
    cantidad_recibida > 0 AND cantidad_conforme >= 0 AND cantidad_rechazada >= 0
    AND cantidad_conforme + cantidad_rechazada = cantidad_recibida
  )
) ENGINE=InnoDB;

CREATE TABLE incidencia_recepcion (
  id_incidencia BIGINT AUTO_INCREMENT PRIMARY KEY,
  id_recepcion BIGINT NOT NULL,
  id_producto BIGINT,
  tipo ENUM('DANIO','FALTANTE','SOBRANTE','DOCUMENTACION','OTRO') NOT NULL,
  descripcion VARCHAR(700) NOT NULL,
  accion ENUM('CAMBIO','REEMBOLSO','ACEPTACION_PARCIAL','PENDIENTE') NOT NULL DEFAULT 'PENDIENTE',
  estado ENUM('ABIERTA','EN_GESTION','RESUELTA') NOT NULL DEFAULT 'ABIERTA',
  registrada_por BIGINT NOT NULL,
  creado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_inc_recepcion FOREIGN KEY (id_recepcion) REFERENCES recepcion(id_recepcion),
  CONSTRAINT fk_inc_producto FOREIGN KEY (id_producto) REFERENCES producto(id_producto),
  CONSTRAINT fk_inc_usuario FOREIGN KEY (registrada_por) REFERENCES usuario(id_usuario)
) ENGINE=InnoDB;

-- Taller y salidas
CREATE TABLE requerimiento (
  id_requerimiento BIGINT AUTO_INCREMENT PRIMARY KEY,
  numero VARCHAR(30) NOT NULL UNIQUE,
  solicitado_por BIGINT NOT NULL,
  placa_unidad VARCHAR(15),
  tipo_mantenimiento ENUM('PREVENTIVO','CORRECTIVO','SINIESTRO','OTRO') NOT NULL,
  prioridad ENUM('BAJA','MEDIA','ALTA','URGENTE') NOT NULL DEFAULT 'MEDIA',
  estado ENUM('BORRADOR','ENVIADO','ATENDIDO_PARCIAL','ATENDIDO','RECHAZADO','ANULADO') NOT NULL DEFAULT 'BORRADOR',
  motivo VARCHAR(500) NOT NULL,
  creado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_req_usuario FOREIGN KEY (solicitado_por) REFERENCES usuario(id_usuario)
) ENGINE=InnoDB;

CREATE TABLE detalle_requerimiento (
  id_detalle_requerimiento BIGINT AUTO_INCREMENT PRIMARY KEY,
  id_requerimiento BIGINT NOT NULL,
  id_producto BIGINT NOT NULL,
  cantidad_solicitada DECIMAL(14,3) NOT NULL,
  cantidad_atendida DECIMAL(14,3) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_dreq_producto (id_requerimiento, id_producto),
  CONSTRAINT fk_dreq_req FOREIGN KEY (id_requerimiento) REFERENCES requerimiento(id_requerimiento) ON DELETE CASCADE,
  CONSTRAINT fk_dreq_producto FOREIGN KEY (id_producto) REFERENCES producto(id_producto),
  CONSTRAINT ck_dreq_cantidad CHECK (cantidad_solicitada > 0 AND cantidad_atendida >= 0 AND cantidad_atendida <= cantidad_solicitada)
) ENGINE=InnoDB;

CREATE TABLE orden_salida (
  id_orden_salida BIGINT AUTO_INCREMENT PRIMARY KEY,
  numero VARCHAR(30) NOT NULL UNIQUE,
  id_requerimiento BIGINT NOT NULL,
  id_almacen BIGINT NOT NULL,
  preparada_por BIGINT NOT NULL,
  entregada_a VARCHAR(150) NOT NULL,
  estado ENUM('PREPARADA','ENTREGADA','ANULADA') NOT NULL DEFAULT 'PREPARADA',
  fecha_preparacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  fecha_entrega DATETIME,
  KEY idx_salida_requerimiento (id_requerimiento),
  CONSTRAINT fk_sal_req FOREIGN KEY (id_requerimiento) REFERENCES requerimiento(id_requerimiento),
  CONSTRAINT fk_sal_almacen FOREIGN KEY (id_almacen) REFERENCES almacen(id_almacen),
  CONSTRAINT fk_sal_usuario FOREIGN KEY (preparada_por) REFERENCES usuario(id_usuario)
) ENGINE=InnoDB;

CREATE TABLE evidencia_salida (
  id_evidencia BIGINT AUTO_INCREMENT PRIMARY KEY,
  id_orden_salida BIGINT NOT NULL,
  nombre_original VARCHAR(255) NOT NULL,
  ruta_segura VARCHAR(500) NOT NULL,
  tipo_mime VARCHAR(80) NOT NULL,
  tamano_bytes BIGINT NOT NULL,
  sha256 CHAR(64) NOT NULL,
  cargada_por BIGINT NOT NULL,
  cargada_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_evi_salida FOREIGN KEY (id_orden_salida) REFERENCES orden_salida(id_orden_salida),
  CONSTRAINT fk_evi_usuario FOREIGN KEY (cargada_por) REFERENCES usuario(id_usuario),
  CONSTRAINT ck_evi_tipo CHECK (tipo_mime IN ('image/jpeg','image/png','application/pdf')),
  CONSTRAINT ck_evi_tamano CHECK (tamano_bytes > 0 AND tamano_bytes <= 5242880)
) ENGINE=InnoDB;

-- Trazabilidad y controles
CREATE TABLE movimiento_inventario (
  id_movimiento BIGINT AUTO_INCREMENT PRIMARY KEY,
  id_inventario BIGINT NOT NULL,
  tipo ENUM('INGRESO','SALIDA','REVERSO_INGRESO','REVERSO_SALIDA','AJUSTE') NOT NULL,
  cantidad DECIMAL(14,3) NOT NULL,
  saldo_anterior DECIMAL(14,3) NOT NULL,
  saldo_nuevo DECIMAL(14,3) NOT NULL,
  origen_tipo ENUM('RECEPCION','ORDEN_SALIDA','ANULACION','AJUSTE') NOT NULL,
  origen_id BIGINT NOT NULL,
  registrado_por BIGINT NOT NULL,
  fecha DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  anulado BOOLEAN NOT NULL DEFAULT FALSE,
  id_movimiento_reverso BIGINT,
  CONSTRAINT fk_mov_inv FOREIGN KEY (id_inventario) REFERENCES inventario(id_inventario),
  CONSTRAINT fk_mov_usuario FOREIGN KEY (registrado_por) REFERENCES usuario(id_usuario),
  CONSTRAINT fk_mov_reverso FOREIGN KEY (id_movimiento_reverso) REFERENCES movimiento_inventario(id_movimiento),
  CONSTRAINT ck_mov_cantidad CHECK (cantidad > 0),
  UNIQUE KEY uk_mov_origen (id_inventario, tipo, origen_tipo, origen_id)
) ENGINE=InnoDB;

CREATE TABLE solicitud_anulacion (
  id_solicitud BIGINT AUTO_INCREMENT PRIMARY KEY,
  id_movimiento BIGINT NOT NULL,
  solicitada_por BIGINT NOT NULL,
  autorizada_por BIGINT,
  motivo VARCHAR(500) NOT NULL,
  estado ENUM('PENDIENTE','APROBADA','RECHAZADA') NOT NULL DEFAULT 'PENDIENTE',
  solicitada_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  resuelta_en DATETIME,
  CONSTRAINT fk_anul_mov FOREIGN KEY (id_movimiento) REFERENCES movimiento_inventario(id_movimiento),
  CONSTRAINT fk_anul_sol FOREIGN KEY (solicitada_por) REFERENCES usuario(id_usuario),
  CONSTRAINT fk_anul_aut FOREIGN KEY (autorizada_por) REFERENCES usuario(id_usuario)
) ENGINE=InnoDB;

CREATE TABLE reapertura_oc (
  id_reapertura BIGINT AUTO_INCREMENT PRIMARY KEY,
  id_orden_compra BIGINT NOT NULL,
  solicitada_por BIGINT NOT NULL,
  autorizada_por BIGINT NOT NULL,
  motivo VARCHAR(500) NOT NULL,
  version_anterior INT NOT NULL,
  requiere_nueva_aprobacion BOOLEAN NOT NULL DEFAULT TRUE,
  fecha DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_reap_oc FOREIGN KEY (id_orden_compra) REFERENCES orden_compra(id_orden_compra),
  CONSTRAINT fk_reap_sol FOREIGN KEY (solicitada_por) REFERENCES usuario(id_usuario),
  CONSTRAINT fk_reap_aut FOREIGN KEY (autorizada_por) REFERENCES usuario(id_usuario)
) ENGINE=InnoDB;

CREATE TABLE solicitud_reapertura_oc (
  id_solicitud_reapertura BIGINT AUTO_INCREMENT PRIMARY KEY,
  id_orden_compra BIGINT NOT NULL,
  solicitada_por BIGINT NOT NULL,
  autorizada_por BIGINT,
  motivo VARCHAR(500) NOT NULL,
  requiere_nueva_aprobacion BOOLEAN NOT NULL DEFAULT TRUE,
  estado ENUM('PENDIENTE','APROBADA','RECHAZADA') NOT NULL DEFAULT 'PENDIENTE',
  solicitada_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  resuelta_en DATETIME,
  CONSTRAINT fk_sreap_oc FOREIGN KEY (id_orden_compra) REFERENCES orden_compra(id_orden_compra),
  CONSTRAINT fk_sreap_sol FOREIGN KEY (solicitada_por) REFERENCES usuario(id_usuario),
  CONSTRAINT fk_sreap_aut FOREIGN KEY (autorizada_por) REFERENCES usuario(id_usuario)
) ENGINE=InnoDB;

CREATE TABLE auditoria (
  id_auditoria BIGINT AUTO_INCREMENT PRIMARY KEY,
  id_usuario BIGINT,
  accion VARCHAR(70) NOT NULL,
  entidad VARCHAR(70) NOT NULL,
  entidad_id BIGINT NOT NULL,
  detalle JSON,
  fecha DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  ip VARCHAR(45),
  CONSTRAINT fk_aud_usuario FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS historial_orden_compra (
  id_historial BIGINT AUTO_INCREMENT PRIMARY KEY,
  id_orden_compra BIGINT NOT NULL,
  version INT NOT NULL,
  datos JSON NOT NULL,
  registrada_por BIGINT NOT NULL,
  fecha DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_historial_oc_version(id_orden_compra,version),
  FOREIGN KEY(id_orden_compra) REFERENCES orden_compra(id_orden_compra),
  FOREIGN KEY(registrada_por) REFERENCES usuario(id_usuario)
) ENGINE=InnoDB;

INSERT INTO rol(nombre, descripcion) VALUES
('ADMINISTRADOR','Gestion integral del sistema'),
('ALMACEN','Catalogo, recepciones, inventario y salidas'),
('COMPRAS','Proveedores y ordenes de compra'),
('GERENCIA','Aprobacion y rechazo de adquisiciones'),
('TALLER','Registro y consulta de requerimientos')
ON DUPLICATE KEY UPDATE descripcion=VALUES(descripcion);

INSERT INTO permiso(codigo, descripcion) VALUES
('USUARIOS_ADMINISTRAR','Administrar usuarios y perfiles'),
('PRODUCTOS_ADMINISTRAR','Administrar productos'),
('INVENTARIO_CONSULTAR','Consultar existencias'),
('OC_REGISTRAR','Registrar ordenes de compra'),
('OC_AUTORIZAR','Aprobar o rechazar ordenes de compra'),
('OC_REABRIR','Reabrir ordenes de compra'),
('OC_REAPERTURA_SOLICITAR','Solicitar reapertura de ordenes de compra'),
('OC_REAPERTURA_AUTORIZAR','Autorizar reapertura de ordenes de compra'),
('RECEPCION_REGISTRAR','Registrar y confirmar recepciones'),
('REQUERIMIENTO_REGISTRAR','Registrar requerimientos de Taller'),
('SALIDA_CONFIRMAR','Preparar y confirmar salidas'),
('MOVIMIENTO_ANULAR','Autorizar anulaciones'),
('AUDITORIA_CONSULTAR','Consultar auditoria y reportes')
ON DUPLICATE KEY UPDATE descripcion=VALUES(descripcion);

INSERT IGNORE INTO rol_permiso(id_rol,id_permiso)
SELECT r.id_rol,p.id_permiso FROM rol r CROSS JOIN permiso p WHERE r.nombre='ADMINISTRADOR';
INSERT IGNORE INTO rol_permiso(id_rol,id_permiso)
SELECT r.id_rol,p.id_permiso FROM rol r JOIN permiso p ON p.codigo IN
('PRODUCTOS_ADMINISTRAR','INVENTARIO_CONSULTAR','RECEPCION_REGISTRAR','SALIDA_CONFIRMAR') WHERE r.nombre='ALMACEN';
INSERT IGNORE INTO rol_permiso(id_rol,id_permiso)
SELECT r.id_rol,p.id_permiso FROM rol r JOIN permiso p ON p.codigo IN
('OC_REGISTRAR','OC_REABRIR','OC_REAPERTURA_SOLICITAR','INVENTARIO_CONSULTAR') WHERE r.nombre='COMPRAS';
INSERT IGNORE INTO rol_permiso(id_rol,id_permiso)
SELECT r.id_rol,p.id_permiso FROM rol r JOIN permiso p ON p.codigo IN ('OC_AUTORIZAR','OC_REAPERTURA_AUTORIZAR') WHERE r.nombre='GERENCIA';
INSERT IGNORE INTO rol_permiso(id_rol,id_permiso)
SELECT r.id_rol,p.id_permiso FROM rol r JOIN permiso p ON p.codigo='REQUERIMIENTO_REGISTRAR' WHERE r.nombre='TALLER';

INSERT INTO almacen(codigo,nombre,ubicacion) VALUES ('ALM-01','Almacen principal','Chorrillos, Lima')
ON DUPLICATE KEY UPDATE nombre=VALUES(nombre), ubicacion=VALUES(ubicacion);

CREATE OR REPLACE VIEW vw_stock_alerta AS
SELECT p.id_producto,p.codigo,p.nombre,a.codigo AS almacen,COALESCE(i.stock_actual,0) AS stock_actual,p.stock_minimo,
       CASE WHEN COALESCE(i.stock_actual,0)=0 THEN 'AGOTADO' WHEN i.stock_actual<=p.stock_minimo THEN 'BAJO' ELSE 'NORMAL' END AS nivel
FROM producto p CROSS JOIN almacen a LEFT JOIN inventario i ON i.id_producto=p.id_producto AND i.id_almacen=a.id_almacen
WHERE p.activo=TRUE AND a.activo=TRUE;

CREATE OR REPLACE VIEW vw_trazabilidad_stock AS
SELECT m.id_movimiento,p.codigo AS producto,a.codigo AS almacen,m.tipo,m.cantidad,
       m.saldo_anterior,m.saldo_nuevo,m.origen_tipo,m.origen_id,u.correo,m.fecha,m.anulado
FROM movimiento_inventario m
JOIN inventario i ON i.id_inventario=m.id_inventario
JOIN producto p ON p.id_producto=i.id_producto
JOIN almacen a ON a.id_almacen=i.id_almacen
JOIN usuario u ON u.id_usuario=m.registrado_por;

DELIMITER $$

CREATE TRIGGER trg_inventario_bi
BEFORE INSERT ON inventario FOR EACH ROW
BEGIN
  IF NEW.stock_actual < 0 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='El stock no puede ser negativo'; END IF;
END$$

CREATE TRIGGER trg_inventario_bu
BEFORE UPDATE ON inventario FOR EACH ROW
BEGIN
  IF NEW.stock_actual < 0 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Stock insuficiente'; END IF;
END$$

CREATE TRIGGER trg_oc_total_ai
AFTER INSERT ON detalle_orden_compra FOR EACH ROW
BEGIN
  UPDATE orden_compra SET total=(SELECT COALESCE(SUM(subtotal),0) FROM detalle_orden_compra WHERE id_orden_compra=NEW.id_orden_compra)
  WHERE id_orden_compra=NEW.id_orden_compra;
END$$

CREATE TRIGGER trg_oc_total_au
AFTER UPDATE ON detalle_orden_compra FOR EACH ROW
BEGIN
  UPDATE orden_compra SET total=(SELECT COALESCE(SUM(subtotal),0) FROM detalle_orden_compra WHERE id_orden_compra=NEW.id_orden_compra)
  WHERE id_orden_compra=NEW.id_orden_compra;
END$$

CREATE TRIGGER trg_oc_total_ad
AFTER DELETE ON detalle_orden_compra FOR EACH ROW
BEGIN
  UPDATE orden_compra SET total=(SELECT COALESCE(SUM(subtotal),0) FROM detalle_orden_compra WHERE id_orden_compra=OLD.id_orden_compra)
  WHERE id_orden_compra=OLD.id_orden_compra;
END$$

CREATE PROCEDURE sp_decidir_oc(IN p_oc BIGINT, IN p_usuario BIGINT, IN p_decision VARCHAR(10), IN p_comentario VARCHAR(500))
BEGIN
  DECLARE v_estado VARCHAR(30);
  SELECT estado INTO v_estado FROM orden_compra WHERE id_orden_compra=p_oc FOR UPDATE;
  IF v_estado IS NULL OR v_estado <> 'PENDIENTE_APROBACION' THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='La OC no esta pendiente de aprobacion'; END IF;
  IF p_decision IS NULL OR p_decision NOT IN ('APROBADA','RECHAZADA') THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Decision invalida'; END IF;
  INSERT INTO autorizacion_oc(id_orden_compra,id_usuario,decision,comentario) VALUES(p_oc,p_usuario,p_decision,p_comentario);
  UPDATE orden_compra SET estado=p_decision WHERE id_orden_compra=p_oc;
  INSERT INTO auditoria(id_usuario,accion,entidad,entidad_id,detalle) VALUES(p_usuario,'DECIDIR_OC','ORDEN_COMPRA',p_oc,JSON_OBJECT('decision',p_decision,'comentario',p_comentario));
END$$

CREATE PROCEDURE sp_confirmar_recepcion(IN p_recepcion BIGINT, IN p_usuario BIGINT)
BEGIN
  DECLARE v_estado VARCHAR(20); DECLARE v_oc BIGINT; DECLARE v_almacen BIGINT; DECLARE v_estado_oc VARCHAR(30);
  DECLARE fin INT DEFAULT 0; DECLARE v_producto BIGINT; DECLARE v_cantidad DECIMAL(14,3); DECLARE v_inv BIGINT; DECLARE v_saldo DECIMAL(14,3);
  DECLARE cur CURSOR FOR SELECT id_producto,cantidad_conforme FROM detalle_recepcion WHERE id_recepcion=p_recepcion AND cantidad_conforme>0 ORDER BY id_producto;
  DECLARE CONTINUE HANDLER FOR NOT FOUND SET fin=1;
  SELECT estado,id_orden_compra,id_almacen INTO v_estado,v_oc,v_almacen FROM recepcion WHERE id_recepcion=p_recepcion FOR UPDATE;
  SELECT estado INTO v_estado_oc FROM orden_compra WHERE id_orden_compra=v_oc FOR UPDATE;
  IF v_estado IS NULL OR v_estado <> 'BORRADOR' THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='La recepcion ya fue procesada'; END IF;
  IF v_estado_oc NOT IN ('APROBADA','PARCIAL') THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='La OC no esta aprobada'; END IF;
  IF NOT EXISTS(SELECT 1 FROM detalle_recepcion WHERE id_recepcion=p_recepcion) THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='La recepcion no tiene detalle'; END IF;
  SET fin=0;
  IF EXISTS(SELECT 1 FROM detalle_recepcion dr LEFT JOIN detalle_orden_compra dc
    ON dc.id_orden_compra=v_oc AND dc.id_producto=dr.id_producto
    WHERE dr.id_recepcion=p_recepcion AND (dc.id_producto IS NULL OR
      dr.cantidad_conforme + COALESCE((SELECT SUM(m.cantidad) FROM movimiento_inventario m
        JOIN inventario i ON i.id_inventario=m.id_inventario
        JOIN recepcion r ON r.id_recepcion=m.origen_id
        WHERE m.origen_tipo='RECEPCION' AND m.tipo='INGRESO' AND m.anulado=FALSE
          AND r.id_orden_compra=v_oc AND i.id_producto=dr.id_producto),0)>dc.cantidad))
  THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Producto ajeno a la OC o cantidad superior al pendiente'; END IF;
  OPEN cur;
  bucle: LOOP
    FETCH cur INTO v_producto,v_cantidad; IF fin=1 THEN LEAVE bucle; END IF;
    INSERT INTO inventario(id_producto,id_almacen,stock_actual) VALUES(v_producto,v_almacen,0)
      ON DUPLICATE KEY UPDATE id_inventario=LAST_INSERT_ID(id_inventario);
    SELECT id_inventario,stock_actual INTO v_inv,v_saldo FROM inventario WHERE id_producto=v_producto AND id_almacen=v_almacen FOR UPDATE;
    UPDATE inventario SET stock_actual=stock_actual+v_cantidad WHERE id_inventario=v_inv;
    INSERT INTO movimiento_inventario(id_inventario,tipo,cantidad,saldo_anterior,saldo_nuevo,origen_tipo,origen_id,registrado_por)
      VALUES(v_inv,'INGRESO',v_cantidad,v_saldo,v_saldo+v_cantidad,'RECEPCION',p_recepcion,p_usuario);
  END LOOP; CLOSE cur;
  UPDATE recepcion SET estado=IF(EXISTS(SELECT 1 FROM detalle_recepcion WHERE id_recepcion=p_recepcion AND cantidad_rechazada>0),'OBSERVADA','CONFIRMADA') WHERE id_recepcion=p_recepcion;
  UPDATE orden_compra SET estado=IF(EXISTS(
    SELECT 1 FROM detalle_orden_compra d WHERE d.id_orden_compra=v_oc AND d.cantidad>
      COALESCE((SELECT SUM(m.cantidad) FROM movimiento_inventario m
        JOIN inventario i ON i.id_inventario=m.id_inventario JOIN recepcion r ON r.id_recepcion=m.origen_id
        WHERE m.origen_tipo='RECEPCION' AND m.tipo='INGRESO' AND m.anulado=FALSE
          AND r.id_orden_compra=v_oc AND i.id_producto=d.id_producto),0)), 'PARCIAL','RECIBIDA')
    WHERE id_orden_compra=v_oc;
  INSERT INTO auditoria(id_usuario,accion,entidad,entidad_id,detalle) VALUES(p_usuario,'CONFIRMAR_RECEPCION','RECEPCION',p_recepcion,JSON_OBJECT('ordenCompra',v_oc));
END$$

CREATE PROCEDURE sp_confirmar_salida(IN p_salida BIGINT, IN p_usuario BIGINT)
BEGIN
  DECLARE v_estado VARCHAR(20); DECLARE v_req BIGINT; DECLARE v_almacen BIGINT; DECLARE fin INT DEFAULT 0;
  DECLARE v_req_estado VARCHAR(30); DECLARE v_det BIGINT; DECLARE v_producto BIGINT; DECLARE v_cantidad DECIMAL(14,3); DECLARE v_inv BIGINT; DECLARE v_saldo DECIMAL(14,3);
  DECLARE cur CURSOR FOR SELECT d.id_detalle_requerimiento,d.id_producto,d.cantidad_solicitada-d.cantidad_atendida
    FROM orden_salida s JOIN detalle_requerimiento d ON d.id_requerimiento=s.id_requerimiento WHERE s.id_orden_salida=p_salida AND d.cantidad_solicitada>d.cantidad_atendida ORDER BY d.id_producto;
  DECLARE CONTINUE HANDLER FOR NOT FOUND SET fin=1;
  SELECT estado,id_requerimiento,id_almacen INTO v_estado,v_req,v_almacen FROM orden_salida WHERE id_orden_salida=p_salida FOR UPDATE;
  IF v_estado IS NULL OR v_estado <> 'PREPARADA' THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='La salida ya fue procesada'; END IF;
  SELECT estado INTO v_req_estado FROM requerimiento WHERE id_requerimiento=v_req FOR UPDATE;
  IF v_req_estado NOT IN ('ENVIADO','ATENDIDO_PARCIAL') THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Requerimiento no elegible'; END IF;
  IF NOT EXISTS(SELECT 1 FROM evidencia_salida WHERE id_orden_salida=p_salida) THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Debe adjuntar evidencia firmada'; END IF;
  SET fin=0;
  IF NOT EXISTS(SELECT 1 FROM detalle_requerimiento WHERE id_requerimiento=v_req AND cantidad_solicitada>cantidad_atendida)
    THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='El requerimiento no tiene cantidades pendientes'; END IF;
  OPEN cur;
  bucle: LOOP
    FETCH cur INTO v_det,v_producto,v_cantidad; IF fin=1 THEN LEAVE bucle; END IF;
    SET v_inv=NULL; SET v_saldo=NULL;
    SELECT id_inventario,stock_actual INTO v_inv,v_saldo FROM inventario WHERE id_producto=v_producto AND id_almacen=v_almacen FOR UPDATE;
    IF v_inv IS NULL THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Stock inexistente para el producto'; END IF;
    IF v_cantidad<=0 THEN ITERATE bucle; END IF;
    IF v_saldo < v_cantidad THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Stock insuficiente para confirmar la salida'; END IF;
    UPDATE inventario SET stock_actual=stock_actual-v_cantidad WHERE id_inventario=v_inv;
    UPDATE detalle_requerimiento SET cantidad_atendida=cantidad_atendida+v_cantidad WHERE id_detalle_requerimiento=v_det;
    INSERT INTO movimiento_inventario(id_inventario,tipo,cantidad,saldo_anterior,saldo_nuevo,origen_tipo,origen_id,registrado_por)
      VALUES(v_inv,'SALIDA',v_cantidad,v_saldo,v_saldo-v_cantidad,'ORDEN_SALIDA',p_salida,p_usuario);
  END LOOP; CLOSE cur;
  UPDATE orden_salida SET estado='ENTREGADA',fecha_entrega=NOW() WHERE id_orden_salida=p_salida;
  UPDATE requerimiento SET estado='ATENDIDO' WHERE id_requerimiento=v_req;
  INSERT INTO auditoria(id_usuario,accion,entidad,entidad_id,detalle) VALUES(p_usuario,'CONFIRMAR_SALIDA','ORDEN_SALIDA',p_salida,JSON_OBJECT('requerimiento',v_req));
END$$

CREATE PROCEDURE sp_reabrir_oc(IN p_oc BIGINT, IN p_solicitante BIGINT, IN p_autorizador BIGINT, IN p_motivo VARCHAR(500), IN p_nueva_aprobacion BOOLEAN)
BEGIN
  DECLARE v_estado VARCHAR(30); DECLARE v_version INT;
  SELECT estado,version INTO v_estado,v_version FROM orden_compra WHERE id_orden_compra=p_oc FOR UPDATE;
  IF v_estado IS NULL OR v_estado NOT IN ('APROBADA','RECHAZADA') THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='La OC no es elegible para reapertura'; END IF;
  INSERT INTO reapertura_oc(id_orden_compra,solicitada_por,autorizada_por,motivo,version_anterior,requiere_nueva_aprobacion)
    VALUES(p_oc,p_solicitante,p_autorizador,p_motivo,v_version,p_nueva_aprobacion);
  UPDATE orden_compra SET estado='REABIERTA',version=version+1 WHERE id_orden_compra=p_oc;
  INSERT INTO auditoria(id_usuario,accion,entidad,entidad_id,detalle) VALUES(p_autorizador,'REABRIR_OC','ORDEN_COMPRA',p_oc,JSON_OBJECT('motivo',p_motivo,'versionAnterior',v_version));
END$$

CREATE PROCEDURE sp_anular_movimiento(IN p_movimiento BIGINT, IN p_autorizador BIGINT, IN p_motivo VARCHAR(500))
BEGIN
  DECLARE v_inv BIGINT; DECLARE v_tipo VARCHAR(20); DECLARE v_cantidad DECIMAL(14,3); DECLARE v_anulado BOOLEAN; DECLARE v_saldo DECIMAL(14,3); DECLARE v_reverso BIGINT; DECLARE v_origen VARCHAR(30); DECLARE v_origen_id BIGINT; DECLARE v_producto BIGINT; DECLARE v_req BIGINT;
  SELECT id_inventario,tipo,cantidad,anulado,origen_tipo,origen_id INTO v_inv,v_tipo,v_cantidad,v_anulado,v_origen,v_origen_id FROM movimiento_inventario WHERE id_movimiento=p_movimiento FOR UPDATE;
  IF v_inv IS NULL THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Movimiento inexistente'; END IF;
  IF v_anulado THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='El movimiento ya fue anulado'; END IF;
  IF v_tipo NOT IN ('INGRESO','SALIDA') THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='El movimiento no es anulable'; END IF;
  SELECT stock_actual INTO v_saldo FROM inventario WHERE id_inventario=v_inv FOR UPDATE;
  IF v_tipo='INGRESO' AND v_saldo<v_cantidad THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='No se puede revertir: el stock ya fue consumido'; END IF;
  UPDATE inventario SET stock_actual=IF(v_tipo='INGRESO',stock_actual-v_cantidad,stock_actual+v_cantidad) WHERE id_inventario=v_inv;
  INSERT INTO movimiento_inventario(id_inventario,tipo,cantidad,saldo_anterior,saldo_nuevo,origen_tipo,origen_id,registrado_por)
    VALUES(v_inv,IF(v_tipo='INGRESO','REVERSO_INGRESO','REVERSO_SALIDA'),v_cantidad,v_saldo,IF(v_tipo='INGRESO',v_saldo-v_cantidad,v_saldo+v_cantidad),'ANULACION',p_movimiento,p_autorizador);
  SET v_reverso=LAST_INSERT_ID();
  UPDATE movimiento_inventario SET anulado=TRUE,id_movimiento_reverso=v_reverso WHERE id_movimiento=p_movimiento;
  SELECT id_producto INTO v_producto FROM inventario WHERE id_inventario=v_inv;
  IF v_origen='ORDEN_SALIDA' THEN
    SELECT id_requerimiento INTO v_req FROM orden_salida WHERE id_orden_salida=v_origen_id FOR UPDATE;
    UPDATE detalle_requerimiento SET cantidad_atendida=cantidad_atendida-v_cantidad WHERE id_requerimiento=v_req AND id_producto=v_producto;
    UPDATE requerimiento SET estado=IF(EXISTS(SELECT 1 FROM detalle_requerimiento WHERE id_requerimiento=v_req AND cantidad_atendida>0),'ATENDIDO_PARCIAL','ENVIADO') WHERE id_requerimiento=v_req;
    IF NOT EXISTS(SELECT 1 FROM movimiento_inventario WHERE origen_tipo='ORDEN_SALIDA' AND origen_id=v_origen_id AND tipo='SALIDA' AND anulado=FALSE)
    THEN UPDATE orden_salida SET estado='ANULADA' WHERE id_orden_salida=v_origen_id; END IF;
  END IF;
  IF v_origen='RECEPCION' THEN
    UPDATE orden_compra o JOIN recepcion r ON r.id_orden_compra=o.id_orden_compra SET o.estado='PARCIAL' WHERE r.id_recepcion=v_origen_id;
    IF NOT EXISTS(SELECT 1 FROM movimiento_inventario WHERE origen_tipo='RECEPCION' AND origen_id=v_origen_id AND tipo='INGRESO' AND anulado=FALSE)
    THEN UPDATE recepcion SET estado='ANULADA' WHERE id_recepcion=v_origen_id; END IF;
  END IF;
  INSERT INTO auditoria(id_usuario,accion,entidad,entidad_id,detalle) VALUES(p_autorizador,'ANULAR_MOVIMIENTO','MOVIMIENTO_INVENTARIO',p_movimiento,JSON_OBJECT('motivo',p_motivo,'reverso',v_reverso));
END$$

DELIMITER ;
