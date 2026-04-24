package controladores;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.*;
import lombok.*;

@Named(value = "certificadoController")
@SessionScoped
@Getter @Setter
public class CertificadoController implements Serializable {

    private static final long serialVersionUID = 1L;

    // ===========================================================
    // === INYECCIÓN =============================================
    // ===========================================================
    @Inject
    private GeneralController generalController;

    // ===========================================================
    // === NAVEGACIÓN / ESTADO ===================================
    // ===========================================================
    private String vistaActiva; // "PENDIENTES", "FIRMADOS", "TOKEN", "DECANATURA"
    private String strBusqueda;

    // ===========================================================
    // === LISTAS CERTIFICADOS (BASE / VIEW) =====================
    // ===========================================================
    private List<Certificado> lstPendientesBase;
    private List<Certificado> lstPendientes;

    private List<Certificado> lstFirmadosBase;
    private List<Certificado> lstFirmados;

    // TOKEN
    private List<Certificado> lstTokenBase;        // base general que contiene candidatos (algunos ya con token)
    private List<Certificado> lstToken;            // vista: SOLO pendientes (sin token)
    private List<Certificado> lstTokenGenerados;   // nuevos tokens generados (para vista "Reportes Token")
    private List<Certificado> lstSeleccionados;    // selección de la tabla

    // ===========================================================
    // === REGISTRO DECANATURA (BASE / VIEW) =====================
    // ===========================================================
    private Integer numInicio;
    private Integer numFin;

    private Integer totalRegistrados;
    private Integer totalDisponibles;
    private Integer ultimoAsignado;
    private Integer proximoDisponible;

    private List<RegistroDecanatura> lstRegistrosBase;
    private List<RegistroDecanatura> lstRegistros; // vista

    // ===========================================================
    // === INIT ===================================================
    // ===========================================================
    @PostConstruct
    public void init() {
        strBusqueda = "";
        vistaActiva = "PENDIENTES";

        // certificados
        lstPendientesBase = new ArrayList<>();
        lstPendientes = new ArrayList<>();

        lstFirmadosBase = new ArrayList<>();
        lstFirmados = new ArrayList<>();

        lstTokenBase = new ArrayList<>();
        lstToken = new ArrayList<>();
        lstTokenGenerados = new ArrayList<>();
        lstSeleccionados = new ArrayList<>();

        // decanatura
        lstRegistrosBase = new ArrayList<>();
        lstRegistros = new ArrayList<>();

        numInicio = null;
        numFin = null;

        totalRegistrados = 0;
        totalDisponibles = 0;
        ultimoAsignado = 0;
        proximoDisponible = 0;

        // demo
        cargarEjemploPendientes();
        cargarEjemploFirmados();
        cargarEjemploToken();

        // arranque
        doIniciarPendientes();
    }

    // ===========================================================
    // === INICIO DE CADA VISTA ==================================
    // ===========================================================
    public void doIniciarPendientes() {
        vistaActiva = "PENDIENTES";
        strBusqueda = "";
        lstPendientes = new ArrayList<>(lstPendientesBase);
        info("Pendientes", "Vista Pendientes iniciada");
    }

    public void doIniciarFirmados() {
        vistaActiva = "FIRMADOS";
        strBusqueda = "";
        lstFirmados = new ArrayList<>(lstFirmadosBase);
        info("Firmados", "Vista Firmados iniciada");
    }

    public void doIniciarToken() {
        vistaActiva = "TOKEN";
        strBusqueda = "";
        if (lstSeleccionados != null) lstSeleccionados.clear();

        refrescarPendientesToken();
        info("Token", "Vista Token iniciada (pendientes)");
    }

    public void doIniciarRegistroDecanatura() {
        vistaActiva = "DECANATURA";
        strBusqueda = "";
        lstRegistros = new ArrayList<>(lstRegistrosBase);
        actualizarIndicadores();
        info("Decanatura", "Vista Registro Decanatura iniciada");
    }

