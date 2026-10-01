package ar.edu.utn.dds.k3003.bot.session;

// Cada acción define, en orden, qué campos le va a pedir el bot al usuario antes de llamar a la
// fachada correspondiente, y a qué componente (Modulo) pertenece - así el bot sabe qué cliente
// HTTP usar y qué handler ejecuta la acción. "Simple: recibe un comando, devuelve una respuesta"
// (CLAUDE.md §8.4) - nada de NLP, solo una secuencia fija de prompts por texto.
public enum Accion {

    // ---- Donadores y Entidades ----
    REGISTRAR_DONADOR(Modulo.DONADORES, "Nombre", "Apellido", "Edad", "Email", "Número de documento", "Domicilio"),
    MIS_ESTADISTICAS(Modulo.DONADORES, "Tu ID de donador"),
    BUSCAR_DONADOR(Modulo.DONADORES, "ID del donador a buscar"),
    LISTAR_DONADORES(Modulo.DONADORES),

    CREAR_ENTIDAD(Modulo.DONADORES, "Razón social", "Domicilio", "Teléfono", "Correo"),
    EDITAR_ENTIDAD(Modulo.DONADORES, "ID de la entidad a editar", "Razón social", "Domicilio", "Teléfono", "Correo"),
    BUSCAR_ENTIDAD(Modulo.DONADORES, "ID de la entidad a buscar"),
    LISTAR_ENTIDADES(Modulo.DONADORES),

    ALTA_NECESIDAD(Modulo.DONADORES, "ID de la entidad", "Nivel de urgencia (1-10)", "Descripción",
            "ID del producto solicitado", "Cantidad objetivo", "Tipo (EXTRAORDINARIA o RECURRENTE)"),
    MODIFICAR_NECESIDAD(Modulo.DONADORES, "ID de la necesidad a modificar", "ID de la entidad", "Nivel de urgencia (1-10)",
            "Descripción", "ID del producto solicitado", "Cantidad objetivo",
            "Tipo (EXTRAORDINARIA o RECURRENTE)"),
    BORRAR_NECESIDAD(Modulo.DONADORES, "ID de la necesidad a borrar"),
    BUSCAR_NECESIDAD(Modulo.DONADORES, "ID de la necesidad a buscar"),

    // ---- Donaciones ----
    REGISTRAR_DONACION(Modulo.DONACIONES, "ID del donador", "ID del depósito", "Descripción",
            "ID del producto (Donaciones)", "Cantidad a donar"),
    CONSULTAR_DONACION(Modulo.DONACIONES, "ID de la donación"),
    LISTAR_DONACIONES(Modulo.DONACIONES),
    LISTAR_DONACIONES_DE_DONADOR(Modulo.DONACIONES, "ID del donador"),
    CAMBIAR_ESTADO_DONACION(Modulo.DONACIONES, "ID de la donación", "Nuevo estado (INGRESADA, ACEPTADA o CONQUEJA)"),
    REGISTRAR_QUEJA_DONACION(Modulo.DONACIONES, "ID de la donación", "Descripción de la queja"),
    CREAR_PRODUCTO(Modulo.DONACIONES, "Nombre del producto", "Descripción del producto",
            "ID de categoría", "ID de identificador"),
    LISTAR_PRODUCTOS(Modulo.DONACIONES),
    CREAR_CATEGORIA(Modulo.DONACIONES, "Nombre de la categoría", "Descripción de la categoría"),
    CREAR_IDENTIFICADOR(Modulo.DONACIONES, "Tipo (QR o CODIGODEBARRAS)", "Descripción del identificador"),

    // ---- Logística ----
    CREAR_DEPOSITO(Modulo.LOGISTICA, "Nombre del depósito", "Dirección", "Capacidad máxima"),
    LISTAR_DEPOSITOS(Modulo.LOGISTICA),
    BUSCAR_DEPOSITO(Modulo.LOGISTICA, "ID del depósito a buscar"),
    CONFIGURAR_ALGORITMO(Modulo.LOGISTICA, "ID del depósito",
            "Algoritmo (SUB_ATENDIDOS o PRIORIDAD_POR_SCORE)"),
    CONSULTAR_STOCK(Modulo.LOGISTICA, "ID del producto a consultar"),
    LISTAR_ASIGNACIONES(Modulo.LOGISTICA),
    REPORTAR_ENTREGA(Modulo.LOGISTICA, "ID del paquete", "ID de la donación",
            "Nombre del producto entregado", "Cantidad entregada"),

    // ---- Incentivos ----
    CREAR_MISION(Modulo.INCENTIVOS, "Nombre de la misión", "ID de la insignia",
            "Categoría inicio (OCASIONAL, COLABORADOR, TRANSFORMADOR, SALVADOR o REVOLUCIONARIO)",
            "Categoría fin (OCASIONAL, COLABORADOR, TRANSFORMADOR, SALVADOR o REVOLUCIONARIO)",
            "Tipo (COMPLETITUD, DONACIONES_EXITOSAS, DONACIONES_ASCENDENTES o REVOLUCION_DONADORA)",
            "ID de la siguiente misión (o - si no hay)"),
    CREAR_INSIGNIA(Modulo.INCENTIVOS, "Nombre de la insignia", "Descripción de la insignia"),
    ASIGNAR_MISION(Modulo.INCENTIVOS, "ID del donador", "ID de la misión a asignar"),
    PROCESAR_DONADOR(Modulo.INCENTIVOS, "ID del donador a procesar"),
    CONSULTAR_PROGRESO(Modulo.INCENTIVOS, "ID del donador");

    private final Modulo modulo;
    private final String[] campos;

    Accion(Modulo modulo, String... campos) {
        this.modulo = modulo;
        this.campos = campos;
    }

    public Modulo getModulo() {
        return modulo;
    }

    public String[] getCampos() {
        return campos;
    }

    public int cantidadCampos() {
        return campos.length;
    }
}
