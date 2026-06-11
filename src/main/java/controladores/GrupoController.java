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
    private Integer idTipoDesarrolloSeleccionado;  // CAMBIADO: antes idCicloSeleccionado
    private Integer idNivelSeleccionado;
    private Integer idGrupoSeleccionado;
    
    // Precios
    private Double precioUNS;
    private Double precioExterno;
    private String codigoPagoUNS;
    private String codigoPagoExterno;
    
    // Catálogos
    private List<ejbCcoCepPersonal> lstDocentes;
    private List<ejbCcoCepCurso> lstCursos;
    private List<ejbCcoCepCecTipoDesarrollo> lstTiposDesarrollo;  // CAMBIADO: antes lstCiclos
//    private List<ejbCcoCepCecNivel> lstNiveles;
    private List<ejbCcoCepCecGrupoCurso> lstGruposCurso;

    private static final String VISTA_LISTA = "LISTA";
    private static final String VISTA_NUEVO = "NUEVO";
    private static final String VISTA_EDITAR = "EDITAR";

    // Servicios
    private ejbCcoCepCursoDocenteServiceLocal srvGrupo;
    private ejbCcoCepPersonalServiceLocal srvPersonal;
    private ejbCcoCepCursoServiceLocal srvCurso;
    private ejbCcoCepCecTipoDesarrolloServiceLocal srvTipoDesarrollo;  // CAMBIADO
