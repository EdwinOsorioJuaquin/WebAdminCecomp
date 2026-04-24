package controladores;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;

import java.io.*;
import java.io.Serializable;
import java.util.*;

import com.google.zxing.*;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.client.j2se.MatrixToImageWriter;

import org.primefaces.model.StreamedContent;
import org.primefaces.model.DefaultStreamedContent;


@Named(value = "certificadoQRController")
@SessionScoped
@Getter
@Setter
public class CertificadoQRController implements Serializable {

    private static final long serialVersionUID = 1L;

    // ============================
    // FILTRO
    // ============================
    private String strBusqueda;

    // ============================
    // LISTAS (SOLICITADOS)
    // ============================
    private List<CertificadoDemo> lstSolicitados;
    private List<CertificadoDemo> lstSolicitadosView;

    // ============================
    // QR
    // ============================
    private CertificadoDemo certificadoSeleccionado;

    // memoria temporal (simula BD)
    private static final Map<String, CertificadoDemo> mapaQR = new HashMap<>();
    
    @PostConstruct
    public void init() {

        lstSolicitados = new ArrayList<>();

        lstSolicitados.add(new CertificadoDemo(
                1001,"Juan Pérez","G1",
                "Excel Avanzado","REG-2024-001",
                "Virtual","UNS",15
        ));

        lstSolicitados.add(new CertificadoDemo(
                1002,"María López","G2",
                "Power BI","REG-2024-002",
                "Presencial","Externo",18
        ));

        lstSolicitadosView = new ArrayList<>(lstSolicitados);
    }

    // ============================
    // BUSCAR
    // ============================
    public void doBuscarPendientes(){

        String q = (strBusqueda == null) ? "" : strBusqueda.toLowerCase().trim();

        if(q.isBlank()){
            lstSolicitadosView = new ArrayList<>(lstSolicitados);
            return;
        }

        List<CertificadoDemo> filtrado = new ArrayList<>();

        for(CertificadoDemo c : lstSolicitados){
            if(c.getEstudiante().toLowerCase().contains(q)){
                filtrado.add(c);
            }
        }

        lstSolicitadosView = filtrado;
    }

    // ============================
    // GENERAR QR
    // ============================
    public void generarQR(CertificadoDemo cert){

        try{

            if(cert == null) return;

            // 🔒 evitar duplicados
            if(cert.getQrBytes() != null){
                return;
            }

            String clave = UUID.randomUUID()
                    .toString()
                    .replace("-", "")
                    .substring(0, 12);

            String url = "http://localhost:8080/WebAdminCecomp-1.0/validarCertificado.xhtml?clave=" + clave;

            cert.setClaveQR(clave);
            cert.setUrlValidacion(url);

            byte[] qr = generarImagenQR(url);
            cert.setQrBytes(qr);

            mapaQR.put(clave, cert);

        }catch(Exception e){
            e.printStackTrace();
        }
    }

    // ============================
    // SELECCIONAR (para dialog)
    // ============================
    public void seleccionarCertificado(CertificadoDemo cert){
        this.certificadoSeleccionado = cert;
    }

    // ============================
    // PREVIEW QR
    // ============================
    public StreamedContent getQrPreview(){

        if(certificadoSeleccionado == null || certificadoSeleccionado.getQrBytes() == null){
            return null;
        }

        return DefaultStreamedContent.builder()
                .contentType("image/png")
                .stream(() -> new ByteArrayInputStream(certificadoSeleccionado.getQrBytes()))
                .build();
    }

    // ============================
    // DESCARGAR QR (DESDE DIALOG)
    // ============================
    public StreamedContent getQrDownload(){

        if(certificadoSeleccionado == null || certificadoSeleccionado.getQrBytes() == null){
            return null;
        }

        return DefaultStreamedContent.builder()
                .name("QR_" + certificadoSeleccionado.getId() + ".png")
                .contentType("image/png")
                .stream(() -> new ByteArrayInputStream(certificadoSeleccionado.getQrBytes()))
                .build();
    }

    // ============================
    // GENERAR IMAGEN QR
    // ============================
    private byte[] generarImagenQR(String texto) throws Exception{

        BitMatrix matrix = new MultiFormatWriter().encode(
                texto,
                BarcodeFormat.QR_CODE,
                250,
                250
        );

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix,"PNG",baos);

        return baos.toByteArray();
    }

    // ============================
    // VALIDAR QR
    // ============================
    public CertificadoDemo validar(String clave){

        if(clave == null || clave.isBlank()){
            return null;
        }

        return mapaQR.get(clave);
    }
    
    public void guardarQR(String clave, CertificadoDemo cert){
    mapaQR.put(clave, cert);
}

public CertificadoDemo obtenerPorClave(String clave){
    return mapaQR.get(clave);
}

    // ============================
    // DEMO ENTITY
    // ============================
    @Getter
    @Setter
    public static class CertificadoDemo implements Serializable{

        private int id;
        private String estudiante;
        private String grupo;
        private String curso;
        private String regDecanatura;
        private String modalidad;
        private String procedencia;
        private int nota;

        private String claveQR;
        private String urlValidacion;
        private byte[] qrBytes;

        public CertificadoDemo(int id,String estudiante,String grupo,String curso,
                               String regDecanatura,String modalidad,String procedencia,int nota){

            this.id=id;
            this.estudiante=estudiante;
            this.grupo=grupo;
            this.curso=curso;
            this.regDecanatura=regDecanatura;
            this.modalidad=modalidad;
            this.procedencia=procedencia;
            this.nota=nota;
        }
    }
}