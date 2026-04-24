package controladores;

import jakarta.annotation.PostConstruct;
import static libreriaUdemsi.funciones.libreriaGeneral.doGenerarJNDI;
import jakarta.inject.*;
import jakarta.enterprise.context.*;
import jakarta.faces.context.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.util.*;
import javax.naming.*;
import lombok.*;
import net.sf.jasperreports.engine.JasperPrint;
import org.primefaces.event.*;

@Named(value = "nuevoController")
@SessionScoped
@Getter
@Setter
public class NuevoController implements Serializable {

    private static final long serialVersionUID = 1L;

    //Heredando variables de generalController
    @Inject
    GeneralController generalController;

    //1. Atributos
    private Boolean blnLista;
    private String strValor;

    //2. EJB 
    //ejbMtaMtaMatriculaCabServiceLocal srvMtaMatriculaCab;
    
    public NuevoController() {
        try {
            Context context = (Context) new InitialContext();
            //srvMtaMatriculaCab = (ejbMtaMtaMatriculaCabServiceLocal) context.lookup(doGenerarJNDI("ejbMatricula", "1.0", "ejbMtaMtaMatriculaCabServiceLocal"));
        } catch (NamingException e) {
            System.out.println("error generalController: " + e);
        }
    }



    //3. Acciones JSF
    public String doAccederGenerico() {
        HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        HttpSession sessionGral = request.getSession();
        sessionGral.setAttribute("blnAutorizado", "TRUE");
        this.generalController.doCargarMenu();
        return "generico.ok";
    }

    public void doIniciarPagina() {
    if (strValor == null || strValor.isBlank()) {
        strValor = "LISTA"; // mostrar lista al entrar
    }
    System.out.println("llego a doIniciarPAgina: vista actual = " + strValor);
}


    public void doUpload(FileUploadEvent event) {
        try {
            this.generalController.getArchivo().setRutaFile("E:/UDEMSI-SIIGAA/Admision/");
            this.generalController.getArchivo().setNombreFile("archivo.pdf");
            this.generalController.getArchivo().setInputFile(event.getFile().getInputStream());
            this.generalController.getFramework().doCargarFile(this.generalController.getArchivo());
           this.generalController.getFramework().doMensajeF("CARGA", "Archivo cargado correctamente", 1);
        } catch (IOException ex) {
            System.out.println("[doUpload] Error al cargar archivo " + "(error: " + ex + ")");
        }

    }
    
        public void doDescargar(Integer intTipo) {
        if (generalController.getLstMenu() == null) {
            this.generalController.getFramework().doMensajeF("REPORTE", "No existe reporte por descargar", 2);
        } else {
            try {
                //-----------------------
                Calendar calendar = Calendar.getInstance();
                String txtFecha = String.format("%02d", calendar.get(Calendar.DAY_OF_MONTH)) + String.format("%02d", calendar.get(Calendar.MONTH) + 1) + String.format("%04d", calendar.get(Calendar.YEAR));
                //-----------------------
                Map<String, Object> Parametros = new HashMap<>();
                Parametros.put("REPORT_LOCALE", new Locale.Builder().setLanguage("es").setRegion("PE").build());
                Parametros.put("strTitulo", "LISTA GENERAL");
                Parametros.put("strCodigoBarra", this.generalController.getFramework().doCodigoBarra13(2, 13, 123));
                String bs_nombreDescarga = "lst_descarga_gral";
                JasperPrint jasperPrint;
                switch (intTipo) {
                    case 1 -> {
                        jasperPrint = generalController.getFramework().doGenerarJasper(this.generalController.getLstMenu(), Parametros, "reporteGral");
                        generalController.getArchivo().setStreamedFile(this.generalController.getFramework().doReportePdfStream(jasperPrint, bs_nombreDescarga));
                    }
                    case 2 -> {
                        generalController.getArchivo().setNombreFile(bs_nombreDescarga);
                    }
                }
                this.generalController.getFramework().doMensajeF("REPORTE", "Archivo descargado con éxito", 1);
            } catch (Exception ex) {
                this.generalController.getFramework().setBs_mensaje("Error en el procesamiento del servidor");
                this.generalController.getFramework().doMensajeF("ERROR INTERNO", this.generalController.getFramework().getBs_mensaje(), 3);
                System.out.println("doReporteMatricula: " + ex);
            }
        }
    }
    //4. Metodos privados
        
   //Datos de prueba
   @Getter @Setter
public static class CursoDemo {
    private int id;
    private String nombre;
    private String nivel;
    private String duracion;
    private String precio;
    public CursoDemo(int id, String nombre, String nivel, String duracion, String precio) {
        this.id = id;
        this.nombre = nombre;
        this.nivel = nivel;
        this.duracion = duracion;
        this.precio = precio;
    }
}

private List<CursoDemo> lstCursos;
private CursoDemo nuevoCurso;


@PostConstruct
public void init() {
    strValor = "LISTA";
    lstCursos = Arrays.asList(
        new CursoDemo(1, "Excel Básico", "Básico", "4 semanas", "S/ 120.00"),
        new CursoDemo(2, "Programación Java", "Intermedio", "6 semanas", "S/ 160.00"),
        new CursoDemo(3, "Power BI", "Avanzado", "3 semanas", "S/ 180.00")
    );
    nuevoCurso = new CursoDemo(0, "", "", "", "");
}


}
