package ar.edu.utn.dds.k3003.bot.client;

import ar.edu.utn.dds.k3003.bot.dtos.incentivos.InsigniaDTO;
import ar.edu.utn.dds.k3003.bot.dtos.incentivos.MisionDTO;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;

// Cliente HTTP hacia Incentivos. Mismo patrón que DonadoresClient (§bot/client), timeout largo
// por el cold start de Render (30-90s en el plan free).
@Component
public class IncentivosClient {

    private final RestClient restClient;

    public IncentivosClient(@Value("${incentivos.url}") String baseUrl) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(90));
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    public MisionDTO crearMision(MisionDTO mision) {
        return restClient.post().uri("/misiones")
                .body(mision).retrieve().body(MisionDTO.class);
    }

    public MisionDTO buscarMisionPorID(String id) {
        return restClient.get().uri("/misiones/{id}", id)
                .retrieve().body(MisionDTO.class);
    }

    public InsigniaDTO crearInsignia(InsigniaDTO insignia) {
        return restClient.post().uri("/insignias")
                .body(insignia).retrieve().body(InsigniaDTO.class);
    }

    public void asignarMisionADonador(String donadorID, MisionDTO mision) {
        restClient.post().uri("/donadores/{id}/misiones", donadorID)
                .body(mision).retrieve().toBodilessEntity();
    }

    public void procesarDonador(String donadorID) {
        restClient.post().uri("/donadores/{id}/procesar", donadorID)
                .retrieve().toBodilessEntity();
    }

    // La fachada devuelve 200 con body null cuando el donador no tiene misión en curso: RestClient
    // deserializa eso como null sin problema, así que el handler solo tiene que chequear null.
    public MisionDTO misionEnCurso(String donadorID) {
        return restClient.get().uri("/donadores/{id}/misiones", donadorID)
                .retrieve().body(MisionDTO.class);
    }

    public List<InsigniaDTO> insigniasDeDonador(String donadorID) {
        InsigniaDTO[] resultado = restClient.get().uri("/donadores/{id}/insignias", donadorID)
                .retrieve().body(InsigniaDTO[].class);
        return List.of(resultado);
    }
}
