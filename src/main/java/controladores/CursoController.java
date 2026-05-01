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

    @Inject
    private GeneralController generalController;

    // ============================
    // 1. ATRIBUTOS
    // ============================
    private List<ejbCcoCursoDTO> lstCursosDTO;
    private List<ejbCcoCursoDTO> lstCursosViewDTO;
    private ejbCcoCursoDTO clsCursoDTO;
    private String strBusqueda;
    private String strValor;
    private List<String> temas;
    private Integer idEdicion;
    private List<ejbCcoCepNivelModalidad> lstNivelesModalidad;
    private Integer idNivelSeleccionado;

    // Constantes de vista
    private static final String VISTA_LISTA = "LISTA";
    private static final String VISTA_NUEVO = "NUEVO";
    private static final String VISTA_TEMAS = "TEMAS";

    // ============================
    // 2. EJB
    // ============================
    private ejbCcoCepCursoServiceLocal srvCurso;
    private ejbCcoCepNivelModalidadServiceLocal srvNivelModalidad;

    // ============================
    // 3. CONSTRUCTOR
    // ============================
    public CursoController() {
        try {
            Context context = new InitialContext();
            srvCurso = (ejbCcoCepCursoServiceLocal) context.lookup(doGenerarJNDI("ejbCecomp", "1.0", "ejbCcoCepCursoServiceLocal"));
            srvNivelModalidad = (ejbCcoCepNivelModalidadServiceLocal) context.lookup(doGenerarJNDI("ejbCecomp", "1.0", "ejbCcoCepNivelModalidadServiceLocal"));
        } catch (NamingException e) {
            System.out.println("Error cursoController: " + e);
        }
    }

    // ============================
    // 4. ACCIONES JSF
    // ============================

    public void doIniciarPagina() {
        System.out.println("Llegó a doIniciarPagina");
        strValor = VISTA_LISTA;
        strBusqueda = "";
        cargarCursos();
        cargarNivelesModalidad();
        clsCursoDTO = new ejbCcoCursoDTO();
        temas = buildTemasVacios(4);
        idEdicion = null;
        idNivelSeleccionado = null;
    }

    private void cargarNivelesModalidad() {
        try {
            lstNivelesModalidad = srvNivelModalidad.listarNivelesModalidad();
            System.out.println("Niveles cargados: " + (lstNivelesModalidad != null ? lstNivelesModalidad.size() : 0));
        } catch (Exception e) {
            System.out.println("Error al cargar niveles: " + e);
            lstNivelesModalidad = new ArrayList<>();
        }
    }

    private void cargarCursos() {
        try {
            List<ejbCcoCepCurso> cursos = srvCurso.listarActivos();
            lstCursosDTO = new ArrayList<>();
            for (ejbCcoCepCurso curso : cursos) {
                lstCursosDTO.add(new ejbCcoCursoDTO(curso));
            }
            lstCursosViewDTO = new ArrayList<>(lstCursosDTO);
        } catch (Exception e) {
            System.out.println("Error al cargar cursos: " + e);
            lstCursosDTO = new ArrayList<>();
            lstCursosViewDTO = new ArrayList<>();
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
                String nombre = (dto.getNombreCurso() == null) ? "" : dto.getNombreCurso().toLowerCase();
                if (nombre.contains(q)) filtrado.add(dto);
            }
            lstCursosViewDTO = filtrado;
        }

        generalController.getFramework().doMensajeF("BÚSQUEDA", "Filtro aplicado correctamente", 1);
    }

    public void doNuevo() {
        System.out.println("Llegó a doNuevo");
        strValor = VISTA_NUEVO;
        idEdicion = null;
        clsCursoDTO = new ejbCcoCursoDTO();
        clsCursoDTO.setActivo(true);
        temas = buildTemasVacios(4);
        idNivelSeleccionado = null;

        // Asignar nivel por defecto (el primero de la lista, normalmente "01" Básico)
        if (lstNivelesModalidad != null && !lstNivelesModalidad.isEmpty()) {
            idNivelSeleccionado = lstNivelesModalidad.get(0).getIdNivMod();
            System.out.println("Nivel por defecto: " + idNivelSeleccionado);
        }
    }

    public void doEditar(ejbCcoCursoDTO cursoDTO) {
        System.out.println("Llegó a doEditar");
        if (cursoDTO == null || cursoDTO.getCurso() == null) return;

        strValor = VISTA_NUEVO;
        idEdicion = cursoDTO.getIdCurso();
        
        // Recargar entidad completa desde BD
        ejbCcoCepCurso cursoCompleto = srvCurso.buscarPorId(cursoDTO.getIdCurso());
        clsCursoDTO = new ejbCcoCursoDTO(cursoCompleto);

        // Cargar el ID del nivel actual
        if (cursoCompleto.getCepNivelModalidad() != null) {
            idNivelSeleccionado = cursoCompleto.getCepNivelModalidad().getIdNivMod();
        } else {
            idNivelSeleccionado = null;
        }

        if (lstNivelesModalidad == null || lstNivelesModalidad.isEmpty()) {
            cargarNivelesModalidad();
        }
        
        temas = buildTemasVacios(4);
    }

    public void doGuardar() {
        System.out.println("Llegó a doGuardar");
        if (!validarCursoBasico()) return;

        try {
            // 1. Obtener la entidad gestionada del nivel seleccionado
            ejbCcoCepNivelModalidad nivel = null;
            if (idNivelSeleccionado != null) {
                nivel = srvNivelModalidad.buscarPorId(idNivelSeleccionado);
                System.out.println("Nivel seleccionado: " + nivel.getNomNivMod() + " (ID: " + nivel.getIdNivMod() + ")");
            }

            // 2. Construir la entidad Curso con los datos del DTO
            ejbCcoCepCurso cursoGuardar = new ejbCcoCepCurso();
            cursoGuardar.setNomCurso(clsCursoDTO.getNombreCurso());
            cursoGuardar.setDuracion(clsCursoDTO.getDuracion());
            cursoGuardar.setBandera(clsCursoDTO.getActivo());
            
            if (clsCursoDTO.getCurso() != null) {
                cursoGuardar.setAbreviatura(clsCursoDTO.getCurso().getAbreviatura());
            }
            cursoGuardar.setCepNivelModalidad(nivel);

            if (idEdicion == null || idEdicion == 0) {
                // Nuevo curso - asignar ID manualmente
                int nuevoId = obtenerNuevoIdCurso();
                System.out.println("nuevo id: " + nuevoId);
                cursoGuardar.setIdCurso(nuevoId);
                srvCurso.crear(cursoGuardar);
                generalController.getFramework().doMensajeF("GUARDAR", "Curso agregado correctamente", 1);
            } else {
                // Actualizar curso existente
                cursoGuardar.setIdCurso(idEdicion);
                srvCurso.actualizar(cursoGuardar);
                generalController.getFramework().doMensajeF("ACTUALIZAR", "Curso actualizado correctamente", 1);
            }

            // Recargar datos y volver a la lista
            cargarCursos();
            doBuscar();
            strValor = VISTA_LISTA;
            idNivelSeleccionado = null;

        } catch (Exception e) {
            System.out.println("Error al guardar: " + e.getMessage());
            e.printStackTrace();
            generalController.getFramework().doMensajeF("ERROR", "Error al guardar el curso", 3);
        }
    }

    public void doEliminar(ejbCcoCursoDTO cursoDTO) {
        System.out.println("Llegó a doEliminar");
        if (cursoDTO == null || cursoDTO.getCurso() == null) return;

        ejbCcoCepCurso curso = cursoDTO.getCurso();
        curso.setBandera(false);
        srvCurso.actualizar(curso);
        generalController.getFramework().doMensajeF("BAJA", "Curso dado de baja correctamente", 2);

        cargarCursos();
        doBuscar();
    }

    public void doCambiarEstado(ejbCcoCursoDTO cursoDTO) {
        System.out.println("Llegó a doCambiarEstado");
        if (cursoDTO == null || cursoDTO.getCurso() == null) return;

        ejbCcoCepCurso curso = cursoDTO.getCurso();
        boolean nuevoEstado = !curso.getBandera();
        curso.setBandera(nuevoEstado);
        srvCurso.actualizar(curso);

        String mensaje = nuevoEstado ? "Curso activado correctamente" : "Curso dado de baja correctamente";
        int tipoMsg = nuevoEstado ? 1 : 2;
        generalController.getFramework().doMensajeF(nuevoEstado ? "ACTIVAR" : "BAJA", mensaje, tipoMsg);

        cargarCursos();
        doBuscar();
    }

    public void doContinuarTemas() {
        System.out.println("Llegó a doContinuarTemas");
        if (!validarCursoBasico()) return;
        if (temas == null || temas.isEmpty()) temas = buildTemasVacios(4);
        strValor = VISTA_TEMAS;
    }

    public void doVolver() {
        System.out.println("Llegó a doVolver");
        strValor = VISTA_LISTA;
        strBusqueda = "";
        doBuscar();
    }

    public void doVolverCursoDesdeTemas() {
        System.out.println("Llegó a doVolverCursoDesdeTemas");
        strValor = VISTA_NUEVO;
    }

    // ============================
    // 5. MÉTODOS PRIVADOS
    // ============================

    private int obtenerNuevoIdCurso() {
        int maxId = 0;
        if (lstCursosDTO != null && !lstCursosDTO.isEmpty()) {
            for (ejbCcoCursoDTO dto : lstCursosDTO) {
                Integer id = dto.getIdCurso();
                if (id != null && id > maxId) {
                    maxId = id;
                }
            }
        }
        return maxId + 1;
    }

    private boolean validarCursoBasico() {
        if (clsCursoDTO == null) clsCursoDTO = new ejbCcoCursoDTO();

        if (isBlank(clsCursoDTO.getNombreCurso())) {
            generalController.getFramework().doMensajeF("VALIDACIÓN", "Ingrese el nombre del curso", 2);
            return false;
        }
        if (isBlank(clsCursoDTO.getDuracion())) {
            generalController.getFramework().doMensajeF("VALIDACIÓN", "Ingrese la duración del curso", 2);
            return false;
        }
        if (clsCursoDTO.getCurso() == null || isBlank(clsCursoDTO.getCurso().getAbreviatura())) {
            generalController.getFramework().doMensajeF("VALIDACIÓN", "Ingrese la abreviatura del curso", 2);
            return false;
        }
        return true;
    }

    private boolean validarTemas() {
        List<String> limp = limpiarTemas(temas);
        if (limp.isEmpty()) {
            generalController.getFramework().doMensajeF("VALIDACIÓN", "Ingrese al menos 1 tema", 2);
            return false;
        }
        return true;
    }

    private List<String> buildTemasVacios(int n) {
        List<String> t = new ArrayList<>();
        for (int i = 0; i < n; i++) t.add("");
        return t;
    }

    private List<String> limpiarTemas(List<String> in) {
        if (in == null) return new ArrayList<>();
        List<String> out = new ArrayList<>();
        for (String s : in) {
            if (s != null && !s.trim().isBlank()) out.add(s.trim());
        }
        return out;
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isBlank();
    }
}