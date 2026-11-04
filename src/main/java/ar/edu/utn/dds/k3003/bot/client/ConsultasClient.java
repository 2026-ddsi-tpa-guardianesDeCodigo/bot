package ar.edu.utn.dds.k3003.bot.client;

import com.fasterxml.jackson.databind.JsonNode;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

// Cliente HTTP hacia el Servicio de Consultas. Devuelve el JSON tal cual: el bot solo lo muestra y
// así no duplica el modelo de lectura en DTOs. consultas.url no lleva /consultas porque ya viene en
// el path de cada endpoint, y el gateway le deja ese segmento al servicio.
@Component
public class ConsultasClient {

    private static final int MAXIMO_DE_NECESIDADES = 10;

    private final RestClient restClient;

    public ConsultasClient(@Value("${consultas.url}") String baseUrl) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(90));
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    public JsonNode fichaDonador(String donadorID) {
        return restClient.get().uri("/consultas/donadores/{id}/ficha", donadorID)
                .retrieve().body(JsonNode.class);
    }

    public JsonNode necesidadesUrgentes(int urgenciaMinima) {
        return restClient.get()
                .uri("/consultas/necesidades/urgentes?urgenciaMin={urgencia}&size={size}", urgenciaMinima, MAXIMO_DE_NECESIDADES)
                .retrieve().body(JsonNode.class);
    }
}
