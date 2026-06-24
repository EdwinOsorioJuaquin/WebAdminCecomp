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

    private ejbCcoDrtPersonanatural personaEdit;
    private ejbCcoCcoAlumnoExterno alumnoEdit;
    private String codigoGenerado;
    private String mensajeExito;

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
        codigoGenerado = null;
        mensajeExito = null;
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

    public void doGuardarAlumno() {
        try {
            if (!validarPersona()) return;

            // 1. Preparar nombre completo
            String nombreCompleto = (
                (personaEdit.getApPaterno() != null ? personaEdit.getApPaterno() : "") + " " +
                (personaEdit.getApMaterno() != null ? personaEdit.getApMaterno() : "") + " " +
                (personaEdit.getNombre() != null ? personaEdit.getNombre() : "")
            ).trim();
            personaEdit.setNombreCompleto(nombreCompleto);

            // 2. Guardar persona
            personaEdit = srvAlumno.guardarPersona(personaEdit);

            if (personaEdit == null || personaEdit.getIdDir() == null) {
                generalController.getFramework().doMensajeF("ERROR", "Error al guardar los datos personales", 3);
                return;
            }

            // 3. Preparar alumno
            int nuevoIdAlumno = srvAlumno.obtenerUltimoIdAlumno();
            alumnoEdit.setIdCcoUsuEx(nuevoIdAlumno);
            alumnoEdit.setDrtPersonanatural(personaEdit);
            alumnoEdit.setActivo((short) 1);
            alumnoEdit.setAnulado((short) 0);
            alumnoEdit.setPassword(personaEdit.getNumeroPndid()); // Contraseña = DNI
            alumnoEdit.setCorreoLogin(personaEdit.getEmailPrin()); // Por si acaso

            // 4. Guardar alumno (el trigger generará el codigo_alu)
            srvAlumno.guardarAlumnoExterno(alumnoEdit);

            // 5. Obtener el código generado por el trigger
            ejbCcoCcoAlumnoExterno alumnoGuardado = srvAlumno.buscarPorIdDir(personaEdit.getIdDir());
            if (alumnoGuardado != null) {
                codigoGenerado = alumnoGuardado.getCodigoAlu();
            } else {
                codigoGenerado = "(pendiente de generación)";
            }

            // 6. Mensaje de éxito con código y contraseña
            mensajeExito = "Alumno registrado correctamente.\n" +
                           "Código de usuario: " + codigoGenerado + "\n" +
                           "Contraseña: " + personaEdit.getNumeroPndid();

            generalController.getFramework().doMensajeF("ÉXITO", mensajeExito, 1);

            // 7. Limpiar formulario para un nuevo registro
            limpiarFormulario();
            codigoGenerado = null;

        } catch (Exception e) {
            e.printStackTrace();
            generalController.getFramework().doMensajeF("ERROR", "Error al registrar alumno: " + e.getMessage(), 3);
        }
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

    public String getMensajeInfo() {
        return "El código de alumno se generará automáticamente (Inicial Apellido Paterno + Inicial Apellido Materno + DNI).\n" +
               "La contraseña será el DNI registrado.";
    }
}