package com.happysadduck.quantumegg.registry;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

import com.happysadduck.quantumegg.quantum_egg_mod_t;
import com.happysadduck.quantumegg.blocks.quantum_box_entity_t;
import com.happysadduck.quantumegg.blocks.quantum_box_t;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

public class mod_blocks_t {
        public static final Block QUANTUM_BOX = new quantum_box_t(AbstractBlock.Settings.create()
                        .solidBlock((state, world, pos) -> false)
                        .pistonBehavior(PistonBehavior.BLOCK).strength(0.3f, 0.3f).sounds(BlockSoundGroup.GLASS)
                        .nonOpaque());
        public static BlockEntityType<quantum_box_entity_t> QUANTUM_BOX_ENTITY;

        public static void init() {
                Identifier idqb = Identifier.of(quantum_egg_mod_t.MOD_ID, "quantum_box");
                Registry.register(Registries.BLOCK, idqb, QUANTUM_BOX);
                Registry.register(Registries.ITEM, idqb, new BlockItem(QUANTUM_BOX,
                                new Item.Settings().component(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true)));
                QUANTUM_BOX_ENTITY = Registry.register(
                                Registries.BLOCK_ENTITY_TYPE,
                                idqb,
                                BlockEntityType.Builder.create(quantum_box_entity_t::new, QUANTUM_BOX).build());
                ItemGroupEvents.modifyEntriesEvent(ItemGroups.REDSTONE).register(entries -> {
                        entries.add(QUANTUM_BOX);
                });
        }
}
