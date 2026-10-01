package ar.edu.utn.dds.k3003.bot;

import ar.edu.utn.dds.k3003.bot.dtos.donaciones.EstadoDonacionEnum;
import ar.edu.utn.dds.k3003.bot.dtos.donaciones.TipoIdentificadorEnum;
import ar.edu.utn.dds.k3003.bot.dtos.incentivos.CategoriaDonadorEnum;
import ar.edu.utn.dds.k3003.bot.dtos.incentivos.TipoMisionEnum;
import ar.edu.utn.dds.k3003.bot.dtos.logistica.TipoAlgoritmoEnum;
import ar.edu.utn.dds.k3003.bot.dtos.TipoNecesidadMaterialEnum;
import ar.edu.utn.dds.k3003.bot.handler.DonacionesHandler;
import ar.edu.utn.dds.k3003.bot.handler.DonadoresHandler;
import ar.edu.utn.dds.k3003.bot.handler.IncentivosHandler;
import ar.edu.utn.dds.k3003.bot.handler.LogisticaHandler;
import ar.edu.utn.dds.k3003.bot.session.Accion;
import ar.edu.utn.dds.k3003.bot.session.Modulo;
import ar.edu.utn.dds.k3003.bot.session.SesionManager;
import ar.edu.utn.dds.k3003.bot.session.SesionUsuario;
import ar.edu.utn.dds.k3003.bot.session.TipoUsuario;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;

// Capa de presentación pura: único punto de entrada de Telegram. Desde la Entrega 5 habla con
// los 4 componentes (antes solo con Donadores y Entidades) - la ejecución de cada acción contra
// su fachada vive en un handler por módulo (ar.edu.utn.dds.k3003.bot.handler), este componente
// solo arma menús, valida campos de forma genérica y despacha al handler que corresponda según
// Accion.getModulo().
@Component
public class DonaTrackBot extends TelegramLongPollingBot {

    private final String username;
    private final SesionManager sesionManager;
    private final DonadoresHandler donadoresHandler;
    private final DonacionesHandler donacionesHandler;
    private final LogisticaHandler logisticaHandler;
    private final IncentivosHandler incentivosHandler;

    public DonaTrackBot(
            @Value("${telegram.bot.token}") String token,
            @Value("${telegram.bot.username}") String username,
            SesionManager sesionManager,
            DonadoresHandler donadoresHandler,
            DonacionesHandler donacionesHandler,
            LogisticaHandler logisticaHandler,
            IncentivosHandler incentivosHandler) {
        super(token);
        this.username = username;
        this.sesionManager = sesionManager;
        this.donadoresHandler = donadoresHandler;
        this.donacionesHandler = donacionesHandler;
        this.logisticaHandler = logisticaHandler;
        this.incentivosHandler = incentivosHandler;
    }

