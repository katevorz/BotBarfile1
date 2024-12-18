package org.Baraxolkabot.MassegeKeyboard;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.Baraxolkabot.Base.Database;
import org.Baraxolkabot.Category.Category;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class BotBaraxolka extends TelegramLongPollingBot {

    @Override
    public String getBotUsername() {
        return "BaraxolkaBotic"; // Замените на имя вашего бота
    }

    @Override
    public String getBotToken() {
        return "7591311099:AAHUxm40Jo-IBXovfw89bkIPlLPG9PBcuXY"; // Замените на токен вашего бота
    }

    private void saveProductToDatabase(Product product) {
        Database.insertProduct(
                product.getName(),
                product.getCategory().getName(),
                Double.parseDouble(product.getPrice().toString()),
                product.getDescription(),
                product.getTelegramHandle(),
                product.getPhotoId()

        );
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage()) {
            long chatId = update.getMessage().getChatId();
            if (update.getMessage().hasText()) {
                String messageText = update.getMessage().getText();
                if (Product.addingState.containsKey(chatId) && Product.addingState.get(chatId).equals("Searching")) {
                    //sendResponse(chatId, "Введите название товара, который вы хотите найти:");
                    performSearch(chatId, messageText);
                    Product.addingState.remove(chatId);
                    return;
                }
                if (Product.addingState.containsKey(chatId) && Product.addingState.get(chatId).equals("Deleting")) {
                    // Если пользователь ввел название товара, пытаемся удалить его
                    boolean isDeleted = Database.deleteProductByName(messageText); // Удаляем товар по названию
                    if (isDeleted) {
                        sendResponse(chatId, "Товар " + messageText + " успешно удален.");
                    } else {
                        sendResponse(chatId, "Товар " + messageText + " не найден или у вас нет прав на его удаление.");
                    }
                    Product.addingState.remove(chatId); // Удаляем состояние после обработки
                    return; // Завершаем метод после обработки названия товара
                }

                // Проверяем состояние добавления товара
                if (Product.addingState.containsKey(chatId)) {
                    if (Product.categoryState.get(chatId) == null) {
                        Category chosenCategory = findCategoryByName(messageText);
                        if (chosenCategory != null) {
                            Product.categoryState.put(chatId, chosenCategory);
                            sendResponse(chatId, "Вы выбрали: " + chosenCategory.getName() + ". Теперь введите название продукта: ");
                        } else {
                            sendResponse(chatId, "Неверная категория. Выберите правильную категорию:");
                        }
                    } else if (Product.addingState.get(chatId).isEmpty()) {
                        // Ожидаем название товара
                        Product.addingState.put(chatId, messageText); // Сохраняем название товара
                        sendResponse(chatId, "Введите цену товара:");
                        Product.priceState.put(chatId, null); // Инициализируем состояние цены
                    } else if (Product.priceState.get(chatId) == null) {
                        // Ожидаем цену товара
                        try {
                            //String price = String.parseDouble(messageText);
                            String price = messageText;
                            Product.priceState.put(chatId, price);
                            sendResponse(chatId, "Введите описание товара:");
                            Product.descriptionState.put(chatId, ""); // Инициализируем состояние описания
                        } catch (NumberFormatException e) {
                            sendResponse(chatId, "Пожалуйста, введите корректную цену:");
                        }
                    } else if (Product.descriptionState.get(chatId).isEmpty()) {
                        // Ожидаем описание товара
                        Product.descriptionState.put(chatId, messageText);

                        String telegramHandle = update.getMessage().getFrom().getUserName();

                        Product.phoneState.put(chatId, telegramHandle); // Save Telegram handle
                        sendResponse(chatId, "Теперь отправьте фото товара:");
                    }
                } else {
                    handleCommands(chatId, messageText);
                }
            } else if (update.getMessage().hasPhoto()) {
                handlePhotoMessage(chatId, update);
            }
        }
    }

    private void handleCommands(long chatId, String messageText) {
        switch (messageText) {
            case "/start":
                sendWelcomeMessage(chatId);
                break;
            case "Добавить товар":
                initiateProductAddition(chatId);
                break;
            case "Показать все товары":
                sendListings(chatId);
                break;
            case "Поиск товара":
                initiateSearch(chatId);
                break;
            case "Удалить товар":
                sendDeleteProductPrompt(chatId);
                break;
            default:
                sendResponse(chatId, "Используйте кнопку 'Добавить товар' для добавления товара или 'Показать все товары' для просмотра.");
                break;
        }

    }

    private void initiateSearch(long chatId) {
        Product.addingState.put(chatId, "Searching");
        sendResponse(chatId, "Введите ключевое слово для поиска:");
    }

    private void performSearch(long chatId, String keyword) {
        List<Product> results = Database.searchProducts(keyword);

        if (results.isEmpty()) {
            sendResponse(chatId, "По вашему запросу ничего не найдено.");
        } else {
            for (Product product : results) {
                sendPhoto(chatId, product);
            }
        }
    }

    private void initiateProductAddition(long chatId) {
        Product.addingState.put(chatId, ""); // Инициализируем состояние добавления товара
        Product.categoryState.put(chatId, null); // Сбрасываем состояние категории
        sendCategorySelection(chatId);
    }

    private void handlePhotoMessage(long chatId, Update update) {
        String productName = Product.addingState.get(chatId);
        if (productName != null && !productName.isEmpty()) {
            // Предполагается, что фото будет сохранено и связано с товаром
            sendResponse(chatId, "Фото получено! Ваш товар был успешно добавлен.");
            String photoId = update.getMessage().getPhoto().get(0).getFileId(); // Получаем ID фото
            // Добавляем товар с фото в список
            Product product = new Product(productName, Product.categoryState.get(chatId), new BigDecimal(Product.priceState.get(chatId)), Product.descriptionState.get(chatId), Product.phoneState.get(chatId), photoId);
            saveProductToDatabase(product);

        }
        sendWelcomeMessage(chatId);

        // Здесь можно завершить процесс добавления товара и очистить состояния
        Product.addingState.remove(chatId);
        Product.categoryState.remove(chatId);
        Product.priceState.remove(chatId);
        Product.descriptionState.remove(chatId);
        Product.phoneState.remove(chatId);
    }

    private void sendWelcomeMessage(long chatId) {
        String welcomeText = "Добро пожаловать в Marketplace Bot! Используйте кнопки ниже для взаимодействия.";
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(welcomeText);

        // Создаем клавиатуру
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        KeyboardRow row1 = new KeyboardRow();
        row1.add("Добавить товар");
        row1.add("Показать все товары");

        KeyboardRow row2 = new KeyboardRow();
        row2.add("Поиск товара");
        row2.add("Удалить товар");

        List<KeyboardRow> keyboard = new ArrayList<>();
        keyboard.add(row1);
        keyboard.add(row2);
        keyboardMarkup.setKeyboard(keyboard);

        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendCategorySelection(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Пожалуйста, выберите категорию: ");

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> KeyboardRows = new ArrayList<>();

        for (Category category : Product.categories) {
            KeyboardRow row = new KeyboardRow();
            row.add(category.getName());
            KeyboardRows.add(row);
        }
        keyboardMarkup.setKeyboard(KeyboardRows);
        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private Category findCategoryByName(String name) {
        for (Category category : Product.categories) {
            if (category.getName().equalsIgnoreCase(name)) {
                return category;
            }
        }
        return null;
    }

    private void sendListings(long chatId) {
        List<Product> products = Database.getProducts();
        if (products.isEmpty()) {
            sendResponse(chatId, "Список объявлений пуст.");
            return;
        }

        for (Product product : products) {
            sendPhoto(chatId, product);
        }
    }

    private void sendDeleteProductPrompt(long chatId) {
        String promptText = "Введите название товара, который вы хотите удалить:";
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(promptText);
        Product.addingState.put(chatId, "Deleting");
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }


    private void sendPhoto(long chatId, Product product) {
        SendPhoto photoMessage = new SendPhoto();
        photoMessage.setChatId(String.valueOf(chatId));

        // Используем InputFile для передачи ID фото
        photoMessage.setPhoto(new InputFile(product.getPhotoId()));

        String caption = "Товар: " + product.getName() + "\n" +
                "Описание: " + product.getDescription() + "\n" +
                "Цена: " + product.getPrice() + "\n" +
                "Продавца: " + product.getTelegramHandle();

        photoMessage.setCaption(caption);


        try {
            execute(photoMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendResponse(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public BotBaraxolka() {
        Product.categories.add(new Category("Электроника"));
        Product.categories.add(new Category("Одежда"));
        Product.categories.add(new Category("Книги"));
    }
}

