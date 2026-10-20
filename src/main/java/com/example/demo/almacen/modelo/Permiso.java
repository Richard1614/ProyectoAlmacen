package com.example.demo.almacen.modelo;

import jakarta.persistence.*;

@Entity
@Table(name = "permiso")
public class Permiso {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_permiso")
    private Long id;
    @Column(nullable = false, unique = true, length = 60)
    private String codigo;
    @Column(nullable = false, length = 150)
    private String descripcion;
    public Long getId() { return id; }
    public String getCodigo() { return codigo; }
    public String getDescripcion() { return descripcion; }
}
