package ar.edu.utn.dds.k3003.bot.dtos.logistica;

import java.time.LocalDateTime;

public record AsignacionDTO(
        String id,
        String paqueteID,
        String necesidadID,
        LocalDateTime fecha,
        EstadoAsignacionEnum estado,
        OrigenAsignacionEnum origen) {}
