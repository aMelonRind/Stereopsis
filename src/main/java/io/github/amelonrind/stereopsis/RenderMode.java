package io.github.amelonrind.stereopsis;

import com.google.gson.annotations.SerializedName;
import net.minecraft.text.Text;

import static io.github.amelonrind.stereopsis.Stereopsis.mc;
import static io.github.amelonrind.stereopsis.Stereopsis.righting;

@SuppressWarnings("unused")
public enum RenderMode {
    @SerializedName("duo")
    DUO,
    @SerializedName("single")
    SINGLE,
    @SerializedName("single_synced")
    SINGLE_SYNCED;

    private static RenderMode mode;
    private static boolean isDuo;
    private static boolean isSynced;

    private final Text name;
    private final Text desc;

    static {
        setInternal(DUO);
    }

    public static RenderMode get() {
        return mode;
    }

    public static void set(RenderMode value) {
        mc.execute(() -> setInternal(value));
    }

    private static void setInternal(RenderMode value) {
        mode = value;
        isDuo = mode == DUO;
        isSynced = mode == SINGLE_SYNCED;
        righting = true;
    }

    public static boolean isDuo() {
        return isDuo;
    }

    public static boolean isSynced() {
        return isSynced;
    }

    RenderMode() {
        String pre = "render_mode." + name().toLowerCase();
        name = Stereopsis.translatable(pre + ".name");
        desc = Stereopsis.translatable(pre + ".description");
    }

    public Text getName() {
        return name;
    }

    public Text getDescription() {
        return desc;
    }

}
