package org.Baraxolkabot.botBaraxolka;

import org.Baraxolkabot.category.Category;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;


public class BotBaraxolka extends TelegramLongPollingBot {

    private final MessageHandler messageHandler;

    public BotBaraxolka() {
        this.messageHandler = new MessageHandler(this);
        //Database.getAllCategories();
        Product.categories.add(new Category("Электроника"));
        Product.categories.add(new Category("Одежда"));
        Product.categories.add(new Category("Книги"));
    }

    @Override
    public String getBotUsername() {
        return "Baraxolochkamoya";
    }

    @Override
    public String getBotToken() {
        return "7550754797:AAG5D23DWX67uiLgZLuKZWw2l96RkAoLD4g";
    }

    @Override
    public void onUpdateReceived(Update update) {
        messageHandler.handleUpdate(update);
    }
}