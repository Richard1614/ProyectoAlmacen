package com.example.demo.almacen.repositorio;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.demo.almacen.modelo.Producto;
public interface ProductoRepository extends JpaRepository<Producto,Long> { }
