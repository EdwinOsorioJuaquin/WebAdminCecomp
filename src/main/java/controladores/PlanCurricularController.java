package controladores;

import ejbCecomp.ejb.negocio.ejbCcoCepCecPlanServiceLocal;
import ejbCecomp.ejb.negocio.ejbCcoCepCecSesionServiceLocal;
import ejbCecomp.ejb.negocio.ejbCcoCepCecTemaServiceLocal;
import ejbCecomp.entidades.ejbCcoCepCecPlan;
import ejbCecomp.entidades.ejbCcoCepCecSesion;
import ejbCecomp.entidades.ejbCcoCepCecSesionPK;
import ejbCecomp.entidades.ejbCcoCepCecTema;
import ejbCecomp.entidades.ejbCcoCepCecTemaPK;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import lombok.Getter;
import lombok.Setter;
import static libreriaUdemsi.funciones.libreriaGeneral.doGenerarJNDI;

@Named(value = "planCurricularController")
@SessionScoped
@Getter
@Setter
public class PlanCurricularController implements Serializable {

    @Inject
    private GeneralController generalController;

    private List<ejbCcoCepCecPlan> lstPlanes;
    private List<ejbCcoCepCecPlan> lstPlanesView;
    private ejbCcoCepCecPlan clsPlan;
    private String strBusqueda;
    private String strValor;
    private Integer idEdicion;
    
    private List<ejbCcoCepCecSesion> lstSesiones;
    private ejbCcoCepCecSesion clsSesion;
    private Integer idPlanActual;
    private String vistaSesion;
    
    private List<ejbCcoCepCecTema> lstSubtemas;
    private ejbCcoCepCecTema clsSubtema;
    private Integer idSesionActual;
    private String vistaSubtema;
    private Integer idTemEditando; // Para saber si estamos editando

    private static final String VISTA_LISTA = "LISTA";
    private static final String VISTA_NUEVO = "NUEVO";
    private static final String VISTA_SESIONES = "SESIONES";
    private static final String VISTA_SUBTEMAS = "SUBTEMAS";

    private ejbCcoCepCecPlanServiceLocal srvPlan;
    private ejbCcoCepCecSesionServiceLocal srvSesion;
    private ejbCcoCepCecTemaServiceLocal srvTema;

    public PlanCurricularController() {
        try {
            Context context = new InitialContext();
            srvPlan = (ejbCcoCepCecPlanServiceLocal) context.lookup(doGenerarJNDI("ejbCecomp", "1.0", "ejbCcoCepCecPlanServiceLocal"));
            srvSesion = (ejbCcoCepCecSesionServiceLocal) context.lookup(doGenerarJNDI("ejbCecomp", "1.0", "ejbCcoCepCecSesionServiceLocal"));
            srvTema = (ejbCcoCepCecTemaServiceLocal) context.lookup(doGenerarJNDI("ejbCecomp", "1.0", "ejbCcoCepCecTemaServiceLocal"));
        } catch (NamingException e) {
            System.out.println("Error planCurricularController: " + e);
        }
    }

    // ==================== PLANES ====================
    
    public void doIniciarPagina() {
        System.out.println("Llegó a doIniciarPagina");
        strValor = VISTA_LISTA;
        strBusqueda = "";
        cargarPlanes();
        clsPlan = new ejbCcoCepCecPlan();
        clsPlan.setEstadoPland(true);
        clsPlan.setFechaRegis(new Date());
        idEdicion = null;
    }

    private void cargarPlanes() {
        try {
            lstPlanes = srvPlan.listarActivos();
            lstPlanesView = new ArrayList<>(lstPlanes);
        } catch (Exception e) {
            lstPlanes = new ArrayList<>();
            lstPlanesView = new ArrayList<>();
        }
    }

    public void doBuscar() {
        String q = (strBusqueda == null) ? "" : strBusqueda.trim().toLowerCase();
        if (q.isBlank()) {
            lstPlanesView = new ArrayList<>(lstPlanes);
        } else {
            List<ejbCcoCepCecPlan> filtrado = new ArrayList<>();
            for (ejbCcoCepCecPlan plan : lstPlanes) {
                String nombre = plan.getNomPland() != null ? plan.getNomPland().toLowerCase() : "";
                if (nombre.contains(q)) filtrado.add(plan);
            }
            lstPlanesView = filtrado;
        }
        generalController.getFramework().doMensajeF("BÚSQUEDA", "Filtro aplicado correctamente", 1);
    }

    public void doNuevo() {
        strValor = VISTA_NUEVO;
        idEdicion = null;
        clsPlan = new ejbCcoCepCecPlan();
        clsPlan.setEstadoPland(true);
        clsPlan.setFechaRegis(new Date());
    }

    public void doEditar(ejbCcoCepCecPlan plan) {
        if (plan == null) return;
        strValor = VISTA_NUEVO;
        idEdicion = plan.getIdPland();
        clsPlan = srvPlan.buscarPorId(plan.getIdPland());
    }

