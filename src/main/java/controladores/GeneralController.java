package controladores;

import ejbPasaporte.ejb.negocio.ejbPspPspGroupUserServiceLocal;
import ejbPasaporte.ejb.negocio.ejbPspPspUsuarioServiceLocal;
import ejbPasaporte.entidades.ejbPspPspGroupuser;
import ejbPasaporte.entidades.ejbPspPspUsuario;
import jakarta.inject.*;
import jakarta.enterprise.context.*;
import jakarta.faces.context.*;
import jakarta.faces.application.ViewExpiredException;
import jakarta.servlet.http.*;
import jakarta.servlet.ServletContext;
import java.util.*;
import java.text.*;
import libreriaUdemsi.clases.*;
import libreriaUdemsi.controlador.*;
import lombok.*;
//import ejbPasaporte.entidades.*;
//import ejbPasaporte.ejb.negocio.*;
import javax.naming.*;
import static libreriaUdemsi.funciones.libreriaGeneral.doGenerarJNDI;

import jakarta.annotation.PostConstruct;
import static libreriaUdemsi.funciones.libreriaGeneral.doGenerarJNDI;

@Named(value = "generalController")
@SessionScoped
@Getter
@Setter
public class GeneralController extends BaseController {

    //1. Atributos
//    HttpSession session;
//    private String strTiempo;
//    private long lngTiempo;
    /////////////////////////////////////////////////////////////////////////////
    //---Variables globales propias de la aplicación-----------------------------
    /**
     * Identifica al usuario de la aplicacion desde pssUsurio [Trabajador UNS]
     */
    private ejbPspPspUsuario clsPspUsuario;
    private ejbPspPspGroupuser grupoUsuario;
    private Boolean blnCargado = Boolean.FALSE;
    //private ejbPspFxaEstudiante usuarioActual;
  

    //2. EJB
    ejbPspPspUsuarioServiceLocal srvPspUsuario;
    ejbPspPspGroupUserServiceLocal srvPspGroupUser;
    //ejbPspFxaEstudianteServiceLocal srvFxaEstudiante;
    

    public GeneralController() {
        try {
            Context context = (Context) new InitialContext();
            srvPspUsuario = (ejbPspPspUsuarioServiceLocal) context.lookup(doGenerarJNDI("ejbPasaporte", "1.0", "ejbPspPspUsuarioServiceLocal"));
            srvPspGroupUser = (ejbPspPspGroupUserServiceLocal) context.lookup(doGenerarJNDI("ejbPasaporte", "1.0", "ejbPspPspGroupUserServiceLocal"));
        } catch (NamingException e) {
            System.out.println("error generalController: " + e);
        }
    }

    //3. Acciones JSF Generales
    /////////////////////////////////////////////////////////////////////////////////////////
    public void doIniciarAplicacion() {
        //----Iniciando para valores de autentificacion 
        System.out.println("llego a doIniciarAplicacion");
        this.setUsuario(new gralUsuarioSistema());
        this.getUsuario().setBlnLogueado(Boolean.FALSE);
        this.setModulo(new gralModuloWeb());
        this.setArchivo(new gralArchivoServidor());
        this.setClsBusqueda(new gralBusqueda());
        //----Inciando el Servidor
        this.getModulo().setIntServidor(Integer.parseInt(this.getFramework().doLeerVariableXml(this.getRutaXml(), "Servidor")));
        //----Iniciando valores de la aplicacion
        this.getModulo().setNombre("WEB ADMIN CECOMP");
        this.getModulo().setTitulo("Admin CECOMP");
        this.getModulo().setCabecera("CENTRO DE COMPUTO - UNS");
        this.getModulo().setIntTipo(2);//1: Alumno 2:Trabajador 3:Externo
        this.getModulo().setVersion("1.0.0");
        this.getModulo().setIntUsuarioAdm(273);
        this.getModulo().setIntUsuarioMan(274);
        this.getModulo().setBlnDescarga(Boolean.FALSE);
        this.getModulo().setPresentacion("El módulo de Administrador CECOMP está diseñado para gestionar de forma ágil ");
        this.getModulo().setPresentacion(this.getModulo().getPresentacion() + "y segura el control de pagos, matrículas en cursos, certificaciones,");
        this.getModulo().setPresentacion(this.getModulo().getPresentacion() + " y además de gestionar cada curso, alumno, docente, grupos del CENTRO DE COMPUTO DE LA UNS. <br/>");

        this.setModulo(this.doCargarColor(this.getModulo()));
        //Solo para pruebas en desarrollo
        if (this.getModulo().getIntServidor() == 0) {
            //Pregrado
//            this.getUsuario().setStrUsuario("0202012031");
//            this.getUsuario().setStrSeguridad("71744022");
//            this.getUsuario().setStrClave("12345");

//            this.getUsuario().setStrUsuario("0202114004");
//            this.getUsuario().setStrSeguridad("60411361");
//            this.getUsuario().setStrClave("12345");
            this.getUsuario().setStrUsuario("0202223027");
            this.getUsuario().setStrSeguridad("71761422");
            this.getUsuario().setStrClave("12345");
            //Posgrado
//            this.getUsuario().setStrUsuario("2023832018");
//            this.getUsuario().setStrSeguridad("32955439");
//            this.getUsuario().setStrClave("12345");
        }
    }

