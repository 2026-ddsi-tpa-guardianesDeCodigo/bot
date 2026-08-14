package ar.edu.utn.dds.k3003.bot.session;

// Cada acción define, en orden, qué campos le va a pedir el bot al usuario antes de llamar a la
// fachada. "Simple: recibe un comando, devuelve una respuesta" (CLAUDE.md §8.4) - nada de NLP,
// solo una secuencia fija de prompts por texto.
public enum Accion {
    REGISTRAR_DONADOR("Nombre", "Apellido", "Edad", "Email", "Número de documento", "Domicilio"),
    MIS_ESTADISTICAS("Tu ID de donador"),
    BUSCAR_DONADOR("ID del donador a buscar"),
    LISTAR_DONADORES(),

    CREAR_ENTIDAD("Razón social", "Domicilio", "Teléfono", "Correo"),
    EDITAR_ENTIDAD("ID de la entidad a editar", "Razón social", "Domicilio", "Teléfono", "Correo"),
    BUSCAR_ENTIDAD("ID de la entidad a buscar"),
    LISTAR_ENTIDADES(),

    ALTA_NECESIDAD("ID de la entidad", "Nivel de urgencia (1-10)", "Descripción",
            "ID del producto solicitado", "Cantidad objetivo", "Tipo (EXTRAORDINARIA o RECURRENTE)"),
    MODIFICAR_NECESIDAD("ID de la necesidad a modificar", "ID de la entidad", "Nivel de urgencia (1-10)",
            "Descripción", "ID del producto solicitado", "Cantidad objetivo",
            "Tipo (EXTRAORDINARIA o RECURRENTE)"),
    BORRAR_NECESIDAD("ID de la necesidad a borrar"),
    BUSCAR_NECESIDAD("ID de la necesidad a buscar");

    private final String[] campos;

    Accion(String... campos) {
        this.campos = campos;
    }

    public String[] getCampos() {
        return campos;
    }

    public int cantidadCampos() {
        return campos.length;
    }
}
