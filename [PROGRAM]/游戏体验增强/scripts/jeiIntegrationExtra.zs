import mods.jei.category.Custom;
import mods.jei.JEI;
import mods.jei.category.SimpleJeiCategory;
import mods.jei.category.JeiCategory;
import crafttweaker.api.text.TextComponent;
import mods.jei.component.JeiDrawable;
import crafttweaker.api.resource.ResourceLocation;
import crafttweaker.api.item.IItemStack;

// ** Made by DrIceTea **

// DISABLED 黑市  soul_shard.json omega_soul_shard.json
// 商店  shop_pedestal.json
// 宝库锻炉  recipes\gear_recipes.json
// 饰品锻炉  recipes\trinket_recipes.json
// 工具装配台  recipes\tool_recipes.json
// 珠宝  recipes\jewel_crafting_recipes.json



// 黑市  soul_shard.json omega_soul_shard.json


// 商店  shop_pedestal.json
var vendor = JeiCategory.create<Custom>("vendor_shop", new TextComponent("购物基座"), <item:the_vault:shop_pedestal>, [<item:the_vault:shop_pedestal>]) as Custom;
vendor.background = JeiDrawable.blank(180, 90) as JeiDrawable;

for y in 0 .. 4 {
    for x in 0 .. 9 {
        vendor.addDrawable(1 + (x * 20), 5 + (y * 20), JeiDrawable.of(new ResourceLocation("create", "textures/gui/jei/widgets.png") as ResourceLocation, 0, 0, 18, 18) as JeiDrawable);
    }
}
for y in 0 .. 4 {
    for x in 0 .. 9 {
        vendor.addSlot(10 * y + x, 2 + (x * 20), 6 + (y * 20), false);
    }
}

function vendorItem(item as string, minPrice as int, maxPrice as int, chance as int, level as string) as IItemStack {
  return <item:${item}>.withTag({display: {Lore: ["[{\"text\":\"\",\"italic\":false,\"color\":\"gray\"}]", "[{\"text\":\"宝库等级: " + level + "\",\"italic\":false,\"color\":\"gray\"}]", "[{\"text\":\"\",\"italic\":false,\"color\":\"gray\"}]", "[{\"text\":\"最低价格: " + minPrice + "\",\"italic\":false,\"color\":\"gray\"}]", "[{\"text\":\"最高价格: " + maxPrice + "\",\"italic\":false,\"color\":\"gray\"}]", "[{\"text\":\"权重: " + chance + "\",\"italic\":false,\"color\":\"gray\"}]"]}});
}

function vendorInscriptionItem(item as string, minPrice as int, maxPrice as int, chance as int, level as string) as IItemStack {
  return <item:${item}>.withTag({pool: "the_vault:pedestal", display: {Lore: ["[{\"text\":\"\",\"italic\":false,\"color\":\"gray\"}]", "[{\"text\":\"宝库等级: " + level + "\",\"italic\":false,\"color\":\"gray\"}]", "[{\"text\":\"\",\"italic\":false,\"color\":\"gray\"}]", "[{\"text\":\"最低价格: " + minPrice + "\",\"italic\":false,\"color\":\"gray\"}]", "[{\"text\":\"最高价格: " + maxPrice + "\",\"italic\":false,\"color\":\"gray\"}]", "[{\"text\":\"权重: " + chance + "\",\"italic\":false,\"color\":\"gray\"}]"]}});
}

function vendorPoolItem(item as string, nbtpool as string, minPrice as int, maxPrice as int, chance as int, level as string) as IItemStack {
  return <item:${item}>.withTag({pool: nbtpool, display: {Lore: ["[{\"text\":\"\",\"italic\":false,\"color\":\"gray\"}]", "[{\"text\":\"宝库等级: " + level + "\",\"italic\":false,\"color\":\"gray\"}]", "[{\"text\":\"\",\"italic\":false,\"color\":\"gray\"}]", "[{\"text\":\"最低价格: " + minPrice + "\",\"italic\":false,\"color\":\"gray\"}]", "[{\"text\":\"最高价格: " + maxPrice + "\",\"italic\":false,\"color\":\"gray\"}]", "[{\"text\":\"权重: " + chance + "\",\"italic\":false,\"color\":\"gray\"}]"]}});
}

function vendorIdItem(item as string, nbtid as string, minPrice as int, maxPrice as int, chance as int, level as string) as IItemStack {
  return <item:${item}>.withTag({id: nbtid, display: {Lore: ["[{\"text\":\"\",\"italic\":false,\"color\":\"gray\"}]", "[{\"text\":\"宝库等级: " + level + "\",\"italic\":false,\"color\":\"gray\"}]", "[{\"text\":\"\",\"italic\":false,\"color\":\"gray\"}]", "[{\"text\":\"最低价格: " + minPrice + "\",\"italic\":false,\"color\":\"gray\"}]", "[{\"text\":\"最高价格: " + maxPrice + "\",\"italic\":false,\"color\":\"gray\"}]", "[{\"text\":\"权重: " + chance + "\",\"italic\":false,\"color\":\"gray\"}]"]}});
}

