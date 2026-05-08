package controladores;

import ejbDirectorio.entidades.ejbDrtDrtEstadocivil;
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
import ejbPasaporte.entidades.*;
import ejbPasaporte.ejb.negocio.*;
import javax.naming.*;
import static libreriaUdemsi.funciones.libreriaGeneral.doGenerarJNDI;

import jakarta.annotation.PostConstruct;
import static libreriaUdemsi.funciones.libreriaGeneral.doGenerarJNDI;

@Named(value = "generalController")
@SessionScoped
@Getter
@Setter
public class GeneralController extends BaseController {

    private static final long serialVersionUID = 1L;

    //1. Atributos
//    HttpSession session;
//    private String strTiempo;
//    private long lngTiempo;
    private Boolean blnMostrarSeccion01 = Boolean.FALSE;
    private Boolean blnMostrarLista01 = Boolean.FALSE;
    /////////////////////////////////////////////////////////////////////////////
    //---Variables globales propias de la aplicación-----------------------------
    /**
     * Identifica al usuario de la aplicacion desde pssUsurio [Trabajador UNS]
     */
    private ejbPspPspUsuario clsPspUsuario;
    private ejbPspPspGroupuser grupoUsuario;
    private Boolean blnCargado = Boolean.FALSE;
    //private ejbPspFxaEstudiante usuarioActual;
    
    //Cambiar, el método esta en ejbPasaporte
    private List<ejbDrtDrtEstadocivil> lstEstadoCivil;

  

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
        //----Inciando el Servidor
        System.out.println("Leyendo variable Servidor del XML...");
        String servidor = this.getFramework().doLeerVariableXml(this.getRutaXml(), "Servidor");
        System.out.println("Valor de Servidor: '" + servidor + "'");
        
        System.out.println("IntServidor seteado: " + this.getModulo().getIntServidor());

        //----Iniciando valores de la aplicacion
        this.getModulo().setNombre("CECOMP ADMINISTRADOR");
        this.getModulo().setTitulo("Administrador CECOMP");
        this.getModulo().setCabecera("ADMINISTRADOR");
        this.getModulo().setIntTipo(2);//1: Alumno 2:Trabajador 3:Externo
        this.getModulo().setVersion("1.0.1");
        this.getModulo().setIntUsuarioAdm(255);
        this.getModulo().setIntUsuarioMan(254);
        this.getModulo().setBlnDescarga(Boolean.FALSE);

        this.getModulo().setPresentacion("El módulo Web CECOMP Administrador permite a los trabajadores de CECOMP consultar y generar");
        this.getModulo().setPresentacion(getModulo().getPresentacion() + " los certificados y grupos de los cursos de CECOMP.");
        this.getModulo().setPresentacion(this.getModulo().getPresentacion() + " <br/>");

