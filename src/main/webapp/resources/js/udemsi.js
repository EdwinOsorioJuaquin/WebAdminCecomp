//no permite el uso de los botones de retroceso del navegador---
history.pushState(null, null, location.href);
window.onpopstate = function () {
    history.go(1);
};
//----------------------------------------------------------------
var miVentana;
function num(e) {
    evt = e ? e : event;
    tcl = (window.Event) ? evt.which : evt.keyCode;
    if (tcl == 8 || tcl == 127 || tcl == 13) {
        return true;
    }
    return false;
}
function solonum(event) {
    if (event) {
        var charCode = (event.which) ? event.which : event.keyCode;
        if ((charCode < 48 || charCode > 57) && charCode != 8 && charCode != 9 && charCode != 37 && charCode != 14 && charCode != 39 && charCode != 35 && charCode != 36 && charCode != 46)
            return false;
    }
    return true;
}
function numDecimal(e, field) {
    key = e.keyCode ? e.keyCode : e.which
    // backspace
    if (key == 8)
        return true
    //otras teclas 
    if (key == 8 || key == 9 || key == 37 || key == 14 || key == 39 || key == 35 || key == 36)
        return true

    // 0-9
    if (key > 47 && key < 58) {
        if (field.value == "")
            return true
        regexp = /^((([0-9])(\.){,1})|20{,1})$/
        //regexp = /.[0-9]{1}$/
        return (regexp.test(field.value))

    }

    // .
    if (key == 46) {
        if (field.value == "")
            return false
        regexp = /^[0-9]+$/
        return regexp.test(field.value)
    }
    // other key
    return false

}
function mayus(e) {
    e.value = e.value.toUpperCase();
}
function abrirVentana(ServidorWF) {
    if ((ServidorWF == 0)) { //Para desarrollo
        window.open("", "Bienvenido", "menubar=no,toolbar=n0, directories=no");
    } else if ((ServidorWF == 1)) { //Para tester
        window.open("", "Bienvenido", "menubar=no,toolbar=n0, directories=no");
    } else { //para Producción    
        window.open("", "Bienvenido", "menubar=no,toolbar=n0, directories=no");
    }
    var padre = window.open("", "_parent", "menubar=no");
    padre.close();
}
function amplia() {
    window.moveTo(0, 0);
    window.resizeTo(screen.availWidth, screen.availHeight);
}
function abrir() {
    x = window.open('pag', 'nombrepopup', 'status=no,scrollbars=no,menubar=no,width=800,height=545,top=20,left=0')
    var ventana = window.self;
    ventana.opener = window.parent.self;
    ventana.close();
}

function abrirVentana(ventana) {
    miVentana = window.open(ventana, '_self');
}
function cerrarVentana() {
    miVentana.close();
}
function nobackbutton() {
    window.location.hash = "no-back-button";
    window.location.hash = "Again-No-back-button"; //chrome
    window.onhashchange = function () {
        window.location.hash = "no-back-button";
    };

}
function selection() {
    document.getElementById('txtCodigo').select();
}
function alertaMenu2() {
    alert("ventana");
}
var myVar;

function myFunction() {
    myVar = setTimeout(showPage, 3000);
}

function showPage() {
    document.getElementById("divCarga").style.display = "none";
    document.getElementById("divImagen").style.display = "block";
}
function toggle(id) {
    var element = document.getElementById(id);
    if (element.style.display === 'block') {
        element.style.display = 'none';
    } else {
        element.style.display = 'block';
    }
}
function mostrar(id) {
    var element = document.getElementById(id);
    element.style.display = 'block';
}
function ocultar(id) {
    var element = document.getElementById(id);
    element.style.display = 'none';
}

function tipoAplicacion(idTipo) {
//    alert(idTipo);
    if (idTipo === 1) {
        document.documentElement.style.setProperty('--colorModuloClaro', 'hsl(0, 100%, 50%)');
        document.documentElement.style.setProperty('--colorModuloOscuro', 'hsl(0, 100%, 45%)');
    }
    if (idTipo === 2) {
        document.documentElement.style.setProperty('--colorModuloClaro', 'hsl(120, 40%, 40%)');
        document.documentElement.style.setProperty('--colorModuloOscuro', 'hsl(120, 40%, 30%)');
    }
    if (idTipo === 3) {
        document.documentElement.style.setProperty('--colorModuloClaro', 'hsl(198, 66%, 45%)');
        document.documentElement.style.setProperty('--colorModuloOscuro', 'hsl(198, 66%, 40%)');
    }

}

function enfocar(id) {
    var elemento = document.getElementById(id);
    if (elemento) {
        elemento.focus();
    } else {
        console.warn("No se encontró el elemento con ID:", id);
    }
}
            