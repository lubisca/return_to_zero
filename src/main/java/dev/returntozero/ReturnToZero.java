package dev.returntozero;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReturnToZero implements ModInitializer {
	public static final String MOD_ID = "return_to_zero";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static final CheckpointManager MANAGER = new CheckpointManager();

	@Override
	public void onInitialize() {
		ModConfig.load();

		ServerLifecycleEvents.SERVER_STARTED.register(MANAGER::load);
		ServerLifecycleEvents.SERVER_STOPPING.register(MANAGER::save);
		ServerTickEvents.END_SERVER_TICK.register(MANAGER::tick);
		ServerLivingEntityEvents.ALLOW_DEATH.register(MANAGER::onAllowDeath);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> MANAGER.createIfMissing(handler.getPlayer()));
		PlayerBlockBreakEvents.AFTER.register(BlockEditTracker::onBreak);

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				CheckpointCommand.register(dispatcher, MANAGER));

		LOGGER.info("Return to Zero carregado.");
	}
}
