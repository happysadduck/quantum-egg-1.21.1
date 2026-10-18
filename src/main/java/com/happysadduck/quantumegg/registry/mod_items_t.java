package com.happysadduck.quantumegg.registry;

import com.happysadduck.quantumegg.quantum_egg_mod_t;
import com.happysadduck.quantumegg.items.quantum_egg_t;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class mod_items_t {
    public static final Item QUANTUM_EGG = new quantum_egg_t(
            new Item.Settings().maxCount(1).component(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true)
                    .component(mod_data_components_t.BX, 0.0).component(mod_data_components_t.BY, 0.0)
                    .component(mod_data_components_t.BZ, 1.0)
                    .component(mod_data_components_t.UUID, -1).component(mod_data_components_t.MEASURED, 1));

    public static void init() {
        Registry.register(Registries.ITEM, Identifier.of(quantum_egg_mod_t.MOD_ID, "quantum_egg"), QUANTUM_EGG);
    }
}
