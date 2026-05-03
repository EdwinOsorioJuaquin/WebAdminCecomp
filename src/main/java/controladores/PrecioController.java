package controladores;

import ejbCecomp.ejb.negocio.ejbCcoCcoPreciosServiceLocal;
import ejbCecomp.entidades.ejbCcoCcoPrecios;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import lombok.Getter;
import lombok.Setter;
import static libreriaUdemsi.funciones.libreriaGeneral.doGenerarJNDI;

@Named(value = "precioController")
@SessionScoped
@Getter
@Setter
public class PrecioController implements Serializable {

    @Inject
    private GeneralController generalController;

    private List<ejbCcoCcoPrecios> lstPrecios;
    private List<ejbCcoCcoPrecios> lstPreciosView;
    private ejbCcoCcoPrecios clsPrecio;
    private String strBusqueda;
    private String strValor;
    private Integer idEdicion;

    private static final String VISTA_LISTA = "LISTA";
    private static final String VISTA_NUEVO = "NUEVO";

    private ejbCcoCcoPreciosServiceLocal srvPrecio;

    public PrecioController() {
        try {
            Context context = new InitialContext();
            srvPrecio = (ejbCcoCcoPreciosServiceLocal) context.lookup(doGenerarJNDI("ejbCecomp", "1.0", "ejbCcoCcoPreciosServiceLocal"));
        } catch (NamingException e) {
            System.out.println("Error precioController: " + e);
        }
    }

    public void doIniciarPagina() {
        System.out.println("Llegó a doIniciarPagina");
        strValor = VISTA_LISTA;
        strBusqueda = "";
        cargarPrecios();
        clsPrecio = new ejbCcoCcoPrecios();
        idEdicion = null;
    }

    private void cargarPrecios() {
        try {
            lstPrecios = srvPrecio.listarActivos();
            lstPreciosView = new ArrayList<>(lstPrecios);
        } catch (Exception e) {
            lstPrecios = new ArrayList<>();
            lstPreciosView = new ArrayList<>();
        }
    }

    public void doBuscar() {
        String q = (strBusqueda == null) ? "" : strBusqueda.trim().toLowerCase();

        if (q.isBlank()) {
            lstPreciosView = new ArrayList<>(lstPrecios);
        } else {
            List<ejbCcoCcoPrecios> filtrado = new ArrayList<>();
            for (ejbCcoCcoPrecios p : lstPrecios) {
                String concepto = p.getConcepto() != null ? p.getConcepto().toLowerCase() : "";
                String tipoPrecio = p.getTipoPrecio() != null ? p.getTipoPrecio().toLowerCase() : "";
                if (concepto.contains(q) || tipoPrecio.contains(q)) {
                    filtrado.add(p);
                }
            }
            lstPreciosView = filtrado;
        }
        generalController.getFramework().doMensajeF("BÚSQUEDA", "Filtro aplicado correctamente", 1);
    }

    public void doNuevo() {
        strValor = VISTA_NUEVO;
        idEdicion = null;
        clsPrecio = new ejbCcoCcoPrecios();
        clsPrecio.setActivo((short) 1);
    }

    public void doEditar(ejbCcoCcoPrecios precio) {
        if (precio == null) return;
        strValor = VISTA_NUEVO;
        idEdicion = precio.getIdPrecio();
        clsPrecio = srvPrecio.buscarPorId(precio.getIdPrecio());
    }

    public void doGuardar() {
        if (!validarPrecio()) return;

        try {
            if (idEdicion == null || idEdicion == 0) {
                int nuevoId = obtenerNuevoIdPrecio();
                clsPrecio.setIdPrecio(nuevoId);
                srvPrecio.crear(clsPrecio);
                generalController.getFramework().doMensajeF("GUARDAR", "Precio agregado correctamente", 1);
            } else {
                srvPrecio.actualizar(clsPrecio);
                generalController.getFramework().doMensajeF("ACTUALIZAR", "Precio actualizado correctamente", 1);
            }

            cargarPrecios();
            lstPreciosView = new ArrayList<>(lstPrecios);
            strValor = VISTA_LISTA;

        } catch (Exception e) {
            generalController.getFramework().doMensajeF("ERROR", "Error al guardar el precio", 3);
        }
    }

    public void doEliminar(ejbCcoCcoPrecios precio) {
        if (precio == null) return;
        precio.setActivo((short) 0);
        srvPrecio.actualizar(precio);
        generalController.getFramework().doMensajeF("BAJA", "Precio dado de baja correctamente", 2);
        cargarPrecios();
        doBuscar();
    }

    public void doCambiarEstado(ejbCcoCcoPrecios precio) {
        if (precio == null) return;
        short nuevoEstado = precio.getActivo() == 1 ? (short) 0 : (short) 1;
        precio.setActivo(nuevoEstado);
        srvPrecio.actualizar(precio);
        String mensaje = nuevoEstado == 1 ? "Precio activado correctamente" : "Precio dado de baja correctamente";
        int tipoMsg = nuevoEstado == 1 ? 1 : 2;
        generalController.getFramework().doMensajeF(nuevoEstado == 1 ? "ACTIVAR" : "BAJA", mensaje, tipoMsg);
        cargarPrecios();
        doBuscar();
    }

    public void doVolver() {
        strValor = VISTA_LISTA;
        strBusqueda = "";
        doBuscar();
    }

    private int obtenerNuevoIdPrecio() {
        int maxId = 0;
        if (lstPrecios != null && !lstPrecios.isEmpty()) {
            for (ejbCcoCcoPrecios p : lstPrecios) {
                Integer id = p.getIdPrecio();
                if (id != null && id > maxId) maxId = id;
            }
        }
        return maxId + 1;
    }

    private boolean validarPrecio() {
        if (clsPrecio == null) clsPrecio = new ejbCcoCcoPrecios();

        if (clsPrecio.getConcepto() == null || clsPrecio.getConcepto().trim().isEmpty()) {
            generalController.getFramework().doMensajeF("VALIDACIÓN", "Ingrese el concepto del precio", 2);
            return false;
        }
        if (clsPrecio.getTipoPrecio() == null || clsPrecio.getTipoPrecio().trim().isEmpty()) {
            generalController.getFramework().doMensajeF("VALIDACIÓN", "Ingrese el tipo de precio", 2);
            return false;
        }
        if (clsPrecio.getMonto() == null || clsPrecio.getMonto().compareTo(BigDecimal.ZERO) <= 0) {
            generalController.getFramework().doMensajeF("VALIDACIÓN", "Ingrese un monto válido", 2);
            return false;
        }
        return true;
    }
}