package baby.sv.yeseverbf.tpwarp;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

/**
 * 打开传送菜单：列出所有传送点。
 */
public final class TeleportMenu {

    // 按格子顺序循环使用的图标：末影之眼 / 火把 / 烈焰粉 / 风弹
    private static final Item[] ICONS = {Items.ENDER_EYE, Items.TORCH, Items.BLAZE_POWDER, Items.WIND_CHARGE};

    private TeleportMenu() {
    }

    public static void open(ServerPlayerEntity player) {
        List<TeleportPoint> points = new ArrayList<>(WarpManager.get().all());
        int rows = Math.max(1, Math.min(6, (points.size() + 8) / 9));
        if (points.isEmpty()) {
            rows = 1;
        }
        int size = rows * 9;
        SimpleInventory inventory = new SimpleInventory(size);
        for (int i = 0; i < points.size() && i < size; i++) {
            inventory.setStack(i, icon(points.get(i), i));
        }
        final int finalRows = rows;
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, playerInventory, p) ->
                        new TeleportMenuScreenHandler(syncId, playerInventory, inventory, finalRows, points),
                Text.literal("传送菜单")));
    }

    private static ItemStack icon(TeleportPoint point, int index) {
        ItemStack stack = new ItemStack(ICONS[index % ICONS.length]);
        stack.set(DataComponentTypes.CUSTOM_NAME,
                Text.literal(point.displayTitle()).formatted(Formatting.AQUA).styled(s -> s.withItalic(false)));
        if (point.description != null && !point.description.isEmpty()) {
            List<Text> lore = new ArrayList<>();
            for (String line : point.description.split("\\\\n")) {
                lore.add(Text.literal(line).formatted(Formatting.GRAY).styled(s -> s.withItalic(false)));
            }
            stack.set(DataComponentTypes.LORE, new LoreComponent(lore));
        }
        return stack;
    }
}
