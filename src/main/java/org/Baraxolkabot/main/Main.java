package org.Baraxolkabot.main;
import org.Baraxolkabot.base.Database;
import org.Baraxolkabot.botBaraxolka.BotBaraxolka;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import org.telegram.telegrambots.meta.TelegramBotsApi;


public class Main {
    public static void main(String[] args) {
        try {
            Database.initializeDatabase();
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(new BotBaraxolka());
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}

