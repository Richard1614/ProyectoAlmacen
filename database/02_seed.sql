USE austral_almacen;

-- Datos de demostracion sin usuarios ni contrasenas.
-- El administrador se crea de forma segura al iniciar Spring Boot mediante
-- ADMIN_EMAIL y ADMIN_PASSWORD; la contrasena se guarda con BCrypt.

INSERT INTO proveedor (ruc, razon_social, contacto, correo, telefono, direccion, activo) VALUES
('20100070970', 'Ferreyros S.A.', 'Area comercial', 'ventas.demo@ferreyros.local', '014000000', 'Lima', TRUE),
('20512345678', 'Repuestos Industriales del Sur S.A.C.', 'Maria Torres', 'ventas@repuestos-sur.local', '015550101', 'Arequipa', TRUE),
('20698765432', 'Suministros Automotrices Andinos S.A.C.', 'Carlos Mendoza', 'pedidos@saa.local', '015550202', 'Lima', TRUE)
ON DUPLICATE KEY UPDATE
  razon_social = VALUES(razon_social), contacto = VALUES(contacto),
  correo = VALUES(correo), telefono = VALUES(telefono),
  direccion = VALUES(direccion), activo = VALUES(activo);

INSERT INTO producto (codigo, nombre, descripcion, unidad_medida, stock_minimo, activo) VALUES
('FIL-ACE-001', 'Filtro de aceite', 'Filtro de aceite para mantenimiento preventivo', 'UNIDAD', 10, TRUE),
('FIL-AIR-001', 'Filtro de aire', 'Filtro de aire para unidades de transporte', 'UNIDAD', 8, TRUE),
('ACE-15W40-20', 'Aceite de motor 15W-40', 'Balde de aceite de motor de 20 litros', 'BALDE', 5, TRUE),
('REF-ROJO-01', 'Refrigerante rojo', 'Galon de refrigerante de larga duracion', 'GALON', 6, TRUE),
('PAST-FRE-01', 'Pastillas de freno', 'Juego de pastillas de freno', 'JUEGO', 4, TRUE),
('COR-ALT-001', 'Correa de alternador', 'Correa de repuesto para alternador', 'UNIDAD', 5, TRUE),
('BOM-AGU-001', 'Bomba de agua', 'Bomba de agua para motor diesel', 'UNIDAD', 2, TRUE),
('BAT-12V-001', 'Bateria 12 V', 'Bateria automotriz de 12 voltios', 'UNIDAD', 3, TRUE),
('LLA-IMP-001', 'Llave de impacto', 'Herramienta neumatica para taller', 'UNIDAD', 1, TRUE),
('GUA-NIT-100', 'Guantes de nitrilo', 'Caja de 100 guantes de proteccion', 'CAJA', 10, TRUE)
ON DUPLICATE KEY UPDATE
  nombre = VALUES(nombre), descripcion = VALUES(descripcion),
  unidad_medida = VALUES(unidad_medida), stock_minimo = VALUES(stock_minimo),
  activo = VALUES(activo);

-- El stock real solo aumenta mediante una recepcion confirmada.
INSERT INTO inventario (id_producto, id_almacen, stock_actual)
SELECT p.id_producto, a.id_almacen, 0
FROM producto p
JOIN almacen a ON a.codigo = 'ALM-01'
WHERE p.codigo IN (
  'FIL-ACE-001','FIL-AIR-001','ACE-15W40-20','REF-ROJO-01','PAST-FRE-01',
  'COR-ALT-001','BOM-AGU-001','BAT-12V-001','LLA-IMP-001','GUA-NIT-100'
)
ON DUPLICATE KEY UPDATE id_inventario = id_inventario;
