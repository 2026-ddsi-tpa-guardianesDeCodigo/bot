package ar.edu.utn.dds.k3003.bot;

import ar.edu.utn.dds.k3003.bot.client.DonadoresClient;
import ar.edu.utn.dds.k3003.bot.dtos.DonadorDTO;
import ar.edu.utn.dds.k3003.bot.dtos.DonadorStatsDTO;
import ar.edu.utn.dds.k3003.bot.dtos.EntidadBeneficaDTO;
import ar.edu.utn.dds.k3003.bot.dtos.EstadoDonadorEnum;
import ar.edu.utn.dds.k3003.bot.dtos.NecesidadMaterialDTO;
import ar.edu.utn.dds.k3003.bot.dtos.TipoNecesidadMaterialEnum;
import ar.edu.utn.dds.k3003.bot.session.Accion;
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

// Capa de presentación pura: solo habla por REST con Donadores y Entidades (vía
// DonadoresClient), nunca con el dominio directo ni con los otros 3 componentes - mismo rol que
// cumple el bot en la práctica Copia.me a partir de su 3ra iteración.
@Component
public class DonaTrackBot extends TelegramLongPollingBot {

    private final String username;
    private final DonadoresClient donadoresClient;
    private final SesionManager sesionManager;

    public DonaTrackBot(
            @Value("${telegram.bot.token}") String token,
            @Value("${telegram.bot.username}") String username,
            DonadoresClient donadoresClient,
            SesionManager sesionManager) {
        super(token);
        this.username = username;
        this.donadoresClient = donadoresClient;
        this.sesionManager = sesionManager;
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
            enviarSeleccionTipo(chatId);
            return;
        }
        if (texto.equalsIgnoreCase("/menu")) {
            sesion.finalizarAccion();
            enviarMenuOSeleccionTipo(chatId, sesion);
            return;
        }
        if (texto.equalsIgnoreCase("/cancelar")) {
            sesion.finalizarAccion();
            enviarTexto(chatId, "Cancelado.");
            enviarMenuOSeleccionTipo(chatId, sesion);
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
        enviarMenuOSeleccionTipo(chatId, sesion);
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

        if (data.startsWith("tipo:")) {
            TipoUsuario tipo = TipoUsuario.valueOf(data.substring("tipo:".length()));
            sesion.setTipoUsuario(tipo);
            enviarMenu(chatId, tipo);
            return;
        }

        if (data.startsWith("accion:")) {
            Accion accion = Accion.valueOf(data.substring("accion:".length()));
            sesion.iniciarAccion(accion);
            if (accion.cantidadCampos() == 0) {
                String resultado = ejecutarAccion(sesion);
                sesion.finalizarAccion();
                enviarTexto(chatId, resultado);
                enviarMenuOSeleccionTipo(chatId, sesion);
            } else {
                enviarTexto(chatId, sesion.campoActual() + ":");
            }
        }
    }

    // ---- Validación simple de campos numéricos / enum ----

    private String validar(String campo, String valor) {
        switch (campo) {
            case "Edad":
            case "Nivel de urgencia (1-10)":
            case "Cantidad objetivo":
                try {
                    Integer.parseInt(valor.trim());
                } catch (NumberFormatException e) {
                    return "Eso no es un número válido. Probá de nuevo - " + campo + ":";
                }
                return null;
            case "Tipo (EXTRAORDINARIA o RECURRENTE)":
                try {
                    TipoNecesidadMaterialEnum.valueOf(valor.trim().toUpperCase());
                } catch (IllegalArgumentException e) {
                    return "Tiene que ser EXTRAORDINARIA o RECURRENTE. Probá de nuevo:";
                }
                return null;
            default:
                return null;
        }
    }

    // ---- Ejecución de la acción contra Donadores y Entidades ----

