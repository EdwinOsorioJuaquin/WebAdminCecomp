package controladores;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.*;
import lombok.*;

@Named(value = "grupoController")
@SessionScoped
@Getter
@Setter
public class GrupoController implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final String VISTA_LISTA = "LISTA";
    private static final String VISTA_NUEVO = "NUEVO";

    @Inject
    private GeneralController generalController;

    private String strValor;
    private String strBusqueda;

    // combos
    private List<String> lstCursos;
    private List<String> lstModalidades;
    private List<String> lstDocentes;
    private List<String> lstDias;

    // data
    private List<GrupoDemo> lstGruposBase;
    private List<GrupoDemo> lstGruposView;

    private GrupoDemo nuevoGrupo;
    private Integer idEdicion; // null si es nuevo

    @PostConstruct
    public void init() {
        strValor = "LISTA";
        strBusqueda = "";

        lstCursos = Arrays.asList("AUTOCAD BÁSICO", "EXCEL AVANZADO", "JAVA POO");
        lstModalidades = Arrays.asList("PRESENCIAL", "VIRTUAL", "HÍBRIDO");
        lstDocentes = Arrays.asList("Trejo Obregón Rodrigo Emilio", "Zapata López Carla", "Docente Invitado");
        lstDias = Arrays.asList("LUNES", "MARTES", "MIÉRCOLES", "JUEVES", "VIERNES", "SÁBADO","DOMINGO");

        lstGruposBase = new ArrayList<>();

        // ===== DEMO 1 =====
        GrupoDemo g1 = new GrupoDemo();
        g1.setId(1);
        g1.setCodigo("GRUPO01");
        g1.setCurso("JAVA POO");
        g1.setModalidad("PRESENCIAL");
        g1.setDocente("Trejo Obregón Rodrigo Emilio");
        g1.setFechaInicio(parseFecha("01-01-2025"));
        g1.setFechaFin(parseFecha("01-02-2025"));
        g1.setHoraInicio(parseHora("18:00"));
        g1.setHoraFin(parseHora("21:00"));
        g1.setDias(new ArrayList<>(Arrays.asList("LUNES", "MARTES")));
        g1.setActivo(true);
        g1.setEstado("ACTIVO");
        lstGruposBase.add(g1);

        // ===== DEMO 2 =====
        GrupoDemo g2 = new GrupoDemo();
        g2.setId(2);
        g2.setCodigo("GRUPO02");
        g2.setCurso("AUTOCAD BÁSICO");
        g2.setModalidad("VIRTUAL");
        g2.setDocente("Zapata López Carla");
        g2.setFechaInicio(parseFecha("02-02-2025"));
        g2.setFechaFin(parseFecha("02-03-2025"));
        g2.setHoraInicio(parseHora("18:00"));
        g2.setHoraFin(parseHora("21:00"));
        g2.setDias(new ArrayList<>(Arrays.asList("MARTES", "JUEVES")));
        g2.setActivo(false);
        g2.setEstado("BAJA");
        lstGruposBase.add(g2);

        // vista inicial
        lstGruposView = new ArrayList<>(lstGruposBase);

        nuevoGrupo = new GrupoDemo();
        idEdicion = null;
    }

    public void doIniciarPagina() {
        if (strValor == null || strValor.isBlank()) strValor = VISTA_LISTA;
        if (lstGruposView == null) lstGruposView = new ArrayList<>(lstGruposBase);
    }

    // ============================
    // LISTA
    // ============================
    public void doBuscar() {
        String filtro = (strBusqueda == null) ? "" : strBusqueda.trim().toUpperCase();

        if (filtro.isBlank()) {
            lstGruposView = new ArrayList<>(lstGruposBase);
            generalController.getFramework().doMensajeF("BÚSQUEDA", "Mostrando todos los grupos", 1);
            return;
        }

        List<GrupoDemo> out = new ArrayList<>();
        for (GrupoDemo g : lstGruposBase) {
            if (contiene(g.getCodigo(), filtro)
                    || contiene(g.getDocente(), filtro)
                    || contiene(g.getCurso(), filtro)) {
                out.add(g);
            }
        }
        lstGruposView = out;

        generalController.getFramework().doMensajeF("BÚSQUEDA",
                "Se encontraron " + lstGruposView.size() + " grupos", 1);
    }

    public void doVolver() {
        strValor = VISTA_LISTA;
        doBuscar(); // mantiene filtro
    }

    // ============================
    // NUEVO / EDITAR
    // ============================
    public void doNuevo() {
        strValor = VISTA_NUEVO;
        idEdicion = null;

        nuevoGrupo = new GrupoDemo();
        nuevoGrupo.setEstado("ACTIVO");
        nuevoGrupo.setActivo(true);
        nuevoGrupo.setDias(new ArrayList<>());
    }

    public void doEditar(GrupoDemo g) {
        if (g == null) return;

        strValor = VISTA_NUEVO;
        idEdicion = g.getId();

        // CLON para no mutar fila hasta guardar
        nuevoGrupo = g.cloneLite();
    }

    public void doGuardar() {
        if (!validar()) return;

        // normalizar estado
        nuevoGrupo.setEstado(nuevoGrupo.isActivo() ? "ACTIVO" : "BAJA");

        if (idEdicion == null) {
            GrupoDemo nuevo = nuevoGrupo.cloneLite();
            nuevo.setId(nextId());
            lstGruposBase.add(nuevo);
            generalController.getFramework().doMensajeF("GUARDAR", "Grupo creado correctamente", 1);
        } else {
            GrupoDemo real = findById(idEdicion);
            if (real == null) {
                generalController.getFramework().doMensajeF("ERROR", "No se encontró el grupo a editar", 3);
                return;
            }
            real.copyFrom(nuevoGrupo);
            generalController.getFramework().doMensajeF("ACTUALIZAR", "Grupo actualizado correctamente", 1);
        }

        // refrescar y volver
        doBuscar();
        strValor = VISTA_LISTA;
        idEdicion=null;
        nuevoGrupo= new GrupoDemo();
    }

    // ============================
    // BAJA / ACTIVAR (toggle)
    // ============================
    public void doCambiarEstado(GrupoDemo g) {
        if (g == null) return;

        if (g.isActivo()) {
            g.setActivo(false);
            g.setEstado("BAJA");
            generalController.getFramework().doMensajeF("BAJA", "Grupo dado de baja", 2);
        } else {
            g.setActivo(true);
            g.setEstado("ACTIVO");
            generalController.getFramework().doMensajeF("ACTIVAR", "Grupo activado", 1);
        }

        doBuscar();
    }

    // ============================
    // HELPERS
    // ============================
    private boolean validar() {
        if (nuevoGrupo == null) nuevoGrupo = new GrupoDemo();

        if (isBlank(nuevoGrupo.getCodigo())) {
            generalController.getFramework().doMensajeF("VALIDACIÓN", "Ingrese el nombre/código del grupo (ej: GRUPO03)", 2);
            return false;
        }
        if (isBlank(nuevoGrupo.getCurso())) {
            generalController.getFramework().doMensajeF("VALIDACIÓN", "Seleccione un curso", 2);
            return false;
        }
        if (isBlank(nuevoGrupo.getDocente())) {
            generalController.getFramework().doMensajeF("VALIDACIÓN", "Seleccione un docente", 2);
            return false;
        }
        if (nuevoGrupo.getDias() == null || nuevoGrupo.getDias().isEmpty()) {
            generalController.getFramework().doMensajeF("VALIDACIÓN", "Seleccione al menos un día", 2);
            return false;
        }
        if (nuevoGrupo.getFechaInicio() == null || nuevoGrupo.getFechaFin() == null) {
            generalController.getFramework().doMensajeF("VALIDACIÓN", "Ingrese fecha inicio y fin", 2);
            return false;
        }
        if (nuevoGrupo.getHoraInicio() == null || nuevoGrupo.getHoraFin() == null) {
            generalController.getFramework().doMensajeF("VALIDACIÓN", "Ingrese hora inicio y fin", 2);
            return false;
        }

        if (isBlank(nuevoGrupo.getModalidad())) {
            generalController.getFramework().doMensajeF("VALIDACIÓN", "Seleccione una modalidad", 2);
            return false;
        }
        // normalizar código
        nuevoGrupo.setCodigo(nuevoGrupo.getCodigo().trim().toUpperCase());

        // validar fechas (inicio <= fin)
        if (nuevoGrupo.getFechaInicio().after(nuevoGrupo.getFechaFin())) {
            generalController.getFramework().doMensajeF("VALIDACIÓN",
                    "La Fecha Inicio no puede ser mayor que la Fecha Fin", 2);
            return false;
        }

        // validar horas (inicio < fin)
        if (!nuevoGrupo.getHoraInicio().before(nuevoGrupo.getHoraFin())) {
            generalController.getFramework().doMensajeF("VALIDACIÓN",
                    "La Hora Inicio debe ser menor que la Hora Fin", 2);
            return false;
        }

        // validar duplicado de código (solo cuando es nuevo o cambias código)
        if (existeCodigoDuplicado(nuevoGrupo.getCodigo(), idEdicion)) {
            generalController.getFramework().doMensajeF("VALIDACIÓN",
                    "Ya existe un grupo con el código " + nuevoGrupo.getCodigo(), 2);
            return false;
        }

        return true;
    }

    private GrupoDemo findById(int id) {
        for (GrupoDemo g : lstGruposBase) if (g.getId() == id) return g;
        return null;
    }

    private int nextId() {
        int max = 0;
        for (GrupoDemo g : lstGruposBase) if (g.getId() > max) max = g.getId();
        return max + 1;
    }

    private boolean contiene(String campo, String filtro) {
        if (campo == null) return false;
        return campo.toUpperCase().contains(filtro);
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isBlank();
    }
    
    private static final java.text.SimpleDateFormat FMT_FECHA = new java.text.SimpleDateFormat("dd-MM-yyyy");
    private static final java.text.SimpleDateFormat FMT_HORA  = new java.text.SimpleDateFormat("HH:mm");

    private Date parseFecha(String s) {
        try {
            return (s == null || s.isBlank()) ? null : FMT_FECHA.parse(s.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private Date parseHora(String s) {
        try {
            return (s == null || s.isBlank()) ? null : FMT_HORA.parse(s.trim());
        } catch (Exception e) {
            return null;
        }
    }
    
    private boolean existeCodigoDuplicado(String codigo, Integer idActual) {
        if (codigo == null) return false;
        for (GrupoDemo g : lstGruposBase) {
            if (g.getCodigo() != null && g.getCodigo().equalsIgnoreCase(codigo)) {
                if (idActual == null || g.getId() != idActual) {
                    return true;
                }
            }
        }
        return false;
    }


    // ============================
    // ENTITY DEMO
    // ============================
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Setter
    public static class GrupoDemo implements Serializable {
        private int id;

        // IMPORTANTE: en la tabla usas registro.codigo
        private String codigo;

        private String docente;
        private String curso;

        private Date fechaInicio;
        private Date fechaFin;
        private Date horaInicio;
        private Date horaFin;


        private List<String> dias;
        private String modalidad;

        private String estado; // ACTIVO/BAJA
        private boolean activo;

        public String getHorario() {
            String hi = formatearHora(horaInicio);
            String hf = formatearHora(horaFin);
            if (hi.isBlank() && hf.isBlank()) return "";
            return hi + " - " + hf;
        }


        public String getDiasTexto() {
            return (dias == null || dias.isEmpty()) ? "" : String.join("-", dias);
        }
        
        public String getFechaInicioTexto() {
            return formatearFecha(fechaInicio);
        }
        public String getFechaFinTexto() {
            return formatearFecha(fechaFin);
        }
        
        private static String formatearFecha(Date d) {
            if (d == null) return "";
            return new java.text.SimpleDateFormat("dd-MM-yyyy").format(d);
        }
        private static String formatearHora(Date d) {
            if (d == null) return "";
            return new java.text.SimpleDateFormat("HH:mm").format(d);
        }



        public GrupoDemo cloneLite() {
            GrupoDemo x = new GrupoDemo();
            x.id = this.id;
            x.codigo = this.codigo;
            x.docente = this.docente;
            x.curso = this.curso;
            x.fechaInicio = this.fechaInicio;
            x.fechaFin = this.fechaFin;
            x.horaInicio = this.horaInicio;
            x.horaFin = this.horaFin;
            x.modalidad = this.modalidad;
            x.dias = (this.dias == null) ? new ArrayList<>() : new ArrayList<>(this.dias);
            x.estado = this.estado;
            x.activo = this.activo;
            return x;
        }

        public void copyFrom(GrupoDemo src) {
            this.codigo = src.codigo;
            this.docente = src.docente;
            this.curso = src.curso;
            this.fechaInicio = src.fechaInicio;
            this.fechaFin = src.fechaFin;
            this.horaInicio = src.horaInicio;
            this.horaFin = src.horaFin;
            this.modalidad = src.modalidad;
            this.dias = (src.dias == null) ? new ArrayList<>() : new ArrayList<>(src.dias);
            this.activo = src.activo;
            this.estado = src.activo ? "ACTIVO" : "BAJA";
        }
    }
}
