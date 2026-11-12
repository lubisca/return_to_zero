package dev.returntozero;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.WorldSavePath;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Guarda um checkpoint por jogador e cuida da morte, do checkpoint automatico e do arquivo de save. */
public final class CheckpointManager {
	private static final int SANITY_DELAY_TICKS = 10;

	private final Map<UUID, PlayerSnapshot> snapshots = new HashMap<>();
	private final Map<UUID, Integer> pendingSanity = new HashMap<>();

	private static File saveFile(MinecraftServer server) {
		return server.getSavePath(WorldSavePath.ROOT).resolve("return_to_zero.dat").toFile();
	}

	/** Cria o checkpoint do jogador, substituindo o anterior, e zera o registro de blocos dele. */
	public void create(ServerPlayerEntity player) {
		snapshots.put(player.getUuid(), PlayerSnapshot.capture(player));
		BlockEditTracker.clear(player.getUuid());
	}

	public void createIfMissing(ServerPlayerEntity player) {
		if (!snapshots.containsKey(player.getUuid())) {
			create(player);
		}
	}

	public boolean hasCheckpoint(ServerPlayerEntity player) {
		return snapshots.containsKey(player.getUuid());
	}

	/** Restaura o jogador. Retorna false se ele nao tem checkpoint. */
	public boolean restore(ServerPlayerEntity player, boolean fromDeath) {
		PlayerSnapshot snapshot = snapshots.get(player.getUuid());
		if (snapshot == null) {
			return false;
		}
		MinecraftServer server = player.getServer();
		if (server == null) {
			return false;
		}

		if (ModConfig.get().undoPlayerBlocks) {
			BlockEditTracker.revert(server, player.getUuid());
		} else {
			BlockEditTracker.clear(player.getUuid());
		}

		snapshot.restore(player, server);
		player.getServerWorld().playSound(null, player.getBlockPos(), SoundEvents.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, SoundCategory.PLAYERS, 1.0f, 1.0f);

		if (fromDeath) {
			// Aplica a sanidade alguns ticks depois, para nao ser sobrescrita por eventos da mesma morte.
			pendingSanity.put(player.getUuid(), SANITY_DELAY_TICKS);
			ReturnToZero.LOGGER.info("[return_to_zero] {} morreu e foi restaurado ao checkpoint; sanidade sera ajustada em {} ticks.",
					player.getName().getString(), SANITY_DELAY_TICKS);
		}
		return true;
	}

	private static void applySanityLoss(ServerPlayerEntity player, MinecraftServer server) {
		if (!FabricLoader.getInstance().isModLoaded("sanity_renewed")) {
			return;
		}
		ModConfig cfg = ModConfig.get();
		double loss = Math.max(cfg.sanityLossOnRevive, 0.0);
		double floor = Math.max(cfg.sanityFloorOnRevive, 0.0);

		Float before = readSanityPercent(player);
		if (before != null) {
			double target = before - loss;
			if (floor > 0 && target < floor) {
				target = floor;
			}
			target = Math.max(0.0, Math.min(100.0, target));
			boolean ok = writeSanityPercent(player, (float) target);
			ReturnToZero.LOGGER.info("[return_to_zero] {} reviveu: sanidade antes={} alvo={} (perda={} piso={}) escreveu={} final={}",
					player.getName().getString(), before, target, loss, floor, ok, readSanityPercent(player));
			if (ok) {
				return;
			}
		}

		// Plano B: se nao deu para ler/escrever direto, tenta pelos comandos do Sanity.
		String id = player.getUuidAsString();
		if (loss > 0) {
			runCommand(server, "sanity add " + id + " -" + loss);
		}
		if (floor > 0) {
			runCommand(server, "sanity set " + id + " " + floor);
		}
		ReturnToZero.LOGGER.warn("[return_to_zero] {}: usei o plano B (comandos); sanidade final={}",
				player.getName().getString(), readSanityPercent(player));
	}

	private static void runCommand(MinecraftServer server, String command) {
		try {
			server.getCommandManager().executeWithPrefix(server.getCommandSource().withSilent(), command);
		} catch (Exception e) {
			ReturnToZero.LOGGER.warn("Falhou o comando: " + command, e);
		}
	}

