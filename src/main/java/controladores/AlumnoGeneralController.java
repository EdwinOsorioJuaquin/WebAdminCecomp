package controladores;

import ejbCecomp.clases.ejbCcoAlumnoGeneralDTO;
import ejbCecomp.ejb.negocio.*;
import ejbCecomp.entidades.*;
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

@Named("alumnoGeneralController")
@SessionScoped
@Getter
@Setter
public class AlumnoGeneralController implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private GeneralController generalController;

    private List<ejbCcoAlumnoGeneralDTO> lstAlumnosDTO;
    private List<ejbCcoAlumnoGeneralDTO> lstAlumnosViewDTO;
    private String strBusqueda;

    private ejbCcoCcoAlumnoExternoServiceLocal srvAlumnoExterno;
    private ejbCcoFxaEstudianteServiceLocal srvEstudiante;

    public AlumnoGeneralController() {
        try {
            Context context = new InitialContext();
            srvAlumnoExterno = (ejbCcoCcoAlumnoExternoServiceLocal) context.lookup(
                doGenerarJNDI("ejbCecomp", "1.0", "ejbCcoCcoAlumnoExternoServiceLocal")
            );
            srvEstudiante = (ejbCcoFxaEstudianteServiceLocal) context.lookup(
                doGenerarJNDI("ejbCecomp", "1.0", "ejbCcoFxaEstudianteServiceLocal")
            );
        } catch (NamingException e) {
            System.out.println("Error JNDI: " + e.getMessage());
        }
    }

    public void doIniciarPagina() {
        strBusqueda = "";
        cargarTodosLosAlumnos();
    }

    private void cargarTodosLosAlumnos() {
        lstAlumnosDTO = new ArrayList<>();
        
        // Cargar alumnos externos
        try {
            List<ejbCcoCcoAlumnoExterno> externos = srvAlumnoExterno.listarAlumnosExternos();
            if (externos != null) {
                for (ejbCcoCcoAlumnoExterno externo : externos) {
                    lstAlumnosDTO.add(new ejbCcoAlumnoGeneralDTO(externo));
                }
            }
        } catch (Exception e) {
            System.out.println("Error cargar externos: " + e.getMessage());
        }
        
        // Cargar alumnos universidad
        try {
            List<ejbCcoFxaEstudiante> universidad = srvEstudiante.listarTodosActivos();
            if (universidad != null) {
                for (ejbCcoFxaEstudiante estudiante : universidad) {
                    lstAlumnosDTO.add(new ejbCcoAlumnoGeneralDTO(estudiante));
                }
            }
        } catch (Exception e) {
            System.out.println("Error cargar universidad: " + e.getMessage());
        }
        
        lstAlumnosViewDTO = new ArrayList<>(lstAlumnosDTO);
    }

    public void doBuscar() {
        String q = (strBusqueda == null) ? "" : strBusqueda.trim().toLowerCase();
        
        if (q.isBlank()) {
            lstAlumnosViewDTO = new ArrayList<>(lstAlumnosDTO);
        } else {
            List<ejbCcoAlumnoGeneralDTO> filtrado = new ArrayList<>();
            for (ejbCcoAlumnoGeneralDTO dto : lstAlumnosDTO) {
                String nombre = dto.getNombreCompleto() != null ? dto.getNombreCompleto().toLowerCase() : "";
                String dni = dto.getDni() != null ? dto.getDni().toLowerCase() : "";
                
                if (nombre.contains(q) || dni.contains(q)) {
                    filtrado.add(dto);
                }
            }
            lstAlumnosViewDTO = filtrado;
        }
        generalController.getFramework().doMensajeF("BÚSQUEDA", "Filtro aplicado correctamente", 1);
    }
    
    public void doVolver() {
        strBusqueda = "";
        cargarTodosLosAlumnos();
    }
}