package dev.emi.chime.mixin;

import java.io.InputStreamReader;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.apache.logging.log4j.LogManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.emi.chime.override.ChimeArmorOverrideLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.render.model.json.JsonUnbakedModel;
import net.minecraft.client.render.model.json.ModelOverride;
import net.minecraft.client.render.model.json.ModelOverrideList;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

@Mixin(JsonUnbakedModel.class)
public class JsonUnbakedModelMixin {
	@Unique
	private static final Gson GSON = new Gson();
	@Shadow @Final
	private List<ModelOverride> overrides;
	@Shadow
	public String id;

	@Inject(at = @At("HEAD"), method = "compileOverrides")
	private void compileOverrides(ModelLoader modelLoader, JsonUnbakedModel parent, CallbackInfoReturnable<ModelOverrideList> info) {
		ModelOverride.Deserializer deserializer = new ChimeArmorOverrideLoader.Deserializer2();
		ResourceManager manager = MinecraftClient.getInstance().getResourceManager();
		try {
			if (id.contains("#")) { // Gets rid of an error when loading "minecraft:builtin/missing#missing"
				return;
			}
			Identifier baseId = new Identifier(id);
			Identifier path = new Identifier(baseId.getNamespace(), "overrides/" + baseId.getPath() + ".json");
			if (manager.containsResource(path)) {
				for (Resource r : manager.getAllResources(path)) {
					try (InputStreamReader reader = new InputStreamReader(r.getInputStream())) {
						JsonObject object = GSON.fromJson(reader, JsonObject.class);
						JsonArray arr = object.getAsJsonArray("overrides");
						for (JsonElement el : arr) {
							// What harm could passing null ever have?
							ModelOverride override = deserializer.deserialize(el, null, null);
							overrides.add(override);
						}
					} catch (Exception e) {
						LogManager.getLogger("chime").warn("[chime] Malformed json for item override: " + r.getId(), e);
					}
				}
			}
		} catch (Exception e) {
			LogManager.getLogger("chime").warn("[chime] IO error reading item overrides: " + id, e);
		}
	}
}