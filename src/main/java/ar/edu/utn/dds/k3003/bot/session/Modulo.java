package ar.edu.utn.dds.k3003.bot.session;

// Los 4 componentes con los que el bot puede hablar ahora (Entrega 5, §8.4: "consultas y ABM
// sobre el estado de todos los componentes"). Donadores y Entidades conserva la distinción
// Donador/Admin (TipoUsuario) que ya tenía; los otros 3 no la necesitan porque sus acciones son
// todas operativas/ABM, no hay un "donador final" usándolas por Telegram.
public enum Modulo {
    DONADORES,
    DONACIONES,
    LOGISTICA,
    INCENTIVOS
}
