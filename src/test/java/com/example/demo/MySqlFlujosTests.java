package com.example.demo;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import com.example.demo.almacen.servicio.AlmacenService;
import com.example.demo.almacen.repositorio.ProductoRepository;

/** Opt-in: creates and removes only its own randomly named test database. */
@EnabledIfEnvironmentVariable(named="TEST_MYSQL_URL",matches="jdbc:mysql:.*")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MySqlFlujosTests {
    Connection connection;
    JdbcTemplate jdbc;
    AlmacenService service;
    String database;
    long user,provider,product,warehouse,oc;
    @BeforeAll void esquema() throws Exception {
        connection=DriverManager.getConnection(System.getenv("TEST_MYSQL_URL"),
            System.getenv().getOrDefault("TEST_MYSQL_USER","root"),System.getenv().getOrDefault("TEST_MYSQL_PASSWORD",""));
        database="austral_test_"+UUID.randomUUID().toString().replace("-","");
        connection.createStatement().execute("CREATE DATABASE "+database+" CHARACTER SET utf8mb4");
        connection.setCatalog(database);
        String delimiter=";";
        StringBuilder command=new StringBuilder();
        for(String line:Files.readAllLines(Path.of("database/01_schema.sql"))){
            String trimmed=line.trim();
            if(trimmed.startsWith("--") || trimmed.isEmpty()) continue;
            if(trimmed.startsWith("DELIMITER ")){delimiter=trimmed.substring(10).trim();continue;}
            command.append(line).append('\n');
            if(trimmed.endsWith(delimiter)){
                String sql=command.toString().trim(); sql=sql.substring(0,sql.length()-delimiter.length());
                if(!sql.startsWith("CREATE DATABASE") && !sql.startsWith("USE ")) connection.createStatement().execute(sql);
                command.setLength(0);
            }
        }
        var source=new SingleConnectionDataSource(connection,true);
        jdbc=new JdbcTemplate(source);
        service=new AlmacenService(new NamedParameterJdbcTemplate(source),jdbc,new BCryptPasswordEncoder(4),"target/evidencias-test",mock(ProductoRepository.class));
    }
    @AfterAll void cerrar() throws Exception {
        if(connection!=null){
            connection.rollback(); connection.setAutoCommit(true);
            if(database!=null && database.matches("austral_test_[0-9a-f]{32}")) connection.createStatement().execute("DROP DATABASE "+database);
            connection.close();
        }
    }
    @BeforeEach void datos(){
        try {connection.setAutoCommit(false);} catch(SQLException e){throw new RuntimeException(e);}
        String suffix=UUID.randomUUID().toString().substring(0,8);
        user=insert("INSERT INTO usuario(id_rol,nombre,correo,password_hash) SELECT id_rol,'Prueba',?,? FROM rol WHERE nombre='ADMINISTRADOR'",suffix+"@test.invalid",new BCryptPasswordEncoder(4).encode("Prueba12345678"));
        provider=insert("INSERT INTO proveedor(ruc,razon_social) VALUES(?,?)",String.format("%011d",Math.abs(UUID.randomUUID().getLeastSignificantBits()%100000000000L)),"Prueba");
        product=insert("INSERT INTO producto(codigo,nombre,unidad_medida,stock_minimo) VALUES(?,'Repuesto','UNIDAD',2)",suffix);
        warehouse=jdbc.queryForObject("SELECT id_almacen FROM almacen WHERE codigo='ALM-01'",Long.class);
        oc=service.crearOrdenCompra(provider,LocalDate.now(),"PEN",null,List.of(new AlmacenService.LineaCompra(product,new BigDecimal("10"),BigDecimal.TEN)),user);
    }
    @AfterEach void revertir() throws Exception {connection.rollback();}
    long insert(String sql,Object...args){
        var holder=new org.springframework.jdbc.support.GeneratedKeyHolder();
        jdbc.update(c->{PreparedStatement p=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS);for(int i=0;i<args.length;i++)p.setObject(i+1,args[i]);return p;},holder);
        return holder.getKey().longValue();
    }
    BigDecimal stock(){return jdbc.queryForObject("SELECT COALESCE(SUM(stock_actual),0) FROM inventario WHERE id_producto=? AND id_almacen=?",BigDecimal.class,product,warehouse);}
    void aprobar(){service.decidirOc(oc,user,"APROBADA","Prueba");}
    long recibir(int conforme,int rechazada){
        return service.crearRecepcion(oc,warehouse,UUID.randomUUID().toString(),null,List.of(new AlmacenService.LineaRecepcion(product,BigDecimal.valueOf(conforme+rechazada),BigDecimal.valueOf(conforme),BigDecimal.valueOf(rechazada))),user);
    }
    long salida(int cantidad,boolean evidencia){
        long req=service.crearRequerimiento("TEST-01","PREVENTIVO","MEDIA","Prueba",List.of(new AlmacenService.LineaRequerimiento(product,BigDecimal.valueOf(cantidad))),user);
        long id=service.prepararSalida(req,warehouse,"Operario de prueba",user);
        if(evidencia) insert("INSERT INTO evidencia_salida(id_orden_salida,nombre_original,ruta_segura,tipo_mime,tamano_bytes,sha256,cargada_por) VALUES(?,'firma.pdf','test/firma.pdf','application/pdf',10,?,?)",id,"0".repeat(64),user);
        return id;
    }
    @Test void pf02ProductoNuevoFiguraAgotado(){assertEquals("AGOTADO",jdbc.queryForObject("SELECT nivel FROM vw_stock_alerta WHERE id_producto=?",String.class,product));}
    @Test void pf03CompraPendiente(){assertEquals("PENDIENTE_APROBACION",jdbc.queryForObject("SELECT estado FROM orden_compra WHERE id_orden_compra=?",String.class,oc));}
    @Test void pf04DecisionQuedaAuditada(){aprobar();assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM autorizacion_oc WHERE id_orden_compra=?",Integer.class,oc));}
    @Test void pf05RecepcionCompletaCierraCompra(){aprobar();service.confirmarRecepcion(recibir(10,0),user);assertEquals(0,stock().compareTo(BigDecimal.TEN));assertEquals("RECIBIDA",jdbc.queryForObject("SELECT estado FROM orden_compra WHERE id_orden_compra=?",String.class,oc));}
    @Test void pf06RechazadosNoIncrementanStock(){aprobar();long id=recibir(8,2);service.confirmarRecepcion(id,user);service.registrarIncidencia(id,product,"DANIO","Dos rechazados","CAMBIO",user);assertEquals(0,stock().compareTo(BigDecimal.valueOf(8)));assertEquals("OBSERVADA",jdbc.queryForObject("SELECT estado FROM recepcion WHERE id_recepcion=?",String.class,id));}
    @Test void pf07TallerEsVisibleParaAlmacen(){long id=service.crearRequerimiento("TEST","PREVENTIVO","MEDIA","Prueba",List.of(new AlmacenService.LineaRequerimiento(product,BigDecimal.ONE)),user);assertEquals(1,service.detalleRequerimiento(id).size());}
    @Test void pf08EntregaDescuentaStock(){aprobar();service.confirmarRecepcion(recibir(10,0),user);long id=salida(3,true);service.confirmarSalida(id,user);assertEquals(0,stock().compareTo(BigDecimal.valueOf(7)));}
    @Test void pf09SinInventarioNoSeEntrega(){long id=salida(3,true);assertThrows(org.springframework.dao.DataAccessException.class,()->service.confirmarSalida(id,user));assertEquals("PREPARADA",jdbc.queryForObject("SELECT estado FROM orden_salida WHERE id_orden_salida=?",String.class,id));}
    @Test void pf10AnulacionRestituyeStockYConservaMovimiento(){aprobar();service.confirmarRecepcion(recibir(10,0),user);long id=salida(3,true);service.confirmarSalida(id,user);long mov=jdbc.queryForObject("SELECT id_movimiento FROM movimiento_inventario WHERE origen_tipo='ORDEN_SALIDA' AND origen_id=?",Long.class,id);long solicitud=service.solicitarAnulacion(mov,user,"Error de registro");service.resolverAnulacion(solicitud,user,true);assertEquals(0,stock().compareTo(BigDecimal.TEN));assertEquals("ANULADA",jdbc.queryForObject("SELECT estado FROM orden_salida WHERE id_orden_salida=?",String.class,id));}
    @Test void pf11ReaperturaConservaDetalleAnterior(){aprobar();long solicitud=service.solicitarReapertura(oc,user,"Cambiar cantidad",false);service.resolverReapertura(solicitud,user,true);service.actualizarOrdenCompra(oc,provider,LocalDate.now(),"PEN",null,List.of(new AlmacenService.LineaCompra(product,BigDecimal.ONE,BigDecimal.TEN)),user);assertEquals(1,service.historialOrden(oc).size());assertEquals("PENDIENTE_APROBACION",jdbc.queryForObject("SELECT estado FROM orden_compra WHERE id_orden_compra=?",String.class,oc));}
    @Test void pf13NoHayEntregaSinEvidencia(){long id=salida(1,false);assertThrows(org.springframework.dao.DataAccessException.class,()->service.confirmarSalida(id,user));assertEquals(0,stock().compareTo(BigDecimal.ZERO));}
    @Test void pf14DobleConfirmacionNoDuplicaDescuento(){aprobar();service.confirmarRecepcion(recibir(10,0),user);long id=salida(3,true);service.confirmarSalida(id,user);assertThrows(org.springframework.dao.DataAccessException.class,()->service.confirmarSalida(id,user));assertEquals(0,stock().compareTo(BigDecimal.valueOf(7)));}
    @Test void noRecibeMasDeLoComprado(){aprobar();long id=recibir(11,0);assertThrows(org.springframework.dao.DataAccessException.class,()->service.confirmarRecepcion(id,user));assertEquals(0,stock().compareTo(BigDecimal.ZERO));}
}
