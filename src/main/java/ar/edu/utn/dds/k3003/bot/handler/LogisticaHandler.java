package ar.edu.utn.dds.k3003.bot.handler;

import ar.edu.utn.dds.k3003.bot.client.LogisticaClient;
import ar.edu.utn.dds.k3003.bot.dtos.logistica.AsignacionDTO;
import ar.edu.utn.dds.k3003.bot.dtos.logistica.DepositoDTO;
import ar.edu.utn.dds.k3003.bot.dtos.logistica.PaqueteDTO;
import ar.edu.utn.dds.k3003.bot.dtos.logistica.StockDisponibleDTO;
import ar.edu.utn.dds.k3003.bot.dtos.logistica.TipoAlgoritmoEnum;
import ar.edu.utn.dds.k3003.bot.session.Accion;
import ar.edu.utn.dds.k3003.bot.session.SesionUsuario;

import org.springframework.stereotype.Component;

import java.util.List;

// Ejecuta las acciones del módulo Logística (Entrega 5, §8.4).
@Component
public class LogisticaHandler {

    private final LogisticaClient client;

    public LogisticaHandler(LogisticaClient client) {
        this.client = client;
    }

    public String ejecutar(Accion accion, SesionUsuario sesion) {
        switch (accion) {
            case CREAR_DEPOSITO -> {
                DepositoDTO nuevo = new DepositoDTO(null, null,
                        sesion.getRespuesta("Nombre del depósito"), sesion.getRespuesta("Dirección"),
                        Integer.parseInt(sesion.getRespuesta("Capacidad máxima")), null);
                return "Depósito creado (configurá el algoritmo antes de mandarle donaciones):\n"
                        + formatDeposito(client.crearDeposito(nuevo));
            }
            case LISTAR_DEPOSITOS -> {
                return formatListaDepositos(client.listarDepositos());
            }
            case BUSCAR_DEPOSITO -> {
                return formatDeposito(client.buscarDepositoPorID(sesion.getRespuesta("ID del depósito a buscar")));
            }
            case CONFIGURAR_ALGORITMO -> {
                String id = sesion.getRespuesta("ID del depósito");
                TipoAlgoritmoEnum algoritmo = TipoAlgoritmoEnum.valueOf(
                        sesion.getRespuesta("Algoritmo (SUB_ATENDIDOS o PRIORIDAD_POR_SCORE)").trim().toUpperCase());
                client.configurarAlgoritmo(id, algoritmo);
                return "Depósito " + id + " configurado con algoritmo " + algoritmo + ".";
            }
            case CONSULTAR_STOCK -> {
                return formatStock(client.consultarStock(sesion.getRespuesta("ID del producto a consultar")));
            }
            case LISTAR_ASIGNACIONES -> {
                return formatListaAsignaciones(client.listarAsignaciones());
            }
            case REPORTAR_ENTREGA -> {
                PaqueteDTO paquete = new PaqueteDTO(
                        sesion.getRespuesta("ID del paquete"), sesion.getRespuesta("ID de la donación"),
                        sesion.getRespuesta("Nombre del producto entregado"),
                        Integer.parseInt(sesion.getRespuesta("Cantidad entregada")));
                client.reportarEntrega(paquete);
                return "Entrega del paquete " + paquete.id() + " reportada.";
            }
            default -> {
                return "Acción no soportada.";
            }
        }
    }

    private String formatDeposito(DepositoDTO d) {
        return "ID: " + d.id() + "\nNombre: " + d.nombre() + "\nDirección: " + d.direccion()
                + "\nCapacidad máxima: " + d.capacidadMaxima()
                + "\nAlgoritmo: " + (d.algoritmo() != null ? d.algoritmo() : "sin configurar");
    }

    private String formatListaDepositos(List<DepositoDTO> depositos) {
        if (depositos.isEmpty()) return "No hay depósitos registrados todavía.";
        StringBuilder sb = new StringBuilder();
        for (DepositoDTO d : depositos) {
            sb.append("• ").append(d.id()).append(" - ").append(d.nombre())
                    .append(" (algoritmo: ").append(d.algoritmo() != null ? d.algoritmo() : "sin configurar").append(")\n");
        }
        return sb.toString();
    }

    private String formatStock(StockDisponibleDTO s) {
        return "Producto: " + s.productoID() + "\nCantidad disponible: " + s.cantidadDisponible();
    }

    private String formatListaAsignaciones(List<AsignacionDTO> asignaciones) {
        if (asignaciones.isEmpty()) return "No hay asignaciones para mostrar.";
        StringBuilder sb = new StringBuilder();
        for (AsignacionDTO a : asignaciones) {
            sb.append("• ").append(a.id()).append(" - paquete ").append(a.paqueteID())
                    .append(", necesidad ").append(a.necesidadID()).append(" (").append(a.estado())
                    .append(", ").append(a.origen()).append(")\n");
        }
        return sb.toString();
    }
}
