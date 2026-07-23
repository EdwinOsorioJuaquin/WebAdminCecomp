package controladores;

import ejbCecomp.clases.ejbCcoMatriculaDTO;
import ejbCecomp.ejb.negocio.*;
import ejbCecomp.entidades.*;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import lombok.*;
import static libreriaUdemsi.funciones.libreriaGeneral.doGenerarJNDI;

@Named(value = "fichaController")
@SessionScoped
@Getter
@Setter
public class FichaController implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private GeneralController generalController;

    // ============================
    // LISTAS
    // ============================
    private List<ejbCcoMatriculaDTO> lstMatriculasDTO;
    private List<ejbCcoMatriculaDTO> lstMatriculasViewDTO;
    private String strBusqueda;
    private String strValor;

    // ============================
    // FORMULARIO
    // ============================
    private ejbCcoCepCcoMatriculaCab clsMatriculaEdit;
    private Integer idGrupoSeleccionado;
    private boolean modoEdicion;

    // ============================
    // CONSTANTES DE VISTA
    // ============================
    private static final String VISTA_LISTA = "LISTA";
    private static final String VISTA_NUEVO = "NUEVO";
    private static final String VISTA_EDITAR = "EDITAR";

    // ============================
    // SERVICIOS EJB
    // ============================
    private ejbCcoCepCcoMatriculaCabServiceLocal srvMatricula;
    private ejbCcoCepCursoDocenteServiceLocal srvGrupo;
    private ejbCcoCepCursoServiceLocal srvCurso;
    private ejbCcoDrtPersonanaturalServiceLocal srvPersona;

    // ============================
    // CONSTRUCTOR
    // ============================
    public FichaController() {
        try {
            Context context = new InitialContext();
            srvMatricula = (ejbCcoCepCcoMatriculaCabServiceLocal) context.lookup(
                doGenerarJNDI("ejbCecomp", "1.0", "ejbCcoCepCcoMatriculaCabServiceLocal")
            );
            srvGrupo = (ejbCcoCepCursoDocenteServiceLocal) context.lookup(
                doGenerarJNDI("ejbCecomp", "1.0", "ejbCcoCepCursoDocenteServiceLocal")
            );
            srvCurso = (ejbCcoCepCursoServiceLocal) context.lookup(
                doGenerarJNDI("ejbCecomp", "1.0", "ejbCcoCepCursoServiceLocal")
            );
            srvPersona = (ejbCcoDrtPersonanaturalServiceLocal) context.lookup(
                doGenerarJNDI("ejbCecomp", "1.0", "ejbCcoDrtPersonanaturalServiceLocal")
            );
        } catch (NamingException e) {
            System.out.println("Error JNDI FichaController: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ============================
    // INICIALIZACIÓN
    // ============================
    @PostConstruct
    public void init() {
        strValor = VISTA_LISTA;
        strBusqueda = "";
        lstMatriculasDTO = new ArrayList<>();
        lstMatriculasViewDTO = new ArrayList<>();
        clsMatriculaEdit = new ejbCcoCepCcoMatriculaCab();
    }

    public void doIniciarPagina() {
        if (strValor == null || strValor.isBlank()) {
            strValor = VISTA_LISTA;
        }
        cargarMatriculas();
    }

    // ============================
    // CARGA DE DATOS
    // ============================
    private void cargarMatriculas() {
        try {
            List<ejbCcoCepCcoMatriculaCab> matriculas = srvMatricula.listarTodos();
            lstMatriculasDTO = new ArrayList<>();
            if (matriculas != null) {
                for (ejbCcoCepCcoMatriculaCab matricula : matriculas) {
                    lstMatriculasDTO.add(new ejbCcoMatriculaDTO(matricula));
                }
            }
            lstMatriculasViewDTO = new ArrayList<>(lstMatriculasDTO);
        } catch (Exception e) {
            System.out.println("Error cargar matrículas: " + e.getMessage());
            e.printStackTrace();
            lstMatriculasDTO = new ArrayList<>();
            lstMatriculasViewDTO = new ArrayList<>();
        }
    }

    // ============================
    // BÚSQUEDA
    // ============================
    public void doBuscar() {
        String q = (strBusqueda == null) ? "" : strBusqueda.trim().toLowerCase();

        if (q.isBlank()) {
            lstMatriculasViewDTO = new ArrayList<>(lstMatriculasDTO);
            generalController.getFramework().doMensajeF("BÚSQUEDA", "Mostrando todas las matrículas", 1);
            return;
        }

        List<ejbCcoMatriculaDTO> filtrado = new ArrayList<>();
        for (ejbCcoMatriculaDTO dto : lstMatriculasDTO) {
            String alumno = dto.getNombreCompleto() != null ? dto.getNombreCompleto().toLowerCase() : "";
            String curso = dto.getNombreCurso() != null ? dto.getNombreCurso().toLowerCase() : "";
            if (alumno.contains(q) || curso.contains(q)) {
                filtrado.add(dto);
            }
        }
        lstMatriculasViewDTO = filtrado;
        generalController.getFramework().doMensajeF("BÚSQUEDA",
            "Se encontraron " + lstMatriculasViewDTO.size() + " matrículas", 1);
    }

    // ============================
    // NUEVO
    // ============================
    public void doNuevo() {
        strValor = VISTA_NUEVO;
        modoEdicion = false;
        limpiarFormulario();
    }

    // ============================
    // EDITAR
    // ============================
    public void doEditar(ejbCcoMatriculaDTO dto) {
        if (dto == null || dto.getIdMtaAlu() == null) {
            generalController.getFramework().doMensajeF("ERROR", "Matrícula no válida", 3);
            return;
        }

        strValor = VISTA_EDITAR;
        modoEdicion = true;

        clsMatriculaEdit = srvMatricula.buscarPorId(dto.getIdMtaAlu());
        if (clsMatriculaEdit != null) {
            // Obtener el grupo seleccionado
            if (clsMatriculaEdit.getCepCursoDocente() != null) {
                idGrupoSeleccionado = clsMatriculaEdit.getCepCursoDocente().getIdAd();
            }
        } else {
            generalController.getFramework().doMensajeF("ERROR", "No se encontró la matrícula", 3);
            doVolver();
        }
    }

    // ============================
    // GUARDAR
    // ============================
    public void doGuardar() {
        if (!validarMatricula()) return;

        try {
            // Asignar grupo
            if (idGrupoSeleccionado != null) {
                ejbCcoCepCursoDocente grupo = srvGrupo.buscarPorId(idGrupoSeleccionado);
                clsMatriculaEdit.setCepCursoDocente(grupo);
            }

            // Asignar persona (si es nuevo y se seleccionó)
            if (!modoEdicion && alumnoSeleccionado != null) {
                ejbCcoDrtPersonanatural persona = srvPersona.buscarPorId(alumnoSeleccionado.getIdDir());
                clsMatriculaEdit.setDrtPersonanatural(persona);
            }

            if (modoEdicion) {
                clsMatriculaEdit = srvMatricula.actualizar(clsMatriculaEdit);
                generalController.getFramework().doMensajeF("ACTUALIZAR", "Matrícula actualizada correctamente", 1);
            } else {
                clsMatriculaEdit = srvMatricula.crear(clsMatriculaEdit);
                generalController.getFramework().doMensajeF("GUARDAR", "Matrícula registrada correctamente", 1);
            }

            cargarMatriculas();
            strValor = VISTA_LISTA;

        } catch (Exception e) {
            e.printStackTrace();
            generalController.getFramework().doMensajeF("ERROR", "Error al guardar: " + e.getMessage(), 3);
        }
    }

    // ============================
    // VOLVER
    // ============================
    public void doVolver() {
        strValor = VISTA_LISTA;
        strBusqueda = "";
        cargarMatriculas();
        limpiarFormulario();
        alumnoSeleccionado = null;
    }

    // ============================
    // BÚSQUEDA DE ALUMNOS (para nuevo)
    // ============================
    private String strBusquedaAlumno;
    private List<AlumnoBusquedaDTO> lstAlumnosBusqueda;
    private AlumnoBusquedaDTO alumnoSeleccionado;

    public void doBuscarAlumnos() {
        if (strBusquedaAlumno == null || strBusquedaAlumno.trim().isEmpty()) {
            generalController.getFramework().doMensajeF("BÚSQUEDA", "Ingrese un DNI o nombre para buscar", 2);
            return;
        }

        String q = strBusquedaAlumno.trim().toLowerCase();
        lstAlumnosBusqueda = new ArrayList<>();

        try {
            // Buscar por DNI
            ejbCcoDrtPersonanatural persona = srvPersona.buscarPorDni(q);
            if (persona != null) {
                AlumnoBusquedaDTO dto = new AlumnoBusquedaDTO();
                dto.setIdDir(persona.getIdDir());
                dto.setDni(persona.getNumeroPndid());
                dto.setNombreCompleto(persona.getNombreCompleto());
                dto.setTipo("UNS");
                lstAlumnosBusqueda.add(dto);
            }

            // También buscar por nombre (aproximado)
            // NOTA: Esto es un ejemplo, idealmente debería haber un método en el DAO
            // para buscar por nombre. Por ahora solo funciona con DNI exacto.

            if (lstAlumnosBusqueda.isEmpty()) {
                generalController.getFramework().doMensajeF("BÚSQUEDA", "No se encontraron alumnos", 1);
            } else {
                generalController.getFramework().doMensajeF("BÚSQUEDA", "Alumno encontrado", 1);
            }

        } catch (Exception e) {
            e.printStackTrace();
            generalController.getFramework().doMensajeF("ERROR", "Error al buscar: " + e.getMessage(), 3);
        }
    }

    public void doLimpiarBusquedaAlumno() {
        strBusquedaAlumno = null;
        lstAlumnosBusqueda = null;
        alumnoSeleccionado = null;
    }

    public void doSeleccionarAlumno(AlumnoBusquedaDTO alumno) {
        this.alumnoSeleccionado = alumno;
        lstAlumnosBusqueda = null;
        strBusquedaAlumno = null;
        generalController.getFramework().doMensajeF("SELECCIÓN", "Alumno seleccionado: " + alumno.getNombreCompleto(), 1);
    }

    // ============================
    // MÉTODOS PRIVADOS
    // ============================
    private void limpiarFormulario() {
        clsMatriculaEdit = new ejbCcoCepCcoMatriculaCab();
        clsMatriculaEdit.setNotaFinal(0);
        idGrupoSeleccionado = null;
        alumnoSeleccionado = null;
        lstAlumnosBusqueda = null;
        strBusquedaAlumno = null;
    }

    private boolean validarMatricula() {
        if (idGrupoSeleccionado == null) {
            generalController.getFramework().doMensajeF("VALIDACIÓN", "Debe seleccionar un grupo", 2);
            return false;
        }
        if (!modoEdicion && alumnoSeleccionado == null) {
            generalController.getFramework().doMensajeF("VALIDACIÓN", "Debe seleccionar un alumno", 2);
            return false;
        }
        return true;
    }

    // ============================
    // DTO PARA BÚSQUEDA DE ALUMNOS
    // ============================
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AlumnoBusquedaDTO implements Serializable {
        private Integer idDir;
        private String dni;
        private String nombreCompleto;
        private String tipo;
    }

    // ============================
    // MÉTODOS PARA LA VISTA
    // ============================
    public boolean isMostrarLista() {
        return VISTA_LISTA.equals(strValor);
    }

    public boolean isMostrarFormulario() {
        return VISTA_NUEVO.equals(strValor) || VISTA_EDITAR.equals(strValor);
    }
}