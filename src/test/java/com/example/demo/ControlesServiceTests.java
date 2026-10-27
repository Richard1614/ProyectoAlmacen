package com.example.demo;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.mock.web.MockMultipartFile;
import com.example.demo.almacen.servicio.AlmacenService;
import com.example.demo.almacen.repositorio.ProductoRepository;

class ControlesServiceTests {
    JdbcTemplate jdbc;
    AlmacenService service;
    @BeforeEach void preparar(){
        jdbc=mock(JdbcTemplate.class);
        service=new AlmacenService(mock(NamedParameterJdbcTemplate.class),jdbc,new BCryptPasswordEncoder(4),"target/evidencias-test",mock(ProductoRepository.class));
    }
    @Test void pf01PasswordTieneHashYRechazaClaveIncorrecta(){
        var encoder=new BCryptPasswordEncoder(4);
        String hash=encoder.encode("UnaClaveDePrueba123");
        assertNotEquals("UnaClaveDePrueba123",hash);
        assertTrue(encoder.matches("UnaClaveDePrueba123",hash));
        assertFalse(encoder.matches("otra",hash));
    }
    @Test void pf03CompraSinDetalleEsRechazada(){
        assertThrows(IllegalArgumentException.class,()->service.crearOrdenCompra(1,LocalDate.now(),"PEN",null,List.of(),1));
        verifyNoInteractions(jdbc);
    }
    @Test void pf05RecepcionRechazaCompraNoAprobada(){
        when(jdbc.queryForObject(anyString(),eq(String.class),eq(1L))).thenReturn("PENDIENTE_APROBACION");
        var linea=new AlmacenService.LineaRecepcion(1,BigDecimal.ONE,BigDecimal.ONE,BigDecimal.ZERO);
        assertThrows(IllegalArgumentException.class,()->service.crearRecepcion(1,1,"GUIA",null,List.of(linea),1));
        verify(jdbc,never()).update(anyString(),any(Object[].class));
    }
    @Test void pf07RequerimientoSinProductosEsRechazado(){
        assertThrows(IllegalArgumentException.class,()->service.crearRequerimiento("ABC-123","PREVENTIVO","MEDIA","Prueba",List.of(),1));
    }
    @Test void pf08NoSePreparaSalidaDeRequerimientoAtendido(){
        when(jdbc.queryForObject(anyString(),eq(String.class),eq(1L))).thenReturn("ATENDIDO");
        assertThrows(IllegalArgumentException.class,()->service.prepararSalida(1,1,"Operario",1));
    }
    @Test void pf10NoSeSolicitaAnulacionDeUnReverso(){
        when(jdbc.queryForMap(anyString(),eq(1L))).thenReturn(Map.of("tipo","REVERSO_SALIDA","anulado",false));
        assertThrows(IllegalArgumentException.class,()->service.solicitarAnulacion(1,1,"Error"));
    }
    @Test void pf10SolicitudResueltaNoSeProcesaOtraVez(){
        when(jdbc.queryForMap(anyString(),eq(1L))).thenReturn(Map.of("estado","APROBADA"));
        assertThrows(IllegalArgumentException.class,()->service.resolverAnulacion(1,1,true));
        verify(jdbc,never()).update(anyString(),any(Object[].class));
    }
    @Test void pf11NoSeEditaCompraAprobadaSinReapertura(){
        when(jdbc.queryForObject(anyString(),eq(String.class),eq(1L))).thenReturn("APROBADA");
        assertThrows(IllegalArgumentException.class,()->service.actualizarOrdenCompra(1,1,LocalDate.now(),"PEN",null,List.of(new AlmacenService.LineaCompra(1,BigDecimal.ONE,BigDecimal.ONE)),1));
    }
    @Test void pf11TodaEdicionVuelveAGerencia(){
        when(jdbc.queryForObject(anyString(),eq(String.class),eq(1L))).thenReturn("REABIERTA");
        service.actualizarOrdenCompra(1,2,LocalDate.of(2026,1,1),"PEN","Cambio",List.of(new AlmacenService.LineaCompra(1,BigDecimal.ONE,BigDecimal.TEN)),3);
        verify(jdbc).update(startsWith("UPDATE orden_compra SET id_proveedor="),eq(2L),eq(LocalDate.of(2026,1,1)),eq("PEN"),eq("Cambio"),eq("PENDIENTE_APROBACION"),eq(1L));
        var order=inOrder(jdbc);
        order.verify(jdbc).update(startsWith("INSERT IGNORE INTO historial_orden_compra"),eq(3L),eq(1L));
        order.verify(jdbc).update("DELETE FROM detalle_orden_compra WHERE id_orden_compra=?",1L);
    }
    @Test void rnf08MimeFalsoEsRechazado(){
        assertThrows(IllegalArgumentException.class,()->AlmacenService.validarContenido("<script>".getBytes(),"image/png"));
        assertDoesNotThrow(()->AlmacenService.validarContenido("%PDF-1.7".getBytes(),"application/pdf"));
    }
    @Test void rnf08NoSeAdjuntaAUnaSalidaYaEntregada(){
        when(jdbc.queryForObject(anyString(),eq(String.class),eq(1L))).thenReturn("ENTREGADA");
        assertThrows(IllegalArgumentException.class,()->service.guardarEvidencia(1,new MockMultipartFile("archivo","firma.pdf","application/pdf","%PDF-1.7".getBytes()),1));
    }
}
