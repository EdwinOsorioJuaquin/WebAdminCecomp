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
import org.primefaces.model.file.UploadedFile;

import java.io.*;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Named(value = "cargaNotasController")
@SessionScoped
@Getter
@Setter
public class CargaNotasController implements Serializable {

    private List<CertificadoQRController.CertificadoDemo> lstNotas;

    @PostConstruct
    public void init(){
        lstNotas = new ArrayList<>();
    }

    // ============================
    // SUBIR ARCHIVO CSV
    // ============================
    public void handleFileUpload(FileUploadEvent event){

        UploadedFile file = event.getFile();

        if(file == null){
            return;
        }

        try(BufferedReader br = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))){

            lstNotas.clear();

            String linea;
            boolean primera = true;

            while((linea = br.readLine()) != null){

                // saltar cabecera
                if(primera){
                    primera = false;
                    continue;
                }

                String[] data = linea.split(",");

                if(data.length < 7) continue;

                CertificadoQRController.CertificadoDemo c =
                        new CertificadoQRController.CertificadoDemo(
                                Integer.parseInt(data[0].trim()),
                                data[1].trim(),
                                "G1", // grupo demo
                                data[2].trim(),
                                data[6].trim(),
                                data[4].trim(),
                                data[5].trim(),
                                Integer.parseInt(data[3].trim())
                        );

                lstNotas.add(c);
            }

        }catch(Exception e){
            e.printStackTrace();
        }
    }

 
}