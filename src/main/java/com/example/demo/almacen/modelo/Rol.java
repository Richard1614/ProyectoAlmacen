package com.example.demo.almacen.modelo;

import java.util.LinkedHashSet;
import java.util.Set;
import jakarta.persistence.*;

@Entity
@Table(name = "rol")
public class Rol {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_rol")
    private Long id;
    @Column(nullable = false, unique = true, length = 40)
    private String nombre;
    @Column(nullable = false, length = 150)
    private String descripcion;
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "rol_permiso", joinColumns = @JoinColumn(name = "id_rol"), inverseJoinColumns = @JoinColumn(name = "id_permiso"))
    private Set<Permiso> permisos = new LinkedHashSet<>();
    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public Set<Permiso> getPermisos() { return permisos; }
}
