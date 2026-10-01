package ar.edu.utn.dds.k3003.bot.handler;

import ar.edu.utn.dds.k3003.bot.client.DonadoresClient;
import ar.edu.utn.dds.k3003.bot.dtos.DonadorDTO;
import ar.edu.utn.dds.k3003.bot.dtos.DonadorStatsDTO;
import ar.edu.utn.dds.k3003.bot.dtos.EntidadBeneficaDTO;
import ar.edu.utn.dds.k3003.bot.dtos.EstadoDonadorEnum;
import ar.edu.utn.dds.k3003.bot.dtos.NecesidadMaterialDTO;
import ar.edu.utn.dds.k3003.bot.dtos.TipoNecesidadMaterialEnum;
import ar.edu.utn.dds.k3003.bot.session.Accion;
import ar.edu.utn.dds.k3003.bot.session.SesionUsuario;

import org.springframework.stereotype.Component;

import java.util.List;

// Ejecuta las acciones del módulo Donadores y Entidades - misma lógica que tenía DonaTrackBot
// antes del refactor a handlers por módulo (Entrega 5, §8.4).
@Component
public class DonadoresHandler {

    private final DonadoresClient client;

    public DonadoresHandler(DonadoresClient client) {
        this.client = client;
    }

    public String ejecutar(Accion accion, SesionUsuario sesion) {
        switch (accion) {
            case REGISTRAR_DONADOR -> {
                DonadorDTO nuevo = new DonadorDTO(null,
                        sesion.getRespuesta("Nombre"), sesion.getRespuesta("Apellido"),
                        Integer.parseInt(sesion.getRespuesta("Edad")), sesion.getRespuesta("Email"),
                        sesion.getRespuesta("Número de documento"), sesion.getRespuesta("Domicilio"),
                        EstadoDonadorEnum.VERIFICADO, "OCASIONAL");
                return "Listo, te registré:\n" + formatDonador(client.agregarDonador(nuevo));
            }
            case MIS_ESTADISTICAS -> {
                return formatStats(client.estadisticasDonador(sesion.getRespuesta("Tu ID de donador")));
            }
            case BUSCAR_DONADOR -> {
                return formatDonador(client.buscarDonadorPorID(sesion.getRespuesta("ID del donador a buscar")));
            }
            case LISTAR_DONADORES -> {
                return formatListaDonadores(client.obtenerDonadores());
            }
            case CREAR_ENTIDAD -> {
                EntidadBeneficaDTO nueva = new EntidadBeneficaDTO(null,
                        sesion.getRespuesta("Razón social"), sesion.getRespuesta("Domicilio"),
                        sesion.getRespuesta("Teléfono"), sesion.getRespuesta("Correo"));
                return "Entidad creada:\n" + formatEntidad(client.agregarEntidad(nueva));
            }
            case EDITAR_ENTIDAD -> {
                String id = sesion.getRespuesta("ID de la entidad a editar");
                EntidadBeneficaDTO editada = new EntidadBeneficaDTO(id,
                        sesion.getRespuesta("Razón social"), sesion.getRespuesta("Domicilio"),
                        sesion.getRespuesta("Teléfono"), sesion.getRespuesta("Correo"));
                return "Entidad editada:\n" + formatEntidad(client.editarEntidad(id, editada));
            }
            case BUSCAR_ENTIDAD -> {
                return formatEntidad(client.buscarEntidadPorID(sesion.getRespuesta("ID de la entidad a buscar")));
            }
            case LISTAR_ENTIDADES -> {
                return formatListaEntidades(client.obtenerEntidades());
            }
            case ALTA_NECESIDAD -> {
                NecesidadMaterialDTO nueva = new NecesidadMaterialDTO(null,
                        sesion.getRespuesta("ID de la entidad"),
                        Integer.parseInt(sesion.getRespuesta("Nivel de urgencia (1-10)")),
                        sesion.getRespuesta("Descripción"),
                        Integer.parseInt(sesion.getRespuesta("Cantidad objetivo")),
                        sesion.getRespuesta("ID del producto solicitado"),
                        TipoNecesidadMaterialEnum.valueOf(
                                sesion.getRespuesta("Tipo (EXTRAORDINARIA o RECURRENTE)").trim().toUpperCase()));
                return "Necesidad creada:\n" + formatNecesidad(client.registrarNecesidad(nueva));
            }
            case MODIFICAR_NECESIDAD -> {
                String id = sesion.getRespuesta("ID de la necesidad a modificar");
                NecesidadMaterialDTO editada = new NecesidadMaterialDTO(id,
                        sesion.getRespuesta("ID de la entidad"),
                        Integer.parseInt(sesion.getRespuesta("Nivel de urgencia (1-10)")),
                        sesion.getRespuesta("Descripción"),
                        Integer.parseInt(sesion.getRespuesta("Cantidad objetivo")),
                        sesion.getRespuesta("ID del producto solicitado"),
                        TipoNecesidadMaterialEnum.valueOf(
                                sesion.getRespuesta("Tipo (EXTRAORDINARIA o RECURRENTE)").trim().toUpperCase()));
                return "Necesidad modificada:\n" + formatNecesidad(client.editarNecesidad(id, editada));
            }
            case BORRAR_NECESIDAD -> {
                String id = sesion.getRespuesta("ID de la necesidad a borrar");
                client.borrarNecesidad(id);
                return "Necesidad " + id + " borrada.";
            }
            case BUSCAR_NECESIDAD -> {
                return formatNecesidad(client.buscarNecesidadPorID(sesion.getRespuesta("ID de la necesidad a buscar")));
            }
            default -> {
                return "Acción no soportada.";
            }
        }
    }

