package controladores;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.*;
import lombok.*;

@Named("registroDecanaturaController")
@SessionScoped
@Getter @Setter
public class RegistroDecanaturaController implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final String VISTA_LISTA  = "LISTA";
    private static final String VISTA_RANGO  = "RANGO";
    private static final String VISTA_EDITAR = "EDITAR";

    @Inject
    private GeneralController generalController;

    // navegación
    private String strValor;

    // rango
    private Integer numInicio;
    private Integer numFin;

    // búsqueda
    private String strBusqueda;

    // data
    private List<RegistroDecanatura> lstBase;
    private List<RegistroDecanatura> lstView;

    // KPIs
    private Integer totalRegistrados;
    private Integer totalDisponibles;
    private Integer ultimoAsignado;
    private Integer proximoDisponible;

    // edición
    private Integer idEdicion;           // id del real
    private RegistroDecanatura edit;     // clon editable

    private static final SimpleDateFormat FMT = new SimpleDateFormat("dd-MM-yyyy");

    @PostConstruct
    public void init() {
        strValor = VISTA_LISTA;
        strBusqueda = "";

        lstBase = new ArrayList<>();
        // DEMO
        lstBase.add(new RegistroDecanatura(1, 1001, "DISPONIBLE", "-", hoy(), true));
        lstBase.add(new RegistroDecanatura(2, 1002, "ASIGNADO", "Trejo Obregón Rodrigo Emilio", hoy(), true));
        lstBase.add(new RegistroDecanatura(3, 1003, "DISPONIBLE", "-", hoy(), true));
        lstBase.add(new RegistroDecanatura(4, 1004, "BAJA", "-", hoy(), false));

        lstView = new ArrayList<>(lstBase);

        numInicio = null;
        numFin = null;

        idEdicion = null;
        edit = new RegistroDecanatura();

        recalcularKpis();
    }

    public void doIniciarPagina() {
        if (strValor == null || strValor.isBlank()) strValor = VISTA_LISTA;
        if (lstView == null) lstView = new ArrayList<>(lstBase);
        recalcularKpis();
    }

    // =========================
    // LISTA
    // =========================
    public void doBuscar() {
        String filtro = (strBusqueda == null) ? "" : strBusqueda.trim();

        if (filtro.isBlank()) {
            lstView = new ArrayList<>(lstBase);
            generalController.getFramework().doMensajeF("BÚSQUEDA", "Mostrando todos los registros", 1);
            return;
        }

        Integer nro = null;
        try { nro = Integer.parseInt(filtro); } catch (Exception e) { }

        List<RegistroDecanatura> out = new ArrayList<>();
        for (RegistroDecanatura r : lstBase) {
            if (nro != null && r.getNumero() == nro) out.add(r);
        }
        lstView = out;

        generalController.getFramework().doMensajeF("BÚSQUEDA", "Se encontraron " + out.size() + " registros", 1);
    }

    public void doNuevoRango() {
        strValor = VISTA_RANGO;
        numInicio = null;
        numFin = null;
    }

    public void doVolverLista() {
        strValor = VISTA_LISTA;
        doBuscar();
        recalcularKpis();
    }

    // =========================
    // RANGO
    // =========================
    public void doRegistrarRango() {
        if (numInicio == null || numFin == null) {
            generalController.getFramework().doMensajeF("VALIDACIÓN", "Ingrese Desde y Hasta", 2);
            return;
        }
        if (numInicio <= 0 || numFin <= 0) {
            generalController.getFramework().doMensajeF("VALIDACIÓN", "Los números deben ser mayores a 0", 2);
            return;
        }
        if (numInicio > numFin) {
            generalController.getFramework().doMensajeF("VALIDACIÓN", "Desde no puede ser mayor que Hasta", 2);
            return;
        }

        int creados = 0;
        for (int n = numInicio; n <= numFin; n++) {
            if (!existeNumero(n)) {
                lstBase.add(new RegistroDecanatura(nextId(), n, "DISPONIBLE", "-", hoy(), true));
                creados++;
            }
        }

        generalController.getFramework().doMensajeF("GUARDAR", "Rango registrado. Nuevos: " + creados, 1);

        strValor = VISTA_LISTA;
        doBuscar();
        recalcularKpis();
    }

    // =========================
    // EDITAR
    // =========================
    public void doEditar(RegistroDecanatura r) {
        if (r == null) return;

        // reglas (opcional)
        if (!r.isActivo()) {
            generalController.getFramework().doMensajeF("RESTRICCIÓN", "No se puede editar un registro en BAJA", 2);
            return;
        }
        if ("ASIGNADO".equals(r.getEstado())) {
            generalController.getFramework().doMensajeF("RESTRICCIÓN", "No se puede editar un registro ASIGNADO", 2);
            return;
        }

        idEdicion = r.getId();
        edit = r.cloneLite();
        strValor = VISTA_EDITAR;
    }

    public void doGuardarEdicion() {
        if (idEdicion == null) return;

        if (edit == null || edit.getNumero() <= 0) {
            generalController.getFramework().doMensajeF("VALIDACIÓN", "Número inválido", 2);
            return;
        }

        // duplicado
        RegistroDecanatura real = findById(idEdicion);
        if (real == null) {
            generalController.getFramework().doMensajeF("ERROR", "No se encontró el registro a editar", 3);
            return;
        }

        if (existeNumero(edit.getNumero()) && edit.getNumero() != real.getNumero()) {
            generalController.getFramework().doMensajeF("VALIDACIÓN", "Ese número ya existe", 2);
            return;
        }

        // aplicar cambios editables
        real.setNumero(edit.getNumero());
        // si quieres permitir editar asignadoA cuando DISPONIBLE:
        real.setAsignadoA(edit.getAsignadoA());

        generalController.getFramework().doMensajeF("ACTUALIZAR", "Registro actualizado", 1);

        // limpiar y volver
        idEdicion = null;
        edit = new RegistroDecanatura();
        strValor = VISTA_LISTA;

        doBuscar();
        recalcularKpis();
    }

    public void doCancelarEdicion() {
        idEdicion = null;
        edit = new RegistroDecanatura();
        strValor = VISTA_LISTA;
        doBuscar();
        recalcularKpis();
    }

    // =========================
    // BAJA / ACTIVAR
    // =========================
    public void doCambiarEstado(RegistroDecanatura r) {
        if (r == null) return;

        // regla normal: no permitir baja si está ASIGNADO
        if ("ASIGNADO".equals(r.getEstado()) && r.isActivo()) {
            generalController.getFramework().doMensajeF("RESTRICCIÓN", "No se puede dar de baja un registro ASIGNADO", 2);
            return;
        }

        if (r.isActivo()) {
            r.setActivo(false);
            r.setEstado("BAJA");
            generalController.getFramework().doMensajeF("BAJA", "Registro dado de baja", 2);
        } else {
            r.setActivo(true);
            if (r.getAsignadoA() == null || r.getAsignadoA().isBlank() || "-".equals(r.getAsignadoA())) {
                r.setEstado("DISPONIBLE");
            }
            generalController.getFramework().doMensajeF("ACTIVAR", "Registro activado", 1);
        }

        doBuscar();
        recalcularKpis();
    }

    // =========================
    // KPIs
    // =========================
    private void recalcularKpis() {
        totalRegistrados = lstBase.size();

        int disp = 0;
        Integer minDisp = null;
        int maxAsign = 0;

        for (RegistroDecanatura r : lstBase) {
            if (r.isActivo() && "DISPONIBLE".equals(r.getEstado())) {
                disp++;
                if (minDisp == null || r.getNumero() < minDisp) minDisp = r.getNumero();
            }
            if ("ASIGNADO".equals(r.getEstado())) {
                if (r.getNumero() > maxAsign) maxAsign = r.getNumero();
            }
        }

        totalDisponibles = disp;
        ultimoAsignado = (maxAsign == 0) ? null : maxAsign;
        proximoDisponible = minDisp;
    }

    // =========================
    // Helpers
    // =========================
    private RegistroDecanatura findById(int id) {
        for (RegistroDecanatura r : lstBase) if (r.getId() == id) return r;
        return null;
    }

    private boolean existeNumero(int n) {
        for (RegistroDecanatura r : lstBase) if (r.getNumero() == n) return true;
        return false;
    }

    private int nextId() {
        int max = 0;
        for (RegistroDecanatura r : lstBase) if (r.getId() > max) max = r.getId();
        return max + 1;
    }

    private String hoy() {
        return FMT.format(new Date());
    }

    // =========================
    // Entity
    // =========================
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter @Setter
    public static class RegistroDecanatura implements Serializable {
        private int id;
        private int numero;
        private String estado;       // DISPONIBLE / ASIGNADO / BAJA
        private String asignadoA;
        private String fechaRegistro;
        private boolean activo;

        public RegistroDecanatura cloneLite() {
            return new RegistroDecanatura(id, numero, estado, asignadoA, fechaRegistro, activo);
        }
    }
}
