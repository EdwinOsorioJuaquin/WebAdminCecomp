/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package controladores;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.*;
import lombok.*;


@Named("tipoPagoController")
@SessionScoped
@Getter
@Setter
public class TipoPagoController implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final String VISTA_LISTA = "LISTA";
    private static final String VISTA_NUEVO = "NUEVO";

    @Inject
    private GeneralController generalController;

    private String strValor;
    private String strBusqueda;

    private List<TipoPagoDemo> lstTiposBase;
    private List<TipoPagoDemo> lstTiposView;

    private TipoPagoDemo nuevoTipo;
    private Integer idEdicion;

    @PostConstruct
    public void init() {
        strValor = VISTA_LISTA;
        strBusqueda = "";

        lstTiposBase = new ArrayList<>();
        lstTiposBase.add(new TipoPagoDemo(1, "MATRÍCULA", "ACTIVO", true));
        lstTiposBase.add(new TipoPagoDemo(2, "CERTIFICACIÓN", "ACTIVO", true));
        lstTiposBase.add(new TipoPagoDemo(3, "OTROS", "BAJA", false));

        lstTiposView = new ArrayList<>(lstTiposBase);

        nuevoTipo = new TipoPagoDemo();
        idEdicion = null;
    }

    public void doIniciarPagina() {
        if (strValor == null || strValor.isBlank()) strValor = VISTA_LISTA;
        if (lstTiposView == null) lstTiposView = new ArrayList<>(lstTiposBase);
    }

    public void doBuscar() {
        String filtro = (strBusqueda == null) ? "" : strBusqueda.trim().toUpperCase();

        if (filtro.isBlank()) {
            lstTiposView = new ArrayList<>(lstTiposBase);
            generalController.getFramework().doMensajeF("BÚSQUEDA", "Mostrando todos los tipos de pago", 1);
            return;
        }

        List<TipoPagoDemo> out = new ArrayList<>();
        for (TipoPagoDemo t : lstTiposBase) {
            if (t.getNombre() != null && t.getNombre().toUpperCase().contains(filtro)) {
                out.add(t);
            }
        }
        lstTiposView = out;
        generalController.getFramework().doMensajeF("BÚSQUEDA", "Se encontraron " + out.size() + " registros", 1);
    }

    public void doNuevo() {
        strValor = VISTA_NUEVO;
        idEdicion = null;

        nuevoTipo = new TipoPagoDemo();
        nuevoTipo.setEstado("ACTIVO");
        nuevoTipo.setActivo(true);
    }

    public void doEditar(TipoPagoDemo t) {
        if (t == null) return;
        strValor = VISTA_NUEVO;
        idEdicion = t.getId();
        nuevoTipo = t.cloneLite();
    }

    public void doGuardar() {
        if (!validar()) return;

        nuevoTipo.setNombre(nuevoTipo.getNombre().trim().toUpperCase());
        nuevoTipo.setEstado(nuevoTipo.isActivo() ? "ACTIVO" : "BAJA");

        if (idEdicion == null) {
            TipoPagoDemo n = nuevoTipo.cloneLite();
            n.setId(nextId());
            lstTiposBase.add(n);
            generalController.getFramework().doMensajeF("GUARDAR", "Tipo de pago creado", 1);
        } else {
            TipoPagoDemo real = findById(idEdicion);
            if (real == null) {
                generalController.getFramework().doMensajeF("ERROR", "No se encontró el registro", 3);
                return;
            }
            real.copyFrom(nuevoTipo);
            generalController.getFramework().doMensajeF("ACTUALIZAR", "Tipo de pago actualizado", 1);
        }

        doVolver();
    }

    public void doCambiarEstado(TipoPagoDemo t) {
        if (t == null) return;

        if (t.isActivo()) {
            t.setActivo(false);
            t.setEstado("BAJA");
            generalController.getFramework().doMensajeF("BAJA", "Registro dado de baja", 2);
        } else {
            t.setActivo(true);
            t.setEstado("ACTIVO");
            generalController.getFramework().doMensajeF("ACTIVAR", "Registro activado", 1);
        }
        doBuscar();
    }

    public void doVolver() {
        strValor = VISTA_LISTA;
        idEdicion = null;
        nuevoTipo = new TipoPagoDemo();
        doBuscar();
    }

    // ===== helpers =====
    private boolean validar() {
        if (nuevoTipo == null) nuevoTipo = new TipoPagoDemo();

        if (nuevoTipo.getNombre() == null || nuevoTipo.getNombre().trim().isBlank()) {
            generalController.getFramework().doMensajeF("VALIDACIÓN", "Ingrese el nombre del tipo de pago", 2);
            return false;
        }

        String nombre = nuevoTipo.getNombre().trim().toUpperCase();
        if (existeNombreDuplicado(nombre, idEdicion)) {
            generalController.getFramework().doMensajeF("VALIDACIÓN", "Ya existe: " + nombre, 2);
            return false;
        }
        return true;
    }

    private boolean existeNombreDuplicado(String nombre, Integer idActual) {
        for (TipoPagoDemo t : lstTiposBase) {
            if (t.getNombre() != null && t.getNombre().equalsIgnoreCase(nombre)) {
                if (idActual == null || t.getId() != idActual) return true;
            }
        }
        return false;
    }

    private TipoPagoDemo findById(int id) {
        for (TipoPagoDemo t : lstTiposBase) if (t.getId() == id) return t;
        return null;
    }

    private int nextId() {
        int max = 0;
        for (TipoPagoDemo t : lstTiposBase) if (t.getId() > max) max = t.getId();
        return max + 1;
    }

    // ===== entity demo =====
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Setter
    public static class TipoPagoDemo implements Serializable {
        private int id;
        private String nombre;
        private String estado;
        private boolean activo;

        public TipoPagoDemo cloneLite() {
            TipoPagoDemo x = new TipoPagoDemo();
            x.id = this.id;
            x.nombre = this.nombre;
            x.estado = this.estado;
            x.activo = this.activo;
            return x;
        }

        public void copyFrom(TipoPagoDemo src) {
            this.nombre = src.nombre;
            this.activo = src.activo;
            this.estado = src.activo ? "ACTIVO" : "BAJA";
        }
    }
}