    private String ejecutarAccion(SesionUsuario sesion) {
        Accion accion = sesion.getAccionActual();
        try {
            switch (accion) {
                case REGISTRAR_DONADOR -> {
                    DonadorDTO nuevo = new DonadorDTO(null,
                            sesion.getRespuesta("Nombre"), sesion.getRespuesta("Apellido"),
                            Integer.parseInt(sesion.getRespuesta("Edad")), sesion.getRespuesta("Email"),
                            sesion.getRespuesta("Número de documento"), sesion.getRespuesta("Domicilio"),
                            EstadoDonadorEnum.VERIFICADO, "OCASIONAL");
                    return "Listo, te registré:\n" + formatDonador(donadoresClient.agregarDonador(nuevo));
                }
                case MIS_ESTADISTICAS -> {
                    return formatStats(donadoresClient.estadisticasDonador(sesion.getRespuesta("Tu ID de donador")));
                }
                case BUSCAR_DONADOR -> {
                    return formatDonador(donadoresClient.buscarDonadorPorID(sesion.getRespuesta("ID del donador a buscar")));
                }
                case LISTAR_DONADORES -> {
                    return formatListaDonadores(donadoresClient.obtenerDonadores());
                }
                case CREAR_ENTIDAD -> {
                    EntidadBeneficaDTO nueva = new EntidadBeneficaDTO(null,
                            sesion.getRespuesta("Razón social"), sesion.getRespuesta("Domicilio"),
                            sesion.getRespuesta("Teléfono"), sesion.getRespuesta("Correo"));
                    return "Entidad creada:\n" + formatEntidad(donadoresClient.agregarEntidad(nueva));
                }
                case EDITAR_ENTIDAD -> {
                    String id = sesion.getRespuesta("ID de la entidad a editar");
                    EntidadBeneficaDTO editada = new EntidadBeneficaDTO(id,
                            sesion.getRespuesta("Razón social"), sesion.getRespuesta("Domicilio"),
                            sesion.getRespuesta("Teléfono"), sesion.getRespuesta("Correo"));
                    return "Entidad editada:\n" + formatEntidad(donadoresClient.editarEntidad(id, editada));
                }
                case BUSCAR_ENTIDAD -> {
                    return formatEntidad(donadoresClient.buscarEntidadPorID(sesion.getRespuesta("ID de la entidad a buscar")));
                }
                case LISTAR_ENTIDADES -> {
                    return formatListaEntidades(donadoresClient.obtenerEntidades());
                }
                case ALTA_NECESIDAD -> {
                    NecesidadMaterialDTO nueva = new NecesidadMaterialDTO(null,
                            sesion.getRespuesta("ID de la entidad"),
                            Integer.parseInt(sesion.getRespuesta("Nivel de urgencia (1-10)")),
                            sesion.getRespuesta("Descripción"),
                            Integer.parseInt(sesion.getRespuesta("Cantidad objetivo")),
                            sesion.getRespuesta("ID del producto solicitado"),
                            TipoNecesidadMaterialEnum.valueOf(
                                    sesion.getRespuesta("Tipo (EXTRAORDINARIA o RECURRENTE)").trim().toUpperCase()));
                    return "Necesidad creada:\n" + formatNecesidad(donadoresClient.registrarNecesidad(nueva));
                }
                case MODIFICAR_NECESIDAD -> {
                    String id = sesion.getRespuesta("ID de la necesidad a modificar");
                    NecesidadMaterialDTO editada = new NecesidadMaterialDTO(id,
                            sesion.getRespuesta("ID de la entidad"),
                            Integer.parseInt(sesion.getRespuesta("Nivel de urgencia (1-10)")),
                            sesion.getRespuesta("Descripción"),
                            Integer.parseInt(sesion.getRespuesta("Cantidad objetivo")),
                            sesion.getRespuesta("ID del producto solicitado"),
                            TipoNecesidadMaterialEnum.valueOf(
                                    sesion.getRespuesta("Tipo (EXTRAORDINARIA o RECURRENTE)").trim().toUpperCase()));
                    return "Necesidad modificada:\n" + formatNecesidad(donadoresClient.editarNecesidad(id, editada));
                }
                case BORRAR_NECESIDAD -> {
                    String id = sesion.getRespuesta("ID de la necesidad a borrar");
                    donadoresClient.borrarNecesidad(id);
                    return "Necesidad " + id + " borrada.";
                }
                case BUSCAR_NECESIDAD -> {
                    return formatNecesidad(donadoresClient.buscarNecesidadPorID(sesion.getRespuesta("ID de la necesidad a buscar")));
                }
                default -> {
                    return "Acción no soportada.";
                }
            }
        } catch (RestClientResponseException e) {
            return "Donadores y Entidades respondió con error (" + e.getStatusCode().value() + "): "
                    + e.getResponseBodyAsString();
        } catch (RestClientException e) {
            return "No pude conectarme con Donadores y Entidades. ¿Está levantado el servicio?";
        }
    }

    // ---- Menús ----

    private void enviarMenuOSeleccionTipo(Long chatId, SesionUsuario sesion) {
        if (sesion.getTipoUsuario() == null) {
            enviarSeleccionTipo(chatId);
        } else {
            enviarMenu(chatId, sesion.getTipoUsuario());
        }
    }

