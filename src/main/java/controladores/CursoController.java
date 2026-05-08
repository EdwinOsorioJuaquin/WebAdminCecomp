package controladores;

import ejbCecomp.clases.ejbCcoCursoDTO;
import ejbCecomp.ejb.negocio.*;
import ejbCecomp.entidades.*;
import jakarta.enterprise.context.*;
import jakarta.inject.*;
import java.io.*;
import java.util.*;
import javax.naming.*;
import static libreriaUdemsi.funciones.libreriaGeneral.doGenerarJNDI;
import lombok.*;

@Named(value = "cursoController")
@SessionScoped
@Getter
@Setter
public class CursoController implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private GeneralController generalController;

    // ============================
    // ATRIBUTOS
    // ============================
    private List<ejbCcoCursoDTO> lstCursosDTO;
    private List<ejbCcoCursoDTO> lstCursosViewDTO;
    private ejbCcoCursoDTO clsCursoDTO;
    private String strBusqueda;
    private String strValor;
    private Integer idEdicion;
    private List<ejbCcoCepNivelModalidad> lstNivelesModalidad;
    private Integer idNivelSeleccionado;
    private List<ejbCcoCepCecPlan> lstPlanesDisponibles;
    private Integer idPlanSeleccionado;

    private static final String VISTA_LISTA = "LISTA";
    private static final String VISTA_NUEVO = "NUEVO";

    private ejbCcoCepCursoServiceLocal srvCurso;
    private ejbCcoCepNivelModalidadServiceLocal srvNivelModalidad;
    private ejbCcoCepCecPlanServiceLocal srvPlan;

    public CursoController() {
        try {
            Context context = new InitialContext();
            srvCurso = (ejbCcoCepCursoServiceLocal) context.lookup(doGenerarJNDI("ejbCecomp", "1.0", "ejbCcoCepCursoServiceLocal"));
            srvNivelModalidad = (ejbCcoCepNivelModalidadServiceLocal) context.lookup(doGenerarJNDI("ejbCecomp", "1.0", "ejbCcoCepNivelModalidadServiceLocal"));
            srvPlan = (ejbCcoCepCecPlanServiceLocal) context.lookup(doGenerarJNDI("ejbCecomp", "1.0", "ejbCcoCepCecPlanServiceLocal"));
        } catch (NamingException e) {
            System.out.println("Error cursoController constructor: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void doIniciarPagina() {
        System.out.println("Llegó a doIniciarPagina");
        strValor = VISTA_LISTA;
        strBusqueda = "";
        cargarCursos();
        cargarNivelesModalidad();
        cargarPlanesDisponibles();
        limpiarFormulario();
    }

    private void limpiarFormulario() {
        clsCursoDTO = new ejbCcoCursoDTO();
        clsCursoDTO.setCurso(new ejbCcoCepCurso());
        clsCursoDTO.getCurso().setBandera(true);
        idEdicion = null;
        idNivelSeleccionado = null;
        idPlanSeleccionado = null;
    }

    private void cargarNivelesModalidad() {
        try {
            lstNivelesModalidad = srvNivelModalidad.listarNivelesModalidad();
            if (lstNivelesModalidad == null) {
                lstNivelesModalidad = new ArrayList<>();
            }
            System.out.println("Niveles cargados: " + lstNivelesModalidad.size());
        } catch (Exception e) {
            System.out.println("Error al cargar niveles: " + e.getMessage());
            lstNivelesModalidad = new ArrayList<>();
        }
    }

    private void cargarCursos() {
        try {
            List<ejbCcoCepCurso> cursos = srvCurso.listarActivos();
            lstCursosDTO = new ArrayList<>();
            if (cursos != null) {
                for (ejbCcoCepCurso curso : cursos) {
                    ejbCcoCursoDTO dto = new ejbCcoCursoDTO(curso);
                    if (curso.getCepNivelModalidad() != null) {
                        dto.setNombreNivel(curso.getCepNivelModalidad().getNomNivMod());
                    }
                    if (curso.getCepCecPlan() != null) {
                        dto.setNombrePlan(curso.getCepCecPlan().getNomPland());
                    }
                    lstCursosDTO.add(dto);
                }
            }
            lstCursosViewDTO = new ArrayList<>(lstCursosDTO);
        } catch (Exception e) {
            System.out.println("Error al cargar cursos: " + e.getMessage());
            lstCursosDTO = new ArrayList<>();
            lstCursosViewDTO = new ArrayList<>();
        }
    }

    private void cargarPlanesDisponibles() {
        try {
            lstPlanesDisponibles = srvPlan.listarActivos();
            if (lstPlanesDisponibles == null) {
                lstPlanesDisponibles = new ArrayList<>();
            }
            System.out.println("Planes cargados: " + lstPlanesDisponibles.size());
        } catch (Exception e) {
            System.out.println("Error al cargar planes: " + e.getMessage());
            lstPlanesDisponibles = new ArrayList<>();
        }
    }

    public void doBuscar() {
        System.out.println("Llegó a doBuscar");
        String q = (strBusqueda == null) ? "" : strBusqueda.trim().toLowerCase();

        if (q.isBlank()) {
            lstCursosViewDTO = new ArrayList<>(lstCursosDTO);
        } else {
            List<ejbCcoCursoDTO> filtrado = new ArrayList<>();
            for (ejbCcoCursoDTO dto : lstCursosDTO) {
                String nombre = dto.getCurso() != null ? dto.getCurso().getNomCurso() : "";
                if (nombre != null && nombre.toLowerCase().contains(q)) {
                    filtrado.add(dto);
                }
            }
            lstCursosViewDTO = filtrado;
        }
        generalController.getFramework().doMensajeF("BÚSQUEDA", "Filtro aplicado correctamente", 1);
    }

    public void doNuevo() {
        System.out.println("Llegó a doNuevo");
        strValor = VISTA_NUEVO;
        limpiarFormulario();
        
        if ((lstNivelesModalidad == null || lstNivelesModalidad.isEmpty()) && srvNivelModalidad != null) {
            cargarNivelesModalidad();
        }
        if ((lstPlanesDisponibles == null || lstPlanesDisponibles.isEmpty()) && srvPlan != null) {
            cargarPlanesDisponibles();
        }
    }

    public void doEditar(ejbCcoCursoDTO cursoDTO) {
        System.out.println("Llegó a doEditar");
        if (cursoDTO == null || cursoDTO.getCurso() == null || cursoDTO.getCurso().getIdCurso() == null) {
            generalController.getFramework().doMensajeF("ERROR", "Curso no válido para editar", 3);
            return;
        }

        try {
            strValor = VISTA_NUEVO;
            idEdicion = cursoDTO.getCurso().getIdCurso();
            
            ejbCcoCepCurso cursoCompleto = srvCurso.buscarPorId(idEdicion);
            if (cursoCompleto == null) {
                generalController.getFramework().doMensajeF("ERROR", "No se encontró el curso", 3);
                return;
            }
            
            clsCursoDTO = new ejbCcoCursoDTO(cursoCompleto);
            
            if (cursoCompleto.getCepNivelModalidad() != null) {
                idNivelSeleccionado = cursoCompleto.getCepNivelModalidad().getIdNivMod();
            } else {
                idNivelSeleccionado = null;
            }
            
            if (cursoCompleto.getCepCecPlan() != null) {
                idPlanSeleccionado = cursoCompleto.getCepCecPlan().getIdPland();
            } else {
                idPlanSeleccionado = null;
            }
            
            if ((lstNivelesModalidad == null || lstNivelesModalidad.isEmpty()) && srvNivelModalidad != null) {
                cargarNivelesModalidad();
            }
            if ((lstPlanesDisponibles == null || lstPlanesDisponibles.isEmpty()) && srvPlan != null) {
                cargarPlanesDisponibles();
            }
            
        } catch (Exception e) {
            System.out.println("Error al editar curso: " + e.getMessage());
            e.printStackTrace();
            generalController.getFramework().doMensajeF("ERROR", "Error al cargar el curso para editar", 3);
        }
    }

    public void doGuardar() {
        System.out.println("Llegó a doGuardar");
        
        if (!validarCursoBasico()) return;
        
        if (idNivelSeleccionado == null) {
            generalController.getFramework().doMensajeF("VALIDACIÓN", "Debe seleccionar un nivel", 2);
            return;
        }

        try {
            ejbCcoCepNivelModalidad nivel = srvNivelModalidad.buscarPorId(idNivelSeleccionado);
            if (nivel == null) {
                generalController.getFramework().doMensajeF("VALIDACIÓN", "Nivel no válido", 2);
                return;
            }
            
            ejbCcoCepCecPlan plan = null;
            if (idPlanSeleccionado != null && idPlanSeleccionado > 0) {
                plan = srvPlan.buscarPorId(idPlanSeleccionado);
            }

            ejbCcoCepCurso cursoGuardar = clsCursoDTO.getCurso();
            if (cursoGuardar == null) {
                cursoGuardar = new ejbCcoCepCurso();
            }
            
            cursoGuardar.setCepNivelModalidad(nivel);
            cursoGuardar.setCepCecPlan(plan);
            cursoGuardar.setBandera(true);

            if (idEdicion == null) {
                int nuevoId = obtenerNuevoIdCurso();
                cursoGuardar.setIdCurso(nuevoId);
                srvCurso.crear(cursoGuardar);
                generalController.getFramework().doMensajeF("GUARDAR", "Curso agregado correctamente", 1);
            } else {
                cursoGuardar.setIdCurso(idEdicion);
                srvCurso.actualizar(cursoGuardar);
                generalController.getFramework().doMensajeF("ACTUALIZAR", "Curso actualizado correctamente", 1);
            }

            cargarCursos();
            doBuscar();
            strValor = VISTA_LISTA;

        } catch (Exception e) {
            System.out.println("Error al guardar: " + e.getMessage());
            e.printStackTrace();
            generalController.getFramework().doMensajeF("ERROR", "Error al guardar el curso: " + e.getMessage(), 3);
        }
    }

    public void doCambiarEstado(ejbCcoCursoDTO cursoDTO) {
        if (cursoDTO == null || cursoDTO.getCurso() == null || cursoDTO.getCurso().getIdCurso() == null) {
            generalController.getFramework().doMensajeF("ERROR", "Curso no válido", 3);
            return;
        }
        
        try {
            ejbCcoCepCurso curso = srvCurso.buscarPorId(cursoDTO.getCurso().getIdCurso());
            if (curso == null) {
                generalController.getFramework().doMensajeF("ERROR", "No se encontró el curso", 3);
                return;
            }
            
            boolean nuevoEstado = !curso.getBandera();
            curso.setBandera(nuevoEstado);
            srvCurso.actualizar(curso);
            
            String mensaje = nuevoEstado ? "Curso activado correctamente" : "Curso dado de baja correctamente";
            int tipoMsg = nuevoEstado ? 1 : 2;
            generalController.getFramework().doMensajeF(nuevoEstado ? "ACTIVAR" : "BAJA", mensaje, tipoMsg);
            
            cargarCursos();
            doBuscar();
        } catch (Exception e) {
            System.out.println("Error al cambiar estado: " + e.getMessage());
            generalController.getFramework().doMensajeF("ERROR", "Error al cambiar estado del curso", 3);
        }
    }

    public void doVolver() {
        strValor = VISTA_LISTA;
        strBusqueda = "";
        doBuscar();
    }

    private int obtenerNuevoIdCurso() {
        int maxId = 0;
        if (lstCursosDTO != null && !lstCursosDTO.isEmpty()) {
            for (ejbCcoCursoDTO dto : lstCursosDTO) {
                if (dto != null && dto.getCurso() != null && dto.getCurso().getIdCurso() != null) {
                    Integer id = dto.getCurso().getIdCurso();
                    if (id > maxId) maxId = id;
                }
            }
        }
        return maxId + 1;
    }

    private boolean validarCursoBasico() {
        if (clsCursoDTO == null || clsCursoDTO.getCurso() == null) {
            clsCursoDTO = new ejbCcoCursoDTO();
            clsCursoDTO.setCurso(new ejbCcoCepCurso());
        }
        
        ejbCcoCepCurso curso = clsCursoDTO.getCurso();
        
        if (isBlank(curso.getNomCurso())) {
            generalController.getFramework().doMensajeF("VALIDACIÓN", "Ingrese el nombre del curso", 2);
            return false;
        }
        if (isBlank(curso.getDuracion())) {
            generalController.getFramework().doMensajeF("VALIDACIÓN", "Ingrese la duración del curso", 2);
            return false;
        }
        if (isBlank(curso.getAbreviatura())) {
            generalController.getFramework().doMensajeF("VALIDACIÓN", "Ingrese la abreviatura del curso", 2);
            return false;
        }
        return true;
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}