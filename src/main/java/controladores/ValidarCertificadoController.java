/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package controladores;

import controladores.CertificadoQRController.CertificadoDemo;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 *
 * @author Edwin
 */
@Named
@RequestScoped
public class ValidarCertificadoController {

    private CertificadoQRController.CertificadoDemo certificado;

    private String fechaActual;

    @Inject
private CertificadoQRController certificadoQRController;

@PostConstruct
public void init(){

    String clave = FacesContext.getCurrentInstance()
            .getExternalContext()
            .getRequestParameterMap()
            .get("clave");

    certificado = certificadoQRController.obtenerPorClave(clave);

    fechaActual = java.time.LocalDate.now().toString();
}
}
