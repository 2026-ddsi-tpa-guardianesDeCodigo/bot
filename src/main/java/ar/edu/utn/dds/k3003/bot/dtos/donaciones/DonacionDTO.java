package ar.edu.utn.dds.k3003.bot.dtos.donaciones;

// Espejo del DTO de cátedra que expone Donaciones (ver donaciones/.../catedra/dtos/donaciones).
public record DonacionDTO(
        Long id,
        String donadorID,
        String depositoID,
        String descripcion,
        Long productoID,
        Integer cantidad,
        EstadoDonacionEnum estado) {}
