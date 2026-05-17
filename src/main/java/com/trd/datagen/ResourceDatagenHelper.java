package com.trd.datagen;

import com.trd.main.ResourceRegistry;
import com.trd.main.ResourceRegistry.ResourceEntry;
import com.trd.main.ResourceRegistry.ResourceType;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.LanguageProvider;

/**
 * Вспомогательный класс для датагенерации ресурсов
 */
public class ResourceDatagenHelper {

    /**
     * Генерация моделей предметов для всех ресурсов
     */
    public static void generateItemModels(ItemModelProvider provider) {
        for (ResourceEntry resource : ResourceRegistry.getResources()) {
            String name = resource.name;

            // Основная единица (слиток/гранула/кристалл)
            simpleItem(provider, resource.mainUnit);

            // Мелкая единица (самородок/кусочек/осколок)
            if (resource.smallUnit != null) {
                simpleItem(provider, resource.smallUnit);
            }

        }
    }

    /**
     * Генерация переводов для всех ресурсов
     */
    public static void generateTranslations(LanguageProvider provider, String locale) {
        for (ResourceEntry resource : ResourceRegistry.getResources()) {
            String name = resource.name;
            ResourceType type = resource.type;

            if (locale.equals("ru_ru")) {
                addRussianTranslations(provider, resource);
            } else if (locale.equals("uk_ua")) {
                addUkrainianTranslations(provider, resource);
            } else {
                addEnglishTranslations(provider, resource);
            }
        }
    }

    // ============ Английский ============
    private static void addEnglishTranslations(LanguageProvider provider, ResourceEntry resource) {
        String displayName = capitalize(resource.name.replace("_", " "));
        String mainUnit = resource.type.mainUnit;
        String smallUnit = resource.type.smallUnit;
        String blockName = resource.type.blockName;

        provider.add(resource.mainUnit.get(), displayName + " " + capitalize(mainUnit));

        if (resource.smallUnit != null) {
            provider.add(resource.smallUnit.get(), displayName + " " + capitalize(smallUnit));
        }

        if (resource.block != null) {
            provider.add(resource.block.get(), "Block of " + displayName);
        }
    }