vendor.addRecipe([vendorItem("the_vault:plain_burger", 1, 1, 144, "0-29"),
vendorItem("the_vault:cheese_burger", 1, 2, 90, "0-29"),
vendorItem("sophisticatedbackpacks:iron_backpack", 8, 12, 36, "0-29"),
vendorItem("the_vault:helmet", 4, 8, 72, "0-29"),
vendorItem("the_vault:chestplate", 4, 8, 72, "0-29"),
vendorItem("the_vault:leggings", 4, 8, 72, "0-29"),
vendorItem("the_vault:boots", 4, 8, 72, "0-29"),
vendorItem("the_vault:sword", 4, 8, 72, "0-29"),
vendorItem("the_vault:axe", 4, 8, 72, "0-29"),
vendorItem("the_vault:shield", 4, 8, 58, "0-29"),
vendorItem("the_vault:wand", 4, 8, 58, "0-29"),
vendorItem("the_vault:focus", 4, 8, 58, "0-29"),
vendorItem("the_vault:magnet", 4, 8, 58, "0-29"),
vendorItem("the_vault:knowledge_star", 12, 16, 36, "0-29")], []);

vendor.addRecipe([vendorItem("the_vault:double_cheese_burger", 2, 4, 144, "30-49"),
vendorItem("the_vault:deluxe_cheese_burger", 3, 6, 90, "30-49"),
vendorItem("sophisticatedbackpacks:iron_backpack", 8, 12, 61, "30-49"),
vendorItem("sophisticatedbackpacks:gold_backpack", 16, 24, 61, "30-49"),
vendorItem("the_vault:helmet", 16, 24, 124, "30-49"),
vendorItem("the_vault:chestplate", 16, 24, 124, "30-49"),
vendorItem("the_vault:leggings", 16, 24, 124, "30-49"),
vendorItem("the_vault:boots", 16, 24, 124, "30-49"),
vendorItem("the_vault:sword", 16, 24, 124, "30-49"),
vendorItem("the_vault:axe", 16, 24, 124, "30-49"),
vendorItem("the_vault:shield", 16, 24, 100, "30-49"),
vendorItem("the_vault:wand", 16, 24, 100, "30-49"),
vendorItem("the_vault:focus", 16, 24, 100, "30-49"),
vendorItem("the_vault:magnet", 16, 24, 100, "30-49"),
vendorPoolItem("the_vault:inscription", "the_vault:pedestal", 20, 32, 56, "30-49"),
vendorItem("the_vault:jewel_pouch", 16, 32, 80, "30-49"),
vendorItem("the_vault:sour_orange", 16, 24, 61, "30-49"),
vendorItem("the_vault:trinket_scrap", 20, 32, 61, "30-49"),
vendorItem("the_vault:trinket", 28, 64, 30, "30-49"),
vendorItem("the_vault:artifact_fragment", 24, 64, 14, "30-49"),
vendorItem("the_vault:knowledge_star", 12, 16, 61, "30-49"),
vendorItem("the_vault:vault_catalyst_chaos", 32, 64, 30, "30-49"),
vendorItem("the_vault:crystal_seal_cake", 8, 16, 61, "30-49"),
vendorItem("the_vault:mystery_egg", 4, 8, 157, "30-49"),
vendorItem("the_vault:bitter_lemon", 8, 16, 124, "30-49"),
vendorItem("the_vault:hardened_chest_scroll", 16, 32, 30, "30-49"),
vendorItem("the_vault:flesh_chest_scroll", 32, 64, 14, "30-49"),
vendorItem("the_vault:enigma_chest_scroll", 64, 128, 4, "30-49"),
vendorItem("the_vault:mod_box", 6, 12, 157, "30-49"),
vendorItem("the_vault:vault_catalyst_infused", 12, 24, 320, "30-49")], []);

vendor.addRecipe([vendorItem("the_vault:crispy_deluxe_cheese_burger", 4, 8, 144, "50-64"),
vendorItem("the_vault:salty_deluxe_cheese_burger", 6, 10, 90, "50-64"),
vendorItem("sophisticatedbackpacks:iron_backpack", 8, 12, 53, "50-64"),
vendorItem("sophisticatedbackpacks:gold_backpack", 16, 24, 53, "50-64"),
vendorItem("sophisticatedbackpacks:diamond_backpack", 24, 48, 26, "50-64"),
vendorItem("the_vault:helmet", 24, 32, 111, "50-64"),
vendorItem("the_vault:chestplate", 24, 32, 111, "50-64"),
vendorItem("the_vault:leggings", 24, 32, 111, "50-64"),
vendorItem("the_vault:boots", 24, 32, 111, "50-64"),
vendorItem("the_vault:sword", 24, 32, 111, "50-64"),
vendorItem("the_vault:axe", 24, 32, 111, "50-64"),
vendorItem("the_vault:shield", 24, 32, 88, "50-64"),
vendorItem("the_vault:wand", 24, 32, 88, "50-64"),
vendorItem("the_vault:focus", 24, 32, 88, "50-64"),
vendorItem("the_vault:magnet", 24, 32, 88, "50-64"),
vendorPoolItem("the_vault:inscription", "the_vault:pedestal", 24, 48, 54, "50-64"),
vendorItem("the_vault:jewel_pouch", 16, 32, 76, "50-64"),
vendorItem("the_vault:sour_orange", 20, 32, 26, "50-64"),
vendorItem("the_vault:trinket_scrap", 20, 32, 53, "50-64"),
vendorItem("the_vault:trinket", 32, 64, 26, "50-64"),
vendorItem("the_vault:artifact_fragment", 24, 64, 10, "50-64"),
vendorItem("the_vault:unidentified_treasure_key", 32, 64, 10, "50-64"),
vendorItem("the_vault:opportunistic_focus", 24, 64, 26, "50-64"),
vendorItem("the_vault:cryonic_focus", 24, 64, 26, "50-64"),
vendorItem("the_vault:pyretic_focus", 24, 64, 26, "50-64"),
vendorItem("the_vault:vorpal_focus", 24, 64, 20, "50-64"),
vendorItem("the_vault:empowered_chaotic_focus", 24, 32, 40, "50-64"),
vendorItem("the_vault:resilient_focus", 16, 32, 53, "50-64"),
vendorItem("the_vault:fundamental_focus", 4, 12, 88, "50-64"),
vendorItem("the_vault:lost_bounty", 24, 64, 26, "50-64"),
vendorItem("the_vault:capstone_dungeon_hunter", 24, 64, 26, "50-64"),
vendorItem("the_vault:capstone_treasure_hunter", 24, 64, 10, "50-64"),
vendorItem("the_vault:capstone_companion_hunt", 48, 128, 10, "50-64"),
vendorItem("the_vault:key_piece", 4, 8, 139, "50-64"),
vendorItem("the_vault:vault_catalyst_chaos", 32, 64, 26, "50-64"),
vendorItem("the_vault:crystal_seal_cake", 12, 24, 65, "50-64"),
vendorItem("the_vault:mystery_egg", 4, 8, 139, "50-64"),
vendorItem("the_vault:bitter_lemon", 8, 16, 111, "50-64"),
vendorItem("the_vault:hardened_chest_scroll", 16, 32, 26, "50-64"),
vendorItem("the_vault:flesh_chest_scroll", 32, 64, 10, "50-64"),
vendorItem("the_vault:enigma_chest_scroll", 64, 128, 1, "50-64"),
vendorItem("the_vault:mod_box", 6, 12, 139, "50-64"),
vendorPoolItem("the_vault:vault_catalyst_infused", "the_vault:shop_pedestal", 12, 24, 320, "50-64")], []);

