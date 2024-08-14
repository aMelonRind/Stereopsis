package io.github.amelonrind.stereopsis.config;

import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import io.github.amelonrind.stereopsis.Stereopsis;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;

public class Config {
    public static final ConfigClassHandler<Config> HANDLER = ConfigClassHandler.createBuilder(Config.class)
            .id(Identifier.of(Stereopsis.MOD_ID, "main"))
            .serializer(config -> GsonConfigSerializerBuilder.create(config)
                    .setPath(FabricLoader.getInstance().getConfigDir().resolve(Stereopsis.MOD_ID + ".json5"))
                    .setJson5(true)
                    .build())
            .build();

    public static Config get() {
        return HANDLER.instance();
    }

    @SerialEntry(comment = "Experimental! Set to true for parallel mode (AR mode)")
    public boolean flipView = false;

    @SerialEntry(comment = "Set to true to auto enable stereopsis on game launch")
    public boolean enableOnLaunch = false;

    @SerialEntry(comment = "This takes around 0.8% of performance (no other mod, new world, no shaders). If you're using shader pack, you probably want this to be on. It will (hopefully) fix shader rendering problem. I (dev) have no idea how this works, so it's a magic fix.")
    public boolean magicFixForShaders = false;

    @SerialEntry(comment = "The max offset for the view to focus on the crosshair. This value shouldn't be higher than 0.25")
    public float maxXOffset = 0.25f;

    @SerialEntry(comment = "The speed of the horizontal offset. negative for instant")
    public float offsetSpeed = 1.0f;

    @SerialEntry(comment = "The offset of the HUD")
    public int hudOffset = 0;

    public void fixValues() {
        if (maxXOffset < 0.0f) maxXOffset = 0.0f;
        if (maxXOffset > 0.25f) maxXOffset = 0.25f;
        if (Math.abs(hudOffset) > 255) hudOffset = 0;
    }

}