    public void doCargarMenu() {
    List<gralMenuSub> lstSubMenu;
    gralMenuGeneral menu;
    this.getLstMenu().clear();

    //--- MENU 01: USUARIOS ----------------//
    lstSubMenu = new ArrayList<>();
    menu = new gralMenuGeneral(1, "USUARIOS", "fa fa-user", null, "d-block");
    lstSubMenu.add(new gralMenuSub(11, "Alumnos", null, null, null, true));
    lstSubMenu.add(new gralMenuSub(12, "Docentes", null, null, null, true));
    lstSubMenu.add(new gralMenuSub(13, "Decano", null, null, null, true));
    lstSubMenu.add(new gralMenuSub(14, "Director Cecomp", null, null, null, true));
    lstSubMenu.add(new gralMenuSub(15, "Administrador", null, null, null, true));
    this.getLstMenu().add(doAgregarMenu(menu, lstSubMenu));

    //--- MENU 02: GESTIONAR ----------------//
    lstSubMenu = new ArrayList<>();
    menu = new gralMenuGeneral(2, "GESTIONAR", "fa fa-folder-open", null, "d-block");
    lstSubMenu.add(new gralMenuSub(21, "Cursos", null, null, null, true));
    lstSubMenu.add(new gralMenuSub(22, "Fichas", null, null, null, true));
    lstSubMenu.add(new gralMenuSub(23, "Pagos", null, null, null, true));
    lstSubMenu.add(new gralMenuSub(24, "Asignaciones", null, null, null, true));
    lstSubMenu.add(new gralMenuSub(25, "Grupos", null, null, null, true));
    lstSubMenu.add(new gralMenuSub(26, "Tipos de pago", null, null, null, true));
    lstSubMenu.add(new gralMenuSub(27, "Notas Curso", null, null, null, true));
    lstSubMenu.add(new gralMenuSub(28, "Credenciales Acceso", null, null, null, true));
    this.getLstMenu().add(doAgregarMenu(menu, lstSubMenu));

    //--- MENU 03: CERTIFICADOS ----------------//
    lstSubMenu = new ArrayList<>();
    menu = new gralMenuGeneral(3, "CERTIFICADOS", "fa fa-certificate", null, "d-block");
    lstSubMenu.add(new gralMenuSub(31, "Solicitados", null, null, null, true));
    lstSubMenu.add(new gralMenuSub(32, "Pendientes", null, null, null, true));
    lstSubMenu.add(new gralMenuSub(33, "Firmados", null, null, null, true));
    lstSubMenu.add(new gralMenuSub(34, "Generar Token", null, null, null, true));
    lstSubMenu.add(new gralMenuSub(35, "Registros Decanatura", null, null, null, true));
    this.getLstMenu().add(doAgregarMenu(menu, lstSubMenu));

    //--- MENU 04: REPORTES ----------------//
    lstSubMenu = new ArrayList<>();
    menu = new gralMenuGeneral(4, "REPORTES", "fa fa-file-alt", null, "d-block");
    lstSubMenu.add(new gralMenuSub(41, "Certificados", null, null, null, true));
    lstSubMenu.add(new gralMenuSub(42, "Tokens", null, null, null, true));
    this.getLstMenu().add(doAgregarMenu(menu, lstSubMenu));

    //--- MENU 05: CONFIGURACIÓN ----------------//
    lstSubMenu = new ArrayList<>();
    menu = new gralMenuGeneral(5, "CONFIGURACIÓN", "fa fa-cog", null, "d-block");
    lstSubMenu.add(new gralMenuSub(51, "Respaldo", null, null, null, true));
    lstSubMenu.add(new gralMenuSub(52, "Papelera", null, null, null, true));
    lstSubMenu.add(new gralMenuSub(53, "Bitácora", null, null, null, true));
    this.getLstMenu().add(doAgregarMenu(menu, lstSubMenu));
}