    // ===========================================================
    // === BÚSQUEDAS =============================================
    // ===========================================================
    // Pendientes: filtra por estudiante
    public void doBuscarPendientes() {
        String f = normalizar(strBusqueda);
        if (f.isBlank()) {
            lstPendientes = new ArrayList<>(lstPendientesBase);
            info("Búsqueda", "Mostrando todos los pendientes");
            return;
        }

        List<Certificado> out = new ArrayList<>();
        for (Certificado c : lstPendientesBase) {
            if (contiene(c.getEstudiante(), f)) out.add(c);
        }
        lstPendientes = out;
        info("Búsqueda", "Se encontraron " + out.size() + " pendientes");
    }

    // Firmados: filtra por estudiante/curso
    public void doBuscarFirmados() {
        String f = normalizar(strBusqueda);
        if (f.isBlank()) {
            lstFirmados = new ArrayList<>(lstFirmadosBase);
            info("Búsqueda", "Mostrando todos los firmados");
            return;
        }

        List<Certificado> out = new ArrayList<>();
        for (Certificado c : lstFirmadosBase) {
            if (contiene(c.getEstudiante(), f) || contiene(c.getCurso(), f)) out.add(c);
        }
        lstFirmados = out;
        info("Búsqueda", "Se encontraron " + out.size() + " firmados");
    }

    // Token: filtra por estudiante/curso (sobre pendientes)
    public void doBuscarToken() {
        String f = normalizar(strBusqueda);
        List<Certificado> basePend = getPendientesToken();

        if (f.isBlank()) {
            lstToken = new ArrayList<>(basePend);
            info("Búsqueda", "Mostrando certificados pendientes de token");
            return;
        }

        List<Certificado> out = new ArrayList<>();
        for (Certificado c : basePend) {
            if (contiene(c.getEstudiante(), f) || contiene(c.getCurso(), f)) out.add(c);
        }
        lstToken = out;
        info("Búsqueda", "Se encontraron " + out.size() + " pendientes de token");
    }

    // Decanatura: filtra por número (si strBusqueda es número)
    public void doBuscarRegistros() {
        String f = normalizar(strBusqueda);
        if (f.isBlank()) {
            lstRegistros = new ArrayList<>(lstRegistrosBase);
            info("Búsqueda", "Mostrando todos los registros");
            return;
        }

        Integer nro = null;
        try { nro = Integer.parseInt(f); } catch (Exception e) { }

        List<RegistroDecanatura> out = new ArrayList<>();
        if (nro != null) {
            for (RegistroDecanatura r : lstRegistrosBase) {
                if (r.getNumero() == nro) out.add(r);
            }
        }
        lstRegistros = out;
        info("Búsqueda", "Se encontraron " + out.size() + " registros");
    }

    // ===========================================================
    // === TOKEN ==================================================
    // ===========================================================
    public void doGenerarTokenMasivo() {
        if (lstSeleccionados == null || lstSeleccionados.isEmpty()) {
            warn("Validación", "Seleccione al menos un certificado");
            return;
        }

        int generados = 0;

        for (Certificado c : lstSeleccionados) {
            // evitar regenerar
            if (c.getToken() != null && !c.getToken().isBlank()) continue;

            // (opcional) regla: solo si firmados y con reg decanatura válido
            // if (!"SI".equalsIgnoreCase(c.getFirmaDecano()) || !"SI".equalsIgnoreCase(c.getFirmaDirector())) continue;

            c.setToken(generarToken());
            generados++;

            // mover a "generados"
            if (!lstTokenGenerados.contains(c)) {
                lstTokenGenerados.add(c);
            }
        }

        lstSeleccionados.clear();

        // refrescar pendientes de token (ya no deben verse aquí)
        refrescarPendientesToken();

        ok("Token", "Tokens generados: " + generados);
    }

    private void refrescarPendientesToken() {
        lstToken = new ArrayList<>(getPendientesToken());
    }

    private List<Certificado> getPendientesToken() {
        List<Certificado> out = new ArrayList<>();
        for (Certificado c : lstTokenBase) {
            if (c.getToken() == null || c.getToken().isBlank()) {
                out.add(c);
            }
        }
        return out;
    }