vendor.addRecipe([vendorItem("the_vault:salty_deluxe_cheese_burger", 6, 10, 144, "65+"),
vendorItem("the_vault:cheese_burger_feast", 8, 12, 90, "65+"),
vendorItem("the_vault:spicy_hearty_burger", 10, 16, 40, "65+"),
vendorItem("sophisticatedbackpacks:iron_backpack", 12, 24, 74, "65+"),
vendorItem("sophisticatedbackpacks:gold_backpack", 18, 36, 54, "65+"),
vendorItem("sophisticatedbackpacks:diamond_backpack", 24, 48, 34, "65+"),
vendorItem("the_vault:helmet", 12, 24, 110, "65+"),
vendorItem("the_vault:chestplate", 12, 24, 110, "65+"),
vendorItem("the_vault:leggings", 12, 24, 110, "65+"),
vendorItem("the_vault:boots", 12, 24, 110, "65+"),
vendorItem("the_vault:sword", 12, 24, 110, "65+"),
vendorItem("the_vault:axe", 12, 24, 110, "65+"),
vendorItem("the_vault:shield", 12, 24, 86, "65+"),
vendorItem("the_vault:wand", 12, 24, 86, "65+"),
vendorItem("the_vault:focus", 12, 24, 86, "65+"),
vendorItem("the_vault:magnet", 12, 24, 86, "65+"),
vendorPoolItem("the_vault:inscription", "the_vault:pedestal", 12, 36, 56, "65+"),
vendorItem("the_vault:jewel_pouch", 16, 32, 80, "65+"),
vendorItem("the_vault:sour_orange", 16, 32, 27, "65+"),
vendorItem("the_vault:trinket_scrap", 16, 32, 54, "65+"),
vendorItem("the_vault:trinket", 32, 64, 27, "65+"),
vendorItem("the_vault:artifact_fragment", 32, 64, 12, "65+"),
vendorItem("the_vault:unidentified_treasure_key", 32, 64, 12, "65+"),
vendorItem("the_vault:opportunistic_focus", 32, 64, 19, "65+"),
vendorItem("the_vault:cryonic_focus", 32, 64, 19, "65+"),
vendorItem("the_vault:pyretic_focus", 32, 64, 19, "65+"),
vendorItem("the_vault:vorpal_focus", 32, 64, 15, "65+"),
vendorItem("the_vault:empowered_chaotic_focus", 24, 48, 44, "65+"),
vendorItem("the_vault:resilient_focus", 24, 48, 44, "65+"),
vendorItem("the_vault:fundamental_focus", 4, 12, 86, "65+"),
vendorItem("the_vault:lost_bounty", 16, 48, 27, "65+"),
vendorItem("the_vault:capstone_dungeon_hunter", 16, 64, 26, "65+"),
vendorItem("the_vault:capstone_treasure_hunter", 32, 64, 10, "65+"),
vendorItem("the_vault:key_piece", 2, 4, 110, "65+"),
vendorItem("the_vault:vault_catalyst_chaos", 16, 48, 27, "65+"),
vendorItem("the_vault:crystal_seal_cake", 8, 32, 65, "65+"),
vendorItem("the_vault:mystery_egg", 2, 4, 110, "65+"),
vendorItem("the_vault:bitter_lemon", 8, 12, 110, "65+"),
vendorItem("the_vault:hardened_chest_scroll", 12, 24, 27, "65+"),
vendorItem("the_vault:flesh_chest_scroll", 16, 32, 12, "65+"),
vendorItem("the_vault:enigma_chest_scroll", 24, 48, 4, "65+"),
vendorItem("the_vault:mod_box", 2, 4, 110, "65+"),
vendorPoolItem("the_vault:vault_catalyst_infused", "the_vault:shop_pedestal", 10, 20, 320, "65+"),
vendorPoolItem("the_vault:capstone_vendoor_hunter", "the_vault:shop_pedestal", 20, 40, 30, "65+"),
vendorPoolItem("the_vault:capstone_pylon_hunter", "the_vault:shop_pedestal", 20, 40, 30, "65+"),
vendorItem("the_vault:capstone_companion_hunt", 64, 128, 10, "65+")], []);

