package io.github.amelonrind.stereopsis.config;

import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import io.github.amelonrind.stereopsis.RenderMode;
import io.github.amelonrind.stereopsis.Stereopsis;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;

public class Config {
    public static final ConfigClassHandler<Config> HANDLER = ConfigClassHandler.createBuilder(Config.class)
            .id(new Identifier(Stereopsis.MOD_ID, "main"))
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

    @SerialEntry
    public RenderMode renderMode = RenderMode.DUO;

    @SerialEntry(comment = "Set to true to auto enable stereopsis on game launch")
    public boolean enableOnLaunch = false;

    @SerialEntry(comment = "The max offset for the view to focus on the crosshair. This value shouldn't be higher than 0.25")
    public float maxXOffset = 0.25f;

    @SerialEntry(comment = "The speed of the horizontal offset. negative for instant")
    public float offsetSpeed = 1.0f;

    public void apply() {
        if (maxXOffset < 0.0f) maxXOffset = 0.0f;
        if (maxXOffset > 0.25f) maxXOffset = 0.25f;

        RenderMode.set(this.renderMode);
    }

}
