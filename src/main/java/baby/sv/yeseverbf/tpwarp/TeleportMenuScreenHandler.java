package baby.sv.yeseverbf.tpwarp;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

/**
 * 传送菜单界面：箱子样式，点击对应格子即传送。所有物品都不可拿取/移动。
 */
public class TeleportMenuScreenHandler extends GenericContainerScreenHandler {
    private final List<TeleportPoint> points;

    public TeleportMenuScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory,
                                     int rows, List<TeleportPoint> points) {
        super(typeForRows(rows), syncId, playerInventory, inventory, rows);
        this.points = points;
    }

    private static ScreenHandlerType<GenericContainerScreenHandler> typeForRows(int rows) {
        return switch (rows) {
            case 1 -> ScreenHandlerType.GENERIC_9X1;
            case 2 -> ScreenHandlerType.GENERIC_9X2;
            case 3 -> ScreenHandlerType.GENERIC_9X3;
            case 4 -> ScreenHandlerType.GENERIC_9X4;
            case 5 -> ScreenHandlerType.GENERIC_9X5;
            default -> ScreenHandlerType.GENERIC_9X6;
        };
    }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        int containerSlots = getRows() * 9;
        if (slotIndex >= 0 && slotIndex < containerSlots) {
            if (slotIndex < points.size() && player instanceof ServerPlayerEntity serverPlayer) {
                TeleportPoint point = points.get(slotIndex);
                serverPlayer.closeHandledScreen();
                WarpManager.get().teleport(serverPlayer, point);
            }
        }
        // 不允许任何物品移动 / 拿取：重置光标并重新同步
        this.setCursorStack(ItemStack.EMPTY);
        this.sendContentUpdates();
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canInsertIntoSlot(ItemStack stack, net.minecraft.screen.slot.Slot slot) {
        return false;
    }
}