JEI.addIngredient(<item:the_vault:shop_pedestal>);
JEI.addCategory(vendor);




// 宝库锻炉  recipes\gear_recipes.json
var armourCat = JeiCategory.create<Custom>("artisan_station", new TextComponent("宝库锻炉"), <item:the_vault:vault_forge>, [<item:the_vault:vault_forge>]) as Custom;
armourCat.background = JeiDrawable.blank(180, 30) as JeiDrawable;

armourCat.addDrawable(4, 5, JeiDrawable.of(new ResourceLocation("create", "textures/gui/jei/widgets.png") as ResourceLocation, 0, 0, 18, 18) as JeiDrawable);
armourCat.addDrawable(25, 5, JeiDrawable.of(new ResourceLocation("create", "textures/gui/jei/widgets.png") as ResourceLocation, 0, 0, 18, 18) as JeiDrawable);
armourCat.addDrawable(46, 5, JeiDrawable.of(new ResourceLocation("create", "textures/gui/jei/widgets.png") as ResourceLocation, 0, 0, 18, 18) as JeiDrawable);
armourCat.addDrawable(67, 5, JeiDrawable.of(new ResourceLocation("create", "textures/gui/jei/widgets.png") as ResourceLocation, 0, 0, 18, 18) as JeiDrawable);
armourCat.addDrawable(88, 5, JeiDrawable.of(new ResourceLocation("create", "textures/gui/jei/widgets.png") as ResourceLocation, 0, 0, 18, 18) as JeiDrawable);
armourCat.addDrawable(109, 5, JeiDrawable.of(new ResourceLocation("create", "textures/gui/jei/widgets.png") as ResourceLocation, 0, 0, 18, 18) as JeiDrawable);
armourCat.addDrawable(157, 5, JeiDrawable.of(new ResourceLocation("create", "textures/gui/jei/widgets.png") as ResourceLocation, 0, 0, 18, 18) as JeiDrawable);

armourCat.addDrawable(130, 5, JeiDrawable.of(new ResourceLocation("create", "textures/gui/jei/widgets.png") as ResourceLocation, 62, 93, 21, 15) as JeiDrawable);

armourCat.addSlot(0, 5, 6, true);
armourCat.addSlot(1, 26, 6, true);
armourCat.addSlot(2, 47, 6, true);
armourCat.addSlot(3, 68, 6, true);
armourCat.addSlot(4, 89, 6, true);
armourCat.addSlot(5, 110, 6, true);
armourCat.addSlot(6, 158, 6, false);

// Gear
armourCat.addRecipe([<item:the_vault:helmet>], [<item:the_vault:vault_alloy> * 2, <item:the_vault:vault_gold>]);
armourCat.addRecipe([<item:the_vault:chestplate>], [<item:the_vault:vault_alloy> * 2, <item:the_vault:vault_gold>]);
armourCat.addRecipe([<item:the_vault:leggings>], [<item:the_vault:vault_alloy> * 2, <item:the_vault:vault_gold>]);
armourCat.addRecipe([<item:the_vault:boots>], [<item:the_vault:vault_alloy> * 2, <item:the_vault:vault_gold>]);
armourCat.addRecipe([<item:the_vault:axe>], [<item:the_vault:vault_alloy> * 2, <item:the_vault:vault_gold>]);
armourCat.addRecipe([<item:the_vault:sword>], [<item:the_vault:vault_alloy> * 2, <item:the_vault:vault_gold>]);
armourCat.addRecipe([<item:the_vault:shield>], [<item:the_vault:vault_alloy> * 2, <item:the_vault:vault_gold>]);
armourCat.addRecipe([<item:the_vault:wand>], [<item:the_vault:vault_alloy> * 2, <item:the_vault:vault_gold>]);
armourCat.addRecipe([<item:the_vault:focus>], [<item:the_vault:vault_alloy> * 2, <item:the_vault:vault_gold>]);
armourCat.addRecipe([<item:the_vault:magnet>], [<item:the_vault:vault_alloy> * 2, <item:the_vault:vault_gold>]);
armourCat.addRecipe([<item:the_vault:unique_shard>], [<item:the_vault:vault_alloy> * 16, <item:the_vault:vault_gold> * 10, <item:the_vault:unique_shard> * 4]);

// Add category
JEI.addCategory(armourCat);




// 饰品锻炉  recipes\trinket_recipes.json
var trinketCat = JeiCategory.create<Custom>("trinket_forge", new TextComponent("饰品锻炉"), <item:the_vault:trinket_forge>, [<item:the_vault:trinket_forge>]) as Custom;
trinketCat.background = JeiDrawable.blank(180, 30) as JeiDrawable;

trinketCat.addDrawable(4, 5, JeiDrawable.of(new ResourceLocation("create", "textures/gui/jei/widgets.png") as ResourceLocation, 0, 0, 18, 18) as JeiDrawable);
trinketCat.addDrawable(25, 5, JeiDrawable.of(new ResourceLocation("create", "textures/gui/jei/widgets.png") as ResourceLocation, 0, 0, 18, 18) as JeiDrawable);
trinketCat.addDrawable(46, 5, JeiDrawable.of(new ResourceLocation("create", "textures/gui/jei/widgets.png") as ResourceLocation, 0, 0, 18, 18) as JeiDrawable);
trinketCat.addDrawable(67, 5, JeiDrawable.of(new ResourceLocation("create", "textures/gui/jei/widgets.png") as ResourceLocation, 0, 0, 18, 18) as JeiDrawable);
trinketCat.addDrawable(88, 5, JeiDrawable.of(new ResourceLocation("create", "textures/gui/jei/widgets.png") as ResourceLocation, 0, 0, 18, 18) as JeiDrawable);
trinketCat.addDrawable(109, 5, JeiDrawable.of(new ResourceLocation("create", "textures/gui/jei/widgets.png") as ResourceLocation, 0, 0, 18, 18) as JeiDrawable);
trinketCat.addDrawable(157, 5, JeiDrawable.of(new ResourceLocation("create", "textures/gui/jei/widgets.png") as ResourceLocation, 0, 0, 18, 18) as JeiDrawable);