        this.setModulo(this.doCargarColor(this.getModulo()));
        //Solo para pruebas en desarrollo
        if (this.getModulo().getIntServidor() == 0) {
            this.getUsuario().setStrUsuario("HNINAQUISPE");
            this.getUsuario().setStrSeguridad("042899315404");
            this.getUsuario().setStrClave("12345");
        }
    }

    public void doCargarMenu() {

        List<gralMenuSub> lstSubMenu;
        gralMenuGeneral menu;

        this.getLstMenu().clear();

        // =====================================
        // 01. ALUMNOS
        // =====================================
        lstSubMenu = new ArrayList<>();

        menu = new gralMenuGeneral(
                1,
                "ALUMNOS",
                "fa fa-user",
                null,
                "d-block"
        );

        lstSubMenu.add(new gralMenuSub(11, "Registrar Alumnos", null, null, null, true));
        lstSubMenu.add(new gralMenuSub(12, "Lista de Alumnos", null, null, null, true));

        this.getLstMenu().add(doAgregarMenu(menu, lstSubMenu));

        // =====================================
        // 02. CURSOS
        // =====================================
        lstSubMenu = new ArrayList<>();

        menu = new gralMenuGeneral(
                2,
                "CURSOS",
                "fa fa-book",
                null,
                "d-block"
        );

        lstSubMenu.add(new gralMenuSub(21, "Cursos", null, null, null, true));
        lstSubMenu.add(new gralMenuSub(22, "Plan Curricular", null, null, null, true));
        lstSubMenu.add(new gralMenuSub(23, "Precios", null, null, null, true));

        this.getLstMenu().add(doAgregarMenu(menu, lstSubMenu));

        // =====================================
        // 03. GRUPOS
        // =====================================
        lstSubMenu = new ArrayList<>();

        menu = new gralMenuGeneral(
                3,
                "GRUPOS",
                "fa fa-users",
                null,
                "d-block"
        );

        lstSubMenu.add(new gralMenuSub(31, "Grupos", null, null, null, true));
        lstSubMenu.add(new gralMenuSub(32, "Docentes", null, null, null, true));

        this.getLstMenu().add(doAgregarMenu(menu, lstSubMenu));

        // =====================================
        // 04. CERTIFICADOS
        // =====================================
        lstSubMenu = new ArrayList<>();

        menu = new gralMenuGeneral(
                4,
                "CERTIFICADOS",
                "fa fa-certificate",
                null,
                "d-block"
        );

        lstSubMenu.add(new gralMenuSub(41, "Certificados", null, null, null, true));
        lstSubMenu.add(new gralMenuSub(42, "Generar", null, null, null, true));
        lstSubMenu.add(new gralMenuSub(43, "Registros Decanatura", null, null, null, true));

        this.getLstMenu().add(doAgregarMenu(menu, lstSubMenu));

        // =====================================
        // 05. FICHAS
        // =====================================
        lstSubMenu = new ArrayList<>();

        menu = new gralMenuGeneral(
                5,
                "FICHAS",
                "fa fa-file-alt",
                null,
                "d-block"
        );

        lstSubMenu.add(new gralMenuSub(51, "Fichas", null, null, null, true));
        lstSubMenu.add(new gralMenuSub(52, "Notas", null, null, null, true));


        this.getLstMenu().add(doAgregarMenu(menu, lstSubMenu));

        // =====================================
        // 06. PAGOS
        // =====================================
        lstSubMenu = new ArrayList<>();

        menu = new gralMenuGeneral(
                6,
                "PAGOS",
                "fa fa-money-bill",
                null,
                "d-block"
        );

        lstSubMenu.add(new gralMenuSub(61, "Pagos", null, null, null, true));

        this.getLstMenu().add(doAgregarMenu(menu, lstSubMenu));
    }


    public String doNavegacion(int idMenu, int idSubMenu) {

        String ruta;

        System.out.println("Menú seleccionado: " + idMenu + " | Submenú: " + idSubMenu);

        this.getModulo().setBlnPrimerAcceso(Boolean.TRUE);
        this.setStrIcono(this.getLstMenu().get(idMenu - 1).getIcono());

        ruta = switch (idSubMenu) {

            case 0 ->
                "acceso.ok";

            // =====================================
            // 01. ALUMNOS
            // =====================================
            case 11 -> "alumnos.registro";
            case 12 -> "alumnos.lista";

            // =====================================
            // 02. CURSOS
            // =====================================
            case 21 -> "cursos.lista";
            case 22 -> "cursos.plan";
            case 23 -> "cursos.precios";

            // =====================================
            // 03. GRUPOS
            // =====================================
            case 31 -> "grupos.lista";
            case 32 -> "grupos.docentes";

            // =====================================
            // 04. CERTIFICADOS
            // =====================================
            case 41 -> "certificados.lista";
            case 42 -> "certificados.generar";
            case 43 -> "certificados.registros";

            // =====================================
            // 05. FICHAS
            // =====================================
            case 51 -> "fichas.lista";
            case 52 -> "fichas.lista";

            // =====================================
            // 06. PAGOS
            // =====================================
            case 61 -> "pagos.lista";

            // =====================================
            // RUTA POR DEFECTO
            // =====================================
            default -> "construccion.ok";
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
        //1. revisando si existe el usuario
        getSessionGral().setAttribute("blnAutorizado", "FALSE");
        this.getUsuario().setStrUsuario(this.getUsuario().getStrUsuario().trim().toUpperCase());
        this.clsPspUsuario = this.srvPspUsuario.buscarUsuario(this.getUsuario().getStrUsuario().toUpperCase());
        if (this.clsPspUsuario == null) {
            this.getFramework().doMensajeF("ERROR EN USUARIO", "El nombre de usuario es invalido, por favor verifique!", 3);
        } else {
            //Verificar metodo de logueo
            int idRpta = this.srvPspUsuario.autentificarUsuario(clsPspUsuario, this.getUsuario().getStrSeguridad(), this.getFramework().doEncriptar2(this.getUsuario().getStrClave()));
            //SETEANDO A idRpta 0
            idRpta=0;
            switch (idRpta) {
                case 0: //todo correcto
                    //2. revisar si esta asignado al modulo
                    System.out.println("antes de daolocal");
                    ejbPspPspGroupuser groupUser = this.srvPspGroupUser.buscarGrupoUsuario(clsPspUsuario, this.getModulo().getIntUsuarioAdm());
                    if (groupUser == null) {
                        groupUser = this.srvPspGroupUser.buscarGrupoUsuario(clsPspUsuario, this.getModulo().getIntUsuarioMan());
                    }
                    if (groupUser == null) {
                        this.getFramework().doMensajeF("ERROR EN PERMISO", "El usuario no tiene permiso para acceder a la aplicación.", 3);
                    } else {
                        getSessionGral().setAttribute("blnAutorizado", "TRUE");
                        this.getUsuario().setBlnLogueado(Boolean.TRUE);
                        this.getUsuario().setStrNombreCompleto(this.clsPspUsuario.getDrtDirectorio().getDrtPersonanatural().getNombreCompleto());
                        this.doCargarMenu();
                        return "acceso.ok";
                    }
                    break;
                case 1: //Error idCard
                    this.getFramework().doMensajeF("ERROR EN TARJETA", "El número de tarjeta es incorrecto, verifique!.", 3);
                    break;
                case 2: //Error en clave       
                    this.getFramework().doMensajeF("ERROR EN CONTRASEÑA", "La contraseña es incorrecta, verifique!", 3);
                    break;
                case 3: //Error general
                    this.getFramework().doMensajeF("ERROR GENERAL", "Error en el procesamiento del servidor.", 3);
                    break;
            }
        }
        return "";
    }
    
    public void doOcultarSecciones() {
        this.blnMostrarSeccion01 = Boolean.FALSE;
        this.blnMostrarLista01 = Boolean.FALSE;
    }
    
    //4. Acciones JSF Especificas Sde la Aplicacion

    private void setUsuarioActual(Object object) {
        //throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
    

}
