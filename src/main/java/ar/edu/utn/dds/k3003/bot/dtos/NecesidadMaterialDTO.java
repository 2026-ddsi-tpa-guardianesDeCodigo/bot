package ar.edu.utn.dds.k3003.bot.dtos;

public record NecesidadMaterialDTO(
        String id,
        String entidadID,
        Integer nivelDeUrgencia,
        String descripcion,
        Integer cantidadObjetivo,
        String productoSolicitadoID,
        TipoNecesidadMaterialEnum tipo) {}
