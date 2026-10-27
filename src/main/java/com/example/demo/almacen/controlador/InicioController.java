package com.example.demo.almacen.controlador;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
@Controller
public class InicioController {
    @GetMapping({"/", "/index.html"})
    public String inicio() { return "index"; }
}
