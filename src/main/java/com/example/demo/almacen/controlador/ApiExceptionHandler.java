package com.example.demo.almacen.controlador;

import java.util.Map;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler({IllegalArgumentException.class, DataAccessException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    Map<String,String> negocio(Exception ex) { return Map.of("error", mensajeSeguro(ex)); }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    Map<String,String> validacion(MethodArgumentNotValidException ex) {
        String mensaje=ex.getBindingResult().getFieldErrors().stream().findFirst()
            .map(e->e.getField()+": "+e.getDefaultMessage()).orElse("Solicitud invalida");
        return Map.of("error",mensaje);
    }

    private String mensajeSeguro(Exception ex) {
        Throwable causa=ex;
        while(causa.getCause()!=null) causa=causa.getCause();
        String mensaje=causa.getMessage();
        return mensaje==null || mensaje.isBlank() ? "No se pudo completar la operacion" : mensaje;
    }
}
