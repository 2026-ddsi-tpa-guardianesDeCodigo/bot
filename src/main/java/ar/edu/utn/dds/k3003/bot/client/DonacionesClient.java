package ar.edu.utn.dds.k3003.bot.client;

import ar.edu.utn.dds.k3003.bot.dtos.donaciones.CategoriaDTO;
import ar.edu.utn.dds.k3003.bot.dtos.donaciones.DonacionDTO;
import ar.edu.utn.dds.k3003.bot.dtos.donaciones.EstadoDonacionEnum;
import ar.edu.utn.dds.k3003.bot.dtos.donaciones.IdentificadorDTO;
import ar.edu.utn.dds.k3003.bot.dtos.donaciones.ProductoDTO;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

// Cliente HTTP hacia Donaciones. Mismo patrón que DonadoresClient: JdkClientHttpRequestFactory
// (soporta PATCH, a diferencia del SimpleClientHttpRequestFactory por default) + timeout
// explícito para no colgarse si Render está en cold start.
@Component
public class DonacionesClient {

    private final RestClient restClient;

    public DonacionesClient(@Value("${donaciones.url}") String baseUrl) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(90));
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    public DonacionDTO registrarDonacion(DonacionDTO donacion) {
        return restClient.post().uri("/donaciones")
                .body(donacion).retrieve().body(DonacionDTO.class);
    }

    public DonacionDTO buscarDonacionPorID(Long id) {
        return restClient.get().uri("/donaciones/{id}", id)
                .retrieve().body(DonacionDTO.class);
    }

    public List<DonacionDTO> listarDonaciones() {
        DonacionDTO[] resultado = restClient.get().uri("/donaciones")
                .retrieve().body(DonacionDTO[].class);
        return Arrays.asList(resultado);
    }

    public List<DonacionDTO> listarPorDonador(String donadorID) {
        DonacionDTO[] resultado = restClient.get().uri("/donaciones/buscarPorDonador?donadorID={id}", donadorID)
                .retrieve().body(DonacionDTO[].class);
        return Arrays.asList(resultado);
    }

    public DonacionDTO cambiarEstado(Long donacionID, EstadoDonacionEnum estado) {
        return restClient.patch().uri("/donaciones/estado?donacionID={id}", donacionID)
                .body(estado).retrieve().body(DonacionDTO.class);
    }

    public DonacionDTO registrarQueja(Long donacionID, String descripcion) {
        return restClient.patch().uri("/donaciones/queja?donacionID={id}&descripcion={d}", donacionID, descripcion)
                .retrieve().body(DonacionDTO.class);
    }

    public ProductoDTO crearProducto(ProductoDTO producto) {
        return restClient.post().uri("/productos")
                .body(producto).retrieve().body(ProductoDTO.class);
    }

    public List<ProductoDTO> listarProductos() {
        ProductoDTO[] resultado = restClient.get().uri("/productos")
                .retrieve().body(ProductoDTO[].class);
        return Arrays.asList(resultado);
    }

    public CategoriaDTO crearCategoria(CategoriaDTO categoria) {
        return restClient.post().uri("/categorias")
                .body(categoria).retrieve().body(CategoriaDTO.class);
    }

    public IdentificadorDTO crearIdentificador(IdentificadorDTO identificador) {
        return restClient.post().uri("/identificadores")
                .body(identificador).retrieve().body(IdentificadorDTO.class);
    }
}
