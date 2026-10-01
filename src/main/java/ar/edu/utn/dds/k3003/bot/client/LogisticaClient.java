package ar.edu.utn.dds.k3003.bot.client;

import ar.edu.utn.dds.k3003.bot.dtos.logistica.AsignacionDTO;
import ar.edu.utn.dds.k3003.bot.dtos.logistica.DepositoDTO;
import ar.edu.utn.dds.k3003.bot.dtos.logistica.PaqueteDTO;
import ar.edu.utn.dds.k3003.bot.dtos.logistica.StockDisponibleDTO;
import ar.edu.utn.dds.k3003.bot.dtos.logistica.TipoAlgoritmoEnum;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

// Cliente HTTP hacia Logística. Mismo patrón que DonadoresClient (§bot/client), timeout largo por
// el cold start de Render (30-90s en el plan free).
@Component
public class LogisticaClient {

    private final RestClient restClient;

    public LogisticaClient(@Value("${logistica.url}") String baseUrl) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(90));
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    public DepositoDTO crearDeposito(DepositoDTO deposito) {
        return restClient.post().uri("/depositos")
                .body(deposito).retrieve().body(DepositoDTO.class);
    }

    public List<DepositoDTO> listarDepositos() {
        DepositoDTO[] resultado = restClient.get().uri("/depositos")
                .retrieve().body(DepositoDTO[].class);
        return Arrays.asList(resultado);
    }

    public DepositoDTO buscarDepositoPorID(String id) {
        return restClient.get().uri("/depositos/{id}", id)
                .retrieve().body(DepositoDTO.class);
    }

    public void configurarAlgoritmo(String depositoID, TipoAlgoritmoEnum algoritmo) {
        restClient.patch().uri("/depositos/{id}/algoritmo", depositoID)
                .body(algoritmo).retrieve().toBodilessEntity();
    }

    public StockDisponibleDTO consultarStock(String productoID) {
        return restClient.get().uri("/stock?productoID={id}", productoID)
                .retrieve().body(StockDisponibleDTO.class);
    }

    public List<AsignacionDTO> listarAsignaciones() {
        AsignacionDTO[] resultado = restClient.get().uri("/asignaciones")
                .retrieve().body(AsignacionDTO[].class);
        return Arrays.asList(resultado);
    }

    public void reportarEntrega(PaqueteDTO paquete) {
        restClient.post().uri("/entregas")
                .body(paquete).retrieve().toBodilessEntity();
    }
}
