package org.Baraxolkabot.keyboard_and_Massege_send;

import org.Baraxolkabot.category.Category;
import org.Baraxolkabot.botBaraxolka.Product;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;

public class KeyboardUtils {

    public static ReplyKeyboardMarkup createMainKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add("Добавить товар");
        row1.add("Показать все товары");

        KeyboardRow row2 = new KeyboardRow();
        row2.add("Поиск товара");
        row2.add("Удалить товар");

        keyboard.add(row1);
        keyboard.add(row2);
        keyboardMarkup.setKeyboard(keyboard);

        return keyboardMarkup;
    }

    public static ReplyKeyboardMarkup createCategoryKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();

        for (Category category : Product.categories) {
            KeyboardRow row = new KeyboardRow();
            row.add(category.getName());
            keyboard.add(row);
        }

        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }
}