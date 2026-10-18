package com.happysadduck.quantumegg;

import com.happysadduck.quantumegg.registry.mod_blocks_t;
import com.happysadduck.quantumegg.registry.mod_items_t;

import net.fabricmc.api.ModInitializer;

public class quantum_egg_mod_t implements ModInitializer {
	public static final String MOD_ID = "quantum_egg";

	@Override
	public void onInitialize() {
		mod_blocks_t.init();
		mod_items_t.init();
	}
}
