package controladores;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.*;
import lombok.*;

@Named(value = "asignacionController")
@SessionScoped
@Getter
@Setter
public class AsignacionController implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final String VISTA_LISTA = "LISTA";
    private static final String VISTA_DETALLE = "DETALLE";

    @Inject
    private GeneralController generalController;

    private String strValor;

    // búsquedas separadas (para que no se mezclen)
    private String strBusquedaLista;
    private String strBusquedaDetalle;

    // ASIGNACIONES
    private List<AsignacionDemo> lstAsignacionesBase;
    private List<AsignacionDemo> lstAsignacionesView;

    // ALUMNOS
    private List<AlumnoDemo> lstAlumnosBase;
    private List<AlumnoDemo> lstAlumnosView;

    private AsignacionDemo grupoSeleccionado;

    @PostConstruct
    public void init() {
        strValor = VISTA_LISTA;
        strBusquedaLista = "";
        strBusquedaDetalle = "";

        lstAsignacionesBase = new ArrayList<>();
        lstAsignacionesBase.add(new AsignacionDemo("GRUPO01", "Trejo Obregón Rodrigo Emilio",
                "01-01-2025", "01-02-2025", "6:00 PM - 9:00 PM", "LUNES-MARTES"));
        lstAsignacionesBase.add(new AsignacionDemo("GRUPO02", "Zapata López Carla",
                "02-02-2025", "02-03-2025", "6:00 PM - 9:00 PM", "MARTES-JUEVES"));

        lstAsignacionesView = new ArrayList<>(lstAsignacionesBase);

        lstAlumnosBase = new ArrayList<>();
        lstAlumnosView = new ArrayList<>();
    }

    public void doIniciarPagina() {
        if (strValor == null || strValor.isBlank()) strValor = VISTA_LISTA;
        if (lstAsignacionesView == null) lstAsignacionesView = new ArrayList<>(lstAsignacionesBase);
    }

    // ============================
    // BÚSQUEDA
    // ============================
    public void doBuscar() {
        if (VISTA_LISTA.equals(strValor)) {
            buscarAsignaciones();
        } else if (VISTA_DETALLE.equals(strValor)) {
            buscarAlumnos();
        }
    }

    private void buscarAsignaciones() {
        String filtro = (strBusquedaLista == null) ? "" : strBusquedaLista.trim().toUpperCase();

        if (filtro.isBlank()) {
            lstAsignacionesView = new ArrayList<>(lstAsignacionesBase);
            generalController.getFramework().doMensajeF("BÚSQUEDA", "Mostrando todas las asignaciones", 1);
            return;
        }

        List<AsignacionDemo> out = new ArrayList<>();
        for (AsignacionDemo a : lstAsignacionesBase) {
            if (contiene(a.getGrupo(), filtro) || contiene(a.getDocente(), filtro)) {
                out.add(a);
            }
        }
        lstAsignacionesView = out;

        generalController.getFramework().doMensajeF(
                "BÚSQUEDA",
                "Se encontraron " + lstAsignacionesView.size() + " asignaciones",
                1
        );
    }

    private void buscarAlumnos() {
        if (lstAlumnosBase == null) lstAlumnosBase = new ArrayList<>();

        String filtro = (strBusquedaDetalle == null) ? "" : strBusquedaDetalle.trim().toUpperCase();

        if (filtro.isBlank()) {
            lstAlumnosView = new ArrayList<>(lstAlumnosBase);
            generalController.getFramework().doMensajeF("BÚSQUEDA", "Mostrando todos los alumnos del grupo", 1);
            return;
        }

        List<AlumnoDemo> out = new ArrayList<>();
        for (AlumnoDemo al : lstAlumnosBase) {
            if (contiene(al.getNombre(), filtro) || contiene(al.getDni(), filtro)) {
                out.add(al);
            }
        }
        lstAlumnosView = out;

        generalController.getFramework().doMensajeF(
                "BÚSQUEDA",
                "Se encontraron " + lstAlumnosView.size() + " alumnos",
                1
        );
    }

    // ============================
    // DETALLE
    // ============================
    public void doVerDetalle(AsignacionDemo asig) {
        if (asig == null) return;

        grupoSeleccionado = asig;
        strValor = VISTA_DETALLE;

        // reset búsqueda de detalle
        strBusquedaDetalle = "";

        // alumnos mock DEPENDIENDO del grupo (para que sea realista)
        lstAlumnosBase = generarAlumnosMock(asig.getGrupo());
        lstAlumnosView = new ArrayList<>(lstAlumnosBase);

        generalController.getFramework().doMensajeF("DETALLE",
                "Mostrando alumnos del " + asig.getGrupo(), 1);
    }

    public void doVolver() {
        // volver a lista desde donde sea, sin romper filtro de lista
        strValor = VISTA_LISTA;

        // refrescar lista aplicando el filtro actual (si existe)
        buscarAsignaciones();
        generalController.getFramework().doMensajeF("INFO", "Retornando a la lista de asignaciones", 1);
    }

    private List<AlumnoDemo> generarAlumnosMock(String grupo) {
        List<AlumnoDemo> list = new ArrayList<>();
        int base = "GRUPO02".equalsIgnoreCase(grupo) ? 20 : 10;

        for (int i = 1; i <= 8; i++) {
            int nota = (i % 2 == 0) ? 15 : 11;
            String estado = (nota >= 11) ? "APROBADO" : "DESAPROBADO";

            list.add(new AlumnoDemo(
                    "Alumno " + (base + i) + " - " + grupo,
                    "1234567" + i,
                    "91234567" + i,
                    "alumno" + i + "@gmail.com",
                    nota,
                    estado
            ));
        }
        return list;
    }

    private boolean contiene(String campo, String filtro) {
        if (campo == null) return false;
        return campo.toUpperCase().contains(filtro);
    }

    // ======= CLASES DEMO ========
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Setter
    public static class AsignacionDemo implements Serializable {
        private String grupo;
        private String docente;
        private String fechaInicio;
        private String fechaFin;
        private String horario;
        private String dias;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Setter
    public static class AlumnoDemo implements Serializable {
        private String nombre;
        private String dni;
        private String telefono;
        private String correo;
        private Integer notaFinal;
        private String estado;
    }
}