//    private ejbCcoCepCecNivelServiceLocal srvNivel;
    private ejbCcoCepCecGrupoCursoServiceLocal srvGrupoCurso;
    private ejbCcoCepGrupoPrecioServiceLocal srvGrupoPrecio;  // NUEVO

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
//            srvNivel = (ejbCcoCepCecNivelServiceLocal) context.lookup(
//                doGenerarJNDI("ejbCecomp", "1.0", "ejbCcoCepCecNivelServiceLocal")
//            );
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
            List<ejbCcoCepCursoDocente> grupos = srvGrupo.listarTodos();
            lstGruposDTO = new ArrayList<>();
            if (grupos != null) {
                for (ejbCcoCepCursoDocente grupo : grupos) {
                    ejbCcoGrupoDTO dto = new ejbCcoGrupoDTO(grupo);
                    
                    // Cargar precios del grupo
                    if (grupo.getCepGrupoPrecioList() != null) {
                        for (ejbCcoCepGrupoPrecio precio : grupo.getCepGrupoPrecioList()) {
                            if (precio.getActivo() && "UNS".equals(precio.getTipoAlumno())) {
                                dto.setPrecioUNS(precio.getMonto());
                                dto.setCodigoPagoUNS(precio.getCodigoPago());
                            } else if (precio.getActivo() && "EXTERNO".equals(precio.getTipoAlumno())) {
                                dto.setPrecioExterno(precio.getMonto());
                                dto.setCodigoPagoExterno(precio.getCodigoPago());
                            }
                        }
                    }
                    
                    lstGruposDTO.add(dto);
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
//            lstNiveles = srvNivel.listarTodos();
            lstGruposCurso = srvGrupoCurso.listarTodos();
        } catch (Exception e) {
            System.out.println("Error cargar catálogos: " + e.getMessage());
            lstDocentes = new ArrayList<>();
            lstCursos = new ArrayList<>();
            lstTiposDesarrollo = new ArrayList<>();
//            lstNiveles = new ArrayList<>();
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
            idCursoSeleccionado = clsGrupoEdit.getCcoCepCurso()!= null ? clsGrupoEdit.getCcoCepCurso().getIdCurso() : null;
            idTipoDesarrolloSeleccionado = clsGrupoEdit.getCepCecTipoDesarrollo()!= null ? clsGrupoEdit.getCepCecTipoDesarrollo().getIdCiclo() : null;
//            idNivelSeleccionado = clsGrupoEdit.getCepCecNivel() != null ? clsGrupoEdit.getCepCecNivel().getIdNivel() : null;
            idGrupoSeleccionado = clsGrupoEdit.getCepCecGrupoCurso() != null ? clsGrupoEdit.getCepCecGrupoCurso().getIdGrupo() : null;
            
            // Cargar precios
            if (clsGrupoEdit.getCepGrupoPrecioList() != null) {
                for (ejbCcoCepGrupoPrecio precio : clsGrupoEdit.getCepGrupoPrecioList()) {
                    if (precio.getActivo() && "UNS".equals(precio.getTipoAlumno())) {
                        precioUNS = precio.getMonto() != null ? precio.getMonto().doubleValue() : null;
                        codigoPagoUNS = precio.getCodigoPago();
                    } else if (precio.getActivo() && "EXTERNO".equals(precio.getTipoAlumno())) {
                        precioExterno = precio.getMonto() != null ? precio.getMonto().doubleValue() : null;
                        codigoPagoExterno = precio.getCodigoPago();
                    }
                }
            }
        }
    }

    public void doGuardar() {
        if (!validarGrupo()) return;
        
        try {
            // Asignar relaciones
            if (idPersonalSeleccionado != null) {
                clsGrupoEdit.setCepPersonal(srvPersonal.buscarPorId(idPersonalSeleccionado));
            }
            if (idCursoSeleccionado != null) {
                clsGrupoEdit.setCcoCepCurso(srvCurso.buscarPorId(idCursoSeleccionado));
            }
            if (idTipoDesarrolloSeleccionado != null) {
                clsGrupoEdit.setCepCecTipoDesarrollo(srvTipoDesarrollo.buscarPorId(idTipoDesarrolloSeleccionado));
            }
            if (idNivelSeleccionado != null) {
//                clsGrupoEdit.setCepCecNivel(srvNivel.buscarPorId(idNivelSeleccionado));
            }
            if (idGrupoSeleccionado != null) {
                clsGrupoEdit.setCepCecGrupoCurso(srvGrupoCurso.buscarPorId(idGrupoSeleccionado));
            }
            
            clsGrupoEdit.setEstado(true);
            
            if (idEdicion == null) {
                // Crear grupo
                clsGrupoEdit = srvGrupo.crear(clsGrupoEdit);
                
                // Crear precios
                if (precioUNS != null && precioUNS > 0) {
                    ejbCcoCepGrupoPrecio precio = new ejbCcoCepGrupoPrecio();
                    precio.setCepCursoDocente(clsGrupoEdit); // CORREGIDO
                    precio.setTipoAlumno("UNS");
                    precio.setMonto(BigDecimal.valueOf(precioUNS));
                    precio.setCodigoPago(codigoPagoUNS != null ? codigoPagoUNS : "CURSO_UNS_" + clsGrupoEdit.getIdAd());
                    precio.setActivo(true);
                    precio.setFechaRegistro(new Date());
                    srvGrupoPrecio.crear(precio);
                }
                
                if (precioExterno != null && precioExterno > 0) {
                    ejbCcoCepGrupoPrecio precio = new ejbCcoCepGrupoPrecio();
                    precio.setCepCursoDocente(clsGrupoEdit); // CORREGIDO
                    precio.setTipoAlumno("EXTERNO");
                    precio.setMonto(BigDecimal.valueOf(precioExterno));
                    precio.setCodigoPago(codigoPagoExterno != null ? codigoPagoExterno : "CURSO_EXT_" + clsGrupoEdit.getIdAd());
                    precio.setActivo(true);
                    precio.setFechaRegistro(new Date());
                    srvGrupoPrecio.crear(precio);
                }
                
                generalController.getFramework().doMensajeF("GUARDAR", "Grupo agregado correctamente", 1);
            } else {
                // Actualizar grupo
                clsGrupoEdit = srvGrupo.actualizar(clsGrupoEdit);
                
                // Desactivar precios existentes
                if (clsGrupoEdit.getCepGrupoPrecioList() != null) {
                    for (ejbCcoCepGrupoPrecio precioExistente : clsGrupoEdit.getCepGrupoPrecioList()) {
                        precioExistente.setActivo(false);
                        srvGrupoPrecio.actualizar(precioExistente);
                    }
                }
                
                // Crear nuevos precios
                if (precioUNS != null && precioUNS > 0) {
                    ejbCcoCepGrupoPrecio precio = new ejbCcoCepGrupoPrecio();
                    precio.setCepCursoDocente(clsGrupoEdit); // CORREGIDO
                    precio.setTipoAlumno("UNS");
                    precio.setMonto(BigDecimal.valueOf(precioUNS));
                    precio.setCodigoPago(codigoPagoUNS != null ? codigoPagoUNS : "CURSO_UNS_" + clsGrupoEdit.getIdAd());
                    precio.setActivo(true);
                    precio.setFechaRegistro(new Date());
                    srvGrupoPrecio.crear(precio);
                }
                
                if (precioExterno != null && precioExterno > 0) {
                    ejbCcoCepGrupoPrecio precio = new ejbCcoCepGrupoPrecio();
                    precio.setCepCursoDocente(clsGrupoEdit); // CORREGIDO
                    precio.setTipoAlumno("EXTERNO");
                    precio.setMonto(BigDecimal.valueOf(precioExterno));
                    precio.setCodigoPago(codigoPagoExterno != null ? codigoPagoExterno : "CURSO_EXT_" + clsGrupoEdit.getIdAd());
                    precio.setActivo(true);
                    precio.setFechaRegistro(new Date());
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
        idNivelSeleccionado = null;
        idGrupoSeleccionado = null;
        
        precioUNS = null;
        precioExterno = null;
        codigoPagoUNS = null;
        codigoPagoExterno = null;
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
        return true;
    }
}