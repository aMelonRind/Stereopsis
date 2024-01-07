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
import net.minecraft.text.Text;

import java.text.DecimalFormat;

public class ModMenuApiImpl implements ModMenuApi {
    private static final DecimalFormat df = new DecimalFormat("0.000");

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return p -> {
            Config cfg = Config.HANDLER.instance();
            return YetAnotherConfigLib.createBuilder()
                    .title(Text.literal("Stereopsis Config"))
                    .category(ConfigCategory.createBuilder()
                            .name(Text.literal("Settings"))
                            .option(Option.<Boolean>createBuilder()
                                    .name(Text.literal("View Mode"))
                                    .description(OptionDescription.of(Text.literal("Switch between cross mode or parallel mode (Experimental).")))
                                    .binding(false, () -> cfg.flipView, val -> cfg.flipView = val)
                                    .controller(opt -> BooleanControllerBuilder.create(opt)
                                            .formatValue(val -> Text.literal(val ? "AR (Parallel) (Experimental)" : "Cross"))
                                            .coloured(false))
                                    .build())
                            .option(Option.<Boolean>createBuilder()
                                    .name(Text.literal("Enable On Launch"))
                                    .description(OptionDescription.of(Text.literal("If enabled, stereopsis will enable automatically on game launch.")))
                                    .binding(false, () -> cfg.enableOnLaunch, val -> cfg.enableOnLaunch = val)
                                    .controller(TickBoxControllerBuilder::create)
                                    .build())
                            .option(Option.<Float>createBuilder()
                                    .name(Text.literal("Max Horizontal Offset"))
                                    .description(OptionDescription.of(Text.literal("The max offset for the view to focus on the crosshair.")))
                                    .binding(0.25f, () -> cfg.maxXOffset, val -> cfg.maxXOffset = val)
                                    .controller(opt -> FloatSliderControllerBuilder.create(opt)
                                            .range(0.0f, 0.25f)
                                            .step(0.001f)
                                            .formatValue(val -> Text.literal(df.format(val))))
                                    .build())
                            .option(Option.<Float>createBuilder()
                                    .name(Text.literal("Horizontal Offset Speed"))
                                    .binding(1.0f, () -> cfg.offsetSpeed, val -> cfg.offsetSpeed = val < 0 ? -1.0f : val)
                                    .controller(opt -> FloatSliderControllerBuilder.create(opt)
                                            .range(-0.001f, 8.0f)
                                            .step(0.001f)
                                            .formatValue(val -> Text.literal(val < 0 ? "Instant" : val == 0 ? "Disabled" : df.format(val))))
                                    .build())
                            .build())
                    .save(Config.HANDLER::save)
                    .build()
                    .generateScreen(p);
        };
    }

}
