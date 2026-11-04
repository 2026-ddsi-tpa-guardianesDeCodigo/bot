package ar.edu.utn.dds.k3003.bot.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Usa las propiedades reales del bot contra un gateway stub: si cambia un prefijo, se rompe acá y
 * no recién en la demo. Levanta solo los clientes para no conectarse a Telegram.
 */
@SpringBootTest(
        classes = {
            DonadoresClient.class,
            DonacionesClient.class,
            LogisticaClient.class,
            IncentivosClient.class
        },
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
class RutasDelGatewayTest {

    private static final Queue<String> RUTAS = new ConcurrentLinkedQueue<>();
    private static HttpServer gateway;

    @Autowired
    private DonadoresClient donadores;

    @Autowired
    private DonacionesClient donaciones;

    @Autowired
    private LogisticaClient logistica;

    @Autowired
    private IncentivosClient incentivos;

    @DynamicPropertySource
    static void gateway(DynamicPropertyRegistry registro) throws IOException {
        gateway = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        gateway.createContext("/", intercambio -> {
            RUTAS.add(intercambio.getRequestURI().toString());
            String ruta = intercambio.getRequestURI().getPath();
            byte[] cuerpo = (ruta.endsWith("/1") ? "{}" : "[]").getBytes(StandardCharsets.UTF_8);
            intercambio.getResponseHeaders().set("Content-Type", "application/json");
            intercambio.sendResponseHeaders(200, cuerpo.length);
            intercambio.getResponseBody().write(cuerpo);
            intercambio.close();
        });
        gateway.start();
        registro.add("gateway.url", () -> "http://localhost:" + gateway.getAddress().getPort());
    }

    @AfterAll
    static void apagar() {
        gateway.stop(0);
    }

    @BeforeEach
    void limpiar() {
        RUTAS.clear();
    }

    @Test
    void cadaClienteEntraPorElPrefijoDeSuModulo() {
        donadores.buscarDonadorPorID("1");
        donaciones.listarDonaciones();
        logistica.listarDepositos();
        incentivos.buscarMisionPorID("1");

        assertEquals(
                List.of(
                        "/api/donadores-entidades/donadores/1",
                        "/api/donaciones/donaciones",
                        "/api/logistica/depositos",
                        "/api/incentivos/misiones/1"),
                List.copyOf(RUTAS));
    }
}
