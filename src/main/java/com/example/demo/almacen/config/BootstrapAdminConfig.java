package com.example.demo.almacen.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.example.demo.almacen.modelo.Usuario;
import com.example.demo.almacen.repositorio.RolRepository;
import com.example.demo.almacen.repositorio.UsuarioRepository;

@Configuration
public class BootstrapAdminConfig {
    @Bean
    CommandLineRunner bootstrapAdmin(UsuarioRepository usuarios, RolRepository roles, PasswordEncoder encoder,
            @Value("${app.bootstrap.admin-email:}") String correo,
            @Value("${app.bootstrap.admin-password:}") String password,
            @Value("${app.bootstrap.admin-name:Administrador}") String nombre) {
        return args -> {
            if (correo.isBlank() || password.isBlank() || usuarios.findByCorreoIgnoreCase(correo).isPresent()) return;
            if (password.length() < 12) throw new IllegalStateException("ADMIN_PASSWORD debe tener al menos 12 caracteres");
            Usuario admin = new Usuario();
            admin.setNombre(nombre); admin.setCorreo(correo); admin.setPasswordHash(encoder.encode(password)); admin.setActivo(true);
            admin.setRol(roles.findByNombre("ADMINISTRADOR").orElseThrow(() -> new IllegalStateException("Ejecute database/01_schema.sql primero")));
            usuarios.save(admin);
        };
    }
}
