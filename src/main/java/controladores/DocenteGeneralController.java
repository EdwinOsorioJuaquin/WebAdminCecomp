package controladores;

import ejbCecomp.clases.ejbCcoDocenteGeneralDTO;
import ejbCecomp.ejb.negocio.ejbCcoEscPersonalServiceLocal;
import ejbCecomp.entidades.ejbCcoEscPersonal;
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

@Named("docenteGeneralController")
@SessionScoped
@Getter
@Setter
public class DocenteGeneralController implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private GeneralController generalController;

    private List<ejbCcoDocenteGeneralDTO> lstDocentesDTO;
    private List<ejbCcoDocenteGeneralDTO> lstDocentesViewDTO;
    private String strBusqueda;

    private ejbCcoEscPersonalServiceLocal srvDocente;

    public DocenteGeneralController() {
        try {
            Context context = new InitialContext();
            srvDocente = (ejbCcoEscPersonalServiceLocal) context.lookup(
                doGenerarJNDI("ejbCecomp", "1.0", "ejbCcoEscPersonalServiceLocal")
            );
        } catch (NamingException e) {
            System.out.println("Error JNDI: " + e.getMessage());
        }
    }

    public void doIniciarPagina() {
        strBusqueda = "";
        cargarTodosLosDocentes();
    }

    private void cargarTodosLosDocentes() {
        lstDocentesDTO = new ArrayList<>();
        
        try {
            List<ejbCcoEscPersonal> docentes = srvDocente.listarActivos();
            if (docentes != null) {
                for (ejbCcoEscPersonal docente : docentes) {
                    lstDocentesDTO.add(new ejbCcoDocenteGeneralDTO(docente));
                }
            }
        } catch (Exception e) {
            System.out.println("Error cargar docentes: " + e.getMessage());
        }
        
        lstDocentesViewDTO = new ArrayList<>(lstDocentesDTO);
    }

    public void doBuscar() {
        String q = (strBusqueda == null) ? "" : strBusqueda.trim().toLowerCase();
        
        if (q.isBlank()) {
            lstDocentesViewDTO = new ArrayList<>(lstDocentesDTO);
        } else {
            List<ejbCcoDocenteGeneralDTO> filtrado = new ArrayList<>();
            for (ejbCcoDocenteGeneralDTO dto : lstDocentesDTO) {
                String nombre = dto.getNombreCompleto() != null ? dto.getNombreCompleto().toLowerCase() : "";
                String dni = dto.getDni() != null ? dto.getDni().toLowerCase() : "";
                String especialidad = dto.getEspecialidad() != null ? dto.getEspecialidad().toLowerCase() : "";
                
                if (nombre.contains(q) || dni.contains(q) || especialidad.contains(q)) {
                    filtrado.add(dto);
                }
            }
            lstDocentesViewDTO = filtrado;
        }
        generalController.getFramework().doMensajeF("BÚSQUEDA", "Filtro aplicado correctamente", 1);
    }

    public void doVolver() {
        strBusqueda = "";
        cargarTodosLosDocentes();
    }
}