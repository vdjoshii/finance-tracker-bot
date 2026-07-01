package com.financetracker.bot.config;

import com.financetracker.bot.model.Category;
import com.financetracker.bot.repository.CategoryRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Populates a sensible set of default categories and keywords on first run,
 * implementing "Layer 2: Keyword/Rule-Based Matching" from the categorization
 * strategy. Only runs if the categories table is empty, so it never overwrites
 * categories the user has since customized.
 */
@Component
public class DefaultCategorySeeder implements CommandLineRunner {

    private final CategoryRepository categoryRepository;

    public DefaultCategorySeeder(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public void run(String... args) {
        if (categoryRepository.count() > 0) {
            return;
        }

        categoryRepository.saveAll(List.of(
                new Category("Food", List.of(
                        "lunch", "dinner", "breakfast", "restaurant", "zomato", "swiggy",
                        "dmart", "grocery", "groceries", "cafe", "coffee", "snack")),
                new Category("Transport", List.of(
                        "uber", "ola", "auto", "taxi", "petrol", "fuel", "diesel",
                        "bus", "train", "metro", "rapido", "parking")),
                new Category("Shopping", List.of(
                        "amazon", "flipkart", "myntra", "clothes", "shoes", "mall",
                        "shopping", "electronics")),
                new Category("Bills", List.of(
                        "electricity", "wifi", "internet", "recharge", "rent",
                        "mobile", "broadband", "subscription", "emi")),
                new Category("Entertainment", List.of(
                        "movie", "netflix", "spotify", "concert", "game", "pvr", "bookmyshow")),
                new Category("Health", List.of(
                        "medicine", "pharmacy", "doctor", "hospital", "clinic", "gym")),
                new Category("Lena Baki", List.of()),
                new Category("Dena Baki", List.of()),
                new Category("Uncategorized", List.of())
        ));
    }
}