package controladores;

import ejbCecomp.entidades.*;
import ejbCecomp.ejb.negocio.*;
import jakarta.annotation.PostConstruct;
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

    // =========================
    // CONTROL DE PASOS
    // =========================
    private boolean pasoPersona;
    private boolean pasoAlumno;
    private boolean personaGuardada;

    // =========================
    // ENTIDADES
    // =========================
    private ejbCcoDrtPersonanatural personaEdit;
    private ejbCcoCcoAlumnoExterno alumnoEdit;

    // =========================
    // EJB
    // =========================
    private ejbCcoCcoAlumnoExternoServiceLocal srvAlumno;

    public AlumnoController() {
        try {
            Context context = new InitialContext();
            srvAlumno = (ejbCcoCcoAlumnoExternoServiceLocal) context.lookup(
                doGenerarJNDI("ejbCecomp", "1.0", "ejbCcoCcoAlumnoExternoServiceLocal")
            );
        } catch (NamingException e) {
            System.out.println("Error JNDI: " + e);
        }
    }

    // =========================
    // INIT
    // =========================
    public void doIniciarPagina() {
        personaEdit = new ejbCcoDrtPersonanatural();
        alumnoEdit = new ejbCcoCcoAlumnoExterno();

        pasoPersona = true;
        pasoAlumno = false;
        personaGuardada = false;
    }

    // =========================
    // GUARDAR PERSONA
    // =========================
    public void doGuardarPersona() {

    try {

        if (personaEdit.getNumeroPndid() == null || personaEdit.getNumeroPndid().isEmpty()) {
            generalController.getFramework().doMensajeF("ERROR", "DNI requerido", 3);
            return;
        }

        // Nombre completo
        personaEdit.setNombreCompleto(
                personaEdit.getApPaterno() + " "
                + personaEdit.getApMaterno() + " "
                + personaEdit.getNombre()
        );

        personaEdit.setEstadoPernat('A');
        personaEdit.setFechaIng(new Date());
        personaEdit.setUpdateSelf(1);

        // 🔥 VALIDACIÓN IMPORTANTE
        if (personaEdit.getSexo() == null ||
            personaEdit.getIdUbgNac() == 0 ||
            personaEdit.getIdUbgPro() == 0 ||
            personaEdit.getIdColegio() == 0 ||
            personaEdit.getAnioEgresoCole() == 0) {

            generalController.getFramework().doMensajeF("ERROR", "Complete todos los campos obligatorios", 3);
            return;
        }

        personaEdit = srvAlumno.guardarPersona(personaEdit);

        pasoPersona = false;
        pasoAlumno = true;

        generalController.getFramework().doMensajeF("OK", "Persona guardada correctamente", 1);

    } catch (Exception e) {
        e.printStackTrace();
        generalController.getFramework().doMensajeF("ERROR", "Error al guardar persona", 3);
    }
}

    // =========================
    // GUARDAR ALUMNO
    // =========================
    /* 
public void doGuardarAlumno() {
        try {

            if (alumnoEdit.getCorreoLogin() == null || alumnoEdit.getCorreoLogin().isEmpty()) {
                generalController.getFramework().doMensajeF("ERROR", "Correo requerido", 3);
                return;
            }

            if (alumnoEdit.getPassword() == null || alumnoEdit.getPassword().isEmpty()) {
                generalController.getFramework().doMensajeF("ERROR", "Contraseña requerida", 3);
                return;
            }

            // 🔥 RELACIÓN CON PERSONA
            alumnoEdit.setPersona(personaEdit); // ajusta si tu atributo se llama distinto

            alumnoEdit.setEstado('A');
            alumnoEdit.setFechaIng(new Date());

            srvAlumno.guardarAlumno(alumnoEdit);

            generalController.getFramework().doMensajeF("OK", "Alumno creado correctamente", 1);

            doNuevo();

        } catch (Exception e) {
            e.printStackTrace();
            generalController.getFramework().doMensajeF("ERROR", "Error al crear alumno", 3);
        }
    }

*/

    // =========================
    // RESET
    // =========================
    public void doNuevo() {
        personaEdit = new ejbCcoDrtPersonanatural();
        alumnoEdit = new ejbCcoCcoAlumnoExterno();

        pasoPersona = true;
        pasoAlumno = false;
        personaGuardada = false;
    }
}