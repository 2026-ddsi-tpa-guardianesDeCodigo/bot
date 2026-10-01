package ar.edu.utn.dds.k3003.bot.handler;

import ar.edu.utn.dds.k3003.bot.client.IncentivosClient;
import ar.edu.utn.dds.k3003.bot.dtos.incentivos.CategoriaDonadorEnum;
import ar.edu.utn.dds.k3003.bot.dtos.incentivos.InsigniaDTO;
import ar.edu.utn.dds.k3003.bot.dtos.incentivos.MisionDTO;
import ar.edu.utn.dds.k3003.bot.dtos.incentivos.TipoMisionEnum;
import ar.edu.utn.dds.k3003.bot.session.Accion;
import ar.edu.utn.dds.k3003.bot.session.SesionUsuario;

import org.springframework.stereotype.Component;

import java.util.List;

// Ejecuta las acciones del módulo Incentivos (Entrega 5, §8.4).
@Component
public class IncentivosHandler {

    private final IncentivosClient client;

    public IncentivosHandler(IncentivosClient client) {
        this.client = client;
    }

    public String ejecutar(Accion accion, SesionUsuario sesion) {
        switch (accion) {
            case CREAR_MISION -> {
                String siguienteId = sesion.getRespuesta("ID de la siguiente misión (o - si no hay)");
                MisionDTO nueva = new MisionDTO(null,
                        sesion.getRespuesta("Nombre de la misión"), sesion.getRespuesta("ID de la insignia"),
                        parsearCategoria(sesion.getRespuesta(
                                "Categoría inicio (OCASIONAL, COLABORADOR, TRANSFORMADOR, SALVADOR o REVOLUCIONARIO)")),
                        parsearCategoria(sesion.getRespuesta(
                                "Categoría fin (OCASIONAL, COLABORADOR, TRANSFORMADOR, SALVADOR o REVOLUCIONARIO)")),
                        TipoMisionEnum.valueOf(sesion.getRespuesta(
                                "Tipo (COMPLETITUD, DONACIONES_EXITOSAS, DONACIONES_ASCENDENTES o REVOLUCION_DONADORA)")
                                .trim().toUpperCase()),
                        "-".equals(siguienteId.trim()) ? null : siguienteId);
                return "Misión creada:\n" + formatMision(client.crearMision(nueva));
            }
            case CREAR_INSIGNIA -> {
                InsigniaDTO nueva = new InsigniaDTO(null,
                        sesion.getRespuesta("Nombre de la insignia"), sesion.getRespuesta("Descripción de la insignia"));
                InsigniaDTO creada = client.crearInsignia(nueva);
                return "Insignia creada:\nID: " + creada.id() + "\nNombre: " + creada.nombre();
            }
            case ASIGNAR_MISION -> {
                String donadorID = sesion.getRespuesta("ID del donador");
                String misionID = sesion.getRespuesta("ID de la misión a asignar");
                MisionDTO mision = client.buscarMisionPorID(misionID);
                client.asignarMisionADonador(donadorID, mision);
                return "Misión " + misionID + " asignada al donador " + donadorID + ".";
            }
            case PROCESAR_DONADOR -> {
                String donadorID = sesion.getRespuesta("ID del donador a procesar");
                client.procesarDonador(donadorID);
                return "Donador " + donadorID + " procesado.";
            }
            case CONSULTAR_PROGRESO -> {
                String donadorID = sesion.getRespuesta("ID del donador");
                MisionDTO enCurso = client.misionEnCurso(donadorID);
                List<InsigniaDTO> insignias = client.insigniasDeDonador(donadorID);
                StringBuilder sb = new StringBuilder();
                sb.append("Misión en curso: ").append(enCurso != null ? formatMision(enCurso) : "ninguna").append("\n");
                sb.append("Insignias: ");
                if (insignias.isEmpty()) {
                    sb.append("ninguna");
                } else {
                    sb.append(insignias.stream().map(InsigniaDTO::nombre).reduce((a, b) -> a + ", " + b).orElse(""));
                }
                return sb.toString();
            }
            default -> {
                return "Acción no soportada.";
            }
        }
    }

    private CategoriaDonadorEnum parsearCategoria(String valor) {
        return CategoriaDonadorEnum.valueOf(valor.trim().toUpperCase());
    }

    private String formatMision(MisionDTO m) {
        return "ID: " + m.id() + "\nNombre: " + m.nombre() + "\nInsignia: " + m.insigniaID()
                + "\nDe " + m.categoriaInicio() + " a " + m.categoriaFin() + "\nTipo: " + m.tipo()
                + "\nSiguiente: " + (m.siguienteMisionID() != null ? m.siguienteMisionID() : "ninguna");
    }
}
