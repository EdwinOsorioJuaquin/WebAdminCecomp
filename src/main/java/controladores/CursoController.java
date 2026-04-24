package controladores;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.*;
import lombok.*;

@Named(value = "cursoController")
@SessionScoped
@Getter
@Setter
public class CursoController implements Serializable {

    private static final long serialVersionUID = 1L;

    // ============================
    // CONSTANTES DE VISTA
    // ============================
    private static final String VISTA_LISTA = "LISTA";
    private static final String VISTA_NUEVO = "NUEVO";
    private static final String VISTA_TEMAS = "TEMAS";

    @Inject
    private GeneralController generalController;

    // ============================
    // ESTADO
    // ============================
    private String strValor;
    private String strBusqueda;

    // ============================
    // DATOS EN MEMORIA
    // ============================
    private List<CursoDemo> lstCursos;      // fuente real
    private List<CursoDemo> lstCursosView;  // lo que muestra la tabla (filtrado)

    private CursoDemo nuevoCurso;
    private List<String> temas;

    // Control de edición (para no mutar la fila original antes de guardar)
    private Integer idEdicion; // null si es nuevo

    @PostConstruct
    public void init() {
        strValor = VISTA_LISTA;
        strBusqueda = "";

        lstCursos = new ArrayList<>();
        lstCursos.add(new CursoDemo(1, "EXCEL", "AVANZADO", "04 SEMANAS",
                "140.00", "120.00", "110.00", "160.00", "ACTIVO", true, new ArrayList<>()));
        lstCursos.add(new CursoDemo(2, "PROGRAMACIÓN ORIENTADA A OBJETOS EN JAVA", "-", "02 SEMANAS",
                "140.00", "120.00", "110.00", "160.00", "BAJA", false, new ArrayList<>()));
        lstCursos.add(new CursoDemo(3, "OFIMÁTICA EMPRESARIAL", "BÁSICO", "06 SEMANAS",
                "140.00", "120.00", "110.00", "160.00", "ACTIVO", true, new ArrayList<>()));
        lstCursos.add(new CursoDemo(4, "ANÁLISIS DE DATOS CON POWER BI", "AVANZADO", "02 SEMANAS",
                "150.00", "130.00", "120.00", "170.00", "ACTIVO", true, new ArrayList<>()));

        // por defecto vista
        lstCursosView = new ArrayList<>(lstCursos);

        // formulario
        nuevoCurso = new CursoDemo();
        temas = buildTemasVacios(4);
        idEdicion = null;
    }

    // ============================
    // INICIO PÁGINA
    // ============================
    public void doIniciarPagina() {
        if (strValor == null || strValor.isBlank()) strValor = VISTA_LISTA;
        if (lstCursosView == null) lstCursosView = new ArrayList<>(lstCursos);
    }

    // ============================
    // LISTA / BÚSQUEDA
    // ============================
    public void doBuscar() {
        String q = (strBusqueda == null) ? "" : strBusqueda.trim().toLowerCase();

        if (q.isBlank()) {
            lstCursosView = new ArrayList<>(lstCursos);
        } else {
            List<CursoDemo> filtrado = new ArrayList<>();
            for (CursoDemo c : lstCursos) {
                String nombre = (c.getNombre() == null) ? "" : c.getNombre().toLowerCase();
                if (nombre.contains(q)) filtrado.add(c);
            }
            lstCursosView = filtrado;
        }

        generalController.getFramework().doMensajeF("BÚSQUEDA", "Filtro aplicado correctamente", 1);
    }

    // ============================
    // NUEVO / EDITAR
    // ============================
    public void doNuevo() {
        strValor = VISTA_NUEVO;
        idEdicion = null;

        nuevoCurso = new CursoDemo();
        nuevoCurso.setEstado("ACTIVO");
        nuevoCurso.setActivo(true);

        temas = buildTemasVacios(4);
    }

    public void doEditar(CursoDemo curso) {
        if (curso == null) return;

        strValor = VISTA_NUEVO;
        idEdicion = curso.getId();

        // CLONAR para no afectar la tabla hasta guardar
        nuevoCurso = curso.cloneLite();

        // cargar temas del curso (si existen) o crear vacíos
        if (curso.getTemas() != null && !curso.getTemas().isEmpty()) {
            temas = new ArrayList<>(curso.getTemas());
        } else {
            temas = buildTemasVacios(4);
        }
    }

    public void doGuardar() {
        if (!validarCursoBasico()) return;

        // armar copia final con temas
        CursoDemo finalCurso = nuevoCurso.cloneLite();
        finalCurso.setTemas(limpiarTemas(temas));

        if (idEdicion == null || idEdicion == 0) {
            finalCurso.setId(nextId());
            lstCursos.add(finalCurso);
            generalController.getFramework().doMensajeF("GUARDAR", "Curso agregado correctamente", 1);
        } else {
            CursoDemo real = findById(idEdicion);
            if (real == null) {
                generalController.getFramework().doMensajeF("ERROR", "No se encontró el curso a editar", 3);
                return;
            }

            // actualizar campos
            real.setNombre(finalCurso.getNombre());
            real.setNivel(finalCurso.getNivel());
            real.setDuracion(finalCurso.getDuracion());
            real.setPrecioGeneral(finalCurso.getPrecioGeneral());
            real.setPrecioAlumnoUns(finalCurso.getPrecioAlumnoUns());
            real.setPrecioTrabajadorUns(finalCurso.getPrecioTrabajadorUns());
            real.setPrecioExterno(finalCurso.getPrecioExterno());
            real.setActivo(finalCurso.isActivo());
            real.setEstado(finalCurso.isActivo() ? "ACTIVO" : "BAJA");
            real.setTemas(finalCurso.getTemas());

            generalController.getFramework().doMensajeF("ACTUALIZAR", "Curso actualizado correctamente", 1);
        }

        // refrescar tabla y volver
        doBuscar();
        strValor = VISTA_LISTA;
    }