    public void doGuardar() {
        if (!validarPlan()) return;
        try {
            if (idEdicion == null || idEdicion == 0) {
                int nuevoId = obtenerNuevoIdPlan();
                clsPlan.setIdPland(nuevoId);
                srvPlan.crear(clsPlan);
                generalController.getFramework().doMensajeF("GUARDAR", "Plan curricular agregado correctamente", 1);
            } else {
                srvPlan.actualizar(clsPlan);
                generalController.getFramework().doMensajeF("ACTUALIZAR", "Plan curricular actualizado correctamente", 1);
            }
            cargarPlanes();
            strValor = VISTA_LISTA;
        } catch (Exception e) {
            generalController.getFramework().doMensajeF("ERROR", "Error al guardar el plan curricular", 3);
        }
    }

    public void doGestionarSesiones(ejbCcoCepCecPlan plan) {
        if (plan == null) return;
        clsPlan = plan;
        idPlanActual = plan.getIdPland();
        cargarSesiones(idPlanActual);
        strValor = VISTA_SESIONES;
        vistaSesion = VISTA_LISTA;
    }

    public void doVolver() {
        strValor = VISTA_LISTA;
        strBusqueda = "";
        doBuscar();
    }

    // ==================== SESIONES ====================

    private void cargarSesiones(Integer idPlan) {
        try {
            lstSesiones = srvSesion.listarPorPlan(idPlan);
        } catch (Exception e) {
            lstSesiones = new ArrayList<>();
        }
    }

    public void doNuevaSesion() {
        clsSesion = new ejbCcoCepCecSesion();
        clsSesion.setEstadoSesion(true);
        vistaSesion = VISTA_NUEVO;
    }

    public void doEditarSesion(ejbCcoCepCecSesion sesion) {
        if (sesion == null || sesion.getCepCecSesionPK()== null) return;
        clsSesion = srvSesion.buscarPorId(
            sesion.getCepCecSesionPK().getIdPland(),
            sesion.getCepCecSesionPK().getIdSesio()
        );
        vistaSesion = VISTA_NUEVO;
    }

    public void doGuardarSesion() {
        if (!validarSesion()) return;
        try {
            if (clsSesion.getCepCecSesionPK() == null || 
                clsSesion.getCepCecSesionPK().getIdSesio() == 0) {
                
                int nuevoId = obtenerNuevoIdSesion();
                ejbCcoCepCecSesionPK pk = new ejbCcoCepCecSesionPK();
                pk.setIdPland(idPlanActual);
                pk.setIdSesio(nuevoId);
                clsSesion.setCepCecSesionPK(pk);
                
                srvSesion.crear(clsSesion);
                generalController.getFramework().doMensajeF("GUARDAR", "Sesión agregada correctamente", 1);
            } else {
                srvSesion.actualizar(clsSesion);
                generalController.getFramework().doMensajeF("ACTUALIZAR", "Sesión actualizada correctamente", 1);
            }
            cargarSesiones(idPlanActual);
            vistaSesion = VISTA_LISTA;
        } catch (Exception e) {
            generalController.getFramework().doMensajeF("ERROR", "Error al guardar la sesión", 3);
        }
    }

    public void doEliminarSesion(ejbCcoCepCecSesion sesion) {
        if (sesion == null) return;
        sesion.setEstadoSesion(false);
        srvSesion.actualizar(sesion);
        generalController.getFramework().doMensajeF("BAJA", "Sesión desactivada correctamente", 2);
        cargarSesiones(idPlanActual);
    }

    public void doVolverSesiones() {
        vistaSesion = VISTA_LISTA;
    }

    public void doVolverPlanes() {
        strValor = VISTA_LISTA;
        cargarPlanes();
        doBuscar();
    }

    // ==================== SUBTEMAS ====================

    public void doGestionarSubtemas(ejbCcoCepCecSesion sesion) {
        if (sesion == null || sesion.getCepCecSesionPK() == null) return;
        idPlanActual = sesion.getCepCecSesionPK().getIdPland();
        idSesionActual = sesion.getCepCecSesionPK().getIdSesio();
        clsSesion = sesion;
        cargarSubtemas(idPlanActual, idSesionActual);
        strValor = VISTA_SUBTEMAS;
        vistaSubtema = VISTA_LISTA;
    }

    private void cargarSubtemas(Integer idPland, Integer idSesio) {
        try {
            lstSubtemas = srvTema.listarPorSesion(idPland, idSesio);
        } catch (Exception e) {
            lstSubtemas = new ArrayList<>();
        }
    }

    public void doNuevoSubtema() {
        System.out.println("Llegó a doNuevoSubtema");
        idTemEditando = null;
        clsSubtema = new ejbCcoCepCecTema();
        clsSubtema.setEstadoTem(true);
        if (clsSesion != null && clsSesion.getCepCecSesionPK() != null) {
            ejbCcoCepCecTemaPK pk = new ejbCcoCepCecTemaPK();
            pk.setIdPland(clsSesion.getCepCecSesionPK().getIdPland());
            pk.setIdSesio(clsSesion.getCepCecSesionPK().getIdSesio());
            clsSubtema.setCepCecTemaPK(pk);
        }
        vistaSubtema = VISTA_NUEVO;
    }