trinketCat.addDrawable(130, 5, JeiDrawable.of(new ResourceLocation("create", "textures/gui/jei/widgets.png") as ResourceLocation, 62, 93, 21, 15) as JeiDrawable);

trinketCat.addSlot(0, 5, 6, true);
trinketCat.addSlot(1, 26, 6, true);
trinketCat.addSlot(2, 47, 6, true);
trinketCat.addSlot(3, 68, 6, true);
trinketCat.addSlot(4, 89, 6, true);
trinketCat.addSlot(5, 110, 6, true);
trinketCat.addSlot(6, 158, 6, false);

// Trinkets
trinketCat.addRecipe([<item:the_vault:trinket>], [<item:the_vault:vault_diamond> * 64, <item:the_vault:vault_gold> * 32, <item:the_vault:gem_alexandrite> * 192, <item:the_vault:trinket_scrap>]);

// Add category
JEI.addCategory(trinketCat);





// 工具装配台  recipes\tool_recipes.json
JEI.hideIngredient(<item:the_vault:tool>);
var toolCat = JeiCategory.create<Custom>("vault_tool_station", new TextComponent("工具装配台"), <item:the_vault:tool_station>, [<item:the_vault:tool_station>]) as Custom;
toolCat.background = JeiDrawable.blank(180, 30) as JeiDrawable;

toolCat.addDrawable(4, 5, JeiDrawable.of(new ResourceLocation("create", "textures/gui/jei/widgets.png") as ResourceLocation, 0, 0, 18, 18) as JeiDrawable);
toolCat.addDrawable(25, 5, JeiDrawable.of(new ResourceLocation("create", "textures/gui/jei/widgets.png") as ResourceLocation, 0, 0, 18, 18) as JeiDrawable);
toolCat.addDrawable(46, 5, JeiDrawable.of(new ResourceLocation("create", "textures/gui/jei/widgets.png") as ResourceLocation, 0, 0, 18, 18) as JeiDrawable);
toolCat.addDrawable(67, 5, JeiDrawable.of(new ResourceLocation("create", "textures/gui/jei/widgets.png") as ResourceLocation, 0, 0, 18, 18) as JeiDrawable);
toolCat.addDrawable(88, 5, JeiDrawable.of(new ResourceLocation("create", "textures/gui/jei/widgets.png") as ResourceLocation, 0, 0, 18, 18) as JeiDrawable);
toolCat.addDrawable(109, 5, JeiDrawable.of(new ResourceLocation("create", "textures/gui/jei/widgets.png") as ResourceLocation, 0, 0, 18, 18) as JeiDrawable);
toolCat.addDrawable(157, 5, JeiDrawable.of(new ResourceLocation("create", "textures/gui/jei/widgets.png") as ResourceLocation, 0, 0, 18, 18) as JeiDrawable);

toolCat.addDrawable(130, 5, JeiDrawable.of(new ResourceLocation("create", "textures/gui/jei/widgets.png") as ResourceLocation, 62, 93, 21, 15) as JeiDrawable);

toolCat.addSlot(0, 5, 6, true);
toolCat.addSlot(1, 26, 6, true);
toolCat.addSlot(2, 47, 6, true);
toolCat.addSlot(3, 68, 6, true);
toolCat.addSlot(4, 89, 6, true);
toolCat.addSlot(5, 110, 6, true);
toolCat.addSlot(6, 158, 6, false);

// Recipes
toolCat.addRecipe([<item:the_vault:tool>.withTag({offset: 0 as long, vaultGearData: [4088642441712019850, -2908456451661967519, 1514722424829605915, 4616400934536516931, 107798892737, 423054278656, -8852492746032676864, -8559560530332385260, 18372019767163027, 288230376101658624, 8589934592, 184828609003782144, 185388976097100464, 140751079104801, 34359738360, 1024, 1514722424829605888, 5770026126585140547, -8388307835, 2199023255555, -6589878960610344960, 2903310526063837840, -7620749799, 2199023255555, 1130297953353728]})], [<item:the_vault:chromatic_iron_ingot> * 9, <item:the_vault:driftwood> * 2, <item:the_vault:vault_bronze> * 8]);
JEI.addIngredient(<item:the_vault:tool>.withTag({offset: 0 as long, vaultGearData: [4088642441712019850, -2908456451661967519, 1514722424829605915, 4616400934536516931, 107798892737, 423054278656, -8852492746032676864, -8559560530332385260, 18372019767163027, 288230376101658624, 8589934592, 184828609003782144, 185388976097100464, 140751079104801, 34359738360, 1024, 1514722424829605888, 5770026126585140547, -8388307835, 2199023255555, -6589878960610344960, 2903310526063837840, -7620749799, 2199023255555, 1130297953353728]}));