    // ============================
    // ELIMINAR (BAJA LÓGICA)
    // ============================
    public void doEliminar(CursoDemo curso) {
        if (curso == null) return;

        curso.setActivo(false);
        curso.setEstado("BAJA");

        generalController.getFramework().doMensajeF("BAJA", "Curso dado de baja correctamente", 2);

        doBuscar(); // refresca tabla con el filtro actual
    }

    // ============================
    // TEMAS
    // ============================
    public void doContinuarTemas() {
        if (!validarCursoBasico()) return;

        // si entran a temas sin tener lista inicial
        if (temas == null || temas.isEmpty()) temas = buildTemasVacios(4);

        strValor = VISTA_TEMAS;
    }

    public void doGuardarTemas() {
        // Guarda TODO (curso + temas) y vuelve a lista
        if (!validarCursoBasico()) return;
        if (!validarTemas()) return;

        // reutilizamos doGuardar (que ya guarda temas)
        doGuardar();
    }

    // volver desde NUEVO a LISTA
    public void doVolver() {
        strValor = VISTA_LISTA;
        doBuscar();
    }

    // volver desde TEMAS a NUEVO SIN RESETEAR
    public void doVolverCursoDesdeTemas() {
        strValor = VISTA_NUEVO;
    }

    // ============================
    // VALIDACIONES (demo)
    // ============================
    private boolean validarCursoBasico() {
        if (nuevoCurso == null) nuevoCurso = new CursoDemo();

        if (isBlank(nuevoCurso.getNombre())) {
            generalController.getFramework().doMensajeF("VALIDACIÓN", "Ingrese el nombre del curso", 2);
            return false;
        }
        if (isBlank(nuevoCurso.getDuracion())) {
            generalController.getFramework().doMensajeF("VALIDACIÓN", "Ingrese la duración del curso", 2);
            return false;
        }

        // precios: demo simple (si quieres, luego lo pasamos a BigDecimal)
        if (!isDecimal(nuevoCurso.getPrecioGeneral())) {
            generalController.getFramework().doMensajeF("VALIDACIÓN", "Precio público inválido (ej: 140.00)", 2);
            return false;
        }
        if (!isDecimal(nuevoCurso.getPrecioAlumnoUns())) {
            generalController.getFramework().doMensajeF("VALIDACIÓN", "Precio Alumno UNS inválido (ej: 120.00)", 2);
            return false;
        }
        if (!isDecimal(nuevoCurso.getPrecioTrabajadorUns())) {
            generalController.getFramework().doMensajeF("VALIDACIÓN", "Precio Trabajador UNS inválido (ej: 110.00)", 2);
            return false;
        }
        if (!isDecimal(nuevoCurso.getPrecioExterno())) {
            generalController.getFramework().doMensajeF("VALIDACIÓN", "Precio Externo inválido (ej: 160.00)", 2);
            return false;
        }

        // estado coherente con activo
        nuevoCurso.setEstado(Boolean.TRUE.equals(nuevoCurso.isActivo()) ? "ACTIVO" : "BAJA");
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
    
    public void doCambiarEstado(CursoDemo curso) {
        if (curso == null) return;

        if (curso.isActivo()) {
            curso.setActivo(false);
            curso.setEstado("BAJA");
            generalController.getFramework()
                .doMensajeF("BAJA", "Curso dado de baja correctamente", 2);
        } else {
            curso.setActivo(true);
            curso.setEstado("ACTIVO");
            generalController.getFramework()
                .doMensajeF("ACTIVAR", "Curso activado correctamente", 1);
        }

        doBuscar(); // refresca la tabla respetando el filtro
    }


    // ============================
    // HELPERS
    // ============================
    private CursoDemo findById(int id) {
        for (CursoDemo c : lstCursos) if (c.getId() == id) return c;
        return null;
    }

    private int nextId() {
        int max = 0;
        for (CursoDemo c : lstCursos) if (c.getId() > max) max = c.getId();
        return max + 1;
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

    private boolean isDecimal(String s) {
        if (s == null) return false;
        String x = s.trim();
        if (x.isBlank()) return false;
        return x.matches("^\\d+(\\.\\d{1,2})?$"); // 140 o 140.0 o 140.00
    }

    // ============================
    // DEMO ENTITY
    // ============================
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CursoDemo implements Serializable {
        private int id;
        private String nombre;
        private String nivel;
        private String duracion;
        private String precioGeneral;
        private String precioAlumnoUns;
        private String precioTrabajadorUns;
        private String precioExterno;
        private String estado;
        private boolean activo;

        // para que temas se guarde “con el curso”
        private List<String> temas;

        public CursoDemo cloneLite() {
            CursoDemo c = new CursoDemo();
            c.setId(this.id);
            c.setNombre(this.nombre);
            c.setNivel(this.nivel);
            c.setDuracion(this.duracion);
            c.setPrecioGeneral(this.precioGeneral);
            c.setPrecioAlumnoUns(this.precioAlumnoUns);
            c.setPrecioTrabajadorUns(this.precioTrabajadorUns);
            c.setPrecioExterno(this.precioExterno);
            c.setEstado(this.estado);
            c.setActivo(this.activo);
            c.setTemas(this.temas == null ? new ArrayList<>() : new ArrayList<>(this.temas));
            return c;
        }
    }
}