    private String generarToken() {
        return "TK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    // ===========================================================
    // === REGISTRO DECANATURA ===================================
    // ===========================================================
    public void doRegistrarRango() {
        if (numInicio == null || numFin == null) {
            warn("Validación", "Ingrese Desde y Hasta");
            return;
        }
        if (numInicio <= 0 || numFin <= 0) {
            warn("Validación", "Los números deben ser mayores a 0");
            return;
        }
        if (numInicio > numFin) {
            warn("Validación", "Desde no puede ser mayor que Hasta");
            return;
        }

        int creados = 0;

        for (int i = numInicio; i <= numFin; i++) {
            final int numeroActual = i;
            boolean existe = lstRegistrosBase.stream()
                    .anyMatch(r -> r.getNumero() == numeroActual);

            if (!existe) {
                lstRegistrosBase.add(new RegistroDecanatura(numeroActual, "DISPONIBLE", "-", fechaHoy(), true));
                creados++;
            }
        }

        // refrescar vista e indicadores
        lstRegistros = new ArrayList<>(lstRegistrosBase);
        actualizarIndicadores();

        ok("Decanatura", "Rango registrado. Nuevos: " + creados);
    }

    public void actualizarIndicadores() {
        totalRegistrados = lstRegistrosBase.size();

        totalDisponibles = (int) lstRegistrosBase.stream()
                .filter(r -> r.isActivo() && "DISPONIBLE".equals(r.getEstado()))
                .count();

        OptionalInt maxAsignado = lstRegistrosBase.stream()
                .filter(r -> "ASIGNADO".equals(r.getEstado()))
                .mapToInt(RegistroDecanatura::getNumero)
                .max();

        ultimoAsignado = maxAsignado.isPresent() ? maxAsignado.getAsInt() : null;

        proximoDisponible = lstRegistrosBase.stream()
                .filter(r -> r.isActivo() && "DISPONIBLE".equals(r.getEstado()))
                .mapToInt(RegistroDecanatura::getNumero)
                .min()
                .orElse(0);

        if (proximoDisponible == 0) proximoDisponible = null;
    }

    // BAJA/ACTIVAR registro decanatura
    public void doCambiarEstadoRegistro(RegistroDecanatura r) {
        if (r == null) return;

        // regla: no dar de baja si asignado
        if ("ASIGNADO".equals(r.getEstado()) && r.isActivo()) {
            warn("Restricción", "No se puede dar de baja un registro ASIGNADO");
            return;
        }

        if (r.isActivo()) {
            r.setActivo(false);
            r.setEstado("BAJA");
            warn("BAJA", "Registro dado de baja");
        } else {
            r.setActivo(true);
            if ("BAJA".equals(r.getEstado())) r.setEstado("DISPONIBLE");
            ok("ACTIVAR", "Registro activado");
        }

        actualizarIndicadores();
    }

    // ===========================================================
    // === DEMOS ==================================================
    // ===========================================================
    private void cargarEjemploPendientes() {
        lstPendientesBase.add(new Certificado(1, "Trejo Obregón Rodrigo Emilio", "Grupo 1B", "Programación con Java",
                "Remoto", "ALUMNO UNS", "--", "NO", "NO", null));
        lstPendientesBase.add(new Certificado(2, "Trejo Obregón Rodrigo Emilio", "Grupo 1B", "Autocad Básico",
                "Presencial", "ALUMNO UNS", "--", "NO", "NO", null));

        lstPendientes = new ArrayList<>(lstPendientesBase);
    }

    private void cargarEjemploFirmados() {
        lstFirmadosBase.add(new Certificado(10, "Trejo Obregón Rodrigo Emilio", "Grupo 1B", "Ofimática Intermedio",
                "Remoto", "ALUMNO UNS", "0013-2025", "SI", "SI", null));
        lstFirmadosBase.add(new Certificado(11, "Trejo Obregón Rodrigo Emilio", "Grupo 1B", "Power BI",
                "Presencial", "PÚBLICO EN GENERAL", "0014-2025", "SI", "SI", null));

        lstFirmados = new ArrayList<>(lstFirmadosBase);
    }

    private void cargarEjemploToken() {
        // estos son candidatos a token (al inicio sin token)
        lstTokenBase.add(new Certificado(20, "Trejo Obregón Rodrigo Emilio", "Grupo 1B", "Programación con Java",
                "Remoto", "ALUMNO UNS", "0013-2025", "SI", "SI", null));
        lstTokenBase.add(new Certificado(21, "Trejo Obregón Rodrigo Emilio", "Grupo 1B", "Autocad Básico",
                "Presencial", "ALUMNO UNS", "0013-2025", "SI", "SI", null));

        // vista pendiente
        refrescarPendientesToken();
    }

    private String fechaHoy() {
        return new SimpleDateFormat("dd-MM-yyyy").format(new Date());
    }

    // ===========================================================
    // === HELPERS MENSAJES ======================================
    // ===========================================================
    private String normalizar(String s) {
        return (s == null) ? "" : s.trim().toUpperCase();
    }

    private boolean contiene(String campo, String filtroUpper) {
        if (campo == null) return false;
        return campo.toUpperCase().contains(filtroUpper);
    }

    private void ok(String t, String m) {
        if (generalController != null && generalController.getFramework() != null) {
            generalController.getFramework().doMensajeF(t, m, 1);
        } else {
            System.out.println("[OK] " + t + ": " + m);
        }
    }

    private void info(String t, String m) {
        if (generalController != null && generalController.getFramework() != null) {
            generalController.getFramework().doMensajeF(t, m, 1);
        } else {
            System.out.println("[INFO] " + t + ": " + m);
        }
    }

    private void warn(String t, String m) {
        if (generalController != null && generalController.getFramework() != null) {
            generalController.getFramework().doMensajeF(t, m, 2);
        } else {
            System.out.println("[WARN] " + t + ": " + m);
        }
    }

    // ===========================================================
    // === CLASES INTERNAS =======================================
    // ===========================================================
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter @Setter
    public static class Certificado implements Serializable {
        private int id;
        private String estudiante;
        private String grupo;
        private String curso;
        private String modalidad;
        private String procedencia;
        private String regDecanatura;
        private String firmaDecano;
        private String firmaDirector;
        private String token;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Getter @Setter
    public static class RegistroDecanatura implements Serializable {
        private int numero;
        private String estado;       // DISPONIBLE / ASIGNADO / BAJA
        private String asignadoA;
        private String fechaRegistro;
        private boolean activo;
    }
    
    // ===========================================================
// === REPORTES TOKENS =======================================
// ===========================================================
private String strBusquedaTokens;

private List<TokenReporte> lstTokensReportBase; // base
private List<TokenReporte> lstTokensReportView; // vista

// Iniciar vista
public void doIniciarReportesTokens() {
    vistaActiva = "REPORTE_TOKENS";
    if (strBusquedaTokens == null) strBusquedaTokens = "";

    if (lstTokensReportBase == null) lstTokensReportBase = new ArrayList<>();
    if (lstTokensReportView == null) lstTokensReportView = new ArrayList<>();

    // demo inicial si está vacío
    if (lstTokensReportBase.isEmpty()) {
        cargarEjemploReporteTokens();
    }

    // refrescar estados y vista completa
    recalcularEstadosTokens();
    lstTokensReportView = new ArrayList<>(lstTokensReportBase);
}

// Buscar
public void doBuscarReportesTokens() {
    recalcularEstadosTokens();

    String filtro = (strBusquedaTokens == null) ? "" : strBusquedaTokens.trim().toUpperCase();
    if (filtro.isBlank()) {
        lstTokensReportView = new ArrayList<>(lstTokensReportBase);
        return;
    }

    List<TokenReporte> out = new ArrayList<>();
    for (TokenReporte t : lstTokensReportBase) {
        if (contiene(t.getToken(), filtro)
                || contiene(t.getEstudiante(), filtro)
                || contiene(t.getCurso(), filtro)) {
            out.add(t);
        }
    }
    lstTokensReportView = out;
}

// Ver certificado (por ahora demo)
public void doVerCertificadoToken(TokenReporte reg) {
    // Aquí luego: abrir pdf/descargar según tu framework
    // Por ahora solo mensaje:
    // generalController.getFramework().doMensajeF("CERTIFICADO", "Ver certificado de " + reg.getEstudiante(), 1);
    System.out.println("Ver certificado token: " + (reg != null ? reg.getToken() : ""));
}

// Marcar como usado (activa token por 15 días)
public void doMarcarUsado(TokenReporte reg) {
    if (reg == null) return;

    if (reg.getFechaActivacion() == null) {
        reg.setFechaActivacion(new Date());
        reg.setFechaVencimiento(sumarDias(reg.getFechaActivacion(), 15));
    }
    recalcularEstadosTokens();
    doBuscarReportesTokens();
}

// Recalcular estado/días en base a la fecha actual
private void recalcularEstadosTokens() {
    if (lstTokensReportBase == null) return;

    Date hoy = truncarHora(new Date());

    for (TokenReporte t : lstTokensReportBase) {
        if (t.getFechaActivacion() == null) {
            t.setEstado("DISPONIBLE");
            t.setFechaVencimiento(null);
            continue;
        }

        // si tiene activación pero no vencimiento, lo calculamos
        if (t.getFechaVencimiento() == null) {
            t.setFechaVencimiento(sumarDias(t.getFechaActivacion(), 15));
        }

        Date venc = truncarHora(t.getFechaVencimiento());

        if (hoy.after(venc)) {
            t.setEstado("VENCIDO");
        } else {
            t.setEstado("ACTIVO");
        }
    }
}

// Helpers


private Date sumarDias(Date base, int dias) {
    Calendar cal = Calendar.getInstance();
    cal.setTime(base);
    cal.add(Calendar.DAY_OF_MONTH, dias);
    return cal.getTime();
}

private Date truncarHora(Date d) {
    Calendar cal = Calendar.getInstance();
    cal.setTime(d);
    cal.set(Calendar.HOUR_OF_DAY, 0);
    cal.set(Calendar.MINUTE, 0);
    cal.set(Calendar.SECOND, 0);
    cal.set(Calendar.MILLISECOND, 0);
    return cal.getTime();
}

// Demo
private void cargarEjemploReporteTokens() {
    // algunos DISPONIBLES (sin activación)
    lstTokensReportBase.add(new TokenReporte("006841977", "Trejo Obregón Rodrigo Emilio", "Programación Orientada a Objetos con Java", null, null, "DISPONIBLE"));
    lstTokensReportBase.add(new TokenReporte("005841965", "Trejo Obregón Rodrigo Emilio", "Autocad Básico", null, null, "DISPONIBLE"));

    // algunos ACTIVOS (con activación)
    Date act1 = parseFecha("08-08-2025");
    Date ven1 = sumarDias(act1, 15);
    lstTokensReportBase.add(new TokenReporte("004841955", "Trejo Obregón Rodrigo Emilio", "Autocad Básico", act1, ven1, "ACTIVO"));

    // VENCIDO
    Date act2 = parseFecha("10-03-2025");
    Date ven2 = sumarDias(act2, 15);
    lstTokensReportBase.add(new TokenReporte("001841123", "Trejo Obregón Rodrigo Emilio", "Ofimática Empresarial Intermedio", act2, ven2, "VENCIDO"));
}

private Date parseFecha(String s) {
    try {
        return new SimpleDateFormat("dd-MM-yyyy").parse(s);
    } catch (Exception e) {
        return null;
    }
}

// ===========================================================
// === CLASE TokenReporte ====================================
// ===========================================================
@AllArgsConstructor
@NoArgsConstructor
@Getter @Setter
public static class TokenReporte implements Serializable {
    private String token;
    private String estudiante;
    private String curso;

    private Date fechaActivacion;   // null si no usado
    private Date fechaVencimiento;  // activación + 15

    private String estado; // DISPONIBLE / ACTIVO / VENCIDO

    public String getFechaActivacionTxt() {
        return formatearFecha(fechaActivacion);
    }
    public String getFechaVencimientoTxt() {
        return formatearFecha(fechaVencimiento);
    }

    public String getDiasRestantesTxt() {
        if (fechaActivacion == null || fechaVencimiento == null) return "-";

        Date hoy = truncar(new Date());
        Date ven = truncar(fechaVencimiento);

        long diff = ven.getTime() - hoy.getTime();
        long dias = diff / (1000L * 60 * 60 * 24);

        if (dias < 0) return "0";
        return String.format("%02d", dias);
    }

    private static Date truncar(Date d) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(d);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    private static String formatearFecha(Date d) {
        if (d == null) return "-";
        return new SimpleDateFormat("dd-MM-yyyy").format(d);
    }
}

// ===========================================================
// === REPORTE CERTIFICADOS ==================================
// ===========================================================
private String strTokenBusqueda;

private List<ReporteCertificado> lstReporteCertBase;
private List<ReporteCertificado> lstReporteCertView;

public void doIniciarReportesCertificados() {
    vistaActiva = "REPORTE_CERTIFICADOS";
    if (strTokenBusqueda == null) strTokenBusqueda = "";

    if (lstReporteCertBase == null) lstReporteCertBase = new ArrayList<>();
    if (lstReporteCertView == null) lstReporteCertView = new ArrayList<>();

    if (lstReporteCertBase.isEmpty()) {
        cargarEjemploReporteCertificados();
    }

    // refrescar (si fecha emision depende del token)
    lstReporteCertView = new ArrayList<>(lstReporteCertBase);
}

public void doBuscarReportesCertificados() {
    String filtro = (strTokenBusqueda == null) ? "" : strTokenBusqueda.trim().toUpperCase();

    if (filtro.isBlank()) {
        lstReporteCertView = new ArrayList<>(lstReporteCertBase);
        return;
    }

    List<ReporteCertificado> out = new ArrayList<>();
    for (ReporteCertificado r : lstReporteCertBase) {
        if (contiene(r.getToken(), filtro)
                || contiene(r.getEstudiante(), filtro)
                || contiene(r.getCurso(), filtro)) {
            out.add(r);
        }
    }
    lstReporteCertView = out;
}

public void doPrevisualizarCertificado(ReporteCertificado reg) {
    if (reg == null) return;
    System.out.println("Previsualizar PDF certificado: " + reg.getNumeroCertificado() + " token=" + reg.getToken());
    // Aquí luego conectas tu framework PDF viewer / streamed content.
}

public void doDescargarCertificado(ReporteCertificado reg) {
    if (reg == null) return;
    System.out.println("Descargar PDF certificado: " + reg.getNumeroCertificado() + " token=" + reg.getToken());
    // Aquí luego generas Jasper/streamedfile
}

// Demo base
private void cargarEjemploReporteCertificados() {
    // fechaEmision = fechaActivacion del token (si no hay activación: null -> "-")
    Date act1 = parseFecha("01-02-2025");

    lstReporteCertBase.add(new ReporteCertificado(
            "C001", "001-2025", act1,
            "Nombre de Estudiante", "EXCEL",
            "INTERMEDIO", "PRESENCIAL", "CURSO",
            "TK-006841977"
    ));

    lstReporteCertBase.add(new ReporteCertificado(
            "C001", "001-2025", act1,
            "Nombre de Estudiante", "PROGRAMACIÓN ORIENTADA A OBJETOS EN JAVA",
            "-", "VIRTUAL", "CURSO",
            "TK-005841965"
    ));

    // token aún no usado => sin emisión
    lstReporteCertBase.add(new ReporteCertificado(
            "C001", "001-2025", null,
            "Nombre de Estudiante", "OFIMÁTICA EMPRESARIAL",
            "BÁSICO", "VIRTUAL", "EXAMEN SUFIC.",
            "TK-004841955"
    ));
}

// ===========================================================
// === CLASE ReporteCertificado ===============================
// ===========================================================
@AllArgsConstructor
@NoArgsConstructor
@Getter @Setter
public static class ReporteCertificado implements Serializable {
    private String numeroCertificado; // ej: C001
    private String regDecanatura;     // ej: 001-2025
    private Date fechaEmision;        // = fechaActivacion token
    private String estudiante;
    private String curso;
    private String nivel;
    private String modalidad;
    private String categoria;
    private String token;

    public String getFechaEmisionTxt() {
        if (fechaEmision == null) return "-";
        return new SimpleDateFormat("dd-MM-yyyy").format(fechaEmision);
    }
}

}