toolCat.addRecipe([<item:the_vault:tool>.withTag({offset: 0 as long, vaultGearData: [-7920290657449244086, 5809905508407278008, 1514722424829605914, 4616400934536516931, 161485983937, 697932185605, -8852492746032676864, -8559560530332385260, 18372019767163027, 288230376101658624, 8589934592, 184828609003782144, 185388976097100464, 140751079104801, 34359738360, 1024, 1514722424829605888, 5770026126585140547, -7314566011, 2199023255555, -6589878960610344960, 2903310526063837840, -7620749799, 2199023255555, 1130297953353728]})], [<item:the_vault:chromatic_steel_ingot> * 9, <item:the_vault:driftwood> * 36, <item:the_vault:vault_bronze> * 81]);
JEI.addIngredient(<item:the_vault:tool>.withTag({offset: 0 as long, vaultGearData: [-7920290657449244086, 5809905508407278008, 1514722424829605914, 4616400934536516931, 161485983937, 697932185605, -8852492746032676864, -8559560530332385260, 18372019767163027, 288230376101658624, 8589934592, 184828609003782144, 185388976097100464, 140751079104801, 34359738360, 1024, 1514722424829605888, 5770026126585140547, -7314566011, 2199023255555, -6589878960610344960, 2903310526063837840, -7620749799, 2199023255555, 1130297953353728]}));

toolCat.addRecipe([<item:the_vault:tool>.withTag({offset: 0 as long, vaultGearData: [9059907081151508074, 2335859790917641485, 1514722424829605915, 4616400934536516931, -9223371767994609471, 1247687999500, -8852492746032676864, -8559560530332385260, 18372019767163027, 288230376101658624, 8589934592, 184828609003782144, 185388976097100464, 140751079104801, 34359738360, 1024, 1514722424829605888, 5770026126585140547, -5167082363, 2199023255555, -6589878960610344960, 2903310526063837840, -7620749799, 2199023255555, 1130297953353728]})], [<item:the_vault:vaulterite_ingot> * 12, <item:the_vault:driftwood> * 48, <item:the_vault:vault_bronze> * 81]);
JEI.addIngredient(<item:the_vault:tool>.withTag({offset: 0 as long, vaultGearData: [9059907081151508074, 2335859790917641485, 1514722424829605915, 4616400934536516931, -9223371767994609471, 1247687999500, -8852492746032676864, -8559560530332385260, 18372019767163027, 288230376101658624, 8589934592, 184828609003782144, 185388976097100464, 140751079104801, 34359738360, 1024, 1514722424829605888, 5770026126585140547, -5167082363, 2199023255555, -6589878960610344960, 2903310526063837840, -7620749799, 2199023255555, 1130297953353728]}));

toolCat.addRecipe([<item:the_vault:tool>.withTag({offset: 0 as long, vaultGearData: [3224901621940241082, 1974285087738049155, 1514722424829605912, 4616400934536516931, 4611686340974645441, 1522565906448, -8852492746032676864, -8559560530332385260, 18372019767163027, 288230376101658624, 8589934592, 184828609003782144, 185388976097100464, 140751079104801, 34359738360, 1024, 1514722424829605888, 5770026126585140547, -4093340539, 2199023255555, -6589878960610344960, 2903310526063837840, -7620749799, 2199023255555, 1130297953353728]})], [<item:the_vault:black_chromatic_steel_ingot> * 12, <item:the_vault:driftwood> * 64,  <item:the_vault:vault_bronze> * 162]);
JEI.addIngredient(<item:the_vault:tool>.withTag({offset: 0 as long, vaultGearData: [3224901621940241082, 1974285087738049155, 1514722424829605912, 4616400934536516931, 4611686340974645441, 1522565906448, -8852492746032676864, -8559560530332385260, 18372019767163027, 288230376101658624, 8589934592, 184828609003782144, 185388976097100464, 140751079104801, 34359738360, 1024, 1514722424829605888, 5770026126585140547, -4093340539, 2199023255555, -6589878960610344960, 2903310526063837840, -7620749799, 2199023255555, 1130297953353728]}));

toolCat.addRecipe([<item:the_vault:tool>.withTag({offset: 0 as long, vaultGearData: [-6744258008942599590, 7490724894486887956, 1514722424829605912, 4616400934536516931, 376234348737, 1797443813396, -8852492746032676864, -8559560530332385260, 18372019767163027, 288230376101658624, 8589934592, 184828609003782144, 185388976097100464, 140751079104801, 34359738360, 1024, 1514722424829605888, 5770026126585140547, -3019598715, 2199023255555, -6589878960610344960, 2903310526063837840, -7620749799, 2199023255555, 1130297953353728]})], [<item:the_vault:echoing_ingot> * 9, <item:the_vault:driftwood> * 64,  <item:the_vault:vault_bronze> * 256]);
JEI.addIngredient(<item:the_vault:tool>.withTag({offset: 0 as long, vaultGearData: [-6744258008942599590, 7490724894486887956, 1514722424829605912, 4616400934536516931, 376234348737, 1797443813396, -8852492746032676864, -8559560530332385260, 18372019767163027, 288230376101658624, 8589934592, 184828609003782144, 185388976097100464, 140751079104801, 34359738360, 1024, 1514722424829605888, 5770026126585140547, -3019598715, 2199023255555, -6589878960610344960, 2903310526063837840, -7620749799, 2199023255555, 1130297953353728]}));

