package ar.edu.utn.dds.k3003.bot.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ar.edu.utn.dds.k3003.bot.client.ConsultasClient;
import ar.edu.utn.dds.k3003.bot.session.Accion;
import ar.edu.utn.dds.k3003.bot.session.SesionUsuario;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class ConsultasHandlerTest {

    private final ConsultasClient client = mock(ConsultasClient.class);
    private final ConsultasHandler handler = new ConsultasHandler(client);

    @Test
    void laFichaMuestraLoQueCombinaElModeloDeLectura() throws Exception {
        when(client.fichaDonador("12")).thenReturn(json("""
                {"nombre":"Lucía","estado":"VERIFICADO","categoria":"COLABORADOR",
                 "historialCategorias":[{"categoria":"OCASIONAL","desde":null},{"categoria":"COLABORADOR"}],
                 "insignias":["Completitud"],
                 "misionEnCurso":{"nombre":"Donaciones Exitosas","progreso":14,"objetivo":20},
                 "donacionesPorEstado":{"ACEPTADA":14},
                 "quejasRecibidas":3,
                 "entidadesBeneficiadas":[{"razonSocial":"Comedor","unidades":120}]}"""));

        String ficha = handler.ejecutar(Accion.FICHA_DONADOR, sesion(Accion.FICHA_DONADOR, " 12 "));

        assertTrue(ficha.contains("Ficha de Lucía"), ficha);
        assertTrue(ficha.contains("Historial: OCASIONAL COLABORADOR"), ficha);
        assertTrue(ficha.contains("Insignias: Completitud"), ficha);
        assertTrue(ficha.contains("Donaciones Exitosas (14 de 20)"), ficha);
        assertTrue(ficha.contains("ACEPTADA: 14"), ficha);
        assertTrue(ficha.contains("Quejas recibidas: 3"), ficha);
        assertTrue(ficha.contains("Comedor: 120 unidades"), ficha);
    }

    @Test
    void unaFichaConDatosAMedioLlegarNoMuestraNull() throws Exception {
        when(client.fichaDonador("12")).thenReturn(json("""
                {"nombre":null,"estado":null,"categoria":null,"misionEnCurso":null,"insignias":[]}"""));

        String ficha = handler.ejecutar(Accion.FICHA_DONADOR, sesion(Accion.FICHA_DONADOR, "12"));

        assertTrue(ficha.contains("Misión en curso: ninguna"), ficha);
        assertTrue(ficha.contains("Insignias: ninguna"), ficha);
        assertTrue(!ficha.contains("null"), ficha);
    }

    @Test
    void lasNecesidadesMuestranEntidadFaltanteYStockPorDeposito() throws Exception {
        when(client.necesidadesUrgentes(7)).thenReturn(json("""
                {"total":1,"necesidades":[{
                  "nivelDeUrgencia":9,"faltante":60,"asignadoPendiente":30,
                  "entidad":{"razonSocial":"Comedor"},"producto":{"nombre":"Fideos"},
                  "stockPorDeposito":[{"deposito":"Medrano","cantidad":25},{"deposito":"Almagro","cantidad":5}]}]}"""));

        String necesidades = handler.ejecutar(Accion.NECESIDADES_URGENTES, sesion(Accion.NECESIDADES_URGENTES, "7"));

        assertTrue(necesidades.contains("Comedor: Fideos (urgencia 9)"), necesidades);
        assertTrue(necesidades.contains("Faltan 60, asignadas sin entregar: 30"), necesidades);
        assertTrue(necesidades.contains("Stock: Medrano: 25, Almagro: 5"), necesidades);
    }

    @Test
    void sinNecesidadesLoDiceEnLugarDeMostrarUnaListaVacia() throws Exception {
        when(client.necesidadesUrgentes(7)).thenReturn(json("""
                {"total":0,"necesidades":[]}"""));

        assertEquals("No hay necesidades urgentes sin cubrir.",
                handler.ejecutar(Accion.NECESIDADES_URGENTES, sesion(Accion.NECESIDADES_URGENTES, "7")));
    }

    private static SesionUsuario sesion(Accion accion, String respuesta) {
        SesionUsuario sesion = new SesionUsuario();
        sesion.iniciarAccion(accion);
        sesion.registrarRespuestaYAvanzar(respuesta);
        return sesion;
    }

    private static JsonNode json(String texto) throws JsonProcessingException {
        return new ObjectMapper().readTree(texto);
    }
}