    public String doNavegacion(int idMenu, int idSubMenu) {
    String ruta = "";
    System.out.println("Menú seleccionado: " + idMenu + " | Submenú: " + idSubMenu);

    this.getModulo().setBlnPrimerAcceso(Boolean.TRUE);
    this.setStrIcono(this.getLstMenu().get(idMenu - 1).getIcono());

    ruta = switch (idSubMenu) {

        // =========================
        // 01. USUARIOS
        // =========================
        case 11 -> "usuarios.alumnos";
        case 12 -> "usuarios.docentes";
        case 13 -> "usuarios.decano";
        case 14 -> "usuarios.directorcecomp";
        case 15 -> "usuarios.administrador";

        // =========================
        // 02. GESTIONAR
        // =========================
        case 21 -> "gestionar.cursos";
        case 22 -> "gestionar.fichas";
        case 23 -> "gestionar.pagos";
        case 24 -> "gestionar.asignaciones";
        case 25 -> "gestionar.grupos";
        case 26 -> "gestionar.tiposdepago";
        case 27 -> "gestionar.notas";
        case 28 -> "gestionar.credenciales";
        // =========================
        // 03. CERTIFICADOS
        // =========================
        case 31 -> "certificados.solicitados";
        case 32 -> "certificados.pendientes";
        case 33 -> "certificados.firmados";
        case 34 -> "certificados.generartoken";
        case 35 -> "certificados.registrosdecanatura";

        // =========================
        // 04. REPORTES
        // =========================
        case 41 -> "reportes.certificados";
        case 42 -> "reportes.tokens";

        // =========================
        // 05. CONFIGURACIÓN
        // =========================
        case 51 -> "configuracion.respaldo";
        case 52 -> "configuracion.papelera";
        case 53 -> "configuracion.bitacora";

        // =========================
        // RUTA POR DEFECTO
        // =========================
        default -> "inicio.ok";
    };

    return ruta;
}


    public void doDescargarReglamento() {
        try {
            this.getArchivo().setStreamedFile(this.getFramework().doDescargarFile("E:/UDEMSI-SIIGAA/Reglamentos", "ReglamentoCargaLectiva.pdf"));
            if (this.getArchivo().getStreamedFile() == null) {
                this.getFramework().setBs_mensaje("El archivo no existe en el servidor");
                this.getFramework().doMensajeF("DESCARGA", this.getFramework().getBs_mensaje(), 3);
            }
        } catch (Exception ex) {
            System.out.println("DescargaReglamento: Se salto al error");
            this.getFramework().setBs_mensaje("Error en el procesamiento del servidor");
            this.getFramework().doMensajeF("ERROR INTERNO", this.getFramework().getBs_mensaje(), 3);
        }
    }

