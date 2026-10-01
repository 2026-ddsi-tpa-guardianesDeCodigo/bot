package ar.edu.utn.dds.k3003.bot.handler;

import ar.edu.utn.dds.k3003.bot.client.DonacionesClient;
import ar.edu.utn.dds.k3003.bot.dtos.donaciones.CategoriaDTO;
import ar.edu.utn.dds.k3003.bot.dtos.donaciones.DonacionDTO;
import ar.edu.utn.dds.k3003.bot.dtos.donaciones.EstadoDonacionEnum;
import ar.edu.utn.dds.k3003.bot.dtos.donaciones.IdentificadorDTO;
import ar.edu.utn.dds.k3003.bot.dtos.donaciones.ProductoDTO;
import ar.edu.utn.dds.k3003.bot.dtos.donaciones.TipoIdentificadorEnum;
import ar.edu.utn.dds.k3003.bot.session.Accion;
import ar.edu.utn.dds.k3003.bot.session.SesionUsuario;

import org.springframework.stereotype.Component;

import java.util.List;

// Ejecuta las acciones del módulo Donaciones (Entrega 5, §8.4).
@Component
public class DonacionesHandler {

    private final DonacionesClient client;

    public DonacionesHandler(DonacionesClient client) {
        this.client = client;
    }

    public String ejecutar(Accion accion, SesionUsuario sesion) {
        switch (accion) {
            case REGISTRAR_DONACION -> {
                DonacionDTO nueva = new DonacionDTO(null,
                        sesion.getRespuesta("ID del donador"), sesion.getRespuesta("ID del depósito"),
                        sesion.getRespuesta("Descripción"),
                        Long.parseLong(sesion.getRespuesta("ID del producto (Donaciones)")),
                        Integer.parseInt(sesion.getRespuesta("Cantidad a donar")), null);
                return "Donación registrada:\n" + formatDonacion(client.registrarDonacion(nueva));
            }
            case CONSULTAR_DONACION -> {
                return formatDonacion(client.buscarDonacionPorID(Long.parseLong(sesion.getRespuesta("ID de la donación"))));
            }
            case LISTAR_DONACIONES -> {
                return formatListaDonaciones(client.listarDonaciones());
            }
            case LISTAR_DONACIONES_DE_DONADOR -> {
                return formatListaDonaciones(client.listarPorDonador(sesion.getRespuesta("ID del donador")));
            }
            case CAMBIAR_ESTADO_DONACION -> {
                Long id = Long.parseLong(sesion.getRespuesta("ID de la donación"));
                EstadoDonacionEnum estado = EstadoDonacionEnum.valueOf(
                        sesion.getRespuesta("Nuevo estado (INGRESADA, ACEPTADA o CONQUEJA)").trim().toUpperCase());
                return "Donación actualizada:\n" + formatDonacion(client.cambiarEstado(id, estado));
            }
            case REGISTRAR_QUEJA_DONACION -> {
                Long id = Long.parseLong(sesion.getRespuesta("ID de la donación"));
                return "Queja registrada:\n" + formatDonacion(
                        client.registrarQueja(id, sesion.getRespuesta("Descripción de la queja")));
            }
            case CREAR_PRODUCTO -> {
                ProductoDTO nuevo = new ProductoDTO(null,
                        sesion.getRespuesta("Nombre del producto"), sesion.getRespuesta("Descripción del producto"),
                        Long.parseLong(sesion.getRespuesta("ID de categoría")),
                        Long.parseLong(sesion.getRespuesta("ID de identificador")));
                return "Producto creado:\n" + formatProducto(client.crearProducto(nuevo));
            }
            case LISTAR_PRODUCTOS -> {
                return formatListaProductos(client.listarProductos());
            }
            case CREAR_CATEGORIA -> {
                CategoriaDTO nueva = new CategoriaDTO(null,
                        sesion.getRespuesta("Nombre de la categoría"), sesion.getRespuesta("Descripción de la categoría"), null);
                CategoriaDTO creada = client.crearCategoria(nueva);
                return "Categoría creada:\nID: " + creada.id() + "\nNombre: " + creada.nombre();
            }
            case CREAR_IDENTIFICADOR -> {
                IdentificadorDTO nuevo = new IdentificadorDTO(null,
                        TipoIdentificadorEnum.valueOf(sesion.getRespuesta("Tipo (QR o CODIGODEBARRAS)").trim().toUpperCase()),
                        sesion.getRespuesta("Descripción del identificador"));
                IdentificadorDTO creado = client.crearIdentificador(nuevo);
                return "Identificador creado:\nID: " + creado.id() + "\nTipo: " + creado.tipo();
            }
            default -> {
                return "Acción no soportada.";
            }
        }
    }

    private String formatDonacion(DonacionDTO d) {
        return "ID: " + d.id() + "\nDonador: " + d.donadorID() + "\nDepósito: " + d.depositoID()
                + "\nDescripción: " + d.descripcion() + "\nProducto: " + d.productoID()
                + "\nCantidad: " + d.cantidad() + "\nEstado: " + d.estado();
    }

    private String formatListaDonaciones(List<DonacionDTO> donaciones) {
        if (donaciones.isEmpty()) return "No hay donaciones para mostrar.";
        StringBuilder sb = new StringBuilder();
        for (DonacionDTO d : donaciones) {
            sb.append("• ").append(d.id()).append(" - donador ").append(d.donadorID())
                    .append(", producto ").append(d.productoID()).append(" x").append(d.cantidad())
                    .append(" (").append(d.estado()).append(")\n");
        }
        return sb.toString();
    }

    private String formatProducto(ProductoDTO p) {
        return "ID: " + p.id() + "\nNombre: " + p.nombre() + "\nDescripción: " + p.descripcion()
                + "\nCategoría: " + p.categoriaID() + "\nIdentificador: " + p.identificadorID();
    }

    private String formatListaProductos(List<ProductoDTO> productos) {
        if (productos.isEmpty()) return "No hay productos registrados todavía.";
        StringBuilder sb = new StringBuilder();
        for (ProductoDTO p : productos) {
            sb.append("• ").append(p.id()).append(" - ").append(p.nombre()).append("\n");
        }
        return sb.toString();
    }
}
