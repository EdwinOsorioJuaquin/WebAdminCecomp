package controladores;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;

import java.io.*;
import java.io.Serializable;
import java.util.*;

import java.io.ByteArrayOutputStream;
import java.util.UUID;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.client.j2se.MatrixToImageWriter;

import com.google.zxing.common.BitMatrix;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import ejbCecomp.clases.ejbCcoCertificadoPendiente;
import ejbCecomp.ejb.negocio.ejbCcoCcoCertificadoQrServiceLocal;
import jakarta.inject.Inject;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import libreriaUdemsi.controlador.BaseController;
import static libreriaUdemsi.funciones.libreriaGeneral.doGenerarJNDI;



@Named(value = "certificadoPendienteQRController")
@SessionScoped
@Getter
@Setter
public class CertificadoPendienteQRController extends BaseController implements Serializable {

    private static final long serialVersionUID = 1L;

    //Heredando variables de generalController
    @Inject
    GeneralController generalController;

    //1. Atributos
    private Boolean blnLista;
    private String strValor;
    private String strBusqueda;
    
    private List<ejbCcoCertificadoPendiente> lstPendientes;
    private ejbCcoCertificadoPendiente pendienteSeleccionado;

    //2. EJB 
    ejbCcoCcoCertificadoQrServiceLocal srvQr;    
    
    public CertificadoPendienteQRController() {
        try {
            Context context = (Context) new InitialContext();
            srvQr = (ejbCcoCcoCertificadoQrServiceLocal) context.lookup(doGenerarJNDI("ejbCecomp", "1.0", "ejbCcoCcoCertificadoQrServiceLocal"));
        } catch (NamingException e) {
            System.out.println("error generalController: " + e);
        }
    }


    public void doIniciarPagina() {
        lstPendientes=srvQr.listarPendientesQr();
        System.out.println("llego a doIniciarPAgina: vista actual = " + strValor);
    }

    public void doGenerarQr(ejbCcoCertificadoPendiente pendiente) {

        try {

            if (pendiente == null) {
                return;
            }

            if (Boolean.TRUE.equals(pendiente.getTieneQr())) {

                this.getFramework().setBs_mensaje("El certificado ya tiene QR generado");
                this.getFramework().doMensajeF("QR EXISTE", this.getFramework().getBs_mensaje(), 3);
                return;
            }

            String codigoQr =
                    "QR-CECOMP-" +
                    Calendar.getInstance().get(Calendar.YEAR) +
                    "-" +
                    pendiente.getIdCert();

            String urlValidacion =
                    "http://localhost:8080/WebVerificadorCertificadoCecomp/faces/login.xhtml?clave="
                    + codigoQr;

            byte[] imagenQr =
                    generarImagenQR(urlValidacion);

            boolean guardado =
                    srvQr.guardarQr(
                            pendiente.getIdCert(),
                            codigoQr,
                            urlValidacion,
                            imagenQr);

            if (guardado) {

                pendiente.setTieneQr(true);

                this.getFramework().setBs_mensaje("El certificado ya tiene QR generado");
                this.getFramework().doMensajeF("QR EXISTE", this.getFramework().getBs_mensaje(), 3);
                
            } else {

                this.getFramework().setBs_mensaje("QR generado correctamente");
                this.getFramework().doMensajeF("QR GENERADO", this.getFramework().getBs_mensaje(), 1);
                
            }

        } catch (Exception e) {

            e.printStackTrace();

            this.getFramework().setBs_mensaje("No se pudo generar el QR");
            this.getFramework().doMensajeF("ERROR", this.getFramework().getBs_mensaje(), 3);
        }
    }
 
    private byte[] generarImagenQR(String texto) throws Exception {

        BitMatrix matrix =
                new MultiFormatWriter().encode(
                        texto,
                        BarcodeFormat.QR_CODE,
                        250,
                        250);

        ByteArrayOutputStream baos =
                new ByteArrayOutputStream();

        MatrixToImageWriter.writeToStream(
                matrix,
                "PNG",
                baos);

        return baos.toByteArray();
    }
}