    // ============ Русский ============
    private static void addRussianTranslations(LanguageProvider provider, ResourceEntry resource) {
        String name = resource.name;
        ResourceType type = resource.type;

        switch (name) {
            case "steel" -> {
                provider.add(resource.mainUnit.get(), "Стальной " + getRussianUnitName(type.mainUnit, true));
                if (resource.smallUnit != null) provider.add(resource.smallUnit.get(), "Стальной " + getRussianUnitName(type.smallUnit, true));
                if (resource.block != null) provider.add(resource.block.get(), "Стальной блок");
            }
            case "aluminum" -> {
                provider.add(resource.mainUnit.get(), "Алюминиевый " + getRussianUnitName(type.mainUnit, false));
                if (resource.smallUnit != null) provider.add(resource.smallUnit.get(), "Алюминиевый " + getRussianUnitName(type.smallUnit, false));
                if (resource.block != null) provider.add(resource.block.get(), "Алюминиевый блок");
            }
            case "bronze" -> {
                provider.add(resource.mainUnit.get(), "Бронзовый " + getRussianUnitName(type.mainUnit, false));
                if (resource.smallUnit != null) provider.add(resource.smallUnit.get(), "Бронзовый " + getRussianUnitName(type.smallUnit, false));
                if (resource.block != null) provider.add(resource.block.get(), "Бронзовый блок");
            }
            case "zinc" -> {
                provider.add(resource.mainUnit.get(), "Цинковый " + getRussianUnitName(type.mainUnit, false));
                if (resource.smallUnit != null) provider.add(resource.smallUnit.get(), "Цинковый " + getRussianUnitName(type.smallUnit, false));
                if (resource.block != null) provider.add(resource.block.get(), "Цинковый блок");
            }
            case "tin" -> {
                provider.add(resource.mainUnit.get(), "Оловянный " + getRussianUnitName(type.mainUnit, false));
                if (resource.smallUnit != null) provider.add(resource.smallUnit.get(), "Оловянный " + getRussianUnitName(type.smallUnit, false));
                if (resource.block != null) provider.add(resource.block.get(), "Оловянный блок");
            }
            case "industrial_copper" -> {
                provider.add(resource.mainUnit.get(), "Промышленномедный " + getRussianUnitName(type.mainUnit, false));
                if (resource.smallUnit != null) provider.add(resource.smallUnit.get(), "Промышленномедный " + getRussianUnitName(type.smallUnit, false));
                if (resource.block != null) provider.add(resource.block.get(), "Промышленномедный блок");
            }
            case "titanium" -> {
                provider.add(resource.mainUnit.get(), "Титановый " + getRussianUnitName(type.mainUnit, false));
                if (resource.smallUnit != null) provider.add(resource.smallUnit.get(), "Титановый " + getRussianUnitName(type.smallUnit, false));
                if (resource.block != null) provider.add(resource.block.get(), "Титановый блок");
            }
            case "lead" -> {
                provider.add(resource.mainUnit.get(), "Свинцовый " + getRussianUnitName(type.mainUnit, false));
                if (resource.smallUnit != null) provider.add(resource.smallUnit.get(), "Свинцовый " + getRussianUnitName(type.smallUnit, false));
                if (resource.block != null) provider.add(resource.block.get(), "Свинцовый блок");
            }
            case "beryllium" -> {
                provider.add(resource.mainUnit.get(), "Бериллиевый " + getRussianUnitName(type.mainUnit, false));
                if (resource.smallUnit != null) provider.add(resource.smallUnit.get(), "Бериллиевый " + getRussianUnitName(type.smallUnit, false));
                if (resource.block != null) provider.add(resource.block.get(), "Бериллиевый блок");
            }
            case "tungsten" -> {
                provider.add(resource.mainUnit.get(), "Вольфрамовый " + getRussianUnitName(type.mainUnit, false));
                if (resource.smallUnit != null) provider.add(resource.smallUnit.get(), "Вольфрамовый " + getRussianUnitName(type.smallUnit, false));
                if (resource.block != null) provider.add(resource.block.get(), "Вольфрамовый блок");
            }
            case "neodymium" -> {
                provider.add(resource.mainUnit.get(), "Неодимовый " + getRussianUnitName(type.mainUnit, false));
                if (resource.smallUnit != null) provider.add(resource.smallUnit.get(), "Неодимовый " + getRussianUnitName(type.smallUnit, false));
                if (resource.block != null) provider.add(resource.block.get(), "Неодимовый блок");
            }
            default -> {
                String adj = capitalize(name);
                provider.add(resource.mainUnit.get(), adj + " " + getRussianUnitName(type.mainUnit, false));
                if (resource.smallUnit != null) provider.add(resource.smallUnit.get(), adj + " " + getRussianUnitName(type.smallUnit, false));
                if (resource.block != null) provider.add(resource.block.get(), adj + " блок");
            }
        }
    }

