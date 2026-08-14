package ar.edu.utn.dds.k3003.bot.dtos;

public record DonadorDTO(
        String id,
        String nombre,
        String apellido,
        Integer edad,
        String email,
        String nroDocumento,
        String domicilio,
        EstadoDonadorEnum estado,
        String categoria) {}
