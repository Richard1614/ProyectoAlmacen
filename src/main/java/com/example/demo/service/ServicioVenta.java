package com.example.demo.service;

  import java.io.ByteArrayOutputStream;
  import java.util.List;

  import org.springframework.stereotype.Service;

  import com.example.demo.repository.Repositorio;
  import com.itextpdf.text.*;
  import com.itextpdf.text.pdf.*;

  @Service
  public class ServicioVenta {

      private final Repositorio repositorio;

      public ServicioVenta(Repositorio repositorio) {
          this.repositorio = repositorio;
      }

      public byte[] generarPdfVenta(String idVenta) throws Exception {
          List<Object[]> filas = repositorio.obtenerDetalleVenta(idVenta);

          if (filas.isEmpty()) throw new Exception("Venta no encontrada: " + idVenta);

          Object[] cab = filas.get(0);
          String numComprobante  = cab[0] + "";
          String tipoComprobante = cab[1] + "";
          String fecha           = cab[2] + "";
          String hora            = cab[3] + "";
          String clienteNombre   = cab[4] + "";
          String numDocumento    = cab[5] + "";
          String tipoDocumento   = cab[6] + "";
          String clienteDireccion= cab[7] != null ? cab[7] + "" : "-";
          String metodoPago      = cab[8] + "";
          String totalGravado    = "S/. " + String.format("%.2f", Double.parseDouble(cab[9] + ""));
          String igv             = "S/. " + String.format("%.2f", Double.parseDouble(cab[10] + ""));
          String totalPagar      = "S/. " + String.format("%.2f", Double.parseDouble(cab[11] + ""));

          Document doc = new Document(PageSize.A4, 40, 40, 50, 50);
          ByteArrayOutputStream salida = new ByteArrayOutputStream();
          PdfWriter.getInstance(doc, salida);
          doc.open();

          
          Font fuenteTitulo  = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD);
          Font fuenteSubtitulo = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD);
          Font fuenteNormal  = new Font(Font.FontFamily.HELVETICA, 10);
          Font fuenteHeader  = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, BaseColor.WHITE);

          
          Paragraph titulo = new Paragraph("KEREN MASS", fuenteTitulo);
          titulo.setAlignment(Element.ALIGN_CENTER);
          doc.add(titulo);

          Paragraph comprobante = new Paragraph(tipoComprobante + " N°: " + numComprobante, fuenteSubtitulo);
          comprobante.setAlignment(Element.ALIGN_CENTER);
          doc.add(comprobante);
          doc.add(new Paragraph(" "));

          
          doc.add(new Paragraph("Fecha: " + fecha + "   Hora: " + hora, fuenteNormal));
          doc.add(new Paragraph("Cliente: " + clienteNombre, fuenteNormal));
          doc.add(new Paragraph(tipoDocumento + ": " + numDocumento, fuenteNormal));
          doc.add(new Paragraph("Dirección: " + clienteDireccion, fuenteNormal));
          doc.add(new Paragraph("Método de pago: " + metodoPago, fuenteNormal));
          doc.add(new Paragraph(" "));

          
          PdfPTable tabla = new PdfPTable(5);
          tabla.setWidthPercentage(100);
          tabla.setWidths(new float[]{2f, 5f, 2f, 2.5f, 2.5f});

          
          String[] headers = {"Código", "Producto", "Cantidad", "Precio Unit.", "Subtotal"};
          for (String h : headers) {
              PdfPCell celda = new PdfPCell(new Phrase(h, fuenteHeader));
              celda.setBackgroundColor(BaseColor.DARK_GRAY);
              celda.setHorizontalAlignment(Element.ALIGN_CENTER);
              celda.setPadding(6);
              tabla.addCell(celda);
          }

          
          for (Object[] fila : filas) {
              tabla.addCell(new Phrase(fila[12] + "", fuenteNormal)); // id_producto
              tabla.addCell(new Phrase(fila[13] + "", fuenteNormal)); // nom_producto
              tabla.addCell(new Phrase(fila[14] + "", fuenteNormal)); // cantidad

              PdfPCell celdaPrecio = new PdfPCell(new Phrase("S/. " + String.format("%.2f", Double.parseDouble(fila[15] + "")), fuenteNormal));
              celdaPrecio.setHorizontalAlignment(Element.ALIGN_RIGHT);
              tabla.addCell(celdaPrecio);

              PdfPCell celdaSubtotal = new PdfPCell(new Phrase("S/. " + String.format("%.2f", Double.parseDouble(fila[16] + "")), fuenteNormal));
              celdaSubtotal.setHorizontalAlignment(Element.ALIGN_RIGHT);
              tabla.addCell(celdaSubtotal);
          }

          doc.add(tabla);
          doc.add(new Paragraph(" "));

          
          Paragraph pGravado = new Paragraph("Total Gravado: " + totalGravado, fuenteNormal);
          pGravado.setAlignment(Element.ALIGN_RIGHT);
          doc.add(pGravado);

          Paragraph pIgv = new Paragraph("IGV (18%): " + igv, fuenteNormal);
          pIgv.setAlignment(Element.ALIGN_RIGHT);
          doc.add(pIgv);

          Paragraph pTotal = new Paragraph("TOTAL A PAGAR: " + totalPagar, fuenteSubtitulo);
          pTotal.setAlignment(Element.ALIGN_RIGHT);
          doc.add(pTotal);

          doc.close();
          return salida.toByteArray();
      }
  }