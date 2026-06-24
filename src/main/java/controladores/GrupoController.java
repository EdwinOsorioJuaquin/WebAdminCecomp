package controladores;

import ejbCecomp.clases.ejbCcoGrupoDTO;
import ejbCecomp.ejb.negocio.*;
import ejbCecomp.entidades.*;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import lombok.Getter;
import lombok.Setter;
import static libreriaUdemsi.funciones.libreriaGeneral.doGenerarJNDI;

@Named("grupoController")
@SessionScoped
@Getter
@Setter
public class GrupoController implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private GeneralController generalController;

    // Listados
    private List<ejbCcoGrupoDTO> lstGruposDTO;
    private List<ejbCcoGrupoDTO> lstGruposViewDTO;
    private String strBusqueda;
    private String strValor;
    private Integer idEdicion;

    // Entidades para el formulario
    private ejbCcoCepCursoDocente clsGrupoEdit;
    
    // IDs seleccionados
    private Integer idPersonalSeleccionado;
    private Integer idCursoSeleccionado;
    private Integer idTipoDesarrolloSeleccionado;
    private Integer idGrupoSeleccionado;
    
    // Precio
    private Double precioMonto;
    private String codigoPago;
    
    // Catálogos
    private List<ejbCcoCepPersonal> lstDocentes;
    private List<ejbCcoCepCurso> lstCursos;
    private List<ejbCcoCepCecTipoDesarrollo> lstTiposDesarrollo;
    private List<ejbCcoCepCecGrupoCurso> lstGruposCurso;

    private static final String VISTA_LISTA = "LISTA";
    private static final String VISTA_NUEVO = "NUEVO";
    private static final String VISTA_EDITAR = "EDITAR";

    // Servicios
    private ejbCcoCepCursoDocenteServiceLocal srvGrupo;
    private ejbCcoCepPersonalServiceLocal srvPersonal;
    private ejbCcoCepCursoServiceLocal srvCurso;
    private ejbCcoCepCecTipoDesarrolloServiceLocal srvTipoDesarrollo;
    private ejbCcoCepCecGrupoCursoServiceLocal srvGrupoCurso;
    private ejbCcoCepGrupoPrecioServiceLocal srvGrupoPrecio;

    public GrupoController() {
        try {
            Context context = new InitialContext();
            srvGrupo = (ejbCcoCepCursoDocenteServiceLocal) context.lookup(
                doGenerarJNDI("ejbCecomp", "1.0", "ejbCcoCepCursoDocenteServiceLocal")
            );
            srvPersonal = (ejbCcoCepPersonalServiceLocal) context.lookup(
                doGenerarJNDI("ejbCecomp", "1.0", "ejbCcoCepPersonalServiceLocal")
            );
            srvCurso = (ejbCcoCepCursoServiceLocal) context.lookup(
                doGenerarJNDI("ejbCecomp", "1.0", "ejbCcoCepCursoServiceLocal")
            );
            srvTipoDesarrollo = (ejbCcoCepCecTipoDesarrolloServiceLocal) context.lookup(
                doGenerarJNDI("ejbCecomp", "1.0", "ejbCcoCepCecTipoDesarrolloServiceLocal")
            );
            srvGrupoCurso = (ejbCcoCepCecGrupoCursoServiceLocal) context.lookup(
                doGenerarJNDI("ejbCecomp", "1.0", "ejbCcoCepCecGrupoCursoServiceLocal")
            );
            srvGrupoPrecio = (ejbCcoCepGrupoPrecioServiceLocal) context.lookup(
                doGenerarJNDI("ejbCecomp", "1.0", "ejbCcoCepGrupoPrecioServiceLocal")
            );
        } catch (NamingException e) {
            System.out.println("Error JNDI GrupoController: " + e.getMessage());
        }
    }

    public void doIniciarPagina() {
        strValor = VISTA_LISTA;
        strBusqueda = "";
        cargarGrupos();
        cargarCatalogos();
    }

    private void cargarGrupos() {
        try {
            List<ejbCcoCepCursoDocente> grupos = srvGrupo.listarConPrecios();
            lstGruposDTO = new ArrayList<>();
            if (grupos != null) {
                for (ejbCcoCepCursoDocente grupo : grupos) {
                    lstGruposDTO.add(new ejbCcoGrupoDTO(grupo));
                }
            }
            lstGruposViewDTO = new ArrayList<>(lstGruposDTO);
        } catch (Exception e) {
            System.out.println("Error cargar grupos: " + e.getMessage());
            lstGruposDTO = new ArrayList<>();
            lstGruposViewDTO = new ArrayList<>();
        }
    }

    private void cargarCatalogos() {
        try {
            lstDocentes = srvPersonal.listarActivos();
            lstCursos = srvCurso.listarActivos();
            lstTiposDesarrollo = srvTipoDesarrollo.listarTodos();
            lstGruposCurso = srvGrupoCurso.listarTodos();
        } catch (Exception e) {
            System.out.println("Error cargar catálogos: " + e.getMessage());
            lstDocentes = new ArrayList<>();
            lstCursos = new ArrayList<>();
            lstTiposDesarrollo = new ArrayList<>();
            lstGruposCurso = new ArrayList<>();
        }
    }

    public void doBuscar() {
        String q = (strBusqueda == null) ? "" : strBusqueda.trim().toLowerCase();
        
        if (q.isBlank()) {
            lstGruposViewDTO = new ArrayList<>(lstGruposDTO);
        } else {
            List<ejbCcoGrupoDTO> filtrado = new ArrayList<>();
            for (ejbCcoGrupoDTO dto : lstGruposDTO) {
                String docente = dto.getNombreDocente() != null ? dto.getNombreDocente().toLowerCase() : "";
                String curso = dto.getNombreCurso() != null ? dto.getNombreCurso().toLowerCase() : "";
                
                if (docente.contains(q) || curso.contains(q)) {
                    filtrado.add(dto);
                }
            }
            lstGruposViewDTO = filtrado;
        }
        generalController.getFramework().doMensajeF("BÚSQUEDA", "Filtro aplicado correctamente", 1);
    }

    public void doNuevo() {
        strValor = VISTA_NUEVO;
        limpiarFormulario();
        cargarCatalogos();
    }

    public void doEditar(ejbCcoGrupoDTO dto) {
        if (dto == null || dto.getIdAd() == null) {
            generalController.getFramework().doMensajeF("ERROR", "Grupo no válido", 3);
            return;
        }
        
        strValor = VISTA_EDITAR;
        idEdicion = dto.getIdAd();
        
        clsGrupoEdit = srvGrupo.buscarPorId(idEdicion);
        if (clsGrupoEdit != null) {
            idPersonalSeleccionado = clsGrupoEdit.getCepPersonal() != null ? clsGrupoEdit.getCepPersonal().getIdPersonal() : null;
            idCursoSeleccionado = clsGrupoEdit.getCepCurso() != null ? clsGrupoEdit.getCepCurso().getIdCurso() : null;
            idTipoDesarrolloSeleccionado = clsGrupoEdit.getCepCecTipoDesarrollo() != null ? clsGrupoEdit.getCepCecTipoDesarrollo().getIdCiclo() : null;
            idGrupoSeleccionado = clsGrupoEdit.getCepCecGrupoCurso() != null ? clsGrupoEdit.getCepCecGrupoCurso().getIdGrupo() : null;
            
            // Cargar el precio (tomar el primero de la lista)
            if (clsGrupoEdit.getCepGrupoPrecioList() != null && !clsGrupoEdit.getCepGrupoPrecioList().isEmpty()) {
                ejbCcoCepGrupoPrecio precio = clsGrupoEdit.getCepGrupoPrecioList().get(0);
                precioMonto = precio.getMonto() != null ? precio.getMonto().doubleValue() : null;
                codigoPago = precio.getCodigoPago();
            }
        }
        cargarCatalogos();
    }

    public void doGuardar() {
        if (!validarGrupo()) return;
        
        try {
            // Asignar relaciones
            if (idPersonalSeleccionado != null) {
                clsGrupoEdit.setCepPersonal(srvPersonal.buscarPorId(idPersonalSeleccionado));
            }
            if (idCursoSeleccionado != null) {
                clsGrupoEdit.setCepCurso(srvCurso.buscarPorId(idCursoSeleccionado));
            }
            if (idTipoDesarrolloSeleccionado != null) {
                clsGrupoEdit.setCepCecTipoDesarrollo(srvTipoDesarrollo.buscarPorId(idTipoDesarrolloSeleccionado));
            }
            if (idGrupoSeleccionado != null) {
                clsGrupoEdit.setCepCecGrupoCurso(srvGrupoCurso.buscarPorId(idGrupoSeleccionado));
            }
            
            clsGrupoEdit.setEstado(true);
            
            if (idEdicion == null) {
                // Crear grupo
                clsGrupoEdit = srvGrupo.crear(clsGrupoEdit);
                
                // Crear precio del grupo
                if (precioMonto != null && precioMonto > 0) {
                    ejbCcoCepGrupoPrecio precio = new ejbCcoCepGrupoPrecio();
                    precio.setCepCursoDocente(clsGrupoEdit);
                    precio.setMonto(BigDecimal.valueOf(precioMonto));
                    precio.setCodigoPago(codigoPago != null ? codigoPago : "GRUPO_" + clsGrupoEdit.getIdAd());
                    srvGrupoPrecio.crear(precio);
                }
                
                generalController.getFramework().doMensajeF("GUARDAR", "Grupo agregado correctamente", 1);
            } else {
                // Actualizar grupo
                clsGrupoEdit = srvGrupo.actualizar(clsGrupoEdit);
                
                // Actualizar precio existente
                if (clsGrupoEdit.getCepGrupoPrecioList() != null && !clsGrupoEdit.getCepGrupoPrecioList().isEmpty()) {
                    ejbCcoCepGrupoPrecio precioExistente = clsGrupoEdit.getCepGrupoPrecioList().get(0);
                    if (precioMonto != null && precioMonto > 0) {
                        precioExistente.setMonto(BigDecimal.valueOf(precioMonto));
                        precioExistente.setCodigoPago(codigoPago != null ? codigoPago : "GRUPO_" + clsGrupoEdit.getIdAd());
                        srvGrupoPrecio.actualizar(precioExistente);
                    }
                } else if (precioMonto != null && precioMonto > 0) {
                    // Si no tiene precio, crear uno nuevo
                    ejbCcoCepGrupoPrecio precio = new ejbCcoCepGrupoPrecio();
                    precio.setCepCursoDocente(clsGrupoEdit);
                    precio.setMonto(BigDecimal.valueOf(precioMonto));
                    precio.setCodigoPago(codigoPago != null ? codigoPago : "GRUPO_" + clsGrupoEdit.getIdAd());
                    srvGrupoPrecio.crear(precio);
                }
                
                generalController.getFramework().doMensajeF("ACTUALIZAR", "Grupo actualizado correctamente", 1);
            }
            
            cargarGrupos();
            strValor = VISTA_LISTA;
            
        } catch (Exception e) {
            e.printStackTrace();
            generalController.getFramework().doMensajeF("ERROR", "Error al guardar grupo: " + e.getMessage(), 3);
        }
    }

    public void doCambiarEstado(ejbCcoGrupoDTO dto) {
        if (dto == null || dto.getIdAd() == null) {
            generalController.getFramework().doMensajeF("ERROR", "Grupo no válido", 3);
            return;
        }
        
        try {
            ejbCcoCepCursoDocente grupo = srvGrupo.buscarPorId(dto.getIdAd());
            if (grupo == null) {
                generalController.getFramework().doMensajeF("ERROR", "No se encontró el grupo", 3);
                return;
            }
            
            boolean nuevoEstado = !grupo.getEstado();
            grupo.setEstado(nuevoEstado);
            srvGrupo.actualizar(grupo);
            
            String mensaje = nuevoEstado ? "Grupo activado correctamente" : "Grupo desactivado correctamente";
            generalController.getFramework().doMensajeF("ESTADO", mensaje, 1);
            
            cargarGrupos();
            doBuscar();
        } catch (Exception e) {
            System.out.println("Error cambiar estado: " + e.getMessage());
            generalController.getFramework().doMensajeF("ERROR", "Error al cambiar estado", 3);
        }
    }

    public void doVolver() {
        strValor = VISTA_LISTA;
        strBusqueda = "";
        cargarGrupos();
    }

    private void limpiarFormulario() {
        clsGrupoEdit = new ejbCcoCepCursoDocente();
        clsGrupoEdit.setEstado(true);
        clsGrupoEdit.setCerraAper(false);
        clsGrupoEdit.setFecha(new Date());
        
        idEdicion = null;
        idPersonalSeleccionado = null;
        idCursoSeleccionado = null;
        idTipoDesarrolloSeleccionado = null;
        idGrupoSeleccionado = null;
        
        precioMonto = null;
        codigoPago = null;
    }

    private boolean validarGrupo() {
        if (idPersonalSeleccionado == null) {
            generalController.getFramework().doMensajeF("VALIDACIÓN", "Debe seleccionar un docente", 2);
            return false;
        }
        if (idCursoSeleccionado == null) {
            generalController.getFramework().doMensajeF("VALIDACIÓN", "Debe seleccionar un curso", 2);
            return false;
        }
        if (clsGrupoEdit.getFecha() == null) {
            generalController.getFramework().doMensajeF("VALIDACIÓN", "Debe ingresar una fecha de inicio", 2);
            return false;
        }
        if (precioMonto == null || precioMonto <= 0) {
            generalController.getFramework().doMensajeF("VALIDACIÓN", "Debe ingresar un monto válido", 2);
            return false;
        }
        return true;
    }
}