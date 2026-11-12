package dev.returntozero;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

/** Estado do jogador no momento do checkpoint: posicao, vida, fome, XP e inventario. */
public final class PlayerSnapshot {
	private final Identifier world;
	private final double x, y, z;
	private final float yaw, pitch;
	private final float health;
	private final int selectedSlot;
	private final NbtList inventory;
	private final NbtCompound hunger;
	private final int xpLevel;
	private final float xpProgress;
	private final int xpTotal;

	private PlayerSnapshot(Identifier world, double x, double y, double z, float yaw, float pitch, float health,
						   int selectedSlot, NbtList inventory, NbtCompound hunger,
						   int xpLevel, float xpProgress, int xpTotal) {
		this.world = world;
		this.x = x;
		this.y = y;
		this.z = z;
		this.yaw = yaw;
		this.pitch = pitch;
		this.health = health;
		this.selectedSlot = selectedSlot;
		this.inventory = inventory;
		this.hunger = hunger;
		this.xpLevel = xpLevel;
		this.xpProgress = xpProgress;
		this.xpTotal = xpTotal;
	}

	public static PlayerSnapshot capture(ServerPlayerEntity p) {
		NbtCompound hunger = new NbtCompound();
		p.getHungerManager().writeNbt(hunger);
		return new PlayerSnapshot(
				p.getServerWorld().getRegistryKey().getValue(),
				p.getX(), p.getY(), p.getZ(), p.getYaw(), p.getPitch(),
				p.getHealth(),
				p.getInventory().selectedSlot,
				p.getInventory().writeNbt(new NbtList()),
				hunger,
				p.experienceLevel, p.experienceProgress, p.totalExperience);
	}

	public void restore(ServerPlayerEntity p, MinecraftServer server) {
		ServerWorld target = server.getWorld(RegistryKey.of(RegistryKeys.WORLD, world));
		if (target != null) {
			p.teleport(target, x, y, z, yaw, pitch);
		}

		p.getInventory().readNbt(inventory.copy());
		p.getInventory().selectedSlot = selectedSlot;
		p.getHungerManager().readNbt(hunger);
		p.experienceLevel = xpLevel;
		p.experienceProgress = xpProgress;
		p.totalExperience = xpTotal;

		p.clearStatusEffects();
		p.extinguish();
		p.fallDistance = 0.0f;
		p.setVelocity(Vec3d.ZERO);
		p.velocityModified = true;
		p.setHealth(Math.max(health, 1.0f));

		p.playerScreenHandler.sendContentUpdates();
	}

	public NbtCompound toNbt() {
		NbtCompound nbt = new NbtCompound();
		nbt.putString("world", world.toString());
		nbt.putDouble("x", x);
		nbt.putDouble("y", y);
		nbt.putDouble("z", z);
		nbt.putFloat("yaw", yaw);
		nbt.putFloat("pitch", pitch);
		nbt.putFloat("health", health);
		nbt.putInt("slot", selectedSlot);
		nbt.put("inventory", inventory.copy());
		nbt.put("hunger", hunger.copy());
		nbt.putInt("xpLevel", xpLevel);
		nbt.putFloat("xpProgress", xpProgress);
		nbt.putInt("xpTotal", xpTotal);
		return nbt;
	}

	public static PlayerSnapshot fromNbt(NbtCompound nbt) {
		return new PlayerSnapshot(
				new Identifier(nbt.getString("world")),
				nbt.getDouble("x"), nbt.getDouble("y"), nbt.getDouble("z"),
				nbt.getFloat("yaw"), nbt.getFloat("pitch"),
				nbt.getFloat("health"),
				nbt.getInt("slot"),
				nbt.getList("inventory", NbtElement.COMPOUND_TYPE),
				nbt.getCompound("hunger"),
				nbt.getInt("xpLevel"), nbt.getFloat("xpProgress"), nbt.getInt("xpTotal"));
	}
}