	/** Pega o objeto de sanidade do jogador por reflexao, sem depender do codigo do Sanity: Renewed. */
	private static Object sanityObject(ServerPlayerEntity player) throws Exception {
		Class<?> holder = Class.forName("guivnf.sanity_renewed.capability.SanityHolder");
		for (java.lang.reflect.Method m : holder.getMethods()) {
			if (m.getName().equals("get") && m.getParameterCount() == 1
					&& m.getParameterTypes()[0].isAssignableFrom(player.getClass())) {
				return m.invoke(null, player);
			}
		}
		return null;
	}

	/** Sanidade atual de 0 a 100 (100 = sao), ou null se nao conseguir ler. */
	private static Float readSanityPercent(ServerPlayerEntity player) {
		try {
			Object sanity = sanityObject(player);
			if (sanity == null) {
				return null;
			}
			float internal = (Float) sanity.getClass().getMethod("getSanity").invoke(sanity);
			return (1.0f - internal) * 100.0f;
		} catch (Throwable t) {
			ReturnToZero.LOGGER.warn("[return_to_zero] Nao consegui ler a sanidade.", t);
			return null;
		}
	}

	/** Define a sanidade (0 a 100, onde 100 = sao). Retorna true se conseguiu. */
	private static boolean writeSanityPercent(ServerPlayerEntity player, float percent) {
		try {
			Object sanity = sanityObject(player);
			if (sanity == null) {
				return false;
			}
			sanity.getClass().getMethod("setSanity", float.class).invoke(sanity, 1.0f - percent / 100.0f);
			return true;
		} catch (Throwable t) {
			ReturnToZero.LOGGER.warn("[return_to_zero] Nao consegui escrever a sanidade.", t);
			return false;
		}
	}

	/** Evento de morte: se o jogador tem checkpoint, cancela a morte e restaura. */
	public boolean onAllowDeath(LivingEntity entity, DamageSource source, float amount) {
		if (!(entity instanceof ServerPlayerEntity player)) {
			return true;
		}
		if (source.isOf(DamageTypes.GENERIC_KILL)) {
			return true; // /kill continua matando de verdade
		}
		return !restore(player, true);
	}

	/** Checkpoint automatico de todos os jogadores vivos. */
	public void tick(MinecraftServer server) {
		if (!pendingSanity.isEmpty()) {
			java.util.Iterator<Map.Entry<UUID, Integer>> it = pendingSanity.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<UUID, Integer> entry = it.next();
				int left = entry.getValue() - 1;
				if (left > 0) {
					entry.setValue(left);
					continue;
				}
				it.remove();
				ServerPlayerEntity p = server.getPlayerManager().getPlayer(entry.getKey());
				if (p != null) {
					applySanityLoss(p, server);
				}
			}
		}

		int minutes = ModConfig.get().autoIntervalMinutes;
		if (minutes <= 0) {
			return;
		}
		long interval = minutes * 60L * 20L;
		if (server.getTicks() == 0 || server.getTicks() % interval != 0) {
			return;
		}
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			if (player.isAlive() && !player.isSpectator()) {
				create(player);
			}
		}
		save(server);
	}

	public void save(MinecraftServer server) {
		NbtCompound root = new NbtCompound();
		NbtList list = new NbtList();
		for (Map.Entry<UUID, PlayerSnapshot> entry : snapshots.entrySet()) {
			NbtCompound item = new NbtCompound();
			item.putString("uuid", entry.getKey().toString());
			item.put("snapshot", entry.getValue().toNbt());
			list.add(item);
		}
		root.put("checkpoints", list);
		try {
			NbtIo.writeCompressed(root, saveFile(server));
		} catch (IOException e) {
			ReturnToZero.LOGGER.warn("Nao consegui salvar os checkpoints.", e);
		}
	}

	public void load(MinecraftServer server) {
		snapshots.clear();
		File file = saveFile(server);
		if (!file.exists()) {
			return;
		}
		try {
			NbtCompound root = NbtIo.readCompressed(file);
			NbtList list = root.getList("checkpoints", NbtElement.COMPOUND_TYPE);
			for (int i = 0; i < list.size(); i++) {
				NbtCompound item = list.getCompound(i);
				snapshots.put(UUID.fromString(item.getString("uuid")),
						PlayerSnapshot.fromNbt(item.getCompound("snapshot")));
			}
		} catch (Exception e) {
			ReturnToZero.LOGGER.warn("Nao consegui ler os checkpoints salvos.", e);
		}
	}
}
