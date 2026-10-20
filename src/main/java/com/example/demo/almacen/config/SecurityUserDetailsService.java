package com.example.demo.almacen.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.demo.almacen.modelo.Usuario;
import com.example.demo.almacen.repositorio.UsuarioRepository;

@Service
public class SecurityUserDetailsService implements UserDetailsService {
    private final UsuarioRepository usuarios;
    public SecurityUserDetailsService(UsuarioRepository usuarios) { this.usuarios = usuarios; }

    @Override @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String correo) throws UsernameNotFoundException {
        Usuario usuario = usuarios.findByCorreoIgnoreCase(correo)
            .orElseThrow(() -> new UsernameNotFoundException("Credenciales invalidas"));
        List<SimpleGrantedAuthority> autoridades = new ArrayList<>();
        autoridades.add(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().getNombre()));
        usuario.getRol().getPermisos().forEach(p -> autoridades.add(new SimpleGrantedAuthority(p.getCodigo())));
        return User.withUsername(usuario.getCorreo()).password(usuario.getPasswordHash())
            .disabled(!usuario.isActivo()).authorities(autoridades).build();
    }
}
