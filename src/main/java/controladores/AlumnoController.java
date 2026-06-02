package controladores;

import ejbCecomp.entidades.*;
import ejbCecomp.ejb.negocio.*;
import jakarta.faces.view.ViewScoped;
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

@Named("alumnoController")
@ViewScoped
@Getter
@Setter
public class AlumnoController implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private GeneralController generalController;

    private boolean pasoPersona;
    private boolean pasoAlumno;

    private ejbCcoDrtPersonanatural personaEdit;
    private ejbCcoCcoAlumnoExterno alumnoEdit;

    private ejbCcoCcoAlumnoExternoServiceLocal srvAlumno;

    public AlumnoController() {
        try {
            Context context = new InitialContext();
            srvAlumno = (ejbCcoCcoAlumnoExternoServiceLocal) context.lookup(
                doGenerarJNDI("ejbCecomp", "1.0", "ejbCcoCcoAlumnoExternoServiceLocal")
            );
        } catch (NamingException e) {
            System.out.println("Error JNDI AlumnoController: " + e.getMessage());
        }
    }

    public void doIniciarPagina() {
        limpiarFormulario();
        pasoPersona = true;
        pasoAlumno = false;
    }

    private void limpiarFormulario() {
        personaEdit = new ejbCcoDrtPersonanatural();
        alumnoEdit = new ejbCcoCcoAlumnoExterno();
        
        personaEdit.setEstadoPernat('A');
        personaEdit.setFechaIng(new Date());
        personaEdit.setUpdateSelf(1);
        personaEdit.setIdUbgNac(0);
        personaEdit.setIdUbgPro(0);
        personaEdit.setIdColegio(0);
        personaEdit.setAnioEgresoCole(0);
        
        alumnoEdit.setActivo((short) 1);
        alumnoEdit.setAnulado((short) 0);
    }

    public void doGuardarPersona() {
        try {
            if (!validarPersona()) return;

            String nombreCompleto = (
                (personaEdit.getApPaterno() != null ? personaEdit.getApPaterno() : "") + " " +
                (personaEdit.getApMaterno() != null ? personaEdit.getApMaterno() : "") + " " +
                (personaEdit.getNombre() != null ? personaEdit.getNombre() : "")
            ).trim();
            personaEdit.setNombreCompleto(nombreCompleto);

            // Ya no calculas el ID, el DAO lo hace
            personaEdit = srvAlumno.guardarPersona(personaEdit);

            if (personaEdit != null && personaEdit.getIdDir() != null) {
                pasoPersona = false;
                pasoAlumno = true;
                generalController.getFramework().doMensajeF("ÉXITO", "Persona guardada correctamente", 1);
            }

        } catch (Exception e) {
            e.printStackTrace();
            generalController.getFramework().doMensajeF("ERROR", "Error al guardar persona: " + e.getMessage(), 3);
        }
    }

    public void doGuardarAlumno() {
        try {
            if (!validarAlumno()) return;

            if (personaEdit == null || personaEdit.getIdDir() == null) {
                generalController.getFramework().doMensajeF("ERROR", "Primero debe guardar los datos de la persona", 3);
                doVolverPersona();
                return;
            }

            int nuevoIdAlumno = srvAlumno.obtenerUltimoIdAlumno();
            alumnoEdit.setIdCcoUsuEx(nuevoIdAlumno);
            
            alumnoEdit.setDrtPersonanatural(personaEdit);
            alumnoEdit.setActivo((short) 1);
            alumnoEdit.setAnulado((short) 0);

            srvAlumno.guardarAlumnoExterno(alumnoEdit);

            generalController.getFramework().doMensajeF("ÉXITO", "Alumno externo registrado correctamente", 1);

            limpiarFormulario();
            pasoPersona = true;
            pasoAlumno = false;

        } catch (Exception e) {
            e.printStackTrace();
            generalController.getFramework().doMensajeF("ERROR", "Error al guardar alumno: " + e.getMessage(), 3);
        }
    }

    public void doVolverPersona() {
        pasoPersona = true;
        pasoAlumno = false;
    }

    private boolean validarPersona() {
        if (personaEdit.getNumeroPndid() == null || personaEdit.getNumeroPndid().trim().isEmpty()) {
            generalController.getFramework().doMensajeF("VALIDACIÓN", "DNI requerido", 2);
            return false;
        }
        if (personaEdit.getNombre() == null || personaEdit.getNombre().trim().isEmpty()) {
            generalController.getFramework().doMensajeF("VALIDACIÓN", "Nombres requerido", 2);
            return false;
        }
        if (personaEdit.getApPaterno() == null || personaEdit.getApPaterno().trim().isEmpty()) {
            generalController.getFramework().doMensajeF("VALIDACIÓN", "Apellido Paterno requerido", 2);
            return false;
        }
        if (personaEdit.getApMaterno() == null || personaEdit.getApMaterno().trim().isEmpty()) {
            generalController.getFramework().doMensajeF("VALIDACIÓN", "Apellido Materno requerido", 2);
            return false;
        }
        if (personaEdit.getSexo() == null) {
            generalController.getFramework().doMensajeF("VALIDACIÓN", "Sexo requerido", 2);
            return false;
        }
        if (personaEdit.getFechaNac() == null) {
            generalController.getFramework().doMensajeF("VALIDACIÓN", "Fecha de Nacimiento requerida", 2);
            return false;
        }
        if (personaEdit.getDireccion() == null || personaEdit.getDireccion().trim().isEmpty()) {
            generalController.getFramework().doMensajeF("VALIDACIÓN", "Dirección requerida", 2);
            return false;
        }
        if (personaEdit.getCelularPrin() == null || personaEdit.getCelularPrin().trim().isEmpty()) {
            generalController.getFramework().doMensajeF("VALIDACIÓN", "Celular requerido", 2);
            return false;
        }
        if (personaEdit.getEmailPrin() == null || personaEdit.getEmailPrin().trim().isEmpty()) {
            generalController.getFramework().doMensajeF("VALIDACIÓN", "Email requerido", 2);
            return false;
        }
        return true;
    }

    private boolean validarAlumno() {
        if (alumnoEdit.getCorreoLogin() == null || alumnoEdit.getCorreoLogin().trim().isEmpty()) {
            generalController.getFramework().doMensajeF("VALIDACIÓN", "Correo Login requerido", 2);
            return false;
        }
        if (alumnoEdit.getPassword() == null || alumnoEdit.getPassword().trim().isEmpty()) {
            generalController.getFramework().doMensajeF("VALIDACIÓN", "Contraseña requerida", 2);
            return false;
        }
        
        try {
            ejbCcoCcoAlumnoExterno existente = srvAlumno.buscarPorCorreo(alumnoEdit.getCorreoLogin());
            if (existente != null) {
                generalController.getFramework().doMensajeF("VALIDACIÓN", "El correo ya está registrado", 2);
                return false;
            }
        } catch (Exception e) {
            System.out.println("Error validando correo: " + e.getMessage());
        }
        return true;
    }
}