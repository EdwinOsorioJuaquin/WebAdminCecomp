package controladores;

import ejbCecomp.clases.ejbCcoPagoCecompDTO;
import ejbCecomp.ejb.negocio.ejbCcoVwCecompPagosServiceLocal;
import ejbCecomp.entidades.ejbCcoVwCecompPagos;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import lombok.Getter;
import lombok.Setter;
import static libreriaUdemsi.funciones.libreriaGeneral.doGenerarJNDI;

@Named("pagoCecompController")
@SessionScoped
@Getter
@Setter
public class PagoCecompController implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private GeneralController generalController;

    // ============================
    // ATRIBUTOS
    // ============================
    private List<ejbCcoPagoCecompDTO> lstPagosDTO;
    private List<ejbCcoPagoCecompDTO> lstPagosViewDTO;
    private String strBusqueda;
    private Date fechaInicio;
    private Date fechaFin;
    private Integer montoMinimo;
    private Integer montoMaximo;
    private String totalMontoFormateado;

    private ejbCcoVwCecompPagosServiceLocal srvPagos;

    public PagoCecompController() {
        try {
            Context context = new InitialContext();
            srvPagos = (ejbCcoVwCecompPagosServiceLocal) context.lookup(
                doGenerarJNDI("ejbCecomp", "1.0", "ejbCcoVwCecompPagosServiceLocal")
            );
        } catch (NamingException e) {
            System.out.println("Error PagoCecompController constructor: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void doIniciarPagina() {
        System.out.println("Llegó a doIniciarPagina");
        limpiarFiltros();
        cargarPagos();
    }

    private void limpiarFiltros() {
        strBusqueda = null;
        fechaInicio = null;
        fechaFin = null;
        montoMinimo = null;
        montoMaximo = null;
    }

    private void cargarPagos() {
        try {
            System.out.println("=== Controller cargarPagos() INICIO ===");
            List<ejbCcoVwCecompPagos> pagos = srvPagos.listarTodos();
            System.out.println("=== Controller - Pagos recibidos: " + (pagos != null ? pagos.size() : "null") + " ===");

            if (pagos != null && !pagos.isEmpty()) {
                // Imprimir el primer registro para depurar
                ejbCcoVwCecompPagos primerPago = pagos.get(0);
                System.out.println("=== PRIMER PAGO ===");
                System.out.println("  idNumPago: " + primerPago.getIdNumPago());
                System.out.println("  idVoucher: " + primerPago.getIdVoucher());
                System.out.println("  estudiante: " + primerPago.getEstudiante());
                System.out.println("  nombreConcepto: " + primerPago.getNombreConcepto());
                System.out.println("  monto: " + primerPago.getMonto());
                System.out.println("  fechaPago: " + primerPago.getFechaPago());
                System.out.println("  numDocumento: " + primerPago.getNumDocumento());
                System.out.println("  nombrePagador: " + primerPago.getNombrePagador());
                System.out.println("=================");
            }

            lstPagosDTO = new ArrayList<>();
            if (pagos != null && !pagos.isEmpty()) {
                for (ejbCcoVwCecompPagos pago : pagos) {
                    System.out.println("  Procesando pago ID: " + pago.getIdNumPago());
                    ejbCcoPagoCecompDTO dto = new ejbCcoPagoCecompDTO(pago);
                    System.out.println("    DTO creado - fechaFormateada: " + dto.getFechaFormateada());
                    System.out.println("    DTO creado - montoFormateado: " + dto.getMontoFormateado());
                    lstPagosDTO.add(dto);
                }
            } else {
                System.out.println("  No hay pagos para procesar");
            }

            lstPagosViewDTO = new ArrayList<>(lstPagosDTO);
            System.out.println("=== Controller - lstPagosViewDTO size: " + (lstPagosViewDTO != null ? lstPagosViewDTO.size() : "null") + " ===");
            calcularTotalMonto();
            System.out.println("=== Controller cargarPagos() FIN ===");

        } catch (Exception e) {
            System.out.println("=== ERROR en cargarPagos(): " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void calcularTotalMonto() {
        if (lstPagosViewDTO != null && !lstPagosViewDTO.isEmpty()) {
            Long total = lstPagosViewDTO.stream()
                .mapToLong(dto -> dto.getPago() != null && dto.getPago().getMonto() != null ? dto.getPago().getMonto() : 0)
                .sum();
            totalMontoFormateado = String.format("S/ %,.2f", total.doubleValue());
        } else {
            totalMontoFormateado = "S/ 0.00";
        }
    }

    public void doBuscar() {
        System.out.println("Llegó a doBuscar");
        
        try {
            List<ejbCcoVwCecompPagos> pagosFiltrados = srvPagos.buscarPorFiltros(
                strBusqueda, fechaInicio, fechaFin, montoMinimo, montoMaximo
            );
            
            lstPagosViewDTO = new ArrayList<>();
            if (pagosFiltrados != null) {
                for (ejbCcoVwCecompPagos pago : pagosFiltrados) {
                    ejbCcoPagoCecompDTO dto = new ejbCcoPagoCecompDTO(pago);
                    lstPagosViewDTO.add(dto);
                }
            }
            
            calcularTotalMonto();
            
            if (lstPagosViewDTO.isEmpty()) {
                generalController.getFramework().doMensajeF("BÚSQUEDA", "No se encontraron pagos con los filtros aplicados", 1);
            } else {
                generalController.getFramework().doMensajeF("BÚSQUEDA", "Filtro aplicado correctamente", 1);
            }
            
        } catch (Exception e) {
            System.out.println("Error al buscar: " + e.getMessage());
            generalController.getFramework().doMensajeF("ERROR", "Error al buscar pagos: " + e.getMessage(), 3);
        }
    }

    public void doLimpiarFiltros() {
        System.out.println("Llegó a doLimpiarFiltros");
        limpiarFiltros();
        lstPagosViewDTO = new ArrayList<>(lstPagosDTO);
        calcularTotalMonto();
        generalController.getFramework().doMensajeF("LIMPIAR", "Filtros limpiados correctamente", 1);
    }

    public void doVolver() {
        limpiarFiltros();
        cargarPagos();
    }
}