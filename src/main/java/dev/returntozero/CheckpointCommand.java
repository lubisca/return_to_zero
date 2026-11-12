package dev.returntozero;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * /zero                      cria o seu checkpoint            (create your checkpoint)
 * /zero voltar | back        volta ao seu checkpoint          (go back to it)
 * /zero config ...           so para operador (op only):
 *     intervalo | interval [min]     checkpoint automatico, 0 desliga
 *     sanidade  | sanity   [0-100]   sanidade perdida ao reviver
 *     piso      | floor    [0-100]   sanidade minima ao reviver
 *     chance               [0-100]   chance de conquistas
 *     blocos    | blocks   [true/false]  desfazer blocos do jogador
 *     recarregar | reload            relê o arquivo de config
 * Sem valor, cada opcao mostra o valor atual.
 */
public final class CheckpointCommand {
	private CheckpointCommand() {}

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CheckpointManager manager) {
		LiteralArgumentBuilder<ServerCommandSource> config = CommandManager.literal("config")
				.requires(src -> src.hasPermissionLevel(2));

		for (String name : new String[]{"intervalo", "interval"}) {
			config.then(intOption(name, "Checkpoint automatico (min, 0 = desligado)", 0, 1440,
					() -> ModConfig.get().autoIntervalMinutes, v -> ModConfig.get().autoIntervalMinutes = v));
		}
		for (String name : new String[]{"sanidade", "sanity"}) {
			config.then(doubleOption(name, "Sanidade perdida ao reviver (%)",
					() -> ModConfig.get().sanityLossOnRevive, v -> ModConfig.get().sanityLossOnRevive = v));
		}
		for (String name : new String[]{"piso", "floor"}) {
			config.then(doubleOption(name, "Sanidade minima ao reviver (%)",
					() -> ModConfig.get().sanityFloorOnRevive, v -> ModConfig.get().sanityFloorOnRevive = v));
		}
		config.then(doubleOption("chance", "Chance de conquistas (%)",
				() -> ModConfig.get().advancementChance, v -> ModConfig.get().advancementChance = v));
		for (String name : new String[]{"blocos", "blocks"}) {
			config.then(boolOption(name, "Desfazer blocos do jogador",
					() -> ModConfig.get().undoPlayerBlocks, v -> ModConfig.get().undoPlayerBlocks = v));
		}
		for (String name : new String[]{"recarregar", "reload"}) {
			config.then(CommandManager.literal(name).executes(ctx -> {
				ModConfig.load();
				return ok(ctx.getSource(), "Configuracao recarregada.");
			}));
		}

		LiteralArgumentBuilder<ServerCommandSource> root = CommandManager.literal("zero")
				.executes(ctx -> {
					ServerPlayerEntity player = ctx.getSource().getPlayer();
					if (player == null) {
						return fail(ctx.getSource(), "Comando reservado aos jogadores.");
					}
					manager.create(player);
					return ok(ctx.getSource(), "Checkpoint criado!");
				});

		for (String name : new String[]{"voltar", "back"}) {
			root.then(CommandManager.literal(name).executes(ctx -> {
				ServerPlayerEntity player = ctx.getSource().getPlayer();
				if (player == null) {
					return fail(ctx.getSource(), "Comando reservado aos jogadores.");
				}
				if (!manager.restore(player, false)) {
					return fail(ctx.getSource(), "Nenhum checkpoint para restaurar.");
				}
				return ok(ctx.getSource(), "Voce voltou ao seu checkpoint.");
			}));
		}

		dispatcher.register(root.then(config));
	}

	private static LiteralArgumentBuilder<ServerCommandSource> intOption(String name, String label, int min, int max,
																		IntSupplier get, IntConsumer set) {
		return CommandManager.literal(name)
				.executes(ctx -> ok(ctx.getSource(), label + ": " + get.getAsInt()))
				.then(CommandManager.argument("value", IntegerArgumentType.integer(min, max)).executes(ctx -> {
					set.accept(IntegerArgumentType.getInteger(ctx, "value"));
					ModConfig.save();
					return ok(ctx.getSource(), label + ": " + get.getAsInt());
				}));
	}

	private static LiteralArgumentBuilder<ServerCommandSource> doubleOption(String name, String label,
																			DoubleSupplier get, DoubleConsumer set) {
		return CommandManager.literal(name)
				.executes(ctx -> ok(ctx.getSource(), label + ": " + get.getAsDouble()))
				.then(CommandManager.argument("value", DoubleArgumentType.doubleArg(0.0, 100.0)).executes(ctx -> {
					set.accept(DoubleArgumentType.getDouble(ctx, "value"));
					ModConfig.save();
					return ok(ctx.getSource(), label + ": " + get.getAsDouble());
				}));
	}

	private static LiteralArgumentBuilder<ServerCommandSource> boolOption(String name, String label,
																		  BooleanSupplier get, Consumer<Boolean> set) {
		return CommandManager.literal(name)
				.executes(ctx -> ok(ctx.getSource(), label + ": " + get.getAsBoolean()))
				.then(CommandManager.argument("value", BoolArgumentType.bool()).executes(ctx -> {
					set.accept(BoolArgumentType.getBool(ctx, "value"));
					ModConfig.save();
					return ok(ctx.getSource(), label + ": " + get.getAsBoolean());
				}));
	}

	private static int ok(ServerCommandSource source, String message) {
		source.sendFeedback(() -> Text.literal(message), false);
		return 1;
	}

	private static int fail(ServerCommandSource source, String message) {
		source.sendError(Text.literal(message));
		return 0;
	}
}
