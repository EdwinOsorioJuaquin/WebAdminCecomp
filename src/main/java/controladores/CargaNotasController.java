/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package controladores;

/**
 *
 * @author Edwin
 */

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;

import org.primefaces.event.FileUploadEvent;

import java.io.Serializable;
import java.util.*;

@Named(value = "cargaNotasController")
@SessionScoped
@Getter
@Setter
public class CargaNotasController implements Serializable {


    @PostConstruct
    public void init(){
    }

    // ============================
    // SUBIR ARCHIVO CSV
    // ============================
    public void handleFileUpload(FileUploadEvent event){

        
    }

 
}