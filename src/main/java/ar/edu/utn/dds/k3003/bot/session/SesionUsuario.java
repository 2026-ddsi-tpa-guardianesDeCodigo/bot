package ar.edu.utn.dds.k3003.bot.session;

import java.util.LinkedHashMap;
import java.util.Map;

// Estado de conversación de un chat. No hay persistencia - se pierde si el bot se reinicia,
// que es aceptable para un bot que "por ahora corre localmente, un solo proceso a la vez"
// (CLAUDE.md §8.4).
public class SesionUsuario {

    private TipoUsuario tipoUsuario;
    private Accion accionActual;
    private int indiceCampoActual;
    private final Map<String, String> respuestas = new LinkedHashMap<>();

    public TipoUsuario getTipoUsuario() {
        return tipoUsuario;
    }

    public void setTipoUsuario(TipoUsuario tipoUsuario) {
        this.tipoUsuario = tipoUsuario;
    }

    public Accion getAccionActual() {
        return accionActual;
    }

    public boolean tieneAccionEnCurso() {
        return accionActual != null;
    }

    public void iniciarAccion(Accion accion) {
        this.accionActual = accion;
        this.indiceCampoActual = 0;
        this.respuestas.clear();
    }

    public void finalizarAccion() {
        this.accionActual = null;
        this.indiceCampoActual = 0;
        this.respuestas.clear();
    }

    public String campoActual() {
        return accionActual.getCampos()[indiceCampoActual];
    }

    // Guarda la respuesta al campo actual y avanza. Devuelve true si ya se completaron todos
    // los campos de la acción en curso.
    public boolean registrarRespuestaYAvanzar(String respuesta) {
        respuestas.put(accionActual.getCampos()[indiceCampoActual], respuesta);
        indiceCampoActual++;
        return indiceCampoActual >= accionActual.cantidadCampos();
    }

    public String getRespuesta(String campo) {
        return respuestas.get(campo);
    }
}