    public void doEditarSubtema(ejbCcoCepCecTema subtema) {
        System.out.println("Llegó a doEditarSubtema");
        if (subtema == null || subtema.getCepCecTemaPK()== null) return;
        
        idTemEditando = subtema.getCepCecTemaPK().getIdTem();
        idPlanActual = subtema.getCepCecTemaPK().getIdPland();
        idSesionActual = subtema.getCepCecTemaPK().getIdSesio();
        
        clsSubtema = srvTema.buscarPorId(
            idPlanActual,
            idSesionActual,
            idTemEditando
        );
        vistaSubtema = VISTA_NUEVO;
    }

    public void doGuardarSubtema() {
        if (!validarSubtema()) return;
        try {
            if (idTemEditando != null && idTemEditando > 0) {
                // Editar subtema existente
                clsSubtema.getCepCecTemaPK().setIdTem(idTemEditando);
                srvTema.actualizar(clsSubtema);
                generalController.getFramework().doMensajeF("ACTUALIZAR", "Subtema actualizado correctamente", 1);
                idTemEditando = null;
            } else {
                // Crear nuevo subtema
                ejbCcoCepCecTemaPK pk = clsSubtema.getCepCecTemaPK();
                if (pk == null) {
                    pk = new ejbCcoCepCecTemaPK();
                    pk.setIdPland(idPlanActual);
                    pk.setIdSesio(idSesionActual);
                    clsSubtema.setCepCecTemaPK(pk);
                }
                int nuevoId = obtenerNuevoIdTema();
                pk.setIdTem(nuevoId);
                System.out.println("Nuevo ID para subtema: " + nuevoId);
                srvTema.crear(clsSubtema);
                generalController.getFramework().doMensajeF("GUARDAR", "Subtema agregado correctamente", 1);
            }
            
            cargarSubtemas(idPlanActual, idSesionActual);
            vistaSubtema = VISTA_LISTA;
        } catch (Exception e) {
            e.printStackTrace();
            generalController.getFramework().doMensajeF("ERROR", "Error al guardar el subtema", 3);
        }
    }

    public void doEliminarSubtema(ejbCcoCepCecTema subtema) {
        if (subtema == null) return;
        subtema.setEstadoTem(false);
        srvTema.actualizar(subtema);
        generalController.getFramework().doMensajeF("BAJA", "Subtema desactivado correctamente", 2);
        cargarSubtemas(idPlanActual, idSesionActual);
    }

    public void doVolverSubtemas() {
        vistaSubtema = VISTA_LISTA;
    }

    public void doVolverSesionesDesdeSubtemas() {
        strValor = VISTA_SESIONES;
        cargarSesiones(idPlanActual);
        vistaSesion = VISTA_LISTA;
    }

    // ==================== MÉTODOS PRIVADOS ====================

    private int obtenerNuevoIdPlan() {
        int maxId = 0;
        if (lstPlanes != null) {
            for (ejbCcoCepCecPlan plan : lstPlanes) {
                if (plan.getIdPland() != null && plan.getIdPland() > maxId) {
                    maxId = plan.getIdPland();
                }
            }
        }
        return maxId + 1;
    }

    private int obtenerNuevoIdSesion() {
        int maxId = 0;
        if (lstSesiones != null) {
            for (ejbCcoCepCecSesion s : lstSesiones) {
                if (s.getCepCecSesionPK()!= null && 
                    s.getCepCecSesionPK().getIdSesio() > maxId) {
                    maxId = s.getCepCecSesionPK().getIdSesio();
                }
            }
        }
        return maxId + 1;
    }

    private int obtenerNuevoIdTema() {
        int maxId = 0;
        if (lstSubtemas != null) {
            for (ejbCcoCepCecTema t : lstSubtemas) {
                if (t.getCepCecTemaPK()!= null && 
                    t.getCepCecTemaPK().getIdTem() > maxId) {
                    maxId = t.getCepCecTemaPK().getIdTem();
                }
            }
        }
        return maxId + 1;
    }

    private boolean validarPlan() {
        if (clsPlan == null) return false;
        if (clsPlan.getNomPland() == null || clsPlan.getNomPland().trim().isEmpty()) {
            generalController.getFramework().doMensajeF("VALIDACIÓN", "Ingrese el nombre del plan curricular", 2);
            return false;
        }
        return true;
    }

    private boolean validarSesion() {
        if (clsSesion == null) return false;
        if (clsSesion.getNombreSesion() == null || clsSesion.getNombreSesion().trim().isEmpty()) {
            generalController.getFramework().doMensajeF("VALIDACIÓN", "Ingrese el nombre de la sesión", 2);
            return false;
        }
        return true;
    }

    private boolean validarSubtema() {
        if (clsSubtema == null) return false;
        if (clsSubtema.getNomTem() == null || clsSubtema.getNomTem().trim().isEmpty()) {
            generalController.getFramework().doMensajeF("VALIDACIÓN", "Ingrese el nombre del subtema", 2);
            return false;
        }
        return true;
    }
}