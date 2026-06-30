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

    // ============================
    // LISTAS Y FILTROS
    // ============================
    private List<ejbCcoGrupoDTO> lstGruposDTO;
    private List<ejbCcoGrupoDTO> lstGruposViewDTO;
    private String strBusqueda;
    private String strValor;
    private Integer idEdicion;

    // ============================
    // ENTIDAD PRINCIPAL
    // ============================
    private ejbCcoCepCursoDocente clsGrupoEdit;

    // ============================
    // IDs SELECCIONADOS
    // ============================
    private Integer idPersonalSeleccionado;
    private Integer idCursoSeleccionado;
    private Integer idTipoDesarrolloSeleccionado;
    private Integer idGrupoSeleccionado;
    private Integer idHoraSeleccionada;
    private Integer idAulaSeleccionada;

    // ============================
    // PRECIO
    // ============================
    private Double precioMonto;
    private String codigoPago;

    // ============================
    // HORARIOS (Días seleccionados)
    // ============================
    private List<Integer> lstDiasSeleccionados;

    // ============================
    // CATÁLOGOS
    // ============================
    private List<ejbCcoCepPersonal> lstDocentes;
    private List<ejbCcoCepCurso> lstCursos;
    private List<ejbCcoCepCecTipoDesarrollo> lstTiposDesarrollo;
    private List<ejbCcoCepCecGrupoCurso> lstGruposCurso;
    private List<ejbCcoCepHorarioHora> lstHoras;
    private List<ejbCcoCepCecAulaClass> lstAulas;

    // ============================
    // CONSTANTES DE VISTA
    // ============================
    private static final String VISTA_LISTA = "LISTA";
    private static final String VISTA_NUEVO = "NUEVO";
    private static final String VISTA_EDITAR = "EDITAR";

    // ============================
    // SERVICIOS EJB
    // ============================
    private ejbCcoCepCursoDocenteServiceLocal srvGrupo;
    private ejbCcoCepPersonalServiceLocal srvPersonal;
    private ejbCcoCepCursoServiceLocal srvCurso;
    private ejbCcoCepCecTipoDesarrolloServiceLocal srvTipoDesarrollo;
    private ejbCcoCepCecGrupoCursoServiceLocal srvGrupoCurso;
    private ejbCcoCepGrupoPrecioServiceLocal srvGrupoPrecio;
    private ejbCcoCepHorarioDiaServiceLocal srvHorarioDia;
    private ejbCcoCepHorarioHoraServiceLocal srvHorarioHora;
    private ejbCcoCepCecAulaClassServiceLocal srvAula;

    // ============================
    // CONSTRUCTOR
    // ============================
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
            srvHorarioDia = (ejbCcoCepHorarioDiaServiceLocal) context.lookup(
                doGenerarJNDI("ejbCecomp", "1.0", "ejbCcoCepHorarioDiaServiceLocal")
            );
            srvHorarioHora = (ejbCcoCepHorarioHoraServiceLocal) context.lookup(
                doGenerarJNDI("ejbCecomp", "1.0", "ejbCcoCepHorarioHoraServiceLocal")
            );
            srvAula = (ejbCcoCepCecAulaClassServiceLocal) context.lookup(
                doGenerarJNDI("ejbCecomp", "1.0", "ejbCcoCepCecAulaClassServiceLocal")
            );
        } catch (NamingException e) {
            System.out.println("Error JNDI GrupoController: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ============================
    // INICIALIZACIÓN
    // ============================
    public void doIniciarPagina() {
        strValor = VISTA_LISTA;
        strBusqueda = "";
        cargarGrupos();
        cargarCatalogos();
    }

    // ============================
    // CARGA DE DATOS
    // ============================
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
            e.printStackTrace();
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
            lstHoras = srvHorarioHora.listarActivos();
            lstAulas = srvAula.listarTodos();
        } catch (Exception e) {
            System.out.println("Error cargar catálogos: " + e.getMessage());
            e.printStackTrace();
            lstDocentes = new ArrayList<>();
            lstCursos = new ArrayList<>();
            lstTiposDesarrollo = new ArrayList<>();
            lstGruposCurso = new ArrayList<>();
            lstHoras = new ArrayList<>();
            lstAulas = new ArrayList<>();
        }
    }

    // ============================
    // BÚSQUEDA
    // ============================
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

    // ============================
    // NUEVO
    // ============================
    public void doNuevo() {
        strValor = VISTA_NUEVO;
        limpiarFormulario();
        cargarCatalogos();
    }

    // ============================
    // EDITAR
    // ============================
    public void doEditar(ejbCcoGrupoDTO dto) {
        if (dto == null || dto.getIdAd() == null) {
            generalController.getFramework().doMensajeF("ERROR", "Grupo no válido", 3);
            return;
        }

        strValor = VISTA_EDITAR;
        idEdicion = dto.getIdAd();

        clsGrupoEdit = srvGrupo.buscarPorId(idEdicion);
        if (clsGrupoEdit != null) {
            // Datos básicos
            idPersonalSeleccionado = clsGrupoEdit.getCepPersonal() != null ? clsGrupoEdit.getCepPersonal().getIdPersonal() : null;
            idCursoSeleccionado = clsGrupoEdit.getCepCurso() != null ? clsGrupoEdit.getCepCurso().getIdCurso() : null;
            idTipoDesarrolloSeleccionado = clsGrupoEdit.getCepCecTipoDesarrollo() != null ? clsGrupoEdit.getCepCecTipoDesarrollo().getIdCiclo() : null;
            idGrupoSeleccionado = clsGrupoEdit.getCepCecGrupoCurso() != null ? clsGrupoEdit.getCepCecGrupoCurso().getIdGrupo() : null;

            // Precio
            if (clsGrupoEdit.getCepGrupoPrecioList() != null && !clsGrupoEdit.getCepGrupoPrecioList().isEmpty()) {
                ejbCcoCepGrupoPrecio precio = clsGrupoEdit.getCepGrupoPrecioList().get(0);
                precioMonto = precio.getMonto() != null ? precio.getMonto().doubleValue() : null;
                codigoPago = precio.getCodigoPago();
            }

            // Horarios
            lstDiasSeleccionados = new ArrayList<>();
            if (clsGrupoEdit.getCepHorarioDiaList() != null && !clsGrupoEdit.getCepHorarioDiaList().isEmpty()) {
                for (ejbCcoCepHorarioDia horario : clsGrupoEdit.getCepHorarioDiaList()) {
                    if (horario.getDia() != null) {
                        lstDiasSeleccionados.add(horario.getDia().intValue());
                    }
                    if (horario.getCepHorarioHora() != null) {
                        idHoraSeleccionada = horario.getCepHorarioHora().getIdHora();
                    }
                    if (horario.getCepCecAulaClass() != null) {
                        idAulaSeleccionada = horario.getCepCecAulaClass().getIdAulClass();
                    }
                }
            }
        }
        cargarCatalogos();
    }

    // ============================
    // GUARDAR (CREAR/ACTUALIZAR)
    // ============================
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
                // =========================================
                // CREAR NUEVO GRUPO
                // =========================================
                clsGrupoEdit = srvGrupo.crear(clsGrupoEdit);

                // 1. Crear Precio
                if (precioMonto != null && precioMonto > 0) {
                    ejbCcoCepGrupoPrecio precio = new ejbCcoCepGrupoPrecio();
                    precio.setCepCursoDocente(clsGrupoEdit);
                    precio.setMonto(BigDecimal.valueOf(precioMonto));
                    precio.setCodigoPago(codigoPago != null && !codigoPago.trim().isEmpty() 
                        ? codigoPago : "GRUPO_" + clsGrupoEdit.getIdAd());
                    srvGrupoPrecio.crear(precio);
                }

                // 2. Crear Horarios
                if (lstDiasSeleccionados != null && !lstDiasSeleccionados.isEmpty() && idHoraSeleccionada != null) {
                    ejbCcoCepHorarioHora hora = srvHorarioHora.buscarPorId(idHoraSeleccionada);
                    ejbCcoCepCecAulaClass aula = idAulaSeleccionada != null ? srvAula.buscarPorId(idAulaSeleccionada) : null;
                    
                    for (Integer dia : lstDiasSeleccionados) {
                        ejbCcoCepHorarioDia horario = new ejbCcoCepHorarioDia();
                        horario.setCepCursoDocente(clsGrupoEdit);
                        horario.setDia(dia.shortValue());
                        horario.setCepHorarioHora(hora);
                        if (aula != null) {
                            horario.setCepCecAulaClass(aula);
                        }
                        srvHorarioDia.crear(horario);
                    }
                }

                generalController.getFramework().doMensajeF("GUARDAR", "Grupo agregado correctamente", 1);

            } else {
                // =========================================
                // ACTUALIZAR GRUPO EXISTENTE
                // =========================================
                clsGrupoEdit = srvGrupo.actualizar(clsGrupoEdit);

                // 1. Actualizar Precio
                if (clsGrupoEdit.getCepGrupoPrecioList() != null && !clsGrupoEdit.getCepGrupoPrecioList().isEmpty()) {
                    ejbCcoCepGrupoPrecio precioExistente = clsGrupoEdit.getCepGrupoPrecioList().get(0);
                    if (precioMonto != null && precioMonto > 0) {
                        precioExistente.setMonto(BigDecimal.valueOf(precioMonto));
                        precioExistente.setCodigoPago(codigoPago != null && !codigoPago.trim().isEmpty() 
                            ? codigoPago : "GRUPO_" + clsGrupoEdit.getIdAd());
                        srvGrupoPrecio.actualizar(precioExistente);
                    }
                } else if (precioMonto != null && precioMonto > 0) {
                    ejbCcoCepGrupoPrecio precio = new ejbCcoCepGrupoPrecio();
                    precio.setCepCursoDocente(clsGrupoEdit);
                    precio.setMonto(BigDecimal.valueOf(precioMonto));
                    precio.setCodigoPago(codigoPago != null && !codigoPago.trim().isEmpty() 
                        ? codigoPago : "GRUPO_" + clsGrupoEdit.getIdAd());
                    srvGrupoPrecio.crear(precio);
                }

                // ACTUALIZAR HORARIOS (SIN ELIMINAR)
                if (lstDiasSeleccionados != null && !lstDiasSeleccionados.isEmpty() && idHoraSeleccionada != null) {
                    ejbCcoCepHorarioHora hora = srvHorarioHora.buscarPorId(idHoraSeleccionada);
                    ejbCcoCepCecAulaClass aula = idAulaSeleccionada != null ? srvAula.buscarPorId(idAulaSeleccionada) : null;

                    // Obtener horarios existentes
                    List<ejbCcoCepHorarioDia> horariosExistentes = clsGrupoEdit.getCepHorarioDiaList();

                    if (horariosExistentes != null && !horariosExistentes.isEmpty()) {
                        // ACTUALIZAR horarios existentes
                        int index = 0;
                        for (ejbCcoCepHorarioDia horarioExistente : horariosExistentes) {
                            if (index < lstDiasSeleccionados.size()) {
                                Integer dia = lstDiasSeleccionados.get(index);
                                horarioExistente.setDia(dia.shortValue());
                                horarioExistente.setCepHorarioHora(hora);
                                if (aula != null) {
                                    horarioExistente.setCepCecAulaClass(aula);
                                }
                                srvHorarioDia.actualizar(horarioExistente);
                            }
                            index++;
                        }

                        // Si hay más días seleccionados que horarios existentes, crear los faltantes
                        if (index < lstDiasSeleccionados.size()) {
                            for (int i = index; i < lstDiasSeleccionados.size(); i++) {
                                Integer dia = lstDiasSeleccionados.get(i);
                                ejbCcoCepHorarioDia horario = new ejbCcoCepHorarioDia();
                                horario.setCepCursoDocente(clsGrupoEdit);
                                horario.setDia(dia.shortValue());
                                horario.setCepHorarioHora(hora);
                                if (aula != null) {
                                    horario.setCepCecAulaClass(aula);
                                }
                                srvHorarioDia.crear(horario);
                            }
                        }
                    } else {
                        // CREAR nuevos horarios
                        for (Integer dia : lstDiasSeleccionados) {
                            ejbCcoCepHorarioDia horario = new ejbCcoCepHorarioDia();
                            horario.setCepCursoDocente(clsGrupoEdit);
                            horario.setDia(dia.shortValue());
                            horario.setCepHorarioHora(hora);
                            if (aula != null) {
                                horario.setCepCecAulaClass(aula);
                            }
                            srvHorarioDia.crear(horario);
                        }
                    }
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

    // ============================
    // CAMBIAR ESTADO (ACTIVAR/CERRAR)
    // ============================
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

            String mensaje = nuevoEstado ? "Grupo activado correctamente" : "Grupo cerrado correctamente";
            generalController.getFramework().doMensajeF("ESTADO", mensaje, 1);

            cargarGrupos();
            doBuscar();
        } catch (Exception e) {
            System.out.println("Error cambiar estado: " + e.getMessage());
            generalController.getFramework().doMensajeF("ERROR", "Error al cambiar estado", 3);
        }
    }

    // ============================
    // VOLVER
    // ============================
    public void doVolver() {
        strValor = VISTA_LISTA;
        strBusqueda = "";
        cargarGrupos();
    }

    // ============================
    // MÉTODOS PRIVADOS
    // ============================
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
        idHoraSeleccionada = null;
        idAulaSeleccionada = null;
        precioMonto = null;
        codigoPago = null;
        lstDiasSeleccionados = new ArrayList<>();
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
        if (lstDiasSeleccionados == null || lstDiasSeleccionados.isEmpty()) {
            generalController.getFramework().doMensajeF("VALIDACIÓN", "Debe seleccionar al menos un día", 2);
            return false;
        }
        if (idHoraSeleccionada == null) {
            generalController.getFramework().doMensajeF("VALIDACIÓN", "Debe seleccionar una hora", 2);
            return false;
        }
        return true;
    }

    // ============================
    // MÉTODOS PARA LA VISTA
    // ============================
    public boolean isMostrarLista() {
        return VISTA_LISTA.equals(strValor);
    }

    public boolean isMostrarFormulario() {
        return VISTA_NUEVO.equals(strValor) || VISTA_EDITAR.equals(strValor);
    }

    public String getTituloFormulario() {
        if (VISTA_NUEVO.equals(strValor)) return "NUEVO GRUPO";
        if (VISTA_EDITAR.equals(strValor)) return "EDITAR GRUPO";
        return "REGISTRO DE GRUPO";
    }

    public String getLabelBotonGuardar() {
        return VISTA_NUEVO.equals(strValor) ? "Registrar" : "Actualizar";
    }
}