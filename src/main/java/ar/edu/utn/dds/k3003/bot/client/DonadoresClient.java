package ar.edu.utn.dds.k3003.bot.client;

import ar.edu.utn.dds.k3003.bot.dtos.DonadorDTO;
import ar.edu.utn.dds.k3003.bot.dtos.DonadorStatsDTO;
import ar.edu.utn.dds.k3003.bot.dtos.EntidadBeneficaDTO;
import ar.edu.utn.dds.k3003.bot.dtos.NecesidadMaterialDTO;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.List;

// Único cliente saliente del bot: todo lo que pide la tabla de Entrega 4 (§8.4) toca la fachada
// de Donadores y Entidades, no las de los otros 3 componentes.
@Component
public class DonadoresClient {

    private final RestClient restClient;

    public DonadoresClient(@Value("${donadores.url}") String baseUrl) {
        // RestClient no tiene timeout por default: sin esto, si Donadores no responde el bot
        // queda colgado indefinido en vez de degradar con un mensaje de error.
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    public DonadorDTO agregarDonador(DonadorDTO donador) {
        return restClient.post().uri("/donadores")
                .body(donador).retrieve().body(DonadorDTO.class);
    }

    public DonadorDTO buscarDonadorPorID(String id) {
        return restClient.get().uri("/donadores/{id}", id)
                .retrieve().body(DonadorDTO.class);
    }

    public List<DonadorDTO> obtenerDonadores() {
        DonadorDTO[] resultado = restClient.get().uri("/donadores")
                .retrieve().body(DonadorDTO[].class);
        return Arrays.asList(resultado);
    }

    public DonadorStatsDTO estadisticasDonador(String id) {
        return restClient.get().uri("/donadores/{id}/estadisticas", id)
                .retrieve().body(DonadorStatsDTO.class);
    }

    public EntidadBeneficaDTO agregarEntidad(EntidadBeneficaDTO entidad) {
        return restClient.post().uri("/entidades")
                .body(entidad).retrieve().body(EntidadBeneficaDTO.class);
    }

    public EntidadBeneficaDTO editarEntidad(String id, EntidadBeneficaDTO entidad) {
        return restClient.patch().uri("/entidades/{id}", id)
                .body(entidad).retrieve().body(EntidadBeneficaDTO.class);
    }

    public EntidadBeneficaDTO buscarEntidadPorID(String id) {
        return restClient.get().uri("/entidades/{id}", id)
                .retrieve().body(EntidadBeneficaDTO.class);
    }

    public List<EntidadBeneficaDTO> obtenerEntidades() {
        EntidadBeneficaDTO[] resultado = restClient.get().uri("/entidades")
                .retrieve().body(EntidadBeneficaDTO[].class);
        return Arrays.asList(resultado);
    }

    public NecesidadMaterialDTO registrarNecesidad(NecesidadMaterialDTO necesidad) {
        return restClient.post().uri("/necesidades")
                .body(necesidad).retrieve().body(NecesidadMaterialDTO.class);
    }

    public NecesidadMaterialDTO buscarNecesidadPorID(String id) {
        return restClient.get().uri("/necesidades/{id}", id)
                .retrieve().body(NecesidadMaterialDTO.class);
    }

    public NecesidadMaterialDTO editarNecesidad(String id, NecesidadMaterialDTO necesidad) {
        return restClient.patch().uri("/necesidades/{id}", id)
                .body(necesidad).retrieve().body(NecesidadMaterialDTO.class);
    }

    public NecesidadMaterialDTO borrarNecesidad(String id) {
        return restClient.delete().uri("/necesidades/{id}", id)
                .retrieve().body(NecesidadMaterialDTO.class);
    }
}