    private String formatDonador(DonadorDTO d) {
        return "ID: " + d.id() + "\nNombre: " + d.nombre() + " " + d.apellido()
                + "\nEdad: " + d.edad() + "\nEmail: " + d.email()
                + "\nEstado: " + d.estado() + "\nCategoría: " + d.categoria();
    }

    private String formatListaDonadores(List<DonadorDTO> donadores) {
        if (donadores.isEmpty()) return "No hay donadores registrados todavía.";
        StringBuilder sb = new StringBuilder();
        for (DonadorDTO d : donadores) {
            sb.append("• ").append(d.id()).append(" - ").append(d.nombre()).append(" ")
                    .append(d.apellido()).append(" (").append(d.categoria()).append(")\n");
        }
        return sb.toString();
    }

    private String formatStats(DonadorStatsDTO s) {
        return "Nombre: " + s.nombre() + " " + s.apellido()
                + "\nEstado: " + s.estado() + "\nCategoría: " + s.categoria()
                + "\nMisión actual: " + (s.misionActualID() != null ? s.misionActualID() : "ninguna")
                + "\nInsignias: " + (s.insigniasID().isEmpty() ? "ninguna" : String.join(", ", s.insigniasID()));
    }

    private String formatEntidad(EntidadBeneficaDTO e) {
        return "ID: " + e.id() + "\nRazón social: " + e.razonSocial()
                + "\nDomicilio: " + e.domicilio() + "\nTeléfono: " + e.telefono()
                + "\nCorreo: " + e.correo();
    }

    private String formatListaEntidades(List<EntidadBeneficaDTO> entidades) {
        if (entidades.isEmpty()) return "No hay entidades registradas todavía.";
        StringBuilder sb = new StringBuilder();
        for (EntidadBeneficaDTO e : entidades) {
            sb.append("• ").append(e.id()).append(" - ").append(e.razonSocial()).append("\n");
        }
        return sb.toString();
    }

    private String formatNecesidad(NecesidadMaterialDTO n) {
        return "ID: " + n.id() + "\nEntidad: " + n.entidadID()
                + "\nUrgencia: " + n.nivelDeUrgencia() + "\nDescripción: " + n.descripcion()
                + "\nProducto solicitado: " + n.productoSolicitadoID()
                + "\nCantidad objetivo: " + n.cantidadObjetivo() + "\nTipo: " + n.tipo();
    }
}
