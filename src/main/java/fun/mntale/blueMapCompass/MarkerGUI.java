package fun.mntale.blueMapCompass;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Marker selection GUI for BlueMap Compass plugin.
 * Modern, clean, and extensible implementation using Adventure API and best practices.
 */
public class MarkerGUI implements Listener {
    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final String GUI_TITLE_RAW = "<gold>BlueMap Markers";
    private static final int INVENTORY_SIZE = 54;
    private static final int PAGE_SIZE = 45;
    private static final int SLOT_CATEGORY = 45;
    private static final int SLOT_PREV = 48;
    private static final int SLOT_UNTRACK = 49;
    private static final int SLOT_NEXT = 50;
    private static final int SLOT_CLOSE = 53;
    private static final Material CATEGORY_ICON = Material.BOOK;
    private static final NamespacedKey MARKER_ID_KEY = new NamespacedKey("bluemapcompass", "marker_id");

    // Map banner color names to closest MiniMessage hex color codes
    private static final Map<String, String> BANNER_TO_HEX = Map.ofEntries(
        Map.entry("white", "#F9FFFE"),
        Map.entry("orange", "#F9801D"),
        Map.entry("magenta", "#C74EBD"),
        Map.entry("light_blue", "#3AB3DA"),
        Map.entry("yellow", "#FED83D"),
        Map.entry("lime", "#80C71F"),
        Map.entry("pink", "#F38BAA"),
        Map.entry("gray", "#474F52"),
        Map.entry("light_gray", "#9D9D97"),
        Map.entry("cyan", "#169C9C"),
        Map.entry("purple", "#8932B8"),
        Map.entry("blue", "#3C44AA"),
        Map.entry("brown", "#835432"),
        Map.entry("green", "#5E7C16"),
        Map.entry("red", "#B02E26"),
        Map.entry("black", "#1D1D21")
    );

    /**
     * State for each player's open GUI.
     * @param groupIndex current group index
     * @param page current page
     * @param groupIds list of group ids
     */
    private record GuiState(int groupIndex, int page, List<String> groupIds) {
        GuiState withGroupIndex(int newIndex) { return new GuiState(newIndex, 0, groupIds); }
        GuiState withPage(int newPage) { return new GuiState(groupIndex, newPage, groupIds); }
    }

    /**
     * Opens the marker selection GUI for a player.
     * @param player the player
     */
    public static void openFor(Player player) {
        List<MarkerData> markers = getBlueMapMarkers();
        List<String> groupIds = markers.stream().map(MarkerData::groupId).distinct().collect(Collectors.toList());
        if (groupIds.isEmpty()) groupIds = List.of("banner-markers");
        int groupIndex = 0;
        if (groupIds.contains("banner-markers")) groupIndex = groupIds.indexOf("banner-markers");
        int page = 0;
        var pdc = player.getPersistentDataContainer();
        if (pdc.has(MARKER_ID_KEY, PersistentDataType.STRING)) {
            String[] state = pdc.get(MARKER_ID_KEY, PersistentDataType.STRING).split(":");
            if (state.length == 2) {
                try {
                    groupIndex = Integer.parseInt(state[0]);
                    page = Integer.parseInt(state[1]);
                    if (groupIndex < 0 || groupIndex >= groupIds.size()) groupIndex = 0;
                    if (page < 0) page = 0;
                } catch (NumberFormatException ignored) {}
            }
        }
        GuiState guiState = new GuiState(groupIndex, page, groupIds);
        savePlayerGuiState(player, guiState);
        showPage(player, guiState);
    }

    private static void savePlayerGuiState(Player player, GuiState state) {
        var pdc = player.getPersistentDataContainer();
        pdc.set(MARKER_ID_KEY, PersistentDataType.STRING, state.groupIndex() + ":" + state.page());
    }

    private static GuiState getPlayerGuiState(Player player, List<String> groupIds) {
        int groupIndex = 0;
        int page = 0;
        var pdc = player.getPersistentDataContainer();
        if (pdc.has(MARKER_ID_KEY, PersistentDataType.STRING)) {
            String[] state = pdc.get(MARKER_ID_KEY, PersistentDataType.STRING).split(":");
            if (state.length == 2) {
                try {
                    groupIndex = Integer.parseInt(state[0]);
                    page = Integer.parseInt(state[1]);
                    if (groupIndex < 0 || groupIndex >= groupIds.size()) groupIndex = 0;
                    if (page < 0) page = 0;
                } catch (NumberFormatException ignored) {}
            }
        }
        return new GuiState(groupIndex, page, groupIds);
    }

