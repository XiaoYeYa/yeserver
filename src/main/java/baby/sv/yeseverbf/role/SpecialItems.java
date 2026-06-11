package baby.sv.yeseverbf.role;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * 游客专属物品（不可丢弃、不可移动）：传送菜单、飞行控制器。
 * 通过 CUSTOM_DATA 里的标记字段识别，避免与玩家普通物品混淆。
 */
public final class SpecialItems {
    public static final String MARKER_KEY = "yeseverbf_item";
    public static final String TP_MENU = "tp_menu";
    public static final String FLY_CTRL = "fly_ctrl";

    private SpecialItems() {
    }

    public static ItemStack teleportMenu() {
        ItemStack stack = new ItemStack(Items.COMPASS);
        stack.set(DataComponentTypes.CUSTOM_NAME,
                Text.literal("传送菜单").formatted(Formatting.AQUA, Formatting.BOLD).styled(s -> s.withItalic(false)));
        mark(stack, TP_MENU);
        return stack;
    }

    public static ItemStack flightController() {
        ItemStack stack = new ItemStack(Items.FEATHER);
        stack.set(DataComponentTypes.CUSTOM_NAME,
                Text.literal("飞行控制器").formatted(Formatting.YELLOW, Formatting.BOLD).styled(s -> s.withItalic(false)));
        mark(stack, FLY_CTRL);
        return stack;
    }

    private static void mark(ItemStack stack, String value) {
        NbtCompound nbt = new NbtCompound();
        nbt.putString(MARKER_KEY, value);
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
    }

    /** 返回物品的标记（tp_menu / fly_ctrl），非特殊物品返回 null。 */
    public static String markerOf(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        NbtComponent data = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (data == null) {
            return null;
        }
        NbtCompound nbt = data.copyNbt();
        return nbt.contains(MARKER_KEY) ? nbt.getString(MARKER_KEY) : null;
    }

    public static boolean isSpecial(ItemStack stack) {
        return markerOf(stack) != null;
    }
}
