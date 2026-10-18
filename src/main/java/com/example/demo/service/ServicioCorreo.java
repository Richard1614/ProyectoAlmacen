package com.example.demo.service;

  import java.io.ByteArrayOutputStream;
  import java.util.List;

  import org.springframework.beans.factory.annotation.Autowired;
  import org.springframework.mail.javamail.JavaMailSender;
  import org.springframework.mail.javamail.MimeMessageHelper;
  import org.springframework.stereotype.Service;

  import com.example.demo.repository.Repositorio;
  import com.itextpdf.text.*;
  import com.itextpdf.text.pdf.*;

  import jakarta.mail.internet.MimeMessage;

  @Service
  public class ServicioCorreo {

      @Autowired
      private JavaMailSender mailSender;

      @Autowired
      private Repositorio repositorio;

      public void enviarCotizacion(String idCotizacion) throws Exception {

          List<Object[]> materiales = repositorio.obtenerDetalleCotizacion(idCotizacion);
          List<Object[]> proveedores = repositorio.obtenerProveedoresCotizacion(idCotizacion);

          
          Document documento = new Document();
          ByteArrayOutputStream salida = new ByteArrayOutputStream();
          PdfWriter.getInstance(documento, salida);
          documento.open();

          documento.add(new Paragraph("SOLICITUD DE COTIZACION: " + idCotizacion));
          documento.add(new Paragraph("Para consultas contactar al logístico: 905-452-784"));
          documento.add(new Paragraph(" "));

          PdfPTable tabla = new PdfPTable(6);
          tabla.addCell("CODIGO");
          tabla.addCell("DESCRIPCION");
          tabla.addCell("STOCK MIN");
          tabla.addCell("PRECIO UNIT.");
          tabla.addCell("CANTIDAD");
          tabla.addCell("SUBTOTAL");

          for (Object[] fila : materiales) {
              tabla.addCell(fila[0] + "");
              tabla.addCell(fila[1] + "");
              tabla.addCell(fila[2] + "");
              tabla.addCell("S/. " + fila[3]);
              tabla.addCell(fila[4] + "");
              tabla.addCell("S/. " + fila[5]);
          }

          documento.add(tabla);
       
          double total = materiales.stream()
        	      .mapToDouble(f -> Double.parseDouble(f[5].toString()))
        	      .sum();
        	  documento.add(new Paragraph(" "));
        	  documento.add(new Paragraph("TOTAL ESTIMADO: S/. " + String.format("%.2f", total)));
          documento.close();

          byte[] pdfBytes = salida.toByteArray();

      
          for (Object[] prov : proveedores) {
              String correoProveedor = (String) prov[1];
              String nombreProveedor = (String) prov[0];

              MimeMessage mensaje = mailSender.createMimeMessage();
              MimeMessageHelper helper = new MimeMessageHelper(mensaje, true);
              helper.setTo(correoProveedor);
              helper.setSubject("Solicitud de Cotización - " + idCotizacion);
              helper.setText("Estimado " + nombreProveedor + ",\n\nAdjuntamos la solicitud de cotización.\nPara responder comuníquese al: 905-452-784\n\nSaludos.");
              helper.addAttachment("cotizacion_" + idCotizacion + ".pdf",
                  new org.springframework.core.io.ByteArrayResource(pdfBytes));

              mailSender.send(mensaje);
          }
      }
  }