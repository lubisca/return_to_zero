package dev.returntozero.client;

import dev.returntozero.ModConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/** Tela de configuracao (Mod Menu > Return By Death > Configurar). Edita config/return_to_zero.json. */
public final class ModConfigScreen {
	private ModConfigScreen() {}

	public static Screen create(Screen parent) {
		ModConfig cfg = ModConfig.get();

		ConfigBuilder builder = ConfigBuilder.create()
				.setParentScreen(parent)
				.setTitle(Text.literal("Return to Zero"))
				.setSavingRunnable(ModConfig::save);

		ConfigEntryBuilder entries = builder.entryBuilder();
		ConfigCategory general = builder.getOrCreateCategory(Text.literal("Geral"));

		general.addEntry(entries.startIntField(
						Text.literal("Checkpoint automatico (minutos, 0 = desligado)"), cfg.autoIntervalMinutes)
				.setDefaultValue(10).setMin(0).setMax(1440)
				.setTooltip(Text.literal("De quanto em quanto tempo o checkpoint de cada jogador e atualizado."))
				.setSaveConsumer(v -> cfg.autoIntervalMinutes = v)
				.build());

		general.addEntry(entries.startDoubleField(
						Text.literal("Sanidade perdida ao reviver (%)"), cfg.sanityLossOnRevive)
				.setDefaultValue(15.0).setMin(0.0).setMax(100.0)
				.setTooltip(Text.literal("Usa o /sanity add do Sanity: Renewed. 0 desliga."))
				.setSaveConsumer(v -> cfg.sanityLossOnRevive = v)
				.build());

		general.addEntry(entries.startDoubleField(
						Text.literal("Sanidade minima ao reviver (%)"), cfg.sanityFloorOnRevive)
				.setDefaultValue(50.0).setMin(0.0).setMax(100.0)
				.setTooltip(Text.literal("Ao reviver, a sanidade sobe para pelo menos esse valor. 0 desliga."))
				.setSaveConsumer(v -> cfg.sanityFloorOnRevive = v)
				.build());

		general.addEntry(entries.startBooleanToggle(
						Text.literal("Desfazer blocos do proprio jogador"), cfg.undoPlayerBlocks)
				.setDefaultValue(true)
				.setTooltip(Text.literal("Ao morrer, desfaz os blocos que ele colocou ou quebrou desde o checkpoint."))
				.setSaveConsumer(v -> cfg.undoPlayerBlocks = v)
				.build());

		general.addEntry(entries.startIntField(
						Text.literal("Limite de blocos rastreados por jogador"), cfg.maxTrackedBlocksPerPlayer)
				.setDefaultValue(5000).setMin(100).setMax(100000)
				.setTooltip(Text.literal("Protege a memoria. Passou do limite, os blocos novos nao sao desfeitos."))
				.setSaveConsumer(v -> cfg.maxTrackedBlocksPerPlayer = v)
				.build());

		general.addEntry(entries.startDoubleField(
						Text.literal("Chance de conquistas (%)"), cfg.advancementChance)
				.setDefaultValue(100.0).setMin(0.0).setMax(100.0)
				.setTooltip(Text.literal("100 = comportamento normal. Receitas nao sao afetadas."))
				.setSaveConsumer(v -> cfg.advancementChance = v)
				.build());

		return builder.build();
	}
}
