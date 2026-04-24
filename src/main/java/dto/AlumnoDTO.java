package dto;

import java.io.Serializable;
import java.util.Date;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AlumnoDTO implements Serializable {

    private Integer idCcoUsuEx;   // cco_alumno_externo.id_cco_usu_ex
    private Integer idDir;        // drt_personanatural.id_dir

    // =========================
    // CONDICIÓN
    // =========================
    private String condicion;        // (UI)
    private String codigoIdCard;     // opcional
    private String carreraOcupacion; // opcional

    // =========================
    // DATOS PERSONALES
    // =========================
    private String nombres;       // nombre
    private String apellidos;     // ap_paterno + ap_materno
    private String dni;           // numero_pndid
    private String sexo;
    private Date fechaNacimiento;

    // =========================
    // UBIGEO (simplificado UI)
    // =========================
    private String departamento;
    private String provincia;
    private String distrito;

    // =========================
    // CONTACTO
    // =========================
    private String direccion;
    private String correo;
    private String celular;
    private String telefonoFijo;

    // =========================
    // SISTEMA
    // =========================
    private String estado; // ACTIVO / INACTIVO
    private String password;

    // =========================
    // DERIVADO (vista)
    // =========================
    public String getNombreCompleto() {
        return (apellidos + " " + nombres).toUpperCase();
    }
}