    public String doLogueo() {
        try {
            //1. revisando si existe el usuario
            getSessionGral().setAttribute("blnAutorizado", "FALSE");
            this.getUsuario().setStrUsuario(this.getUsuario().getStrUsuario().trim().toUpperCase());
            System.out.println("clave: " + this.getUsuario().getStrClave());
            //----------------------------------------------------------------------------------------
//            this.usuarioActual = this.srvFxaEstudiante.buscarCodigo(this.getUsuario().getStrUsuario().toUpperCase());
//            if (this.usuarioActual == null) {
//                this.getFramework().doMensajeF("ERROR EN USUARIO", "El nombre de usuario es invalido, por favor verifique!", 3);
//            } else {
//                int idRpta = this.srvFxaEstudiante.autentificarEstudiante(this.usuarioActual, this.getUsuario().getStrSeguridad(), this.getFramework().doEncriptar(this.getUsuario().getStrClave(), "SHA-1"));
//                switch (idRpta) {
//                    case 0 -> {
////                        //todo correcto
////                        //----Buscando periodos validos
////                        periodoActivo = srvApcPeriodoAcademico.periodoActivo(this.usuarioActual.getApsAmbitoAcad().getIdAmbito());
////                        periodoMatricula = srvApcPeriodoAcademico.periodoMatriculaActivo(this.usuarioActual.getApsAmbitoAcad().getIdAmbito());
////                        if (periodoMatricula == null) {
////                            periodoMatricula = periodoActivo;
////                        }
//                        //----------------------------------------
//                        getSessionGral().setAttribute("blnAutorizado", "TRUE");
//                        this.getUsuario().setBlnLogueado(Boolean.TRUE);
//                        this.getUsuario().setStrNombreCompleto(this.usuarioActual.getDrtPersonanatural().getNombreCompleto());
//                        this.doCargarMenu();
//                        //Estableciendo manualmente el rol del usuario ya logueado
//                        this.setSessionGral((HttpSession) FacesContext.getCurrentInstance().getExternalContext().getSession(true));
//                        this.getSessionGral().setAttribute("userRole", "USER");
////                        session = (HttpSession) FacesContext.getCurrentInstance().getExternalContext().getSession(true);
////                        session.setAttribute("userRole", "USER");
//                        //-------------------------------------------
//                        return "acceso.ok";
//                    }
//                    case 1 -> //Error idCard
//                        this.getFramework().doMensajeF("Error en número de DNI", "El número de tarjeta es incorrecto, verifique!.", 3);
//                    case 2 -> //Error en clave       
//                        this.getFramework().doMensajeF("Error en contraseña", "La contraseña es incorrecta, verifique!", 3);
//                    case 3 -> //Error general
//                        this.getFramework().doMensajeF("Error de autentificación", "Error en el procesamiento del servidor.", 3);
//                }
//            }


            // ============================
            // LOGIN LOCAL SIN BASE DE DATOS
            // ============================

            // ⚠ Usuario mock (los mismos que cargas en doIniciarAplicacion)
            String mockUsuario = "0202223027";
            String mockDNI = "71761422";
            String mockClave = "12345";

            // Validar usuario
            if (!this.getUsuario().getStrUsuario().equalsIgnoreCase(mockUsuario)) {
                this.getFramework().doMensajeF("ERROR EN USUARIO", "El usuario no existe (modo offline).", 3);
                return "";
            }

            // Validar DNI
            if (!this.getUsuario().getStrSeguridad().equals(mockDNI)) {
                this.getFramework().doMensajeF("ERROR EN DNI", "El DNI es incorrecto (modo offline).", 3);
                return "";
            }

            // Validar clave
            if (!this.getUsuario().getStrClave().equals(mockClave)) {
                this.getFramework().doMensajeF("ERROR EN CONTRASEÑA", "La contraseña es incorrecta (modo offline).", 3);
                return "";
            }

            // ============================
            // Simular usuario encontrado
            // ============================
            this.setUsuarioActual(null); // si no tienes la clase, lo dejamos null

            // ============================
            // Simular login correcto
            // ============================
            getSessionGral().setAttribute("blnAutorizado", "TRUE");
            this.getUsuario().setBlnLogueado(Boolean.TRUE);
            this.getUsuario().setStrNombreCompleto("ADMINISTRADOR DE CECOMP");

            this.doCargarMenu();

            this.setSessionGral((HttpSession) FacesContext.getCurrentInstance().getExternalContext().getSession(true));
            this.getSessionGral().setAttribute("userRole", "ADMIN");

            return "acceso.ok";


        } catch (Exception e) {
            //this.getFramework().doMensajeF("Error general en el servidor", "Error en el servidor.", 3);
            System.out.println("Exception: " + e);
        }
        return "";
    }
    
    //4. Acciones JSF de la Aplicacion

    private void setUsuarioActual(Object object) {
        //throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

}
