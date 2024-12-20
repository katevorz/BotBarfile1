package org.Baraxolkabot.productService;

import org.Baraxolkabot.base.Database;
import org.Baraxolkabot.botBaraxolka.Product;
import org.Baraxolkabot.keyboard_and_Massege_send.KeyboardUtils;
import org.Baraxolkabot.category.Category;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.math.BigDecimal;
import java.util.List;

public class ProductService {

    private final TelegramLongPollingBot bot;

    public ProductService(TelegramLongPollingBot bot) {
        this.bot = bot;
    }

    public void initiateSearch(long chatId) {
        Product.enable.put(chatId, "Searching");
        sendResponse(chatId, "Введите ключевое слово для поиска:");
    }

    public void handleProductAddition(long chatId, String messageText, Update update) {
        if (Product.categoryState.get(chatId) == null) {
            Category chosenCategory = findCategoryByName(messageText);
            if (chosenCategory != null) {
                Product.categoryState.put(chatId, chosenCategory);
                sendResponse(chatId, "Вы выбрали: " + chosenCategory.getName() + ". Теперь введите название продукта: ");
            } else {
                sendResponse(chatId, "Неверная категория. Выберите правильную категорию:");
            }
        } else if (Product.enable.get(chatId).isEmpty()) {
            Product.enable.put(chatId, messageText);
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
            sendResponse(chatId, "Введите ваш Telegram handle (никнейм через @):");
            Product.phoneState.put(chatId, null);
        } else if (Product.phoneState.get(chatId) == null) {
            Product.phoneState.put(chatId, messageText);
            sendResponse(chatId, "Теперь отправьте фото товара:");
        }
    }

    public void handleDeleteProduct(long chatId, String productName, Update update) {
        String telegramHandleFromTelegram = getUsernameFromUpdate(update);
        Product product = Database.getProductByName(productName);
        if (product == null) {
            sendResponse(chatId, "Товар с именем " + productName + " не найден.");
            return;
        }

        if (!product.getTelegramHandle().equalsIgnoreCase(telegramHandleFromTelegram)) {
            sendResponse(chatId, "У вас нет прав на удаление этого товара.");
            return;
        }

        boolean isDeleted = Database.deleteProductByName(productName);
        if (isDeleted) {
            sendResponse(chatId, "Товар " + productName + " успешно удален.");
        } else {
            sendResponse(chatId, "Произошла ошибка при удалении товара.");
        }
    }

    public void handlePhotoMessage(long chatId, Update update) {
        String productName = Product.enable.get(chatId);
        if (productName != null && !productName.isEmpty()) {
            if (Product.categoryState.get(chatId) == null ||
                    Product.priceState.get(chatId) == null ||
                    Product.descriptionState.get(chatId).isEmpty() ||
                    Product.phoneState.get(chatId) == null) {
                sendResponse(chatId, "Не все данные были введены.");
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

            Product.enable.remove(chatId);
            Product.categoryState.remove(chatId);
            Product.priceState.remove(chatId);
            Product.descriptionState.remove(chatId);
            Product.phoneState.remove(chatId);
        } else {
            sendResponse(chatId, "Произошла ошибка. Пожалуйста, начните снова.");
        }
    }

    public void performSearch(long chatId, String keyword) {
        List<Product> results = Database.searchProducts(keyword);

        if (results.isEmpty()) {
            sendResponse(chatId, "По вашему запросу ничего не найдено.");
        } else {
            for (Product product : results) {
                sendPhoto(chatId, product);
            }
        }
    }

    public void initiateProductAddition(long chatId) {
        Product.enable.put(chatId, "");
        Product.categoryState.put(chatId, null);
        sendCategorySelection(chatId);
    }

    public void sendWelcomeMessage(long chatId) {
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


    public void sendListings(long chatId) {
        List<Product> products = Database.getProducts();
        if (products.isEmpty()) {
            sendResponse(chatId, "Список объявлений пуст.");
            return;
        }

        for (Product product : products) {
            sendPhoto(chatId, product);
        }
    }

    public void sendDeleteProductPrompt(long chatId) {
        String promptText = "Введите название товара, который вы хотите удалить:";
        sendResponse(chatId, promptText);
        Product.enable.put(chatId, "Deleting");
    }

    public void sendPhoto(long chatId, Product product) {
        SendPhoto photoMessage = new SendPhoto();
        photoMessage.setChatId(String.valueOf(chatId));

        if (product.getPhotoId() == null || product.getPhotoId().isEmpty()) {
            sendResponse(chatId, "Фото для товара не найдено.");
            return;
        }

        photoMessage.setPhoto(new InputFile(product.getPhotoId()));

        String caption = "Товар: " + product.getName() + "\n" +
                "Категория: " + product.getCategory().getName() + "\n" +
                "Описание: " + product.getDescription() + "\n" +
                "Цена: " + product.getPrice() + "\n" +
                "Пишите сюда, если захотели купить товар, продавец: " + product.getTelegramHandle();
        photoMessage.setCaption(caption);

        try {
            bot.execute(photoMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
            sendResponse(chatId, "Ошибка при отправке фото: " + e.getMessage());
        }
    }

    public void sendResponse(long chatId, String text) {
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

    private Category findCategoryByName(String name) {
        for (Category category : Product.categories) {
            if (category.getName().equalsIgnoreCase(name)) {
                return category;
            }
        }
        return null;
    }

    private String getUsernameFromUpdate(Update update) {

        if (update.getMessage().getFrom().getUserName() != null) {
            return "@" + update.getMessage().getFrom().getUserName();
        }

        String firstName = update.getMessage().getFrom().getFirstName();
        String lastName = update.getMessage().getFrom().getLastName();
        return firstName + (lastName != null ? " " + lastName : "");
    }
}