toolCat.addRecipe([<item:the_vault:tool>.withTag({offset: 0 as long, vaultGearData: [-1334006908380202214, -8838291958584646397, 1514722424829605915, 4616400934536516931, -9223371606933335871, 2072321720342, -8852492746032676864, -8559560530332385260, 18372019767163027, 288230376101658624, 8589934592, 184828609003782144, 185388976097100464, 140751079104801, 34359738360, 1024, 1514722424829605888, 5770026126585140547, -1945856891, 2199023255555, -6589878960610344960, 2903310526063837840, -7620749799, 2199023255555, 1130297953353728]})], [<item:the_vault:omega_pog> * 9, <item:the_vault:echoing_ingot> * 9, <item:the_vault:driftwood> * 64,  <item:the_vault:vault_bronze> * 1024]);
JEI.addIngredient(<item:the_vault:tool>.withTag({offset: 0 as long, vaultGearData: [-1334006908380202214, -8838291958584646397, 1514722424829605915, 4616400934536516931, -9223371606933335871, 2072321720342, -8852492746032676864, -8559560530332385260, 18372019767163027, 288230376101658624, 8589934592, 184828609003782144, 185388976097100464, 140751079104801, 34359738360, 1024, 1514722424829605888, 5770026126585140547, -1945856891, 2199023255555, -6589878960610344960, 2903310526063837840, -7620749799, 2199023255555, 1130297953353728]}));

// Add category
JEI.addCategory(toolCat);



// 珠宝  recipes\jewel_crafting_recipes.json
// <recipetype:the_vault:jewel_crafting_table>.remove(<item:the_vault:jewel>);
var jewelc = JeiCategory.create<Custom>("jewel_crafting_table", new TextComponent("珠宝制作台"), <item:the_vault:jewel_crafting_table>, [<item:the_vault:jewel_crafting_table>]) as Custom;
jewelc.background = JeiDrawable.blank(175, 45) as JeiDrawable;

jewelc.addDrawable(4, 5, JeiDrawable.of(new ResourceLocation("create", "textures/gui/jei/widgets.png") as ResourceLocation, 0, 0, 18, 18) as JeiDrawable);
jewelc.addDrawable(25, 5, JeiDrawable.of(new ResourceLocation("create", "textures/gui/jei/widgets.png") as ResourceLocation, 0, 0, 18, 18) as JeiDrawable);
jewelc.addDrawable(46, 5, JeiDrawable.of(new ResourceLocation("create", "textures/gui/jei/widgets.png") as ResourceLocation, 0, 0, 18, 18) as JeiDrawable);
jewelc.addDrawable(4, 25, JeiDrawable.of(new ResourceLocation("create", "textures/gui/jei/widgets.png") as ResourceLocation, 0, 0, 18, 18) as JeiDrawable);
jewelc.addDrawable(25, 25, JeiDrawable.of(new ResourceLocation("create", "textures/gui/jei/widgets.png") as ResourceLocation, 0, 0, 18, 18) as JeiDrawable);
jewelc.addDrawable(46, 25, JeiDrawable.of(new ResourceLocation("create", "textures/gui/jei/widgets.png") as ResourceLocation, 0, 0, 18, 18) as JeiDrawable);
jewelc.addDrawable(67, 25, JeiDrawable.of(new ResourceLocation("create", "textures/gui/jei/widgets.png") as ResourceLocation, 0, 0, 18, 18) as JeiDrawable);
jewelc.addDrawable(88, 25, JeiDrawable.of(new ResourceLocation("create", "textures/gui/jei/widgets.png") as ResourceLocation, 0, 0, 18, 18) as JeiDrawable);
jewelc.addDrawable(109, 25, JeiDrawable.of(new ResourceLocation("create", "textures/gui/jei/widgets.png") as ResourceLocation, 0, 0, 18, 18) as JeiDrawable);
jewelc.addDrawable(130, 25, JeiDrawable.of(new ResourceLocation("create", "textures/gui/jei/widgets.png") as ResourceLocation, 0, 0, 18, 18) as JeiDrawable);
jewelc.addDrawable(151, 25, JeiDrawable.of(new ResourceLocation("create", "textures/gui/jei/widgets.png") as ResourceLocation, 0, 0, 18, 18) as JeiDrawable);

jewelc.addDrawable(88, 7, JeiDrawable.of(new ResourceLocation("create", "textures/gui/jei/widgets.png") as ResourceLocation, 0, 21, 18, 14) as JeiDrawable);

jewelc.addSlot(0, 5, 6, true);
jewelc.addSlot(1, 26, 6, true);
jewelc.addSlot(2, 47, 6, true);

jewelc.addSlot(3, 5, 26, false);
jewelc.addSlot(4, 26, 26, false);
jewelc.addSlot(5, 47, 26, false);
jewelc.addSlot(6, 68, 26, false);
jewelc.addSlot(7, 89, 26, false);
jewelc.addSlot(8, 110, 26, false);
jewelc.addSlot(9, 131, 26, false);
jewelc.addSlot(10, 152, 26, false);

function jewelItem(size as int, attr as string) as IItemStack {
  return <item:the_vault:jewel>.withTag({size: size, jewelAttribute: attr});
}

