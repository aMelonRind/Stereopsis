package io.github.amelonrind.stereopsis.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.FloatSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import io.github.amelonrind.stereopsis.Stereopsis;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;

public class ModMenuApiImpl implements ModMenuApi {
    private static final DecimalFormat df = new DecimalFormat("0.000");
    private static final Config def = Config.HANDLER.defaults();
    private static final Text AR_TEXT = translatable("value.ar");
    private static final Text CROSS_TEXT = translatable("value.cross");
    private static final Text INSTANT_TEXT = translatable("value.instant");
    private static final Text DISABLED_TEXT = translatable("value.disabled");

    @Contract(value = "_ -> new", pure = true)
    private static @NotNull MutableText translatable(String key) {
        return Stereopsis.translatable("settings." + key);
    }

    @Contract(value = "_ -> new", pure = true)
    private static @NotNull OptionDescription descriptionOf(String key) {
        return OptionDescription.of(translatable(key + ".description"));
    }

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return p -> {
            Config cfg = Config.get();
            return YetAnotherConfigLib.createBuilder()
                    .title(translatable("title"))
                    .category(ConfigCategory.createBuilder()
                            .name(translatable("title"))
                            .option(Option.<Boolean>createBuilder()
                                    .name(translatable("flipView"))
                                    .description(descriptionOf("flipView"))
                                    .binding(def.flipView, () -> cfg.flipView, val -> cfg.flipView = val)
                                    .controller(opt -> BooleanControllerBuilder.create(opt)
                                            .formatValue(val -> val ? AR_TEXT : CROSS_TEXT)
                                            .coloured(false))
                                    .build())
                            .option(Option.<Boolean>createBuilder()
                                    .name(translatable("enableOnLaunch"))
                                    .description(descriptionOf("enableOnLaunch"))
                                    .binding(def.enableOnLaunch, () -> cfg.enableOnLaunch, val -> cfg.enableOnLaunch = val)
                                    .controller(TickBoxControllerBuilder::create)
                                    .build())
                            .option(Option.<Boolean>createBuilder()
                                    .name(translatable("magicFixForShaders"))
                                    .description(descriptionOf("magicFixForShaders"))
                                    .binding(def.magicFixForShaders, () -> cfg.magicFixForShaders, val -> cfg.magicFixForShaders = val)
                                    .controller(TickBoxControllerBuilder::create)
                                    .build())
                            .option(Option.<Float>createBuilder()
                                    .name(translatable("maxXOffset"))
                                    .description(descriptionOf("maxXOffset"))
                                    .binding(def.maxXOffset, () -> cfg.maxXOffset, val -> cfg.maxXOffset = val)
                                    .controller(opt -> FloatSliderControllerBuilder.create(opt)
                                            .range(0.0f, 0.25f)
                                            .step(0.001f)
                                            .formatValue(val -> Text.literal(df.format(val))))
                                    .build())
                            .option(Option.<Float>createBuilder()
                                    .name(translatable("offsetSpeed"))
                                    .binding(def.offsetSpeed, () -> cfg.offsetSpeed, val -> cfg.offsetSpeed = val < 0 ? -1.0f : val)
                                    .controller(opt -> FloatSliderControllerBuilder.create(opt)
                                            .range(-0.001f, 8.0f)
                                            .step(0.001f)
                                            .formatValue(val -> val < 0 ? INSTANT_TEXT : val == 0 ? DISABLED_TEXT : Text.literal(df.format(val))))
                                    .build())
                            .build())
                    .save(Config.HANDLER::save)
                    .build()
                    .generateScreen(p);
        };
    }

}
