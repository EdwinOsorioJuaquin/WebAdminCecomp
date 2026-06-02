package controladores;

import ejbCecomp.clases.ejbCcoAlumnoGeneralDTO;
import ejbCecomp.clases.ejbCcoGrupoDTO;
import ejbCecomp.clases.ejbCcoMatriculaDTO;
import ejbCecomp.entidades.*;
import ejbCecomp.ejb.negocio.*;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import lombok.Getter;
import lombok.Setter;
import static libreriaUdemsi.funciones.libreriaGeneral.doGenerarJNDI;

@Named("matriculaController")
@SessionScoped
@Getter
@Setter
public class MatriculaController implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private GeneralController generalController;

    // Listados
    private List<ejbCcoMatriculaDTO> lstMatriculasDTO;
    private List<ejbCcoMatriculaDTO> lstMatriculasViewDTO;
    private String strBusqueda;
    private String strValor;
    private Integer idEdicion;

    // Entidad para el formulario
    private ejbCcoCepCcoMatriculaCab clsMatriculaEdit;
    
    // IDs seleccionados
    private Integer idGrupoSeleccionado;
    private Integer idAlumnoSeleccionado;
    
    // Búsqueda de alumno
    private String strBusquedaAlumno;
    private List<ejbCcoAlumnoGeneralDTO> lstAlumnosBusqueda;
    private ejbCcoAlumnoGeneralDTO alumnoSeleccionado;
    
    // Catálogos
    private List<ejbCcoCepCursoDocente> lstGrupos;
    private List<ejbCcoGrupoDTO> lstGruposDTO;
    private List<ejbCcoAlumnoGeneralDTO> lstAlumnos;
    
    // Servicios
    private ejbCcoCepCcoMatriculaCabServiceLocal srvMatricula;
    private ejbCcoCepCursoDocenteServiceLocal srvGrupo;
    private ejbCcoCcoAlumnoExternoServiceLocal srvAlumnoExterno;
    private ejbCcoFxaEstudianteServiceLocal srvEstudiante;
    private ejbCcoDrtPersonanaturalServiceLocal srvPersona;

    private static final String VISTA_LISTA = "LISTA";
    private static final String VISTA_NUEVO = "NUEVO";
    private static final String VISTA_EDITAR = "EDITAR";

    public MatriculaController() {
        try {
            Context context = new InitialContext();
            srvMatricula = (ejbCcoCepCcoMatriculaCabServiceLocal) context.lookup(
                doGenerarJNDI("ejbCecomp", "1.0", "ejbCcoCepCcoMatriculaCabServiceLocal")
            );
            srvGrupo = (ejbCcoCepCursoDocenteServiceLocal) context.lookup(
                doGenerarJNDI("ejbCecomp", "1.0", "ejbCcoCepCursoDocenteServiceLocal")
            );
            srvAlumnoExterno = (ejbCcoCcoAlumnoExternoServiceLocal) context.lookup(
                doGenerarJNDI("ejbCecomp", "1.0", "ejbCcoCcoAlumnoExternoServiceLocal")
            );
            srvEstudiante = (ejbCcoFxaEstudianteServiceLocal) context.lookup(
                doGenerarJNDI("ejbCecomp", "1.0", "ejbCcoFxaEstudianteServiceLocal")
            );
            srvPersona = (ejbCcoDrtPersonanaturalServiceLocal) context.lookup(
                doGenerarJNDI("ejbCecomp", "1.0", "ejbCcoDrtPersonanaturalServiceLocal")
            );
        } catch (NamingException e) {
            System.out.println("Error JNDI MatriculaController: " + e.getMessage());
        }
    }

    public void doIniciarPagina() {
        strValor = VISTA_LISTA;
        strBusqueda = "";
        cargarMatriculas();
        cargarCatalogos();
    }

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
            System.out.println("Error cargar matriculas: " + e.getMessage());
            lstMatriculasDTO = new ArrayList<>();
            lstMatriculasViewDTO = new ArrayList<>();
        }
    }
    
    private void cargarCatalogos() {
        try {
            // Cargar grupos activos
            lstGrupos = srvGrupo.listarActivos();
            
            // Convertir grupos a DTO
            lstGruposDTO = new ArrayList<>();
            for (ejbCcoCepCursoDocente grupo : lstGrupos) {
                lstGruposDTO.add(new ejbCcoGrupoDTO(grupo));
            }
            
            // Cargar todos los alumnos
            cargarTodosLosAlumnos();
            
        } catch (Exception e) {
            System.out.println("Error cargar catálogos: " + e.getMessage());
            lstGrupos = new ArrayList<>();
            lstGruposDTO = new ArrayList<>();
            lstAlumnos = new ArrayList<>();
        }
    }
    
    private void cargarTodosLosAlumnos() {
        lstAlumnos = new ArrayList<>();
        
        // Alumnos externos
        try {
            List<ejbCcoCcoAlumnoExterno> externos = srvAlumnoExterno.listarAlumnosExternos();
            if (externos != null) {
                for (ejbCcoCcoAlumnoExterno externo : externos) {
                    lstAlumnos.add(new ejbCcoAlumnoGeneralDTO(externo));
                }
            }
        } catch (Exception e) {
            System.out.println("Error cargar externos: " + e.getMessage());
        }
        
        // Alumnos universidad
        try {
            List<ejbCcoFxaEstudiante> universidad = srvEstudiante.listarTodosActivos();
            if (universidad != null) {
                for (ejbCcoFxaEstudiante estudiante : universidad) {
                    lstAlumnos.add(new ejbCcoAlumnoGeneralDTO(estudiante));
                }
            }
        } catch (Exception e) {
            System.out.println("Error cargar universidad: " + e.getMessage());
        }
    }

    // 🔥 BUSCAR ALUMNOS POR DNI O NOMBRE
    public void doBuscarAlumnos() {
        if (strBusquedaAlumno == null || strBusquedaAlumno.trim().isEmpty()) {
            lstAlumnosBusqueda = new ArrayList<>();
            generalController.getFramework().doMensajeF("BÚSQUEDA", "Ingrese DNI o nombre para buscar", 2);
            return;
        }
        
        String busqueda = strBusquedaAlumno.trim().toLowerCase();
        lstAlumnosBusqueda = lstAlumnos.stream()
            .filter(a -> (a.getDni() != null && a.getDni().toLowerCase().contains(busqueda)) ||
                         (a.getNombreCompleto() != null && a.getNombreCompleto().toLowerCase().contains(busqueda)))
            .collect(Collectors.toList());
        
        if (lstAlumnosBusqueda.isEmpty()) {
            generalController.getFramework().doMensajeF("BÚSQUEDA", "No se encontraron alumnos", 2);
        } else {
            generalController.getFramework().doMensajeF("BÚSQUEDA", "Se encontraron " + lstAlumnosBusqueda.size() + " alumnos", 1);
        }
    }
    
    // 🔥 SELECCIONAR ALUMNO DE LA TABLA DE BÚSQUEDA
    public void doSeleccionarAlumno(ejbCcoAlumnoGeneralDTO alumno) {
        this.alumnoSeleccionado = alumno;
        this.idAlumnoSeleccionado = obtenerIdDirPorAlumno(alumno);
        this.strBusquedaAlumno = alumno.getDni() + " - " + alumno.getNombreCompleto();
        this.lstAlumnosBusqueda = null; // Limpiar resultados
        generalController.getFramework().doMensajeF("ALUMNO", "Alumno seleccionado: " + alumno.getNombreCompleto(), 1);
    }
    
    // 🔥 LIMPIAR BÚSQUEDA
    public void doLimpiarBusquedaAlumno() {
        this.strBusquedaAlumno = "";
        this.alumnoSeleccionado = null;
        this.idAlumnoSeleccionado = null;
        this.lstAlumnosBusqueda = null;
    }

    public void doBuscar() {
        String q = (strBusqueda == null) ? "" : strBusqueda.trim().toLowerCase();
        
        if (q.isBlank()) {
            lstMatriculasViewDTO = new ArrayList<>(lstMatriculasDTO);
        } else {
            List<ejbCcoMatriculaDTO> filtrado = new ArrayList<>();
            for (ejbCcoMatriculaDTO dto : lstMatriculasDTO) {
                String nombreAlumno = dto.getNombreCompleto() != null ? dto.getNombreCompleto().toLowerCase() : "";
                String dni = dto.getDni() != null ? dto.getDni().toLowerCase() : "";
                String nombreCurso = dto.getNombreCurso() != null ? dto.getNombreCurso().toLowerCase() : "";
                
                if (nombreAlumno.contains(q) || dni.contains(q) || nombreCurso.contains(q)) {
                    filtrado.add(dto);
                }
            }
            lstMatriculasViewDTO = filtrado;
        }
        generalController.getFramework().doMensajeF("BÚSQUEDA", "Filtro aplicado correctamente", 1);
    }

    public void doNuevo() {
        strValor = VISTA_NUEVO;
        limpiarFormulario();
    }

    public void doEditar(ejbCcoMatriculaDTO dto) {
        if (dto == null || dto.getIdMtaAlu() == null) {
            generalController.getFramework().doMensajeF("ERROR", "Matrícula no válida", 3);
            return;
        }
        
        strValor = VISTA_EDITAR;
        idEdicion = dto.getIdMtaAlu();
        
        clsMatriculaEdit = srvMatricula.buscarPorId(idEdicion);
        if (clsMatriculaEdit != null) {
            idGrupoSeleccionado = clsMatriculaEdit.getCepCursoDocente() != null ? 
                clsMatriculaEdit.getCepCursoDocente().getIdAd() : null;
            
            if (clsMatriculaEdit.getDrtPersonanatural() != null) {
                idAlumnoSeleccionado = clsMatriculaEdit.getDrtPersonanatural().getIdDir();
                // Buscar el alumno para mostrarlo en el campo
                for (ejbCcoAlumnoGeneralDTO alumno : lstAlumnos) {
                    if (alumno.getDni() != null && alumno.getDni().equals(clsMatriculaEdit.getDrtPersonanatural().getNumeroPndid())) {
                        alumnoSeleccionado = alumno;
                        strBusquedaAlumno = alumno.getDni() + " - " + alumno.getNombreCompleto();
                        break;
                    }
                }
            }
        }
    }

    public void doGuardar() {
        if (!validarMatricula()) return;
        
        try {
            // Asignar relaciones
            if (idGrupoSeleccionado != null) {
                clsMatriculaEdit.setCepCursoDocente(srvGrupo.buscarPorId(idGrupoSeleccionado));
            }
            if (idAlumnoSeleccionado != null) {
                clsMatriculaEdit.setDrtPersonanatural(srvPersona.buscarPorId(idAlumnoSeleccionado));
            }
            
            // Si es nueva matrícula, nota final debe ser null
            if (idEdicion == null) {
                clsMatriculaEdit.setNotaFinal(null);
                clsMatriculaEdit = srvMatricula.crear(clsMatriculaEdit);
                generalController.getFramework().doMensajeF("GUARDAR", "Matrícula registrada correctamente", 1);
            } else {
                clsMatriculaEdit = srvMatricula.actualizar(clsMatriculaEdit);
                generalController.getFramework().doMensajeF("ACTUALIZAR", "Matrícula actualizada correctamente", 1);
            }
            
            cargarMatriculas();
            strValor = VISTA_LISTA;
            
        } catch (Exception e) {
            e.printStackTrace();
            generalController.getFramework().doMensajeF("ERROR", "Error al guardar matrícula: " + e.getMessage(), 3);
        }
    }

    public void doVolver() {
        strValor = VISTA_LISTA;
        strBusqueda = "";
        cargarMatriculas();
        doLimpiarBusquedaAlumno();
    }

    private void limpiarFormulario() {
        clsMatriculaEdit = new ejbCcoCepCcoMatriculaCab();
        clsMatriculaEdit.setNotaFinal(null);
        
        idEdicion = null;
        idGrupoSeleccionado = null;
        idAlumnoSeleccionado = null;
        doLimpiarBusquedaAlumno();
    }

    private boolean validarMatricula() {
        if (idGrupoSeleccionado == null) {
            generalController.getFramework().doMensajeF("VALIDACIÓN", "Debe seleccionar un grupo", 2);
            return false;
        }
        if (idAlumnoSeleccionado == null) {
            generalController.getFramework().doMensajeF("VALIDACIÓN", "Debe seleccionar un alumno", 2);
            return false;
        }
        // Validar nota final si no es null
        if (clsMatriculaEdit.getNotaFinal() != null) {
            if (clsMatriculaEdit.getNotaFinal() < 0 || clsMatriculaEdit.getNotaFinal() > 20) {
                generalController.getFramework().doMensajeF("VALIDACIÓN", "La nota final debe estar entre 0 y 20", 2);
                return false;
            }
        }
        return true;
    }
    
    private Integer obtenerIdDirPorAlumno(ejbCcoAlumnoGeneralDTO alumno) {
        if (alumno == null) return null;
        
        if ("EXTERNO".equals(alumno.getTipo())) {
            ejbCcoCcoAlumnoExterno externo = srvAlumnoExterno.buscarPorCorreo(alumno.getCorreoLogin());
            if (externo != null && externo.getDrtPersonanatural() != null) {
                return externo.getDrtPersonanatural().getIdDir();
            }
        } else {
            ejbCcoFxaEstudiante estudiante = srvEstudiante.buscarPorCodigo(alumno.getCodigoEstudiante());
            if (estudiante != null && estudiante.getDrtPersonanatural() != null) {
                return estudiante.getDrtPersonanatural().getIdDir();
            }
        }
        return null;
    }
    
    // Getter para saber si está en modo edición
    public boolean isModoEdicion() {
        return VISTA_EDITAR.equals(strValor);
    }
}