    private static void showPage(Player player, GuiState state) {
        List<MarkerData> allMarkers = getBlueMapMarkers();
        String groupId = state.groupIds().get(state.groupIndex());
        List<MarkerData> groupMarkers = allMarkers.stream().filter(m -> m.groupId().equals(groupId)).toList();
        int start = state.page() * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, groupMarkers.size());
        String titleRaw = GUI_TITLE_RAW + " <gray>- <yellow>" + groupId;
        Inventory gui = Bukkit.createInventory(null, INVENTORY_SIZE, MM.deserialize(titleRaw));
        if (groupMarkers.isEmpty()) {
            gui.setItem(22, createButton(Material.BARRIER, MM.deserialize("<red>No markers in this category.")));
        } else {
            for (int i = start; i < end; i++) {
                MarkerData marker = groupMarkers.get(i);
                gui.setItem(i - start, createMarkerItem(marker, player));
            }
        }
        if (state.groupIds().size() > 1) {
            gui.setItem(SLOT_CATEGORY, createCategoryButton(state));
        }
        if (state.page() > 0) {
            gui.setItem(SLOT_PREV, createButton(Material.ARROW, MM.deserialize("<green>Previous Page")));
        }
        if (end < groupMarkers.size()) {
            gui.setItem(SLOT_NEXT, createButton(Material.ARROW, MM.deserialize("<green>Next Page")));
        }
        gui.setItem(SLOT_UNTRACK, createButton(Material.BARRIER, MM.deserialize("<red>Untrack All")));
        gui.setItem(SLOT_CLOSE, createButton(Material.BARRIER, MM.deserialize("<red>Close")));
        player.openInventory(gui);
    }

    private static ItemStack createMarkerItem(MarkerData marker, Player player) {
        Material icon = getMarkerIcon(marker.type(), marker);
        Set<String> tracked = WaypointManager.getTrackedMarkers(player);
        boolean isTracked = tracked.contains(marker.id());
        Component displayName;
        if (marker.groupId().equalsIgnoreCase("banner-markers")) {
            String colorName = marker.color() != null ? marker.color().toLowerCase() : "white";
            String hex = BANNER_TO_HEX.getOrDefault(colorName, "#FFFFFF");
            String colorTag = colorName.length() > 0 ? "<color:" + hex + ">" : "";
            displayName = MM.deserialize(colorTag + "<b>" + marker.name() + "</b>");
        } else {
            displayName = MM.deserialize("<gradient:#00c3ff:#ffff1c><b>" + marker.name() + "</b></gradient>");
        }
        List<Component> lore = new ArrayList<>();
        String worldName = marker.world();
        if (worldName.contains("#")) worldName = worldName.substring(0, worldName.indexOf('#'));
        boolean sameWorld = player.getWorld().getName().equals(worldName);
        double distance = -1;
        if (sameWorld) {
            distance = player.getLocation().distance(new org.bukkit.Location(player.getWorld(), marker.x() + 0.5, marker.y(), marker.z() + 0.5));
        }
        String locLine = "Location: " + marker.x() + ", " + marker.y() + ", " + marker.z() + " - " + worldName;
        if (sameWorld) {
            locLine += " (" + Math.round(distance) + "m)";
        }
        lore.add(MM.deserialize("<gray>" + locLine + "</gray>"));
        String owner = marker.placedBy() != null && !marker.placedBy().isEmpty() ? marker.placedBy() : "Unknown";
        lore.add(MM.deserialize("<gray>Owner: <white>" + owner + "</white>"));
        String status = isTracked ? "<green>Waypoint: Show" : "<gray>Status: Hide";
        lore.add(MM.deserialize(status));
        ItemStack item = createButton(icon, displayName, lore);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(MARKER_ID_KEY, PersistentDataType.STRING, marker.id());
            item.setItemMeta(meta);
        }
        return item;
    }

    private static Material getMarkerIcon(String type, MarkerData marker) {
        if (marker.groupId().equalsIgnoreCase("banner-markers")) {
            String colorName = marker.color() != null ? marker.color().toUpperCase() : "WHITE";
            try {
                return Material.valueOf(colorName + "_BANNER");
            } catch (IllegalArgumentException e) {
                return Material.WHITE_BANNER;
            }
        }
        return switch (type.toLowerCase()) {
            default -> Material.NAME_TAG;
        };
    }

    private static ItemStack createButton(Material material, Component name) {
        return createButton(material, name, List.of());
    }

    private static ItemStack createButton(Material material, Component name, List<Component> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(name);
            if (!lore.isEmpty()) meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack createCategoryButton(GuiState state) {
        List<Component> lore = new ArrayList<>();
        for (int i = 0; i < state.groupIds().size(); i++) {
            String name = state.groupIds().get(i);
            if (i == state.groupIndex()) {
                lore.add(MM.deserialize("<green>> " + name));
            } else {
                lore.add(MM.deserialize("<gray>" + name));
            }
        }
        lore.add(Component.empty());
        lore.add(MM.deserialize("<gray>Left: Next, Right: Previous"));
        return createButton(CATEGORY_ICON, MM.deserialize("<aqua>Category: <yellow>" + state.groupIds().get(state.groupIndex())), lore);
    }

    private static List<MarkerData> getBlueMapMarkers() {
        return BlueMapIntegration.getMarkers();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!event.getView().getTitle().contains("BlueMap Markers")) return;
        event.setCancelled(true);
        player.updateInventory();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        ItemMeta meta = clicked.getItemMeta();
        Component displayName = meta != null ? meta.displayName() : null;
        List<MarkerData> markers = getBlueMapMarkers();
        List<String> groupIds = markers.stream().map(MarkerData::groupId).distinct().collect(Collectors.toList());
        if (groupIds.isEmpty()) groupIds = List.of("banner-markers");
        GuiState state = getPlayerGuiState(player, groupIds);
        if (clicked.getType() == Material.BARRIER) {
            if (displayName != null && PlainTextComponentSerializer.plainText().serialize(displayName).equalsIgnoreCase("Untrack All")) {
                WaypointManager.removeAllWaypoints(player);
                player.sendMessage(MM.deserialize("<gray>All waypoints untracked."));
                player.closeInventory();
                return;
            }
            player.closeInventory();
            return;
        }
        if (clicked.getType() == CATEGORY_ICON && displayName != null) {
            String plain = PlainTextComponentSerializer.plainText().serialize(displayName);
            if (plain.startsWith("Category:")) {
                boolean rightClick = event.isRightClick();
                boolean leftClick = event.isLeftClick();
                int groupCount = state.groupIds().size();
                if (groupCount > 1) {
                    int newIndex = state.groupIndex();
                    if (leftClick) newIndex = (newIndex + 1) % groupCount;
                    else if (rightClick) newIndex = (newIndex - 1 + groupCount) % groupCount;
                    GuiState newState = state.withGroupIndex(newIndex);
                    savePlayerGuiState(player, newState);
                    showPage(player, newState);
                }
                return;
            }
        }
        if (clicked.getType() == Material.ARROW && displayName != null && displayName.equals(MM.deserialize("<green>Previous Page"))) {
            GuiState newState = state.withPage(Math.max(0, state.page() - 1));
            savePlayerGuiState(player, newState);
            showPage(player, newState);
            return;
        }
        if (clicked.getType() == Material.ARROW && displayName != null && displayName.equals(MM.deserialize("<green>Next Page"))) {
            GuiState newState = state.withPage(state.page() + 1);
            savePlayerGuiState(player, newState);
            showPage(player, newState);
            return;
        }
        if (meta != null) {
            String markerId = meta.getPersistentDataContainer().get(MARKER_ID_KEY, PersistentDataType.STRING);
            if (markerId != null) {
                List<MarkerData> allMarkers = getBlueMapMarkers();
                MarkerData marker = allMarkers.stream().filter(m -> m.id().equals(markerId)).findFirst().orElse(null);
                if (marker == null) {
                    if (fun.mntale.blueMapCompass.BlueMapCompass.debug) {
                        Bukkit.getLogger().warning("[BlueMapCompass] Marker not found for ID: " + markerId + " (Player: " + player.getName() + ")");
                    }
                    player.sendMessage(MM.deserialize("<red>Marker not found!"));
                    return;
                }
                Set<String> tracked = WaypointManager.getTrackedMarkers(player);
                if (tracked.contains(markerId)) {
                    WaypointManager.removeWaypoint(player, markerId);
                    player.sendMessage(MM.deserialize("<gray>Waypoint untracked."));
                } else {
                    WaypointManager.addTrackedWaypoint(player, marker, fun.mntale.blueMapCompass.BlueMapCompass.instance);
                    player.sendMessage(MM.deserialize("<green>Waypoint tracked!"));
                }
                openFor(player);
                return;
            }
        }
    }
} 