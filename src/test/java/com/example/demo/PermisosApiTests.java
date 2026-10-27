package com.example.demo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.example.demo.almacen.controlador.AlmacenApiController;
import com.example.demo.almacen.config.SecurityConfig;
import com.example.demo.almacen.servicio.AlmacenService;

@WebMvcTest(controllers=AlmacenApiController.class,properties="app.cors.allowed-origins=http://localhost:8080")
@Import(SecurityConfig.class)
class PermisosApiTests {
    @Autowired MockMvc mvc;
    @Autowired org.springframework.web.context.WebApplicationContext context;
    @org.junit.jupiter.api.BeforeEach void prepararSeguridad(){
        mvc=org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(context)
            .apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity()).build();
    }
    @MockitoBean AlmacenService service;
    @Test void pf01AnonimoNoAccede() throws Exception {mvc.perform(get("/api/v1/sesion")).andExpect(status().isUnauthorized());}
    @Test @WithMockUser(authorities="REQUERIMIENTO_REGISTRAR")
    void pf12TallerNoAutorizaCompras() throws Exception {mvc.perform(post("/api/v1/compras/ordenes/1/decision").contentType("application/json").content("{\"decision\":\"APROBADA\"}")).andExpect(status().isForbidden());}
    @Test @WithMockUser(authorities="RECEPCION_REGISTRAR")
    void pf05AlmacenPuedeSeleccionarCompra() throws Exception {mvc.perform(get("/api/v1/compras/ordenes")).andExpect(status().isOk());}
    @Test @WithMockUser(authorities="REQUERIMIENTO_REGISTRAR")
    void pf07TallerPuedeSeleccionarProductos() throws Exception {mvc.perform(get("/api/v1/productos")).andExpect(status().isOk());}
    @Test @WithMockUser(authorities="OC_REGISTRAR")
    void pf03DatosNulosNoLleganAlServicio() throws Exception {mvc.perform(post("/api/v1/compras/ordenes").contentType("application/json").content("{\"proveedorId\":1,\"fechaEmision\":\"2026-10-22\",\"moneda\":\"PEN\",\"lineas\":[{\"productoId\":1,\"cantidad\":null,\"precioUnitario\":1}]}")).andExpect(status().isBadRequest());}
    @Test @WithMockUser(authorities="REQUERIMIENTO_REGISTRAR")
    void pf12TallerNoDescargaEvidencias() throws Exception {mvc.perform(get("/api/v1/evidencias/1/archivo")).andExpect(status().isForbidden());}
}
