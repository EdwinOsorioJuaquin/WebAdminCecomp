package controladores;

import ejbCecomp.clases.ejbCcoDocenteDTO;
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

@Named("docenteCecompController")
@SessionScoped
@Getter
@Setter
public class DocenteCecompController implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private GeneralController generalController;

    // Listados
    private List<ejbCcoDocenteDTO> lstDocentesDTO;
    private List<ejbCcoDocenteDTO> lstDocentesViewDTO;
    private String strBusqueda;
    private String strValor;
    private Integer idEdicion;

    // Control de flujo
    private String strBusquedaPersona;
    private List<ejbCcoDrtPersonanatural> lstPersonasEncontradas;
    private ejbCcoDrtPersonanatural personaSeleccionada;
    private ejbCcoEscPersonal escPersonalSeleccionado;
    
    // Entidades para el formulario
    private ejbCcoDrtPersonanatural clsPersonaEdit;
    private ejbCcoEscPersonal clsEscPersonalEdit;
    private ejbCcoCepPersonal clsCepPersonalEdit;
    
    // Catálogos
    private List<ejbCcoCepTipoPersonal> lstTiposCecomp;
    private Short idTipoCecompSeleccionado;
    
    // Servicios
    private ejbCcoDrtPersonanaturalServiceLocal srvPersona;
    private ejbCcoEscPersonalServiceLocal srvEscPersonal;
    private ejbCcoCepPersonalServiceLocal srvCepPersonal;
    private ejbCcoCepTipoPersonalServiceLocal srvTipoCecomp;

    private static final String VISTA_LISTA = "LISTA";
    private static final String VISTA_BUSQUEDA_EXISTENTE = "BUSQUEDA_EXISTENTE";
    private static final String VISTA_FORMULARIO_PERSONA = "FORMULARIO_PERSONA";
    private static final String VISTA_FORMULARIO_CECOMP = "FORMULARIO_CECOMP";

    public DocenteCecompController() {
        try {
            Context context = new InitialContext();
            srvPersona = (ejbCcoDrtPersonanaturalServiceLocal) context.lookup(
                doGenerarJNDI("ejbCecomp", "1.0", "ejbCcoDrtPersonanaturalServiceLocal")
            );
            srvEscPersonal = (ejbCcoEscPersonalServiceLocal) context.lookup(
                doGenerarJNDI("ejbCecomp", "1.0", "ejbCcoEscPersonalServiceLocal")
            );
            srvCepPersonal = (ejbCcoCepPersonalServiceLocal) context.lookup(
                doGenerarJNDI("ejbCecomp", "1.0", "ejbCcoCepPersonalServiceLocal")
            );
            srvTipoCecomp = (ejbCcoCepTipoPersonalServiceLocal) context.lookup(
                doGenerarJNDI("ejbCecomp", "1.0", "ejbCcoCepTipoPersonalServiceLocal")
            );
        } catch (NamingException e) {
            System.out.println("Error JNDI DocenteCecompController: " + e.getMessage());
        }
    }

    public void doIniciarPagina() {
        strValor = VISTA_LISTA;
        strBusqueda = "";
        cargarDocentes();
        cargarTiposCecomp();
    }

    private void cargarDocentes() {
        try {
            List<ejbCcoCepPersonal> docentes = srvCepPersonal.listarTodos();
            lstDocentesDTO = new ArrayList<>();
            if (docentes != null) {
                for (ejbCcoCepPersonal docente : docentes) {
                    lstDocentesDTO.add(new ejbCcoDocenteDTO(docente));
                }
            }
            lstDocentesViewDTO = new ArrayList<>(lstDocentesDTO);
        } catch (Exception e) {
            System.out.println("Error cargar docentes: " + e.getMessage());
            lstDocentesDTO = new ArrayList<>();
            lstDocentesViewDTO = new ArrayList<>();
        }
    }

    private void cargarTiposCecomp() {
        try {
            lstTiposCecomp = srvTipoCecomp.listarActivos();
            if (lstTiposCecomp == null) {
                lstTiposCecomp = new ArrayList<>();
            }
        } catch (Exception e) {
            System.out.println("Error cargar tipos: " + e.getMessage());
            lstTiposCecomp = new ArrayList<>();
        }
    }

    public void doBuscar() {
        String q = (strBusqueda == null) ? "" : strBusqueda.trim().toLowerCase();
        
        if (q.isBlank()) {
            lstDocentesViewDTO = new ArrayList<>(lstDocentesDTO);
        } else {
            List<ejbCcoDocenteDTO> filtrado = new ArrayList<>();
            for (ejbCcoDocenteDTO dto : lstDocentesDTO) {
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

    public void doSeleccionarNuevo() {
        // Crear nuevas instancias
        clsPersonaEdit = new ejbCcoDrtPersonanatural();
        clsEscPersonalEdit = new ejbCcoEscPersonal();
        clsCepPersonalEdit = new ejbCcoCepPersonal();
        
        // Valores por defecto para persona
        clsPersonaEdit.setEstadoPernat('A');
        clsPersonaEdit.setFechaIng(new Date());
        clsPersonaEdit.setUpdateSelf(1);
        clsPersonaEdit.setIdUbgNac(0);
        clsPersonaEdit.setIdUbgPro(0);
        clsPersonaEdit.setIdColegio(0);
        clsPersonaEdit.setAnioEgresoCole(0);
        
        // Valores por defecto para esc_personal
        clsEscPersonalEdit.setCondicion(1);
        clsEscPersonalEdit.setFechaIng(new Date());
        clsEscPersonalEdit.setIdEsc(null);
        clsEscPersonalEdit.setDrtPersonanatural(clsPersonaEdit);  // 🔥 Asignar persona
        
        // Valores por defecto para cep_personal
        clsCepPersonalEdit.setBandera(true);
        clsCepPersonalEdit.setFechaIng(new Date());
        clsCepPersonalEdit.setIdPersonal(null);
        clsCepPersonalEdit.setEscPersonal(null);
        clsCepPersonalEdit.setCepTipoPersonal(null);
        
        idEdicion = null;
        idTipoCecompSeleccionado = null;
        
        strValor = VISTA_FORMULARIO_PERSONA;
    }

    public void doSeleccionarExistente() {
        strValor = VISTA_BUSQUEDA_EXISTENTE;
        strBusquedaPersona = "";
        cargarTodosLosTrabajadoresUniversitarios();
    }

    private void cargarTodosLosTrabajadoresUniversitarios() {
        try {
            List<ejbCcoEscPersonal> listaEscPersonal = srvEscPersonal.listarNoDocentesCecomp();
            lstPersonasEncontradas = new ArrayList<>();
            
            if (listaEscPersonal != null) {
                for (ejbCcoEscPersonal escPersonal : listaEscPersonal) {
                    if (escPersonal.getDrtPersonanatural()!= null) {
                        lstPersonasEncontradas.add(escPersonal.getDrtPersonanatural());
                    }
                }
            }
            
        } catch (Exception e) {
            System.out.println("Error cargar trabajadores: " + e.getMessage());
            e.printStackTrace();
            lstPersonasEncontradas = new ArrayList<>();
        }
    }

    public void doBuscarPersonaExistente() {
        String q = (strBusquedaPersona == null) ? "" : strBusquedaPersona.trim().toLowerCase();
        
        if (q.isBlank()) {
            cargarTodosLosTrabajadoresUniversitarios();
            return;
        }
        
        try {
            List<ejbCcoEscPersonal> listaEscPersonal = srvEscPersonal.listarNoDocentesCecomp();
            lstPersonasEncontradas = new ArrayList<>();
            
            for (ejbCcoEscPersonal escPersonal : listaEscPersonal) {
                if (escPersonal.getDrtPersonanatural()!= null) {
                    ejbCcoDrtPersonanatural persona = escPersonal.getDrtPersonanatural();
                    String nombre = persona.getNombreCompleto() != null ? persona.getNombreCompleto().toLowerCase() : "";
                    String dni = persona.getNumeroPndid() != null ? persona.getNumeroPndid().toLowerCase() : "";
                    
                    if (nombre.contains(q) || dni.contains(q)) {
                        lstPersonasEncontradas.add(persona);
                    }
                }
            }
            
            if (lstPersonasEncontradas.isEmpty()) {
                generalController.getFramework().doMensajeF("BÚSQUEDA", "No se encontraron trabajadores universitarios", 2);
            }
        } catch (Exception e) {
            System.out.println("Error buscar persona: " + e.getMessage());
            e.printStackTrace();
            generalController.getFramework().doMensajeF("ERROR", "Error al buscar persona", 3);
        }
    }

    public void doSeleccionarPersonaExistente(ejbCcoDrtPersonanatural persona) {
        personaSeleccionada = persona;
        clsPersonaEdit = persona;
        
        escPersonalSeleccionado = srvEscPersonal.buscarPorIdDir(persona.getIdDir());
        
        if (escPersonalSeleccionado != null) {
            clsEscPersonalEdit = escPersonalSeleccionado;
        } else {
            clsEscPersonalEdit = new ejbCcoEscPersonal();
            clsEscPersonalEdit.setDrtPersonanatural(clsPersonaEdit);  // 🔥 Asignar persona
            clsEscPersonalEdit.setIdEsc(null);
            clsEscPersonalEdit.setCondicion(1);
            clsEscPersonalEdit.setFechaIng(new Date());
        }
        
        // Inicializar cep_personal
        clsCepPersonalEdit = new ejbCcoCepPersonal();
        clsCepPersonalEdit.setBandera(true);
        clsCepPersonalEdit.setFechaIng(new Date());
        clsCepPersonalEdit.setIdPersonal(null);
        clsCepPersonalEdit.setEscPersonal(null);
        clsCepPersonalEdit.setCepTipoPersonal(null);
        idTipoCecompSeleccionado = null;
        
        strValor = VISTA_FORMULARIO_CECOMP;
    }

    public void doGuardarPersona() {
        if (!validarPersona()) return;
        
        try {
            String nombreCompleto = (
                (clsPersonaEdit.getApPaterno() != null ? clsPersonaEdit.getApPaterno() : "") + " " +
                (clsPersonaEdit.getApMaterno() != null ? clsPersonaEdit.getApMaterno() : "") + " " +
                (clsPersonaEdit.getNombre() != null ? clsPersonaEdit.getNombre() : "")
            ).trim();
            clsPersonaEdit.setNombreCompleto(nombreCompleto);
            clsPersonaEdit.setEstadoPernat('A');
            clsPersonaEdit.setFechaIng(new Date());
            clsPersonaEdit.setUpdateSelf(1);
            clsPersonaEdit.setIdUbgNac(0);
            clsPersonaEdit.setIdUbgPro(0);
            clsPersonaEdit.setIdColegio(0);
            clsPersonaEdit.setAnioEgresoCole(0);
            
            clsPersonaEdit = srvPersona.crear(clsPersonaEdit);
            
            strValor = VISTA_FORMULARIO_CECOMP;
            
        } catch (Exception e) {
            e.printStackTrace();
            generalController.getFramework().doMensajeF("ERROR", "Error al guardar persona: " + e.getMessage(), 3);
        }
    }

    public void doGuardarDocenteCecomp() {
        if (!validarDocenteCecomp()) return;
        
        try {
            // 1. Guardar o actualizar esc_personal
            if (clsEscPersonalEdit.getIdEsc() == null) {
                // Asegurar que la persona esté asignada
                clsEscPersonalEdit.setDrtPersonanatural(clsPersonaEdit);
                clsEscPersonalEdit.setCondicion(1);
                clsEscPersonalEdit = srvEscPersonal.crear(clsEscPersonalEdit);
            } else {
                clsEscPersonalEdit = srvEscPersonal.actualizar(clsEscPersonalEdit);
            }
            
            // 2. Obtener el objeto ejbCcoCepTipoPersonal desde el ID seleccionado
            if (idTipoCecompSeleccionado != null) {
                ejbCcoCepTipoPersonal tipoSeleccionado = srvTipoCecomp.buscarPorId(idTipoCecompSeleccionado);
                clsCepPersonalEdit.setCepTipoPersonal(tipoSeleccionado);
            }
            
            // 3. Guardar cep_personal
            if (clsCepPersonalEdit.getIdPersonal() == null) {
                // Asegurar que esc_personal esté asignado
                clsCepPersonalEdit.setEscPersonal(clsEscPersonalEdit);
                clsCepPersonalEdit.setBandera(true);
                clsCepPersonalEdit = srvCepPersonal.crear(clsCepPersonalEdit);
            } else {
                clsCepPersonalEdit = srvCepPersonal.actualizar(clsCepPersonalEdit);
            }
            
            generalController.getFramework().doMensajeF("GUARDAR", "Docente CEcomp registrado correctamente", 1);
            
            cargarDocentes();
            strValor = VISTA_LISTA;
            
        } catch (Exception e) {
            e.printStackTrace();
            generalController.getFramework().doMensajeF("ERROR", "Error al guardar docente: " + e.getMessage(), 3);
        }
    }

    public void doEditar(ejbCcoDocenteDTO dto) {
        if (dto == null || dto.getIdPersonal() == null) {
            generalController.getFramework().doMensajeF("ERROR", "Docente no válido", 3);
            return;
        }
        
        strValor = VISTA_FORMULARIO_CECOMP;
        idEdicion = dto.getIdPersonal();
        
        clsCepPersonalEdit = srvCepPersonal.buscarPorId(idEdicion);
        if (clsCepPersonalEdit != null) {
            clsEscPersonalEdit = clsCepPersonalEdit.getEscPersonal();
            if (clsEscPersonalEdit != null && clsEscPersonalEdit.getDrtPersonanatural()!= null) {
                clsPersonaEdit = clsEscPersonalEdit.getDrtPersonanatural();
            }
            if (clsCepPersonalEdit.getCepTipoPersonal()!= null) {
                idTipoCecompSeleccionado = clsCepPersonalEdit.getCepTipoPersonal().getIdTipoCecomp();
            }
        }
    }

    public void doCambiarEstado(ejbCcoDocenteDTO dto) {
        if (dto == null || dto.getIdPersonal() == null) {
            generalController.getFramework().doMensajeF("ERROR", "Docente no válido", 3);
            return;
        }
        
        try {
            ejbCcoCepPersonal cepPersonal = srvCepPersonal.buscarPorId(dto.getIdPersonal());
            if (cepPersonal == null) {
                generalController.getFramework().doMensajeF("ERROR", "No se encontró el docente", 3);
                return;
            }
            
            boolean nuevoEstado = !cepPersonal.getBandera();
            cepPersonal.setBandera(nuevoEstado);
            srvCepPersonal.actualizar(cepPersonal);
            
            String mensaje = nuevoEstado ? "Docente activado correctamente" : "Docente desactivado correctamente";
            int tipoMsg = nuevoEstado ? 1 : 2;
            generalController.getFramework().doMensajeF(nuevoEstado ? "ACTIVAR" : "BAJA", mensaje, tipoMsg);
            
            cargarDocentes();
            doBuscar();
        } catch (Exception e) {
            System.out.println("Error cambiar estado: " + e.getMessage());
            generalController.getFramework().doMensajeF("ERROR", "Error al cambiar estado", 3);
        }
    }

    public void doVolver() {
        strValor = VISTA_LISTA;
        strBusqueda = "";
        cargarDocentes();
    }

    public void doVolverPersona() {
        strValor = VISTA_FORMULARIO_PERSONA;
    }

    private boolean validarPersona() {
        if (clsPersonaEdit.getNumeroPndid() == null || clsPersonaEdit.getNumeroPndid().trim().isEmpty()) {
            generalController.getFramework().doMensajeF("VALIDACIÓN", "DNI requerido", 2);
            return false;
        }
        if (clsPersonaEdit.getNombre() == null || clsPersonaEdit.getNombre().trim().isEmpty()) {
            generalController.getFramework().doMensajeF("VALIDACIÓN", "Nombres requerido", 2);
            return false;
        }
        if (clsPersonaEdit.getApPaterno() == null || clsPersonaEdit.getApPaterno().trim().isEmpty()) {
            generalController.getFramework().doMensajeF("VALIDACIÓN", "Apellido Paterno requerido", 2);
            return false;
        }
        if (clsPersonaEdit.getApMaterno() == null || clsPersonaEdit.getApMaterno().trim().isEmpty()) {
            generalController.getFramework().doMensajeF("VALIDACIÓN", "Apellido Materno requerido", 2);
            return false;
        }
        if (clsPersonaEdit.getDireccion() == null || clsPersonaEdit.getDireccion().trim().isEmpty()) {
            generalController.getFramework().doMensajeF("VALIDACIÓN", "Dirección requerida", 2);
            return false;
        }
        if (clsPersonaEdit.getCelularPrin() == null || clsPersonaEdit.getCelularPrin().trim().isEmpty()) {
            generalController.getFramework().doMensajeF("VALIDACIÓN", "Celular requerido", 2);
            return false;
        }
        if (clsPersonaEdit.getEmailPrin() == null || clsPersonaEdit.getEmailPrin().trim().isEmpty()) {
            generalController.getFramework().doMensajeF("VALIDACIÓN", "Email requerido", 2);
            return false;
        }
        return true;
    }

    private boolean validarDocenteCecomp() {
        if (clsCepPersonalEdit.getFechaIng() == null) {
            generalController.getFramework().doMensajeF("VALIDACIÓN", "Fecha ingreso CEcomp requerida", 2);
            return false;
        }
        if (idTipoCecompSeleccionado == null) {
            generalController.getFramework().doMensajeF("VALIDACIÓN", "Tipo de personal CEcomp requerido", 2);
            return false;
        }
        return true;
    }
}