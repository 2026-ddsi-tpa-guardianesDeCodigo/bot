package ar.edu.utn.dds.k3003.bot.config;

import ar.edu.utn.dds.k3003.bot.DonaTrackBot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

// Registra el bot con long polling apenas arranca la aplicación - mismo patrón que el `main` de
// la práctica Copia.me, adaptado a Spring Boot (acá el bot ya es un @Component, así que solo
// hace falta darlo de alta en la API de Telegram).
@Component
public class TelegramBotConfig implements CommandLineRunner {

    private final DonaTrackBot bot;

    @Autowired
    public TelegramBotConfig(DonaTrackBot bot) {
        this.bot = bot;
    }

    @Override
    public void run(String... args) throws TelegramApiException {
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
        telegramBotsApi.registerBot(bot);
        System.out.println("Bot de Telegram registrado y escuchando (long polling).");
    }
}
