package com.happysadduck.quantumegg.registry;

import java.util.function.UnaryOperator;

import com.happysadduck.quantumegg.quantum_egg_mod_t;
import com.mojang.serialization.Codec;

import net.minecraft.component.ComponentType;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class mod_data_components_t {
        private static <T> ComponentType<T> register(String name,
                        UnaryOperator<ComponentType.Builder<T>> builder_operator) {
                ComponentType<T> type = builder_operator.apply(ComponentType.builder()).build();
                return Registry.register(Registries.DATA_COMPONENT_TYPE, Identifier.of(quantum_egg_mod_t.MOD_ID, name),
                                type);
        }

        public static final ComponentType<Double> BX = register("bx",
                        builder -> builder.codec(Codec.DOUBLE).packetCodec(PacketCodecs.DOUBLE));
        public static final ComponentType<Double> BY = register("by",
                        builder -> builder.codec(Codec.DOUBLE).packetCodec(PacketCodecs.DOUBLE));
        public static final ComponentType<Double> BZ = register("bz",
                        builder -> builder.codec(Codec.DOUBLE).packetCodec(PacketCodecs.DOUBLE));
        public static final ComponentType<Integer> UUID = register("uuid",
                        builder -> builder.codec(Codec.INT).packetCodec(PacketCodecs.INTEGER));
        public static final ComponentType<Integer> MEASURED = register("measured",
                        builder -> builder.codec(Codec.INT).packetCodec(PacketCodecs.INTEGER));
}
