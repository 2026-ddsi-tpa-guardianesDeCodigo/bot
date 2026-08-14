package ar.edu.utn.dds.k3003.bot.session;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SesionManager {

    private final Map<Long, SesionUsuario> sesiones = new ConcurrentHashMap<>();

    public SesionUsuario obtener(Long chatId) {
        return sesiones.computeIfAbsent(chatId, id -> new SesionUsuario());
    }

    public void reiniciar(Long chatId) {
        sesiones.remove(chatId);
    }
}
