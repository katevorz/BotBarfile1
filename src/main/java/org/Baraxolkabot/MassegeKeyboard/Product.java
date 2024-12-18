package org.Baraxolkabot.MassegeKeyboard;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import org.Baraxolkabot.Category.Category;

public class Product {
    private final String name;
    private final String photoId;
    private final String description;
    private final BigDecimal price;
    private final String telegramHandle;
    private final Category categoryName;


    public static final Map<Long, String> addingState = new HashMap<>();
    public static final Map<Long, String> priceState = new HashMap<>();
    public static final Map<Long, String> descriptionState = new HashMap<>();
    public static final Map<Long, String> phoneState = new HashMap<>();
    public static List<Category> categories = new ArrayList<>();
    public static final Map<Long, Category> categoryState = new HashMap<>();


    public Product(String name,Category categoryName, BigDecimal price, String description, String telegramHandle, String photoId) {
        this.name = name;
        this.categoryName = categoryName;
        this.price = price;
        this.description = description;
        this.telegramHandle = telegramHandle;
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

}

