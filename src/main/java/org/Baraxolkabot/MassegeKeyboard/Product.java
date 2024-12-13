package org.Baraxolkabot.MassegeKeyboard;

import java.math.BigDecimal;
import org.Baraxolkabot.Category.Category;

public class Product {
    private final String name;
    private final String photoId;
    private final String description;
    private final BigDecimal price;
    private final String telegramHandle;
    private final Category categoryName;


    public Product(String name,Category categoryName, BigDecimal price, String description, String telegramHandle, String photoId) {
        this.name = name;
        this.categoryName = categoryName;
        this.price = price;
        this.description = description;
        this.telegramHandle = formatTelegramHandle(telegramHandle);
        this.photoId = photoId;
    }

    public String getName() {
        return name;
    }

    public String getPhotoId() {
        return photoId;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getTelegramHandle() {
        return telegramHandle;
    }

    public Category getCategory() {
        return categoryName;
    }

    private String formatTelegramHandle(String handle) {
        if (handle == null) {
        return "@unknown";
        }
        return handle.startsWith("@") ? handle : "@" + handle;
    }

}

