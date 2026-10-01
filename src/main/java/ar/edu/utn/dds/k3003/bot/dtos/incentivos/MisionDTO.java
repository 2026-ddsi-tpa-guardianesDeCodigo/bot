package ar.edu.utn.dds.k3003.bot.dtos.incentivos;

public record MisionDTO(
        String id,
        String nombre,
        String insigniaID,
        CategoriaDonadorEnum categoriaInicio,
        CategoriaDonadorEnum categoriaFin,
        TipoMisionEnum tipo,
        String siguienteMisionID) {}
