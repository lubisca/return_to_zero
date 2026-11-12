package dev.returntozero.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.loader.api.FabricLoader;

/** Liga o botao "Configurar" do Mod Menu a tela do Cloth Config (se o Cloth Config estiver instalado). */
public class ModMenuIntegration implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		if (!FabricLoader.getInstance().isModLoaded("cloth-config")) {
			return parent -> parent;
		}
		return ModConfigScreen::create;
	}
}
