USE austral_almacen;
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

CREATE OR REPLACE VIEW vw_stock_alerta AS
SELECT p.id_producto,p.codigo,p.nombre,a.codigo AS almacen,COALESCE(i.stock_actual,0) AS stock_actual,p.stock_minimo,
       CASE WHEN COALESCE(i.stock_actual,0)=0 THEN 'AGOTADO' WHEN i.stock_actual<=p.stock_minimo THEN 'BAJO' ELSE 'NORMAL' END AS nivel
FROM producto p CROSS JOIN almacen a LEFT JOIN inventario i ON i.id_producto=p.id_producto AND i.id_almacen=a.id_almacen
WHERE p.activo=TRUE AND a.activo=TRUE;


DELIMITER $$
DROP PROCEDURE IF EXISTS sp_decidir_oc$$
DROP PROCEDURE IF EXISTS sp_confirmar_recepcion$$
DROP PROCEDURE IF EXISTS sp_confirmar_salida$$
DROP PROCEDURE IF EXISTS sp_reabrir_oc$$
DROP PROCEDURE IF EXISTS sp_anular_movimiento$$
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
-- Allow a replacement order after all original movements are annulled.
-- Run the following once on installations created before this migration:
-- ALTER TABLE orden_salida DROP INDEX uk_salida_requerimiento, ADD INDEX idx_salida_requerimiento(id_requerimiento);
