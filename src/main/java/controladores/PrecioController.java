package controladores;

import ejbCecomp.ejb.negocio.ejbCcoCepServicioPrecioServiceLocal;
import ejbCecomp.entidades.ejbCcoCepServicioPrecio;
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

    private static final long serialVersionUID = 1L;

    @Inject
    private GeneralController generalController;

    private List<ejbCcoCepServicioPrecio> lstPrecios;
    private List<ejbCcoCepServicioPrecio> lstPreciosView;
    private ejbCcoCepServicioPrecio clsPrecio;
    private String strBusqueda;
    private String strValor;
    private Integer idEdicion;

    private static final String VISTA_LISTA = "LISTA";
    private static final String VISTA_NUEVO = "NUEVO";

    private ejbCcoCepServicioPrecioServiceLocal srvPrecio;

    public PrecioController() {
        try {
            Context context = new InitialContext();
            srvPrecio = (ejbCcoCepServicioPrecioServiceLocal) context.lookup(
                doGenerarJNDI("ejbCecomp", "1.0", "ejbCcoCepServicioPrecioServiceLocal")
            );
        } catch (NamingException e) {
            System.out.println("Error precioController: " + e);
        }
    }

    public void doIniciarPagina() {
        System.out.println("Llegó a doIniciarPagina - Precios Servicios");
        strValor = VISTA_LISTA;
        strBusqueda = "";
        cargarPrecios();
        clsPrecio = new ejbCcoCepServicioPrecio();
        idEdicion = null;
    }

    private void cargarPrecios() {
        try {
            lstPrecios = srvPrecio.listarTodos();
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
            List<ejbCcoCepServicioPrecio> filtrado = new ArrayList<>();
            for (ejbCcoCepServicioPrecio p : lstPrecios) {
                String tipoServicio = p.getTipoServicio() != null ? p.getTipoServicio().toLowerCase() : "";
                String tipoAlumno = p.getTipoAlumno() != null ? p.getTipoAlumno().toLowerCase() : "";
                if (tipoServicio.contains(q) || tipoAlumno.contains(q)) {
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
        clsPrecio = new ejbCcoCepServicioPrecio();
        clsPrecio.setActivo(true);
    }

    public void doEditar(ejbCcoCepServicioPrecio precio) {
        if (precio == null) return;
        strValor = VISTA_NUEVO;
        idEdicion = precio.getIdServicioPrecio();
        clsPrecio = srvPrecio.buscarPorId(precio.getIdServicioPrecio());
        if (clsPrecio == null) {
            clsPrecio = precio;
        }
    }

    public void doGuardar() {
        if (!validarPrecio()) return;

        try {
            if (idEdicion == null || idEdicion == 0) {
                // Nuevo - obtener siguiente ID
                Integer nuevoId = obtenerNuevoIdPrecio();
                clsPrecio.setIdServicioPrecio(nuevoId);
                clsPrecio.setFechaRegistro(new java.util.Date());
                srvPrecio.crear(clsPrecio);
                generalController.getFramework().doMensajeF("GUARDAR", "Precio agregado correctamente", 1);
            } else {
                // Actualizar
                srvPrecio.actualizar(clsPrecio);
                generalController.getFramework().doMensajeF("ACTUALIZAR", "Precio actualizado correctamente", 1);
            }

            cargarPrecios();
            lstPreciosView = new ArrayList<>(lstPrecios);
            strValor = VISTA_LISTA;

        } catch (Exception e) {
            e.printStackTrace();
            generalController.getFramework().doMensajeF("ERROR", "Error al guardar el precio: " + e.getMessage(), 3);
        }
    }

    public void doCambiarEstado(ejbCcoCepServicioPrecio precio) {
        if (precio == null) return;
        boolean nuevoEstado = !precio.getActivo();
        precio.setActivo(nuevoEstado);
        srvPrecio.actualizar(precio);
        String mensaje = nuevoEstado ? "Precio activado correctamente" : "Precio dado de baja correctamente";
        generalController.getFramework().doMensajeF(nuevoEstado ? "ACTIVAR" : "BAJA", mensaje, nuevoEstado ? 1 : 2);
        cargarPrecios();
        doBuscar();
    }

    public void doVolver() {
        strValor = VISTA_LISTA;
        strBusqueda = "";
        cargarPrecios();
        doBuscar();
    }

    private Integer obtenerNuevoIdPrecio() {
        int maxId = 0;
        if (lstPrecios != null && !lstPrecios.isEmpty()) {
            for (ejbCcoCepServicioPrecio p : lstPrecios) {
                Integer id = p.getIdServicioPrecio();
                if (id != null && id > maxId) maxId = id;
            }
        }
        return maxId + 1;
    }

    private boolean validarPrecio() {
        if (clsPrecio == null) clsPrecio = new ejbCcoCepServicioPrecio();

        if (clsPrecio.getTipoServicio() == null || clsPrecio.getTipoServicio().trim().isEmpty()) {
            generalController.getFramework().doMensajeF("VALIDACIÓN", "Ingrese el tipo de servicio", 2);
            return false;
        }
        if (clsPrecio.getTipoAlumno() == null || clsPrecio.getTipoAlumno().trim().isEmpty()) {
            generalController.getFramework().doMensajeF("VALIDACIÓN", "Ingrese el tipo de alumno (UNS/EXTERNO)", 2);
            return false;
        }
        if (clsPrecio.getMonto() == null || clsPrecio.getMonto().compareTo(BigDecimal.ZERO) <= 0) {
            generalController.getFramework().doMensajeF("VALIDACIÓN", "Ingrese un monto válido", 2);
            return false;
        }
        if (clsPrecio.getCodigoPago() == null || clsPrecio.getCodigoPago().trim().isEmpty()) {
            generalController.getFramework().doMensajeF("VALIDACIÓN", "Ingrese el código de pago", 2);
            return false;
        }
        return true;
    }
}