    private void enviarSeleccionTipo(Long chatId) {
        InlineKeyboardMarkup teclado = new InlineKeyboardMarkup(List.of(
                List.of(boton("Soy Donador", "tipo:DONADOR")),
                List.of(boton("Soy Admin", "tipo:ADMIN"))));
        enviarConTeclado(chatId, "¡Hola! Soy el bot de DonaTrack. ¿Cómo querés entrar?", teclado);
    }

    private void enviarMenu(Long chatId, TipoUsuario tipo) {
        InlineKeyboardMarkup teclado = tipo == TipoUsuario.DONADOR
                ? new InlineKeyboardMarkup(List.of(
                        List.of(boton("Registrarme", "accion:REGISTRAR_DONADOR")),
                        List.of(boton("Mis estadísticas", "accion:MIS_ESTADISTICAS")),
                        List.of(boton("Buscar donador por ID", "accion:BUSCAR_DONADOR")),
                        List.of(boton("Listar donadores", "accion:LISTAR_DONADORES"))))
                : new InlineKeyboardMarkup(List.of(
                        List.of(boton("Crear entidad", "accion:CREAR_ENTIDAD")),
                        List.of(boton("Editar entidad", "accion:EDITAR_ENTIDAD")),
                        List.of(boton("Buscar entidad por ID", "accion:BUSCAR_ENTIDAD")),
                        List.of(boton("Listar entidades", "accion:LISTAR_ENTIDADES")),
                        List.of(boton("Alta de necesidad", "accion:ALTA_NECESIDAD")),
                        List.of(boton("Modificar necesidad", "accion:MODIFICAR_NECESIDAD")),
                        List.of(boton("Borrar necesidad", "accion:BORRAR_NECESIDAD")),
                        List.of(boton("Buscar necesidad por ID", "accion:BUSCAR_NECESIDAD"))));
        enviarConTeclado(chatId, "¿Qué querés hacer?", teclado);
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

    // ---- Formateo de respuestas ----

    private String formatDonador(DonadorDTO d) {
        return "ID: " + d.id() + "\nNombre: " + d.nombre() + " " + d.apellido()
                + "\nEdad: " + d.edad() + "\nEmail: " + d.email()
                + "\nEstado: " + d.estado() + "\nCategoría: " + d.categoria();
    }

    private String formatListaDonadores(List<DonadorDTO> donadores) {
        if (donadores.isEmpty()) return "No hay donadores registrados todavía.";
        StringBuilder sb = new StringBuilder();
        for (DonadorDTO d : donadores) {
            sb.append("• ").append(d.id()).append(" - ").append(d.nombre()).append(" ")
                    .append(d.apellido()).append(" (").append(d.categoria()).append(")\n");
        }
        return sb.toString();
    }

    private String formatStats(DonadorStatsDTO s) {
        return "Nombre: " + s.nombre() + " " + s.apellido()
                + "\nEstado: " + s.estado() + "\nCategoría: " + s.categoria()
                + "\nMisión actual: " + (s.misionActualID() != null ? s.misionActualID() : "ninguna")
                + "\nInsignias: " + (s.insigniasID().isEmpty() ? "ninguna" : String.join(", ", s.insigniasID()));
    }

    private String formatEntidad(EntidadBeneficaDTO e) {
        return "ID: " + e.id() + "\nRazón social: " + e.razonSocial()
                + "\nDomicilio: " + e.domicilio() + "\nTeléfono: " + e.telefono()
                + "\nCorreo: " + e.correo();
    }

    private String formatListaEntidades(List<EntidadBeneficaDTO> entidades) {
        if (entidades.isEmpty()) return "No hay entidades registradas todavía.";
        StringBuilder sb = new StringBuilder();
        for (EntidadBeneficaDTO e : entidades) {
            sb.append("• ").append(e.id()).append(" - ").append(e.razonSocial()).append("\n");
        }
        return sb.toString();
    }

    private String formatNecesidad(NecesidadMaterialDTO n) {
        return "ID: " + n.id() + "\nEntidad: " + n.entidadID()
                + "\nUrgencia: " + n.nivelDeUrgencia() + "\nDescripción: " + n.descripcion()
                + "\nProducto solicitado: " + n.productoSolicitadoID()
                + "\nCantidad objetivo: " + n.cantidadObjetivo() + "\nTipo: " + n.tipo();
    }
}
