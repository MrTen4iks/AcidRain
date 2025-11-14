package org.acidRain.acidRain.registry;

import org.acidRain.acidRain.AcidRain;
import org.acidRain.acidRain.managers.LocalizationManager;
import org.acidRain.acidRain.registry.item.ProtectionSuit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ShapedRecipe;

public class RecipeRegistry {
    private final AcidRain plugin;
    private final LocalizationManager localizationManager;
    private final ProtectionSuit protectionSuit;

    public RecipeRegistry(AcidRain plugin, LocalizationManager localizationManager, ProtectionSuit protectionSuit) {
        this.plugin = plugin;
        this.localizationManager = localizationManager;
        this.protectionSuit = protectionSuit;
    }

    public void registerRecipes() {
        try {
            registerHelmetRecipe();
            registerChestplateRecipe();
            registerLeggingsRecipe();
            registerBootsRecipe();
            registerFullSuitRecipe();
        } catch (Exception e) {
            plugin.getLogger().warning(localizationManager.getMessage("console.errors.recipe_registration", e.getMessage()));
        }
    }

    private void registerHelmetRecipe() {
        ShapedRecipe helmetRecipe = new ShapedRecipe(
                new NamespacedKey(plugin, "acidrain_helmet"),
                protectionSuit.createArmorPiece(Material.LEATHER_HELMET, localizationManager.getMessage("items.helmet_name"))
        );
        helmetRecipe.shape("LLL", "L L");
        helmetRecipe.setIngredient('L', Material.LEATHER);
        plugin.getServer().addRecipe(helmetRecipe);
        plugin.getLogger().info(localizationManager.getMessage("console.info.helmet_recipe"));
    }

    private void registerChestplateRecipe() {
        ShapedRecipe chestplateRecipe = new ShapedRecipe(
                new NamespacedKey(plugin, "acidrain_chestplate"),
                protectionSuit.createArmorPiece(Material.LEATHER_CHESTPLATE, localizationManager.getMessage("items.chestplate_name"))
        );
        chestplateRecipe.shape("L L", "LLL", "LLL");
        chestplateRecipe.setIngredient('L', Material.LEATHER);
        plugin.getServer().addRecipe(chestplateRecipe);
        plugin.getLogger().info(localizationManager.getMessage("console.info.chestplate_recipe"));
    }

    private void registerLeggingsRecipe() {
        ShapedRecipe leggingsRecipe = new ShapedRecipe(
                new NamespacedKey(plugin, "acidrain_leggings"),
                protectionSuit.createArmorPiece(Material.LEATHER_LEGGINGS, localizationManager.getMessage("items.leggings_name"))
        );
        leggingsRecipe.shape("LLL", "L L", "L L");
        leggingsRecipe.setIngredient('L', Material.LEATHER);
        plugin.getServer().addRecipe(leggingsRecipe);
        plugin.getLogger().info(localizationManager.getMessage("console.info.leggings_recipe"));
    }

    private void registerBootsRecipe() {
        ShapedRecipe bootsRecipe = new ShapedRecipe(
                new NamespacedKey(plugin, "acidrain_boots"),
                protectionSuit.createArmorPiece(Material.LEATHER_BOOTS, localizationManager.getMessage("items.boots_name"))
        );
        bootsRecipe.shape("L L", "L L");
        bootsRecipe.setIngredient('L', Material.LEATHER);
        plugin.getServer().addRecipe(bootsRecipe);
        plugin.getLogger().info(localizationManager.getMessage("console.info.boots_recipe"));
    }

    private void registerFullSuitRecipe() {
        ShapedRecipe fullSuitRecipe = new ShapedRecipe(
                new NamespacedKey(plugin, "acidrain_full_suit"),
                protectionSuit.createFullProtectionSuit()
        );

        fullSuitRecipe.shape("LNR", "BTB", "VPE");
        fullSuitRecipe.setIngredient('N', Material.END_CRYSTAL);
        fullSuitRecipe.setIngredient('P', Material.NETHERITE_INGOT);
        fullSuitRecipe.setIngredient('B', Material.BREEZE_ROD);
        fullSuitRecipe.setIngredient('T', Material.TOTEM_OF_UNDYING);
        fullSuitRecipe.setIngredient('V', Material.LEATHER_HELMET);
        fullSuitRecipe.setIngredient('E', Material.LEATHER_CHESTPLATE);
        fullSuitRecipe.setIngredient('R', Material.LEATHER_BOOTS);
        fullSuitRecipe.setIngredient('L', Material.LEATHER_LEGGINGS);

        plugin.getServer().addRecipe(fullSuitRecipe);
        plugin.getLogger().info(localizationManager.getMessage("console.info.full_suit_recipe"));
    }
}

