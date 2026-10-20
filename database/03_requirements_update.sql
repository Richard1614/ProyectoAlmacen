USE austral_almacen;

CREATE TABLE IF NOT EXISTS solicitud_reapertura_oc (
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

INSERT INTO permiso(codigo,descripcion) VALUES
('OC_REAPERTURA_SOLICITAR','Solicitar reapertura de ordenes de compra'),
('OC_REAPERTURA_AUTORIZAR','Autorizar reapertura de ordenes de compra')
ON DUPLICATE KEY UPDATE descripcion=VALUES(descripcion);

INSERT IGNORE INTO rol_permiso(id_rol,id_permiso)
SELECT r.id_rol,p.id_permiso FROM rol r JOIN permiso p ON p.codigo='OC_REAPERTURA_SOLICITAR' WHERE r.nombre='COMPRAS';
INSERT IGNORE INTO rol_permiso(id_rol,id_permiso)
SELECT r.id_rol,p.id_permiso FROM rol r JOIN permiso p ON p.codigo='OC_REAPERTURA_AUTORIZAR' WHERE r.nombre='GERENCIA';
INSERT IGNORE INTO rol_permiso(id_rol,id_permiso)
SELECT r.id_rol,p.id_permiso FROM rol r CROSS JOIN permiso p WHERE r.nombre='ADMINISTRADOR';
