package controladores;

import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.engine.JREmptyDataSource;
import java.io.File;
import java.io.FileOutputStream;
import java.io.ByteArrayInputStream;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import java.io.InputStream;
import ejbCecomp.clases.ejbCcoCertificadoDTO;
import ejbCecomp.clases.ejbCcoMatriculaDTO;
import ejbCecomp.entidades.*;
import ejbCecomp.ejb.negocio.*;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import lombok.Getter;
import lombok.Setter;
import net.sf.jasperreports.engine.JasperPrint;
import static libreriaUdemsi.funciones.libreriaGeneral.doGenerarJNDI;

@Named("certificadoController")
@SessionScoped
@Getter
@Setter
public class CertificadoController implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private GeneralController generalController;

    private List<ejbCcoCertificadoDTO> lstCertificadosDTO;
    private List<ejbCcoCertificadoDTO> lstCertificadosViewDTO;
    private List<ejbCcoMatriculaDTO> lstMatriculasDisponibles;
    private String strBusqueda;
    private String strValor;
    
    private ejbCcoCepCecCert clsCertificadoEdit;
    private Integer idMatriculaSeleccionada;
    private Integer resolucionSeleccionada;
    private Date fechaCertificado;
    
    private ejbCcoCepCecCertServiceLocal srvCertificado;
    private ejbCcoCepCcoMatriculaCabServiceLocal srvMatricula;

    private static final String VISTA_LISTA = "LISTA";
    private static final String VISTA_NUEVO = "NUEVO";

    public CertificadoController() {
        try {
            Context context = new InitialContext();
            srvCertificado = (ejbCcoCepCecCertServiceLocal) context.lookup(
                doGenerarJNDI("ejbCecomp", "1.0", "ejbCcoCepCecCertServiceLocal")
            );
            srvMatricula = (ejbCcoCepCcoMatriculaCabServiceLocal) context.lookup(
                doGenerarJNDI("ejbCecomp", "1.0", "ejbCcoCepCcoMatriculaCabServiceLocal")
            );
        } catch (NamingException e) {
            System.out.println("Error JNDI CertificadoController: " + e.getMessage());
        }
    }

    public void doIniciarPagina() {
        strValor = VISTA_LISTA;
        strBusqueda = "";
        cargarCertificados();
        cargarMatriculasDisponibles();
    }

    private void cargarCertificados() {
        try {
            lstCertificadosDTO = srvCertificado.listarCertificadosDTO();
            lstCertificadosViewDTO = new ArrayList<>(lstCertificadosDTO);
        } catch (Exception e) {
            System.out.println("Error cargar certificados: " + e.getMessage());
            lstCertificadosDTO = new ArrayList<>();
            lstCertificadosViewDTO = new ArrayList<>();
        }
    }
    
    private void cargarMatriculasDisponibles() {
        lstMatriculasDisponibles = new ArrayList<>();
        try {
            List<ejbCcoCepCcoMatriculaCab> matriculas = srvMatricula.listarTodos();
            if (matriculas != null) {
                for (ejbCcoCepCcoMatriculaCab mat : matriculas) {
                    if (mat.getNotaFinal() != null && mat.getNotaFinal() >= 14) {
                        boolean tieneCertificado = srvCertificado.yaTieneCertificado(mat.getIdMtaAlu());
                        if (!tieneCertificado) {
                            lstMatriculasDisponibles.add(new ejbCcoMatriculaDTO(mat));
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error cargar matrículas disponibles: " + e.getMessage());
        }
    }

    public void doBuscar() {
        String q = (strBusqueda == null) ? "" : strBusqueda.trim().toLowerCase();
        
        if (q.isBlank()) {
            lstCertificadosViewDTO = new ArrayList<>(lstCertificadosDTO);
        } else {
            List<ejbCcoCertificadoDTO> filtrado = new ArrayList<>();
            for (ejbCcoCertificadoDTO dto : lstCertificadosDTO) {
                String nombreAlumno = dto.getNombreCompleto() != null ? dto.getNombreCompleto().toLowerCase() : "";
                String dni = dto.getDni() != null ? dto.getDni().toLowerCase() : "";
                
                if (nombreAlumno.contains(q) || dni.contains(q)) {
                    filtrado.add(dto);
                }
            }
            lstCertificadosViewDTO = filtrado;
        }
        generalController.getFramework().doMensajeF("BÚSQUEDA", "Filtro aplicado correctamente", 1);
    }

    public void doNuevo() {
        strValor = VISTA_NUEVO;
        limpiarFormulario();
        cargarMatriculasDisponibles();
        strBusqueda = "";
    }

    public void doGenerarCertificado() {
        if (idMatriculaSeleccionada == null) {
            generalController.getFramework().doMensajeF("VALIDACIÓN", "Debe seleccionar una matrícula", 2);
            return;
        }
        
        if (resolucionSeleccionada == null || resolucionSeleccionada <= 0) {
            generalController.getFramework().doMensajeF("VALIDACIÓN", "Debe ingresar un número de resolución válido", 2);
            return;
        }
        
        try {
            ejbCcoCepCcoMatriculaCab matricula = srvMatricula.buscarPorId(idMatriculaSeleccionada);
            if (matricula == null) {
                generalController.getFramework().doMensajeF("ERROR", "No se encontró la matrícula", 3);
                return;
            }
            
            if (matricula.getNotaFinal() == null || matricula.getNotaFinal() < 14) {
                generalController.getFramework().doMensajeF("VALIDACIÓN", 
                    "La matrícula tiene nota " + (matricula.getNotaFinal() != null ? matricula.getNotaFinal() : "sin nota") + 
                    ". Se requiere nota >= 14 para certificar", 2);
                return;
            }
            
            if (srvCertificado.yaTieneCertificado(idMatriculaSeleccionada)) {
                generalController.getFramework().doMensajeF("VALIDACIÓN", "Esta matrícula ya tiene un certificado", 2);
                return;
            }
            
            ejbCcoCepCecCert certificado = srvCertificado.generarCertificado(
                idMatriculaSeleccionada, 
                resolucionSeleccionada, 
                fechaCertificado != null ? fechaCertificado : new Date()
            );
            
            if (certificado != null) {
                generalController.getFramework().doMensajeF("ÉXITO", "Certificado generado correctamente", 1);
                doVolver();
            } else {
                generalController.getFramework().doMensajeF("ERROR", "No se pudo generar el certificado", 3);
            }
        } catch (Exception e) {
            e.printStackTrace();
            generalController.getFramework().doMensajeF("ERROR", "Error al generar certificado: " + e.getMessage(), 3);
        }
    }

    public void doVolver() {
        strValor = VISTA_LISTA;
        strBusqueda = "";
        cargarCertificados();
        cargarMatriculasDisponibles();
    }

    private void limpiarFormulario() {
        clsCertificadoEdit = new ejbCcoCepCecCert();
        idMatriculaSeleccionada = null;
        resolucionSeleccionada = null;
        fechaCertificado = new Date();
    }
    
    public void doSeleccionarMatricula(ejbCcoMatriculaDTO matricula) {
        this.idMatriculaSeleccionada = matricula.getIdMtaAlu();
        generalController.getFramework().doMensajeF("MATRÍCULA", 
            "Matrícula seleccionada: " + matricula.getNombreCompleto() + " - " + matricula.getNombreCurso(), 1);
    }
    
    public void doDescargarCertificado(ejbCcoCertificadoDTO dto) {
        try {

            if (dto == null || dto.getIdCert() == null) {
                generalController.getFramework()
                    .doMensajeF("ERROR", "Certificado no válido", 3);
                return;
            }

            Map<String, Object> parametros = new HashMap<>();

            parametros.put("P_CODIGO",
                    "CERT-" + String.format("%06d", dto.getIdCert()));

            parametros.put("P_NOMBRE_COMPLETO",
                    dto.getNombreCompleto());

            parametros.put("P_CURSO",
                    dto.getNombreCurso());

            parametros.put("P_HORAS", "40");

            SimpleDateFormat sdf =
                    new SimpleDateFormat("dd/MM/yyyy");

            parametros.put("P_FECHA_INICIO",
                    sdf.format(dto.getFechaCert()));

            parametros.put("P_FECHA_FIN",
                    sdf.format(dto.getFechaCert()));

            SimpleDateFormat sdfLugar =
                    new SimpleDateFormat(
                            "dd 'de' MMMM 'de' yyyy",
                            new Locale("es", "PE")
                    );

            parametros.put("P_LUGAR_FECHA",
                    "Nuevo Chimbote, "
                    + sdfLugar.format(new Date()));

            parametros.put("REPORT_LOCALE",
                    new Locale("es", "PE"));

            // IMPORTANTE
            List<String> dummy = new ArrayList<>();
            dummy.add("OK");

            JasperPrint jasperPrint =
                    generalController.getFramework()
                    .doGenerarJasper(
                            dummy,
                            parametros,
                            "CertificadoUNS"
                    );

            generalController.getArchivo().setStreamedFile(
                    generalController.getFramework()
                    .doReportePdfStream(
                            jasperPrint,
                            "Certificado_" + dto.getDni()
                    )
            );

            generalController.getFramework()
                    .doMensajeF(
                            "DESCARGA",
                            "Certificado descargado correctamente",
                            1
                    );

        } catch (Exception e) {

            e.printStackTrace();

            generalController.getFramework()
                    .doMensajeF(
                            "ERROR",
                            "Error: " + e.getMessage(),
                            3
                    );
        }
    }
}