// recipe
jewelc.addRecipe([
  <item:the_vault:jewel>.withTag({vaultGearData: [9102954068834547710, 6532783614268835263, 2398483460923590696, 7301343536245727237, 5575674136, 23103591157858304, 324563159483502678, -8713050946218553528, 12246927038259431, 2583691264, 1653562408960, 3553434665372390400, -6750050586108581548, -1099511626475, 140737488355583, 216454257090494464, 46207190905651203, -6764834407638196052, -5575131904627146640, 4294967295, 128, 256]}),
  <item:the_vault:jewel>.withTag({vaultGearData: [8825495650069423566, 4216641359236630331, 2398483460923590696, 7301343536245727237, 5575674136, 23103591157858304, 324563159483502678, -8713050946218553528, 12246927038259431, 2583691264, 1653562408960, 3553434665372390400, -6750050586108581548, -1099511626475, 140737488355583, 216454257090494464, 46207190905651203, -9179301469094764372, -5575131904627146710, 4294967295, 128, 256]}),
  <item:the_vault:jewel>.withTag({vaultGearData: [-3052785702126954450, -5366752167634382132, 2398483460923590698, 7301343536245727237, 5575674136, 23103591157858304, 324563159483502678, -8713050946218553528, 12246927038259431, 2583691264, 1653562408960, 3553434665372390400, -6750050586108581548, -1099511626475, 140737488355583, 216454257090494464, 46207190905651203, -6766578233079848788, -5575131904627146720, 4294967295, 128, 256]}),
  <item:the_vault:jewel>.withTag({vaultGearData: [-8070502701154904882, -4403879838577836553, 2398483460923590697, 7301343536245727237, 5575674136, 23103591157858304, 324563159483502678, -8713050946218553528, 12246927038259431, 2583691264, 1653562408960, 3553434665372390400, -6750050586108581548, -1099511626475, 140737488355583, 216454257090494464, 46207190905651203, -4274961052042354516, -5575131904627146695, 4294967295, 128, 256]}),
  <item:the_vault:jewel>.withTag({vaultGearData: [-8893279176141568050, 3087837229164464653, 2398483460923590696, 7301343536245727237, 5575674136, 23103591157858304, 324563159483502678, -8713050946218553528, 12246927038259431, 2583691264, 1653562408960, 3553434665372390400, -6750050586108581548, -1099511626475, 140737488355583, 216454257090494464, 46207173725782019, 509602141692928172, -1361116187653112, 576460752304472063, 1152921504606846976]}),
  <item:the_vault:jewel>.withTag({vaultGearData: [-4172656261922641264, 6647731426478739921, 1482247227358347914, 6141032137674326098, 89210786182, 369657458525732864, 5193010551736042848, -9128685118923148160, -5217693920545852104, 10582799418946, 6772991627100160, 387315208474853376, 3462165789867852565, -4503599622039003, 576460752304472063, 1152921504606846976, 4796931737475100720, 2496448324166139914, 1152921504487772800, 34359738368, 68719476736]})
], [<item:the_vault:gemstone> * 1, <item:the_vault:silver_scrap> * 64, <item:the_vault:vault_gold> * 1]);

jewelc.addRecipe([
  <item:the_vault:jewel>.withTag({vaultGearData: [-621097725750612546, -3686966432455235094, 2398483460923590697, 7301343536245727237, 5575674136, 23103591157858304, 324563159483502678, -8713050946218553528, 12246927038259431, 2583691264, 1653562408960, 3553434665372390400, -6750050586108581548, -1099511626475, 140737488355583, 216454257090494464, 46207122186174467, 2703930070277464236, 281474976681585, 8388608, 16777216]}),
  <item:the_vault:jewel>.withTag({vaultGearData: [-8473742563306369586, 6359787398904331890, 2398483460923590698, 7301343536245727237, 5575674136, 23103591157858304, 324563159483502678, -8713050946218553528, 12246927038259431, 2583691264, 1653562408960, 3553434665372390400, -6750050586108581548, -1099511626475, 140737488355583, 216454257090494464, 46207105006305283, -1796161977561472852, 68719476728, 2048, 4096]}),
  <item:the_vault:jewel>.withTag({vaultGearData: [629389184469306990, -3265467394046784944, 2398483460923590696, 7301343536245727237, 5575674136, 23103591157858304, 324563159483502678, -8713050946218553528, 12246927038259431, 2583691264, 1653562408960, 3553434665372390400, -6750050586108581548, -1099511626475, 140737488355583, 216454257090494464, 46207147955978243, -6116378183703813972, -7620749216, 2199023255555, 4398046511104]})
], [<item:the_vault:gemstone> * 1, <item:the_vault:silver_scrap> * 32, <item:the_vault:vault_gold> * 1]);

jewelc.addRecipe([
  <item:the_vault:jewel>.withTag({vaultGearData: [1503123565903661118, -3621554938749270571, 2398483460923590699, 7301343536245727237, 5575674136, 23103591157858304, 324563159483502678, -8713050946218553528, 12246927038259431, 2583691264, 1653562408960, 3553434665372390400, -6750050586108581548, -1099511623915, 140737488355583, 216454257090494464, 46207130776109059, -8788094682176249684, 18014398507621450, 536870912, 1073741824]})
], [<item:the_vault:gemstone> * 8, <item:the_vault:silver_scrap> * 128, <item:the_vault:vault_gold> * 8]);

jewelc.addRecipe([
  <item:the_vault:jewel>.withTag({vaultGearData: [8520766305883925614, -6156266026222443535, 2398483460923590698, 7301343536245727237, 5575674136, 23103591157858304, 324563159483502678, -8713050946218553528, 12246927038259431, 2583691264, 1653562408960, 3553434665372390400, -6750050586108581548, -1099511625195, 140737488355583, 216454257090494464, 46207156545912835, -6117953096671649620, -487727492464, 140737488355583, 281474976710656]})
], [<item:the_vault:gemstone> * 8, <item:the_vault:silver_scrap> * 256, <item:the_vault:vault_gold> * 8]);

jewelc.addRecipe([
  <item:the_vault:jewel>.withTag({vaultGearData: [4692801751300223390, 1527548136219924255, 2398483460923590699, 7301343536245727237, 5575674136, 23103591157858304, 324563159483502678, -8713050946218553528, 12246927038259431, 2583691264, 1653562408960, 3553434665372390400, -6750050586108581548, -1099511622635, 140737488355583, 216454257090494464, 46207139366043651, -1654851781087027028, 1152921504481320625, 34359738368, 68719476736]})
], [<item:the_vault:gemstone> * 32, <item:the_vault:silver_scrap> * 256, <item:the_vault:vault_gold> * 32]);

JEI.addCategory(jewelc);

