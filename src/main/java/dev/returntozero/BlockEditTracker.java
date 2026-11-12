package dev.returntozero;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Anota, por jogador, os blocos que ele colocou ou quebrou com a propria mao desde o ultimo checkpoint.
 * Na morte, desfaz so esses blocos, e apenas se ninguem mexeu neles depois.
 * Nao rastreia explosoes, fogo, agua, pistoes, redstone nem criaturas.
 * O registro fica so em memoria (nao sobrevive ao fechar o jogo).
 */
public final class BlockEditTracker {
	private record Key(Identifier world, long pos) {}

	private static final class Edit {
		final Identifier world;
		final BlockPos pos;
		final BlockState original;
		final NbtCompound originalBlockEntity;
		BlockState expected;

		Edit(Identifier world, BlockPos pos, BlockState original, NbtCompound originalBlockEntity, BlockState expected) {
			this.world = world;
			this.pos = pos;
			this.original = original;
			this.originalBlockEntity = originalBlockEntity;
			this.expected = expected;
		}
	}

	private record Pending(BlockPos pos, BlockState before) {}

	private static final Map<UUID, LinkedHashMap<Key, Edit>> EDITS = new HashMap<>();
	private static final ThreadLocal<Pending> PENDING = new ThreadLocal<>();

	private BlockEditTracker() {}

	public static void clear(UUID player) {
		EDITS.remove(player);
	}

	private static void record(ServerPlayerEntity player, ServerWorld world, BlockPos pos,
							   BlockState before, NbtCompound beforeBlockEntity, BlockState after) {
		ModConfig cfg = ModConfig.get();
		if (!cfg.undoPlayerBlocks) {
			return;
		}
		LinkedHashMap<Key, Edit> map = EDITS.computeIfAbsent(player.getUuid(), id -> new LinkedHashMap<>());
		Key key = new Key(world.getRegistryKey().getValue(), pos.asLong());
		Edit existing = map.get(key);
		if (existing != null) {
			existing.expected = after;
			return;
		}
		if (map.size() >= cfg.maxTrackedBlocksPerPlayer) {
			return;
		}
		map.put(key, new Edit(key.world(), pos.toImmutable(), before, beforeBlockEntity, after));
	}

	/** Chamado pelo evento do Fabric depois que o jogador quebra um bloco. */
	public static void onBreak(World world, PlayerEntity player, BlockPos pos, BlockState oldState, BlockEntity blockEntity) {
		if (!(world instanceof ServerWorld serverWorld) || !(player instanceof ServerPlayerEntity serverPlayer)) {
			return;
		}
		NbtCompound beNbt = blockEntity != null ? blockEntity.createNbtWithId() : null;
		record(serverPlayer, serverWorld, pos, oldState, beNbt, serverWorld.getBlockState(pos));
	}

	/** Chamado pelo mixin antes de o jogador colocar um bloco. */
	public static void beforePlace(ItemPlacementContext ctx) {
		if (ctx.getWorld().isClient) {
			PENDING.remove();
			return;
		}
		BlockPos pos = ctx.getBlockPos();
		PENDING.set(new Pending(pos.toImmutable(), ctx.getWorld().getBlockState(pos)));
	}

	/** Chamado pelo mixin depois que o bloco foi colocado com sucesso. */
	public static void afterPlace(ItemPlacementContext ctx) {
		Pending pending = PENDING.get();
		PENDING.remove();
		if (pending == null) {
			return;
		}
		if (!(ctx.getWorld() instanceof ServerWorld world) || !(ctx.getPlayer() instanceof ServerPlayerEntity player)) {
			return;
		}
		record(player, world, pending.pos(), pending.before(), null, world.getBlockState(pending.pos()));
	}

	public static void discardPending() {
		PENDING.remove();
	}

	/** Desfaz os blocos do jogador. Retorna quantos foram revertidos. */
	public static int revert(MinecraftServer server, UUID player) {
		LinkedHashMap<Key, Edit> map = EDITS.remove(player);
		if (map == null || map.isEmpty()) {
			return 0;
		}
		List<Edit> edits = new ArrayList<>(map.values());
		Collections.reverse(edits);

		int reverted = 0;
		for (Edit edit : edits) {
			ServerWorld world = server.getWorld(RegistryKey.of(RegistryKeys.WORLD, edit.world));
			if (world == null || !world.isChunkLoaded(edit.pos)) {
				continue;
			}
			if (!world.getBlockState(edit.pos).equals(edit.expected)) {
				continue; // alguem mexeu depois, deixa como esta
			}
			world.setBlockState(edit.pos, edit.original, Block.NOTIFY_LISTENERS);
			if (edit.originalBlockEntity != null) {
				BlockEntity be = world.getBlockEntity(edit.pos);
				if (be != null) {
					be.readNbt(edit.originalBlockEntity);
					be.markDirty();
				}
			}
			reverted++;
		}
		return reverted;
	}
}
