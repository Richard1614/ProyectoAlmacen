package com.example.demo;

import static org.junit.jupiter.api.Assertions.assertTrue;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class AustralPilotApplicationTests {
    @Test
    void elEsquemaCumpleElMinimoDelDocumento() throws Exception {
        String sql = Files.readString(Path.of("database", "01_schema.sql"));
        long tablas = sql.lines().filter(l -> l.stripLeading().startsWith("CREATE TABLE ")).count();
        long procedimientos = sql.lines().filter(l -> l.stripLeading().startsWith("CREATE PROCEDURE ")).count();
        long triggers = sql.lines().filter(l -> l.stripLeading().startsWith("CREATE TRIGGER ")).count();
        assertTrue(tablas >= 12, "Se requieren al menos 12 tablas");
        assertTrue(procedimientos >= 4, "Faltan procedimientos almacenados");
        assertTrue(triggers >= 4, "Faltan triggers de integridad");
        assertTrue(sql.contains("vw_trazabilidad_stock"));
        assertTrue(sql.contains("password_hash"));
    }
}
