package dev.returntozero;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/** Configuracao salva em config/return_to_zero.json. */
public final class ModConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static ModConfig instance = new ModConfig();

	/** Intervalo do checkpoint automatico, em minutos. 0 desliga. */
	public int autoIntervalMinutes = 10;
	/** Sanidade perdida ao reviver, em pontos percentuais (0 desliga). */
	public double sanityLossOnRevive = 15.0;
	/** Sanidade minima (em %) com que o jogador volta depois de reviver. 0 desliga. */
	public double sanityFloorOnRevive = 50.0;
	/** Se verdadeiro, desfaz os blocos que o proprio jogador colocou ou quebrou desde o checkpoint. */
	public boolean undoPlayerBlocks = true;
	/** Limite de blocos rastreados por jogador (protege a memoria). */
	public int maxTrackedBlocksPerPlayer = 5000;
	/** Chance (0 a 100) de uma conquista (exceto receitas) ser concedida. 100 = sem efeito. */
	public double advancementChance = 100.0;

	private ModConfig() {}

	public static ModConfig get() {
		return instance;
	}

	private static Path path() {
		return FabricLoader.getInstance().getConfigDir().resolve("return_to_zero.json");
	}

	public static void load() {
		Path file = path();
		if (Files.exists(file)) {
			try (Reader reader = Files.newBufferedReader(file)) {
				ModConfig loaded = GSON.fromJson(reader, ModConfig.class);
				if (loaded != null) {
					instance = loaded;
				}
			} catch (Exception e) {
				ReturnToZero.LOGGER.warn("Nao consegui ler return_to_zero.json, usando os valores padrao.", e);
			}
		}
		save();
	}

	public static void save() {
		try {
			Files.createDirectories(path().getParent());
			try (Writer writer = Files.newBufferedWriter(path())) {
				GSON.toJson(instance, writer);
			}
		} catch (IOException e) {
			ReturnToZero.LOGGER.warn("Nao consegui salvar return_to_zero.json.", e);
		}
	}
}