    @Override
    public String getBotUsername() {
        return username;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasCallbackQuery()) {
            manejarCallback(update.getCallbackQuery());
        } else if (update.hasMessage() && update.getMessage().hasText()) {
            manejarMensaje(update.getMessage());
        }
    }

    // ---- Manejo de mensajes de texto ----

    private void manejarMensaje(Message message) {
        Long chatId = message.getChatId();
        String texto = message.getText().trim();
        SesionUsuario sesion = sesionManager.obtener(chatId);

        if (texto.equalsIgnoreCase("/start")) {
            sesionManager.reiniciar(chatId);
            enviarSeleccionModulo(chatId);
            return;
        }
        if (texto.equalsIgnoreCase("/menu")) {
            sesion.finalizarAccion();
            enviarMenuOSeleccion(chatId, sesion);
            return;
        }
        if (texto.equalsIgnoreCase("/cancelar")) {
            sesion.finalizarAccion();
            enviarTexto(chatId, "Cancelado.");
            enviarMenuOSeleccion(chatId, sesion);
            return;
        }

        if (!sesion.tieneAccionEnCurso()) {
            enviarTexto(chatId, "No entendí ese mensaje. Usá /menu para ver las opciones.");
            return;
        }

        String campoActual = sesion.campoActual();
        String error = validar(campoActual, texto);
        if (error != null) {
            enviarTexto(chatId, error);
            return;
        }

        boolean completo = sesion.registrarRespuestaYAvanzar(texto);
        if (!completo) {
            enviarTexto(chatId, sesion.campoActual() + ":");
            return;
        }

        String resultado = ejecutarAccion(sesion);
        sesion.finalizarAccion();
        enviarTexto(chatId, resultado);
        enviarMenuOSeleccion(chatId, sesion);
    }

    // ---- Manejo de botones ----

    private void manejarCallback(CallbackQuery callback) {
        Long chatId = callback.getMessage().getChatId();
        String data = callback.getData();
        SesionUsuario sesion = sesionManager.obtener(chatId);

        try {
            execute(new AnswerCallbackQuery(callback.getId()));
        } catch (TelegramApiException e) {
            // Solo apaga el "cargando..." del botón - no es crítico si falla.
        }

        if (data.startsWith("modulo:")) {
            Modulo modulo = Modulo.valueOf(data.substring("modulo:".length()));
            sesion.setModulo(modulo);
            sesion.setTipoUsuario(null);
            if (modulo == Modulo.DONADORES) {
                enviarSeleccionTipo(chatId);
            } else {
                enviarMenu(chatId, sesion);
            }
            return;
        }

        if (data.startsWith("tipo:")) {
            TipoUsuario tipo = TipoUsuario.valueOf(data.substring("tipo:".length()));
            sesion.setTipoUsuario(tipo);
            enviarMenu(chatId, sesion);
            return;
        }

        if (data.startsWith("accion:")) {
            Accion accion = Accion.valueOf(data.substring("accion:".length()));
            sesion.iniciarAccion(accion);
            if (accion.cantidadCampos() == 0) {
                String resultado = ejecutarAccion(sesion);
                sesion.finalizarAccion();
                enviarTexto(chatId, resultado);
                enviarMenuOSeleccion(chatId, sesion);
            } else {
                enviarTexto(chatId, sesion.campoActual() + ":");
            }
        }
    }

    // ---- Validación simple de campos numéricos / enum, genérica para los 4 módulos ----

    private String validar(String campo, String valor) {
        try {
            switch (campo) {
                case "Edad":
                case "Nivel de urgencia (1-10)":
                case "Cantidad objetivo":
                case "Cantidad a donar":
                case "Cantidad entregada":
                case "Capacidad máxima":
                    Integer.parseInt(valor.trim());
                    return null;
                case "ID de la donación":
                case "ID del producto (Donaciones)":
                case "ID de categoría":
                case "ID de identificador":
                    Long.parseLong(valor.trim());
                    return null;
                case "Tipo (EXTRAORDINARIA o RECURRENTE)":
                    TipoNecesidadMaterialEnum.valueOf(valor.trim().toUpperCase());
                    return null;
                case "Nuevo estado (INGRESADA, ACEPTADA o CONQUEJA)":
                    EstadoDonacionEnum.valueOf(valor.trim().toUpperCase());
                    return null;
                case "Tipo (QR o CODIGODEBARRAS)":
                    TipoIdentificadorEnum.valueOf(valor.trim().toUpperCase());
                    return null;
                case "Algoritmo (SUB_ATENDIDOS o PRIORIDAD_POR_SCORE)":
                    TipoAlgoritmoEnum.valueOf(valor.trim().toUpperCase());
                    return null;
                case "Categoría inicio (OCASIONAL, COLABORADOR, TRANSFORMADOR, SALVADOR o REVOLUCIONARIO)":
                case "Categoría fin (OCASIONAL, COLABORADOR, TRANSFORMADOR, SALVADOR o REVOLUCIONARIO)":
                    CategoriaDonadorEnum.valueOf(valor.trim().toUpperCase());
                    return null;
                case "Tipo (COMPLETITUD, DONACIONES_EXITOSAS, DONACIONES_ASCENDENTES o REVOLUCION_DONADORA)":
                    TipoMisionEnum.valueOf(valor.trim().toUpperCase());
                    return null;
                default:
                    return null;
            }
        } catch (IllegalArgumentException e) {
            return "Ese valor no es válido para \"" + campo + "\". Probá de nuevo - " + campo + ":";
        }
    }

    // ---- Ejecución de la acción, despachando al handler del módulo que corresponda ----

    private String ejecutarAccion(SesionUsuario sesion) {
        Accion accion = sesion.getAccionActual();
        try {
            return switch (accion.getModulo()) {
                case DONADORES -> donadoresHandler.ejecutar(accion, sesion);
                case DONACIONES -> donacionesHandler.ejecutar(accion, sesion);
                case LOGISTICA -> logisticaHandler.ejecutar(accion, sesion);
                case INCENTIVOS -> incentivosHandler.ejecutar(accion, sesion);
            };
        } catch (RestClientResponseException e) {
            return nombreServicio(accion.getModulo()) + " respondió con error (" + e.getStatusCode().value() + "): "
                    + e.getResponseBodyAsString();
        } catch (RestClientException e) {
            return "No pude conectarme con " + nombreServicio(accion.getModulo()) + ". ¿Está levantado el servicio?";
        }
    }

    private String nombreServicio(Modulo modulo) {
        return switch (modulo) {
            case DONADORES -> "Donadores y Entidades";
            case DONACIONES -> "Donaciones";
            case LOGISTICA -> "Logística";
            case INCENTIVOS -> "Incentivos";
        };
    }

    // ---- Menús ----

    private void enviarMenuOSeleccion(Long chatId, SesionUsuario sesion) {
        if (sesion.getModulo() == null) {
            enviarSeleccionModulo(chatId);
        } else if (sesion.getModulo() == Modulo.DONADORES && sesion.getTipoUsuario() == null) {
            enviarSeleccionTipo(chatId);
        } else {
            enviarMenu(chatId, sesion);
        }
    }

    private void enviarSeleccionModulo(Long chatId) {
        InlineKeyboardMarkup teclado = new InlineKeyboardMarkup(List.of(
                List.of(boton("Donadores y Entidades", "modulo:DONADORES")),
                List.of(boton("Donaciones", "modulo:DONACIONES")),
                List.of(boton("Logística", "modulo:LOGISTICA")),
                List.of(boton("Incentivos", "modulo:INCENTIVOS"))));
        enviarConTeclado(chatId, "¡Hola! Soy el bot de DonaTrack. ¿Con qué módulo querés hablar?", teclado);
    }

    private void enviarSeleccionTipo(Long chatId) {
        InlineKeyboardMarkup teclado = new InlineKeyboardMarkup(List.of(
                List.of(boton("Soy Donador", "tipo:DONADOR")),
                List.of(boton("Soy Admin", "tipo:ADMIN"))));
        enviarConTeclado(chatId, "¿Cómo querés entrar a Donadores y Entidades?", teclado);
    }

    private void enviarMenu(Long chatId, SesionUsuario sesion) {
        InlineKeyboardMarkup teclado = switch (sesion.getModulo()) {
            case DONADORES -> sesion.getTipoUsuario() == TipoUsuario.DONADOR ? menuDonadoresDonador() : menuDonadoresAdmin();
            case DONACIONES -> menuDonaciones();
            case LOGISTICA -> menuLogistica();
            case INCENTIVOS -> menuIncentivos();
        };
        enviarConTeclado(chatId, "¿Qué querés hacer?", teclado);
    }

    private InlineKeyboardMarkup menuDonadoresDonador() {
        return new InlineKeyboardMarkup(List.of(
                List.of(boton("Registrarme", "accion:REGISTRAR_DONADOR")),
                List.of(boton("Mis estadísticas", "accion:MIS_ESTADISTICAS")),
                List.of(boton("Buscar donador por ID", "accion:BUSCAR_DONADOR")),
                List.of(boton("Listar donadores", "accion:LISTAR_DONADORES"))));
    }

    private InlineKeyboardMarkup menuDonadoresAdmin() {
        return new InlineKeyboardMarkup(List.of(
                List.of(boton("Crear entidad", "accion:CREAR_ENTIDAD")),
                List.of(boton("Editar entidad", "accion:EDITAR_ENTIDAD")),
                List.of(boton("Buscar entidad por ID", "accion:BUSCAR_ENTIDAD")),
                List.of(boton("Listar entidades", "accion:LISTAR_ENTIDADES")),
                List.of(boton("Alta de necesidad", "accion:ALTA_NECESIDAD")),
                List.of(boton("Modificar necesidad", "accion:MODIFICAR_NECESIDAD")),
                List.of(boton("Borrar necesidad", "accion:BORRAR_NECESIDAD")),
                List.of(boton("Buscar necesidad por ID", "accion:BUSCAR_NECESIDAD"))));
    }

    private InlineKeyboardMarkup menuDonaciones() {
        return new InlineKeyboardMarkup(List.of(
                List.of(boton("Registrar donación", "accion:REGISTRAR_DONACION")),
                List.of(boton("Consultar donación por ID", "accion:CONSULTAR_DONACION")),
                List.of(boton("Listar donaciones", "accion:LISTAR_DONACIONES")),
                List.of(boton("Listar donaciones de un donador", "accion:LISTAR_DONACIONES_DE_DONADOR")),
                List.of(boton("Cambiar estado de donación", "accion:CAMBIAR_ESTADO_DONACION")),
                List.of(boton("Registrar queja en donación", "accion:REGISTRAR_QUEJA_DONACION")),
                List.of(boton("Crear producto", "accion:CREAR_PRODUCTO")),
                List.of(boton("Listar productos", "accion:LISTAR_PRODUCTOS")),
                List.of(boton("Crear categoría", "accion:CREAR_CATEGORIA")),
                List.of(boton("Crear identificador", "accion:CREAR_IDENTIFICADOR"))));
    }

    private InlineKeyboardMarkup menuLogistica() {
        return new InlineKeyboardMarkup(List.of(
                List.of(boton("Crear depósito", "accion:CREAR_DEPOSITO")),
                List.of(boton("Listar depósitos", "accion:LISTAR_DEPOSITOS")),
                List.of(boton("Buscar depósito por ID", "accion:BUSCAR_DEPOSITO")),
                List.of(boton("Configurar algoritmo de un depósito", "accion:CONFIGURAR_ALGORITMO")),
                List.of(boton("Consultar stock de un producto", "accion:CONSULTAR_STOCK")),
                List.of(boton("Listar asignaciones", "accion:LISTAR_ASIGNACIONES")),
                List.of(boton("Reportar entrega de un paquete", "accion:REPORTAR_ENTREGA"))));
    }

    private InlineKeyboardMarkup menuIncentivos() {
        return new InlineKeyboardMarkup(List.of(
                List.of(boton("Crear misión", "accion:CREAR_MISION")),
                List.of(boton("Crear insignia", "accion:CREAR_INSIGNIA")),
                List.of(boton("Asignar misión a donador", "accion:ASIGNAR_MISION")),
                List.of(boton("Procesar donador", "accion:PROCESAR_DONADOR")),
                List.of(boton("Consultar progreso de un donador", "accion:CONSULTAR_PROGRESO"))));
    }

    private InlineKeyboardButton boton(String texto, String callbackData) {
        InlineKeyboardButton boton = new InlineKeyboardButton(texto);
        boton.setCallbackData(callbackData);
        return boton;
    }

    // ---- Envío de mensajes ----

    private void enviarTexto(Long chatId, String texto) {
        SendMessage mensaje = new SendMessage();
        mensaje.setChatId(chatId.toString());
        mensaje.setText(texto);
        try {
            execute(mensaje);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void enviarConTeclado(Long chatId, String texto, InlineKeyboardMarkup teclado) {
        SendMessage mensaje = new SendMessage();
        mensaje.setChatId(chatId.toString());
        mensaje.setText(texto);
        mensaje.setReplyMarkup(teclado);
        try {
            execute(mensaje);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
