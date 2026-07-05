package baby.sv.yeseverbf.role;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.PlayerListHeaderS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.GameMode;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 身份系统：
 * - 非 OP 玩家 = 永远是游客（Guest）。
 * - OP 玩家 = Staff；OP 可用命令把自己临时切换成游客（forcedGuests 持久化）。
 * 负责：游客状态强制（冒险模式 + 允许飞行 + 专属物品）、TAB/聊天称号、TAB 装饰。
 */
public final class RoleManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = Path.of("config", "yeseverbf_guests.json");
    private static final Type SET_TYPE = new TypeToken<LinkedHashSet<String>>() {
    }.getType();

    // OP 但主动切换到游客模式的玩家
    private static final Set<UUID> forcedGuests = ConcurrentHashMap.newKeySet();

    // 主动切换到旁观者模式的游客（仅本次在线有效，重新进服恢复冒险）
    private static final Set<UUID> spectatorGuests = ConcurrentHashMap.newKeySet();

    private static final int TP_MENU_SLOT = 0;
    private static final int NIGHT_VISION_SLOT = 1;
    private static final int FLY_CTRL_SLOT = 8;

    private RoleManager() {
    }

    // ---------------- 持久化 ----------------

    public static void load() {
        forcedGuests.clear();
        if (Files.exists(PATH)) {
            try {
                String json = Files.readString(PATH);
                Set<String> ids = GSON.fromJson(json, SET_TYPE);
                if (ids != null) {
                    for (String id : ids) {
                        try {
                            forcedGuests.add(UUID.fromString(id));
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void save() {
        try {
            Files.createDirectories(PATH.getParent());
            Set<String> ids = new LinkedHashSet<>();
            for (UUID id : forcedGuests) {
                ids.add(id.toString());
            }
            Files.writeString(PATH, GSON.toJson(ids, SET_TYPE));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ---------------- 身份判定 ----------------

    public static boolean isStaff(ServerPlayerEntity player) {
        return player.hasPermissionLevel(2) && !forcedGuests.contains(player.getUuid());
    }

    public static boolean isGuest(ServerPlayerEntity player) {
        return !isStaff(player);
    }

    /** OP 切换自己到游客 / 切回 Staff。 */
    public static void setForcedGuest(ServerPlayerEntity player, boolean guest) {
        if (guest) {
            forcedGuests.add(player.getUuid());
        } else {
            forcedGuests.remove(player.getUuid());
        }
        save();
        apply(player);
    }

    // ---------------- 状态应用 ----------------

    public static void apply(ServerPlayerEntity player) {
        if (isGuest(player)) {
            applyGuest(player);
        } else {
            applyStaff(player);
        }
        refreshListName(player);
    }

    private static void applyGuest(ServerPlayerEntity player) {
        boolean spectating = spectatorGuests.contains(player.getUuid());
        GameMode target = spectating ? GameMode.SPECTATOR : GameMode.ADVENTURE;
        if (player.interactionManager.getGameMode() != target) {
            player.changeGameMode(target);
        }
        if (!spectating) {
            // 冒险模式下也允许飞行
            if (!player.getAbilities().allowFlying) {
                player.getAbilities().allowFlying = true;
                player.sendAbilitiesUpdate();
            }
            feed(player);
        }
        ensureGuestItems(player);
    }

    /** 设置游客的旁观 / 冒险模式（仅游客可用）。返回是否成功执行。 */
    public static boolean setSpectator(ServerPlayerEntity player, boolean spectate) {
        if (!isGuest(player)) {
            return false;
        }
        if (spectate) {
            spectatorGuests.add(player.getUuid());
            player.changeGameMode(GameMode.SPECTATOR);
        } else {
            spectatorGuests.remove(player.getUuid());
            player.changeGameMode(GameMode.ADVENTURE);
            player.getAbilities().allowFlying = true;
            player.sendAbilitiesUpdate();
            feed(player);
        }
        return true;
    }

    public static boolean isSpectatorGuest(ServerPlayerEntity player) {
        return spectatorGuests.contains(player.getUuid());
    }

    public static void clearSpectator(ServerPlayerEntity player) {
        spectatorGuests.remove(player.getUuid());
    }

    private static void applyStaff(ServerPlayerEntity player) {
        // 管理员不发放游客物品；移除身上残留的游客物品
        removeSpecialItems(player);
    }

    /** 确保两个游客专属物品在指定槽位，且不覆盖玩家其它物品（会把占用的普通物品挪到空槽）。 */
    private static void ensureGuestItems(ServerPlayerEntity player) {
        // 先移除可能存在于其它位置的专属物品，避免重复
        removeSpecialItems(player);
        setSpecialSlot(player, TP_MENU_SLOT, SpecialItems.teleportMenu());
        setSpecialSlot(player, NIGHT_VISION_SLOT, SpecialItems.nightVision());
        setSpecialSlot(player, FLY_CTRL_SLOT, SpecialItems.flightController());
    }

    /** 常驻饱腹：始终保持饱食度、不掉饥饿值。 */
    private static void feed(ServerPlayerEntity player) {
        if (player.getHungerManager().getFoodLevel() < 20) {
            player.getHungerManager().setFoodLevel(20);
        }
        player.getHungerManager().setSaturationLevel(20.0f);
    }

    private static void setSpecialSlot(ServerPlayerEntity player, int slot, ItemStack special) {
        PlayerInventory inv = player.getInventory();
        ItemStack existing = inv.getStack(slot);
        if (!existing.isEmpty() && !SpecialItems.isSpecial(existing)) {
            int empty = firstEmptySlot(inv);
            if (empty >= 0) {
                inv.setStack(empty, existing.copy());
            } else {
                player.dropItem(existing.copy(), false);
            }
        }
        inv.setStack(slot, special);
    }

    private static int firstEmptySlot(PlayerInventory inv) {
        for (int i = 0; i < inv.main.size(); i++) {
            if (i == TP_MENU_SLOT || i == NIGHT_VISION_SLOT || i == FLY_CTRL_SLOT) {
                continue;
            }
            if (inv.getStack(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    private static void removeSpecialItems(ServerPlayerEntity player) {
        PlayerInventory inv = player.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            if (SpecialItems.isSpecial(inv.getStack(i))) {
                inv.setStack(i, ItemStack.EMPTY);
            }
        }
    }

    // ---------------- 称号 ----------------

    /** TAB 列表里的名字：[Staff]/[Guest] + 玩家名。 */
    public static Text formatTabName(ServerPlayerEntity player) {
        return Text.empty().append(badge(player)).append(" ")
                .append(Text.literal(player.getGameProfile().getName()));
    }

    /** 聊天显示：[Staff]/[Guest] 玩家名: 内容 */
    public static Text formatChat(ServerPlayerEntity player, Text content) {
        return Text.empty()
                .append(badge(player))
                .append(" ")
                .append(Text.literal(player.getGameProfile().getName()).formatted(Formatting.WHITE))
                .append(Text.literal(": ").formatted(Formatting.GRAY))
                .append(Text.literal(content.getString()).formatted(Formatting.WHITE));
    }

    public static Text badge(ServerPlayerEntity player) {
        if (isStaff(player)) {
            return Text.literal("[Staff]").formatted(Formatting.RED, Formatting.BOLD);
        }
        return Text.literal("[Guest]").formatted(Formatting.GREEN);
    }

    /** 让所有客户端刷新该玩家的 TAB 名字。 */
    public static void refreshListName(ServerPlayerEntity player) {
        MinecraftServer server = player.getServer();
        if (server != null) {
            server.getPlayerManager().sendToAll(
                    new PlayerListS2CPacket(PlayerListS2CPacket.Action.UPDATE_DISPLAY_NAME, player));
        }
    }

    // ---------------- TAB 装饰 + 周期性维护 ----------------

    public static void tickMaintenance(MinecraftServer server) {
        List<ServerPlayerEntity> players = server.getPlayerManager().getPlayerList();
        int staffCount = 0;
        int guestCount = 0;
        for (ServerPlayerEntity player : players) {
            if (isStaff(player)) {
                staffCount++;
            } else {
                guestCount++;
                if (spectatorGuests.contains(player.getUuid())) {
                    // 主动旁观的游客：维持旁观，不强制拉回冒险
                    if (player.interactionManager.getGameMode() != GameMode.SPECTATOR) {
                        player.changeGameMode(GameMode.SPECTATOR);
                    }
                } else {
                    // 周期性确保游客始终处于冒险模式 + 可飞行 + 常驻饱腹
                    if (player.interactionManager.getGameMode() != GameMode.ADVENTURE) {
                        player.changeGameMode(GameMode.ADVENTURE);
                    }
                    if (!player.getAbilities().allowFlying) {
                        player.getAbilities().allowFlying = true;
                        player.sendAbilitiesUpdate();
                    }
                    feed(player);
                }
            }
        }

        Text header = Text.empty()
                .append(Text.literal("永无之地建筑组").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD))
                .append(Text.literal(" ~").formatted(Formatting.LIGHT_PURPLE));
        Text footer = Text.empty()
                .append(Text.literal("当前有 ").formatted(Formatting.GRAY))
                .append(Text.literal(String.valueOf(staffCount)).formatted(Formatting.AQUA))
                .append(Text.literal(" 个打工人正在打工").formatted(Formatting.GRAY))
                .append(Text.literal("    "))
                .append(Text.literal("当前有 ").formatted(Formatting.GRAY))
                .append(Text.literal(String.valueOf(guestCount)).formatted(Formatting.AQUA))
                .append(Text.literal(" 个游客正在观光").formatted(Formatting.GRAY));

        PlayerListHeaderS2CPacket packet = new PlayerListHeaderS2CPacket(header, footer);
        for (ServerPlayerEntity player : players) {
            player.networkHandler.sendPacket(packet);
        }
    }
}