    // ============ Украинский ============
    private static void addUkrainianTranslations(LanguageProvider provider, ResourceEntry resource) {
        String name = resource.name;
        ResourceType type = resource.type;

        switch (name) {
            case "steel" -> {
                provider.add(resource.mainUnit.get(), "Сталевий " + getUkrainianUnitName(type.mainUnit, true));
                if (resource.smallUnit != null) provider.add(resource.smallUnit.get(), "Сталевий " + getUkrainianUnitName(type.smallUnit, true));
                if (resource.block != null) provider.add(resource.block.get(), "Сталевий блок");
            }
            case "aluminum" -> {
                provider.add(resource.mainUnit.get(), "Алюмінієвий " + getUkrainianUnitName(type.mainUnit, false));
                if (resource.smallUnit != null) provider.add(resource.smallUnit.get(), "Алюмінієвий " + getUkrainianUnitName(type.smallUnit, false));
                if (resource.block != null) provider.add(resource.block.get(), "Алюмінієвий блок");
            }
            case "bronze" -> {
                provider.add(resource.mainUnit.get(), "Бронзовий " + getUkrainianUnitName(type.mainUnit, false));
                if (resource.smallUnit != null) provider.add(resource.smallUnit.get(), "Бронзовий " + getUkrainianUnitName(type.smallUnit, false));
                if (resource.block != null) provider.add(resource.block.get(), "Бронзовий блок");
            }
            case "zinc" -> {
                provider.add(resource.mainUnit.get(), "Цинковий " + getUkrainianUnitName(type.mainUnit, false));
                if (resource.smallUnit != null) provider.add(resource.smallUnit.get(), "Цинковий " + getUkrainianUnitName(type.smallUnit, false));
                if (resource.block != null) provider.add(resource.block.get(), "Цинковий блок");
            }
            case "tin" -> {
                provider.add(resource.mainUnit.get(), "Олов'яний " + getUkrainianUnitName(type.mainUnit, false));
                if (resource.smallUnit != null) provider.add(resource.smallUnit.get(), "Олов'яний " + getUkrainianUnitName(type.smallUnit, false));
                if (resource.block != null) provider.add(resource.block.get(), "Олов'яний блок");
            }
            case "industrial_copper" -> {
                provider.add(resource.mainUnit.get(), "Промисловомедний " + getUkrainianUnitName(type.mainUnit, false));
                if (resource.smallUnit != null) provider.add(resource.smallUnit.get(), "Промисловомедний " + getUkrainianUnitName(type.smallUnit, false));
                if (resource.block != null) provider.add(resource.block.get(), "Промисловомедний блок");
            }
            case "titanium" -> {
                provider.add(resource.mainUnit.get(), "Титановий " + getUkrainianUnitName(type.mainUnit, false));
                if (resource.smallUnit != null) provider.add(resource.smallUnit.get(), "Титановий " + getUkrainianUnitName(type.smallUnit, false));
                if (resource.block != null) provider.add(resource.block.get(), "Титановий блок");
            }
            default -> {
                String adj = capitalize(name);
                provider.add(resource.mainUnit.get(), adj + " " + getUkrainianUnitName(type.mainUnit, false));
                if (resource.smallUnit != null) provider.add(resource.smallUnit.get(), adj + " " + getUkrainianUnitName(type.smallUnit, false));
                if (resource.block != null) provider.add(resource.block.get(), adj + " блок");
            }
        }
    }

    // ============ Вспомогательные методы ============

    private static void simpleItem(ItemModelProvider provider,
                                   net.minecraftforge.registries.RegistryObject<net.minecraft.world.item.Item> item) {
        provider.withExistingParent(item.getId().getPath(),
                new ResourceLocation("item/generated")).texture("layer0",
                new ResourceLocation("trd", "item/" + item.getId().getPath()));
    }

    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        String[] words = str.split("_");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(word.substring(0, 1).toUpperCase())
                        .append(word.substring(1))
                        .append(" ");
            }
        }
        return result.toString().trim();
    }

    private static String getRussianUnitName(String unit, boolean isMasculine) {
        return switch (unit) {
            case "ingot" -> "слиток";
            case "nugget" -> "самородок";
            case "granule" -> "гранула";
            case "piece" -> "кусочек";
            case "crystal" -> "кристалл";
            case "shard" -> "осколок";
            case "gem" -> "самоцвет";
            case "fragment" -> "фрагмент";
            case "lump" -> "кусок";
            case "dust" -> "пыль";
            default -> unit;
        };
    }

    private static String getUkrainianUnitName(String unit, boolean isMasculine) {
        return switch (unit) {
            case "ingot" -> "злиток";
            case "nugget" -> "самородок";
            case "granule" -> "гранула";
            case "piece" -> "шматочок";
            case "crystal" -> "кристал";
            case "shard" -> "уламок";
            case "gem" -> "самоцвіт";
            case "fragment" -> "фрагмент";
            case "lump" -> "брила";
            case "dust" -> "пил";
            default -> unit;
        };
    }
}