package at.petrak.hexcasting.common.lib;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import static at.petrak.hexcasting.api.HexAPI.modLoc;

public class HexKeyMappings {
    public static void registerMappings(BiConsumer<KeyMapping, ResourceLocation> r) {
        for (var e : KEY_MAPPINGS.entrySet()) {
            r.accept(e.getValue(), e.getKey());
        }
    }

    public static final String KEY_CATEGORY_SPELL_DEBUGGER = "key.category.hexcasting.spell_debugger";

    private static final Map<ResourceLocation, KeyMapping> KEY_MAPPINGS = new LinkedHashMap<>();

    public static final KeyMapping DEBUG_TOGGLE_KEY = make("key.hexcasting.debug/toggle", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F10, KEY_CATEGORY_SPELL_DEBUGGER);
    public static final KeyMapping DEBUG_STEP_KEY = make("key.hexcasting.debug/step", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F8, KEY_CATEGORY_SPELL_DEBUGGER);
    public static final KeyMapping DEBUG_SKIP_FRAME_KEY = make("key.hexcasting.debug/skip_frame", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F7, KEY_CATEGORY_SPELL_DEBUGGER);

    private static KeyMapping make(String name, InputConstants.Type type, int defaultKey, String category) {
        var id = modLoc(name);
        var mapping = new KeyMapping(name, type, defaultKey, category);
        var old = KEY_MAPPINGS.put(id, mapping);
        if (old != null) {
            throw new IllegalArgumentException("Typo? Duplicate id " + name);
        }
        return mapping;
    }
}