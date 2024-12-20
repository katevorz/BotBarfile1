package org.Baraxolkabot.botBaraxolka;

import org.Baraxolkabot.productService.ProductService;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

public class MessageHandler {

    private final TelegramLongPollingBot bot;
    private final ProductService productService;

    public MessageHandler(TelegramLongPollingBot bot) {
        this.bot = bot;
        this.productService = new ProductService(bot);
    }

    public void handleUpdate(Update update) {
        if (!update.hasMessage()) return;

        long chatId = update.getMessage().getChatId();
        String messageText = update.getMessage().hasText() ? update.getMessage().getText() : null;

        if (messageText != null) {
            handleTextMessage(chatId, messageText, update);
        } else if (update.getMessage().hasPhoto()) {
            handlePhotoMessage(chatId, update);
        }
    }

    private void handleTextMessage(long chatId, String messageText, Update update) {
        if (Product.enable.containsKey(chatId)) {
            handleStatefulMessage(chatId, messageText, update);
        } else {
            handleCommands(chatId, messageText);
        }
    }

    private void handleStatefulMessage(long chatId, String messageText, Update update) {
        String currentState = Product.enable.get(chatId);

        switch (currentState) {
            case "Searching":
                productService.performSearch(chatId, messageText);
                Product.enable.remove(chatId);
                break;

            case "Deleting":
                productService.handleDeleteProduct(chatId, messageText, update);
                Product.enable.remove(chatId);
                break;

            default:
                productService.handleProductAddition(chatId, messageText, update);
                break;
        }
    }

    private void handleCommands(long chatId, String messageText) {
        switch (messageText) {
            case "/start":
                productService.sendWelcomeMessage(chatId);
                break;
            case "Добавить товар":
                productService.initiateProductAddition(chatId);
                break;
            case "Показать все товары":
                productService.sendListings(chatId);
                break;
            case "Поиск товара":
                productService.initiateSearch(chatId);
                break;
            case "Удалить товар":
                productService.sendDeleteProductPrompt(chatId);
                break;
            default:
                productService.sendResponse(chatId, "Используйте кнопку 'Добавить товар' для добавления товара или 'Показать все товары' для просмотра.");
                break;
        }
    }

    private void handlePhotoMessage(long chatId, Update update) {
        productService.handlePhotoMessage(chatId, update);
    }
}