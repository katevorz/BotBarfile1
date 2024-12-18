package org.Baraxolkabot.MassegeKeyboard;

import org.Baraxolkabot.Base.Database;
import org.Baraxolkabot.Category.Category;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.math.BigDecimal;
import java.util.List;

public class MessageHandler {

    private final TelegramLongPollingBot bot;

    public MessageHandler(TelegramLongPollingBot bot) {
        this.bot = bot;
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
        if (Product.addingState.containsKey(chatId)) {
            handleStatefulMessage(chatId, messageText, update);
        } else {
            handleCommands(chatId, messageText);
        }
    }

    private void handleStatefulMessage(long chatId, String messageText, Update update) {
        String currentState = Product.addingState.get(chatId);

        switch (currentState) {
            case "Searching":
                performSearch(chatId, messageText);
                Product.addingState.remove(chatId);
                break;

            case "Deleting":
                handleDeleteProduct(chatId, messageText, update); // Передаем Update для получения никнейма
                Product.addingState.remove(chatId);
                break;

            default:
                handleProductAddition(chatId, messageText, update);
                break;
        }
    }
    private String getUsernameFromUpdate(Update update) {
        if (update.getMessage().getFrom().getUserName() != null) {
            return update.getMessage().getFrom().getUserName();
        } else {
            // Если никнейм отсутствует, используем имя и фамилию
            String firstName = update.getMessage().getFrom().getFirstName();
            String lastName = update.getMessage().getFrom().getLastName();
            return firstName + (lastName != null ? " " + lastName : "");
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
    private void handleProductAddition(long chatId, String messageText, Update update) {
        if (Product.categoryState.get(chatId) == null) {
            Category chosenCategory = findCategoryByName(messageText);
            if (chosenCategory != null) {
                Product.categoryState.put(chatId, chosenCategory);
                sendResponse(chatId, "Вы выбрали: " + chosenCategory.getName() + ". Теперь введите название продукта: ");
            } else {
                sendResponse(chatId, "Неверная категория. Выберите правильную категорию:");
            }
        } else if (Product.addingState.get(chatId).isEmpty()) {
            Product.addingState.put(chatId, messageText);
            sendResponse(chatId, "Введите цену товара:");
            Product.priceState.put(chatId, null);
        } else if (Product.priceState.get(chatId) == null) {
            try {
                String price = messageText;
                Product.priceState.put(chatId, price);
                sendResponse(chatId, "Введите описание товара:");
                Product.descriptionState.put(chatId, "");
            } catch (NumberFormatException e) {
                sendResponse(chatId, "Пожалуйста, введите корректную цену:");
            }
        } else if (Product.descriptionState.get(chatId).isEmpty()) {
            Product.descriptionState.put(chatId, messageText);
            sendResponse(chatId, "Введите ваш Telegram handle (никнейм):");
            Product.phoneState.put(chatId, null);
        } else if (Product.phoneState.get(chatId) == null) {
            Product.phoneState.put(chatId, messageText);
            sendResponse(chatId, "Теперь отправьте фото товара:");
        }
    }

    private void handleDeleteProduct(long chatId, String productName, Update update) {
        // Получаем Telegram handle пользователя из Telegram
        String telegramHandleFromTelegram = getUsernameFromUpdate(update);

        // Получаем товар из базы данных
        Product product = Database.getProductByName(productName);
        if (product == null) {
            sendResponse(chatId, "Товар с именем " + productName + " не найден.");
            return;
        }

        // Сравниваем Telegram handle из Telegram и Telegram handle, который был введен при добавлении товара
        if (!product.getTelegramHandle().equalsIgnoreCase(telegramHandleFromTelegram)) {
            sendResponse(chatId, "У вас нет прав на удаление этого товара. Введенный вами Telegram handle не совпадает с тем, который был указан при добавлении товара.");
            return;
        }

        // Удаляем товар
        boolean isDeleted = Database.deleteProductByName(productName);
        if (isDeleted) {
            sendResponse(chatId, "Товар " + productName + " успешно удален.");
        } else {
            sendResponse(chatId, "Произошла ошибка при удалении товара.");
        }
    }

    private void handlePhotoMessage(long chatId, Update update) {
        String productName = Product.addingState.get(chatId);
        if (productName != null && !productName.isEmpty()) {
            if (Product.categoryState.get(chatId) == null ||
                    Product.priceState.get(chatId) == null ||
                    Product.descriptionState.get(chatId).isEmpty() ||
                    Product.phoneState.get(chatId) == null) {
                sendResponse(chatId, "Не все данные были введены. Пожалуйста, проверьте и заполните все поля.");
                return;
            }

            String photoId = update.getMessage().getPhoto().get(0).getFileId();
            Product product = new Product(
                    productName,
                    Product.categoryState.get(chatId),
                    new BigDecimal(Product.priceState.get(chatId)),
                    Product.descriptionState.get(chatId),
                    Product.phoneState.get(chatId),
                    photoId
            );

            saveProductToDatabase(product);
            sendResponse(chatId, "Фото получено! Ваш товар был успешно добавлен.");
            sendWelcomeMessage(chatId);

            Product.addingState.remove(chatId);
            Product.categoryState.remove(chatId);
            Product.priceState.remove(chatId);
            Product.descriptionState.remove(chatId);
            Product.phoneState.remove(chatId);
        } else {
            sendResponse(chatId, "Произошла ошибка. Пожалуйста, начните снова.");
        }
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
        Product.addingState.put(chatId, "");
        Product.categoryState.put(chatId, null);
        sendCategorySelection(chatId);
    }

    private void sendWelcomeMessage(long chatId) {
        String welcomeText = "Добро пожаловать в Marketplace Bot! Используйте кнопки ниже для взаимодействия.";
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(welcomeText);
        message.setReplyMarkup(KeyboardUtils.createMainKeyboard());

        try {
            bot.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendCategorySelection(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Пожалуйста, выберите категорию: ");
        message.setReplyMarkup(KeyboardUtils.createCategoryKeyboard());

        try {
            bot.execute(message);
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
            bot.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendPhoto(long chatId, Product product) {
        SendPhoto photoMessage = new SendPhoto();
        photoMessage.setChatId(String.valueOf(chatId));

        // Проверяем, что идентификатор файла не пустой
        if (product.getPhotoId() == null || product.getPhotoId().isEmpty()) {
            sendResponse(chatId, "Фото для товара не найдено.");
            return;
        }

        // Устанавливаем фото по идентификатору
        photoMessage.setPhoto(new InputFile(product.getPhotoId()));

        // Устанавливаем подпись к фото
        String caption = "Товар: " + product.getName() + "\n" +
                "Категория: " + product.getCategory().getName() + "\n" + // Добавляем категорию
                "Описание: " + product.getDescription() + "\n" +
                "Цена: " + product.getPrice() + "\n" +
                "Продавец: " + product.getTelegramHandle();
        photoMessage.setCaption(caption);

        try {
            bot.execute(photoMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
            sendResponse(chatId, "Ошибка при отправке фото: " + e.getMessage());
        }
    }

    private void sendResponse(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        try {
            bot.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
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
}