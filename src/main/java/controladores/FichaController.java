package controladores;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.*;
import lombok.*;
import org.primefaces.event.FileUploadEvent;
import java.io.IOException;

@Named(value = "fichaController")
@SessionScoped
@Getter
@Setter
public class FichaController implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private GeneralController generalController;

    private String strValor;
    private String strBusqueda;

    // Fuente real en memoria
    private List<Ficha> lstFichasBase;

    // Lista para la tabla (filtrada)
    private List<Ficha> lstFichasView;

    @PostConstruct
    public void init() {
        strValor = "LISTA";
        strBusqueda = "";

        // Base mutable (NO Arrays.asList)
        lstFichasBase = new ArrayList<>();
        lstFichasBase.add(new Ficha("F0001", "Edwin Osorio Juaquin", "GRUPO001", "CURSO1", "ALUMNO UNS", "ACTIVO", true));
        lstFichasBase.add(new Ficha("F0002", "Trejo Obregón Rodrigo Emilio", "GRUPO002", "CURSO2", "ALUMNO UNS", "ACTIVO", true));
        lstFichasBase.add(new Ficha("F0003", "Jael Estefanero Palacios",      "GRUPO003", "CURSO3", "ALUMNO EXTERNO", "BAJA", false));

        // Vista inicial
        lstFichasView = new ArrayList<>(lstFichasBase);
    }

    public void doIniciarPagina() {
        if (strValor == null || strValor.isBlank()) strValor = "LISTA";
        if (lstFichasView == null) lstFichasView = new ArrayList<>(lstFichasBase);
    }

    public void doVolver() {
        strValor = "LISTA";
        doBuscar();
        generalController.getFramework().doMensajeF("INFO", "Retornando a la lista de fichas", 1);
    }

    // ============================
    // BÚSQUEDA (desde base)
    // ============================
    public void doBuscar() {
        String filtro = (strBusqueda == null) ? "" : strBusqueda.trim().toUpperCase();

        if (filtro.isBlank()) {
            lstFichasView = new ArrayList<>(lstFichasBase);
            generalController.getFramework().doMensajeF("BÚSQUEDA", "Mostrando todas las fichas", 1);
            return;
        }

        List<Ficha> filtradas = new ArrayList<>();
        for (Ficha f : lstFichasBase) {
            String estudiante = (f.getEstudiante() == null) ? "" : f.getEstudiante().toUpperCase();
            if (estudiante.contains(filtro)) {
                filtradas.add(f);
            }
        }

        lstFichasView = filtradas;
        generalController.getFramework().doMensajeF("BÚSQUEDA",
                "Se encontraron " + lstFichasView.size() + " fichas para el filtro ingresado", 1);
    }

    // ============================
    // BAJA LÓGICA / ACTIVAR
    // ============================
    public void doCambiarEstado(Ficha f) {
        if (f == null) return;

        if (f.isActivo()) {
            f.setActivo(false);
            f.setEstado("BAJA");
            generalController.getFramework().doMensajeF("BAJA", "Ficha " + f.getCodigo() + " dada de baja", 2);
        } else {
            f.setActivo(true);
            f.setEstado("ACTIVO");
            generalController.getFramework().doMensajeF("ACTIVAR", "Ficha " + f.getCodigo() + " activada", 1);
        }

        doBuscar(); // refresca tabla con el filtro actual
    }

    // ============================
    // EDITAR / VER DETALLE (stub)
    // ============================
    public void doEditar(Ficha f) {
        if (f == null) return;

        if (!f.isActivo()) {
            generalController.getFramework().doMensajeF("AVISO", "No se puede editar una ficha en BAJA", 2);
            return;
        }

        generalController.getFramework().doMensajeF("EDITAR",
                "Función de edición pendiente para " + f.getCodigo(), 1);
    }

    public void doVerDetalle(Ficha f) {
        if (f == null) return;
        generalController.getFramework().doMensajeF("DETALLE",
                "Detalle de ficha " + f.getCodigo() + " pendiente de implementar", 1);
    }

    // ============================
    // (Opcional) CARGA / DESCARGA
    // ============================
    public void doUpload(FileUploadEvent event) {
        try {
            generalController.getArchivo().setRutaFile("E:/UDEMSI-SIIGAA/Admision/");
            generalController.getArchivo().setNombreFile("archivo.pdf");
            generalController.getArchivo().setInputFile(event.getFile().getInputStream());
            generalController.getFramework().doCargarFile(generalController.getArchivo());
            generalController.getFramework().doMensajeF("CARGA", "Archivo cargado correctamente", 1);
        } catch (IOException ex) {
            System.out.println("[doUpload] Error al cargar archivo " + ex);
        }
    }

    // ============================
    // ENTITY
    // ============================
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Ficha implements Serializable {
        private String codigo;
        private String estudiante;
        private String grupo;
        private String curso;
        private String condicion;

        // NUEVO: estado lógico
        private String estado; // ACTIVO / BAJA
        private boolean activo;
    }
}
