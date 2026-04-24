/*----------------------------------------------------*/
/*----Mostrar y ocultar css para ocular menu*/
$('document').ready(function(e) {
  $('.collapse').collapse({toggle: false});
});

$(document).ready(function () {

    $("#btnMenu").click(function () {
        if ($('#sideNav').css('display') === 'none') {
            $("#sideNav").show();
            $("#sidenav-overlay").show();
            $("#contenido").removeClass("contenido-exp");
            $("#rutaNavegacion").removeClass("contenido-exp");
            $("#sideNav2").removeClass("side-nav2-subir");
            localStorage.idTipoMenu = 1;
        } else {
            $("#sideNav").hide();
            $("#sidenav-overlay").hide();
            $("#contenido").addClass("contenido-exp");
            $("#rutaNavegacion").addClass("contenido-exp");
            $("#sideNav2").addClass("side-nav2-subir");
            localStorage.idTipoMenu = 0;
        }
    });
    $("#sidenav-overlay").click(function () {
        $("#sideNav").hide(250);
        $("#sidenav-overlay").hide();
        $("#contenido").addClass("contenido-exp");
        $("#rutaNavegacion").addClass("contenido-exp");
    });
    $(".sidebar-dropdown > a").click(function () {
        localStorage.idMenu = $(this).attr('id');
//        alert($(this).attr('id'));
        $(".sidebar-submenu").slideUp(200);
        if ($(this).parent().hasClass("active")) {
            $(".sidebar-dropdown").removeClass("active");
            $(this).parent().removeClass("active");
        } else {
            $(".sidebar-dropdown").removeClass("active");
            $(this).next(".sidebar-submenu").slideDown(200);
            $(this).parent().addClass("active");
        }
    });
    $("#close-sidebar").click(function () {
        $(".page-wrapper").removeClass("toggled");
    });
    $("#show-sidebar").click(function () {
        $(".page-wrapper").addClass("toggled");
    });
});

window.onload = function () {
//alert(localStorage.idMenu);
    if (document.body.clientWidth > 1200) {
        $("#Sub" + localStorage.idMenu).slideDown(200);
        if (localStorage.idTipoMenu === "1") {
            $("#sideNav").show();
            $("#sidenav-overlay").show();
            $("#contenido").removeClass("contenido-exp");
            $("#rutaNavegacion").removeClass("contenido-exp");
            $("#sideNav2").removeClass("side-nav2-subir");
        } else {
            $("#sideNav").hide();
            $("#sidenav-overlay").hide();
            $("#contenido").addClass("contenido-exp");
            $("#rutaNavegacion").addClass("contenido-exp");
            $("#sideNav2").addClass("side-nav2-subir");
        }
    }
};

function limpiarStorage() {
    localStorage.clear();
}

function cerrarMenu() {
    if (document.body.clientWidth <= 1200) {
        $("#sideNav").hide();
        $("#sidenav-overlay").hide();
    }
}

$(document).click(function () {
    $("#dm").hide('slow');
});

$("#dm").click(function (e) {
    e.stopPropagation();
});

$(function () {
    $('[data-toggle="tooltip"]').tooltip();
});

/*------------------------------------------------------*/
/*----Carga de pagina-----*/
//Se activa cuando estas abriendo una pagina
var miModulo;
//---buscando nombre del modulo-
var loc = window.location;
var tipoPathname=loc.pathname.lastIndexOf('faces');
if (tipoPathname === -1) {
    miModulo = loc.pathname.substring(1, loc.pathname.lastIndexOf('/'));
} else {
    miModulo = loc.pathname.substring(1, loc.pathname.lastIndexOf('faces') - 1);
}
var paginaError=loc.protocol+'//'+loc.host+'/'+ miModulo+'/faces/error.xhtml';
//------------------------------
var iniciando = "inicio" + miModulo;
var cargando = "cargado" + miModulo;
localStorage.setItem(iniciando, Date.now());
var onLocalStorageEvent = function (e) {
    if (e.key === iniciando) {
        // se activa si es la primera vez de acceso
        localStorage.setItem(cargando, Date.now());
    }
    if (e.key === cargando) {
        miVentana = window.open(paginaError, '_self');
    }
};
window.addEventListener('storage', onLocalStorageEvent, false);

/*////////////////////////////////////////////////////////////////////////////*/