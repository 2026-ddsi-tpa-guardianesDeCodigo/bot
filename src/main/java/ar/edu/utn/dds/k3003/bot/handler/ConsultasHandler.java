package ar.edu.utn.dds.k3003.bot.handler;

import ar.edu.utn.dds.k3003.bot.client.ConsultasClient;
import ar.edu.utn.dds.k3003.bot.session.Accion;
import ar.edu.utn.dds.k3003.bot.session.SesionUsuario;

import com.fasterxml.jackson.databind.JsonNode;

import org.springframework.stereotype.Component;

// Ejecuta las consultas del modelo de lectura (Entrega 6, PLAN §8.2).
@Component
public class ConsultasHandler {

    private final ConsultasClient client;

    public ConsultasHandler(ConsultasClient client) {
        this.client = client;
    }

    public String ejecutar(Accion accion, SesionUsuario sesion) {
        switch (accion) {
            case FICHA_DONADOR -> {
                return formatFicha(client.fichaDonador(sesion.getRespuesta("ID del donador para la ficha").trim()));
            }
            case NECESIDADES_URGENTES -> {
                int urgencia = Integer.parseInt(sesion.getRespuesta("Urgencia mínima (1-10)").trim());
                return formatNecesidades(client.necesidadesUrgentes(urgencia));
            }
            default -> {
                return "Acción no soportada.";
            }
        }
    }

    private String formatFicha(JsonNode ficha) {
        StringBuilder sb = new StringBuilder();
        sb.append("Ficha de ").append(texto(ficha, "nombre"))
                .append("\nEstado: ").append(texto(ficha, "estado"))
                .append("\nCategoría: ").append(texto(ficha, "categoria"))
                .append("\nHistorial: ");
        for (JsonNode cambio : ficha.path("historialCategorias")) {
            sb.append(texto(cambio, "categoria")).append(" ");
        }
        sb.append("\nInsignias: ").append(ficha.path("insignias").isEmpty() ? "ninguna" : unir(ficha.path("insignias")));

        JsonNode mision = ficha.path("misionEnCurso");
        sb.append("\nMisión en curso: ").append(mision.isObject()
                ? texto(mision, "nombre") + " (" + texto(mision, "progreso") + " de " + texto(mision, "objetivo") + ")"
                : "ninguna");

        sb.append("\nDonaciones:");
        ficha.path("donacionesPorEstado").fields().forEachRemaining(estado ->
                sb.append("\n• ").append(estado.getKey()).append(": ").append(estado.getValue().asText()));
        sb.append("\nQuejas recibidas: ").append(ficha.path("quejasRecibidas").asInt());

        sb.append("\nEntidades beneficiadas:");
        for (JsonNode entidad : ficha.path("entidadesBeneficiadas")) {
            sb.append("\n• ").append(texto(entidad, "razonSocial")).append(": ")
                    .append(texto(entidad, "unidades")).append(" unidades");
        }
        return sb.toString();
    }

    private String formatNecesidades(JsonNode respuesta) {
        JsonNode necesidades = respuesta.path("necesidades");
        if (necesidades.isEmpty()) return "No hay necesidades urgentes sin cubrir.";
        StringBuilder sb = new StringBuilder("Necesidades sin cubrir: " + respuesta.path("total").asInt());
        for (JsonNode necesidad : necesidades) {
            sb.append("\n\n• ").append(texto(necesidad.path("entidad"), "razonSocial"))
                    .append(": ").append(texto(necesidad.path("producto"), "nombre"))
                    .append(" (urgencia ").append(texto(necesidad, "nivelDeUrgencia")).append(")")
                    .append("\nFaltan ").append(texto(necesidad, "faltante"))
                    .append(", asignadas sin entregar: ").append(texto(necesidad, "asignadoPendiente"))
                    .append("\nStock: ").append(formatStock(necesidad.path("stockPorDeposito")));
        }
        return sb.toString();
    }

    private String formatStock(JsonNode depositos) {
        if (depositos.isEmpty()) return "sin stock";
        StringBuilder sb = new StringBuilder();
        for (JsonNode deposito : depositos) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(texto(deposito, "deposito")).append(": ").append(texto(deposito, "cantidad"));
        }
        return sb.toString();
    }

    private String unir(JsonNode valores) {
        StringBuilder sb = new StringBuilder();
        for (JsonNode valor : valores) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(valor.asText());
        }
        return sb.toString();
    }

    // El modelo de lectura puede tener datos a medio llegar: se muestra un guion en lugar de "null".
    private String texto(JsonNode nodo, String campo) {
        return nodo.path(campo).asText("-");
    }
}
