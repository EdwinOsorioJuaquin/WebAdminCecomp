package controladores;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import lombok.*;

@Named(value = "pagoController")
@SessionScoped
@Getter
@Setter
public class PagoController implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private GeneralController generalController;

    private String strValor;
    private String strBusqueda;

    // base (fuente real)
    private List<PagoDemo> lstPagosBase;

    // view (lo que se muestra)
    private List<PagoDemo> lstPagosView;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    @PostConstruct
    public void init() {
        strValor = "LISTA";
        strBusqueda = "";

        lstPagosBase = new ArrayList<>();
        lstPagosBase.add(new PagoDemo(LocalDate.parse("12-08-2025", FMT), "F0001",
                "Trejo Obregón Rodrigo Emilio", "GRUPO01",
                "Programación Orientada a Objetos con Java", "MATRÍCULA",
                new BigDecimal("160.00"), "PEN",
                "ALUMNO UNS", "0443252"));

        lstPagosBase.add(new PagoDemo(LocalDate.parse("05-07-2025", FMT), "F0002",
                "Trejo Obregón Rodrigo Emilio", "GRUPO01",
                "Autocad Básico", "CERTIFICACIÓN",
                new BigDecimal("130.00"), "PEN",
                "ALUMNO UNS", "65534"));

        lstPagosBase.add(new PagoDemo(LocalDate.parse("04-04-2025", FMT), "F0001",
                "Trejo Obregón Rodrigo Emilio", "GRUPO01",
                "Autocad Básico", "MATRÍCULA",
                new BigDecimal("130.00"), "PEN",
                "ALUMNO UNS", "75564"));

        lstPagosBase.add(new PagoDemo(LocalDate.parse("06-11-2024", FMT), "F0001",
                "Trejo Obregón Rodrigo Emilio", "GRUPO01",
                "Ofimática Empresarial Intermedio", "CERTIFICACIÓN",
                new BigDecimal("130.00"), "PEN",
                "ALUMNO UNS", "8765"));

        lstPagosBase.add(new PagoDemo(LocalDate.parse("06-08-2024", FMT), "F0001",
                "Trejo Obregón Rodrigo Emilio", "GRUPO01",
                "Fundamentos de Power BI", "MATRÍCULA",
                new BigDecimal("130.00"), "PEN",
                "ALUMNO UNS", "452332"));

        lstPagosBase.add(new PagoDemo(LocalDate.parse("25-04-2024", FMT), "F0001",
                "Trejo Obregón Rodrigo Emilio", "GRUPO01",
                "Ofimática Empresarial Intermedio", "MATRÍCULA",
                new BigDecimal("130.00"), "PEN",
                "ALUMNO UNS", "1534467"));

        lstPagosBase.add(new PagoDemo(LocalDate.parse("20-02-2024", FMT), "F0001",
                "Trejo Obregón Rodrigo Emilio", "GRUPO01",
                "Ofimática Empresarial Básico", "CERTIFICACIÓN",
                new BigDecimal("100.00"), "PEN",
                "PÚBLICO EN GENERAL", "234532"));

        lstPagosBase.add(new PagoDemo(LocalDate.parse("10-10-2023", FMT), "F0001",
                "Trejo Obregón Rodrigo Emilio", "GRUPO01",
                "Ofimática Empresarial Básico", "MATRÍCULA",
                new BigDecimal("100.00"), "PEN",
                "PÚBLICO EN GENERAL", "53222"));

        // vista inicial (ordenada)
        refrescarView(lstPagosBase);
    }

    public void doIniciarPagina() {
        if (strValor == null || strValor.isBlank()) strValor = "LISTA";
        if (lstPagosView == null) refrescarView(lstPagosBase);
    }

    public void doBuscar() {
        String filtro = (strBusqueda == null) ? "" : strBusqueda.trim().toUpperCase();

        if (filtro.isBlank()) {
            refrescarView(lstPagosBase);
            generalController.getFramework().doMensajeF("BÚSQUEDA", "Mostrando todos los pagos", 1);
            return;
        }

        List<PagoDemo> filtrados = new ArrayList<>();
        for (PagoDemo p : lstPagosBase) {
            if (contiene(p.getEstudiante(), filtro)
                    || contiene(p.getCurso(), filtro)
                    || contiene(p.getFicha(), filtro)
                    || contiene(p.getOperacion(), filtro)
                    || contiene(p.getGrupo(), filtro)
                    || contiene(p.getMotivo(), filtro)
                    || contiene(p.getProcedencia(), filtro)) {
                filtrados.add(p);
            }
        }

        refrescarView(filtrados);
        generalController.getFramework().doMensajeF(
                "BÚSQUEDA",
                "Se encontraron " + lstPagosView.size() + " pagos para el filtro ingresado",
                1
        );
    }

    public void doVolver() {
        strValor = "LISTA";
        generalController.getFramework().doMensajeF("INFO", "Retornando a la lista de pagos", 1);
    }

    // total para footer o KPI
    public String getTotalMontoFormateado() {
        BigDecimal total = BigDecimal.ZERO;
        if (lstPagosView != null) {
            for (PagoDemo p : lstPagosView) {
                if (p.getMonto() != null) total = total.add(p.getMonto());
            }
        }
        return "S/ " + total.setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    // ============================
    // HELPERS
    // ============================
    private void refrescarView(List<PagoDemo> fuente) {
        lstPagosView = new ArrayList<>(fuente);
        // orden: más reciente primero
        lstPagosView.sort((a, b) -> b.getFecha().compareTo(a.getFecha()));
    }

    private boolean contiene(String campo, String filtro) {
        if (campo == null) return false;
        return campo.toUpperCase().contains(filtro);
    }

    // ============================
    // DEMO ENTITY
    // ============================
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Setter
    public static class PagoDemo implements Serializable {
        private LocalDate fecha;
        private String ficha;
        private String estudiante;
        private String grupo;
        private String curso;
        private String motivo;
        private BigDecimal monto;
        private String moneda;       // PEN
        private String procedencia;
        private String operacion;

        // getters de presentación para tu tabla (sin cambiar XHTML)
        public String getFechaTexto() {
            return fecha != null ? fecha.format(FMT) : "";
        }

        public String getMontoTexto() {
            // por defecto PEN -> "S/ "
            String pref = "S/ ";
            return pref + (monto == null ? "0.00" : monto.setScale(2, BigDecimal.ROUND_HALF_UP).toString());
        }
    }
}
