package fr.ChadOW.omegacore.shop;

import fr.ChadOW.api.accounts.UserAccount;
import fr.ChadOW.cinventory.CContent.CInventory;
import fr.ChadOW.cinventory.CContent.CItem;
import fr.ChadOW.cinventory.ItemCreator;
import fr.ChadOW.cinventory.events.clickContent.Action;
import fr.ChadOW.cinventory.events.clickContent.ClickType;
import fr.ChadOW.omegacore.P;
import fr.ChadOW.omegacore.utils.hologram.Hologram;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

public class Shop {

    private final Location location;
    private ItemStack item;
    private int buyPrice;
    private int sellPrice;
    private int amount;
    private final UUID owner;
    private Hologram hologram;
    private Item shopDisplayItem;
    private Horse horse;
    private final CInventory background;

    public Shop(Location location, ItemStack item, int buyPrice, int sellPrice, int amount, UUID owner) {
        this.location = location;
        this.item = item;
        this.buyPrice = buyPrice;
        this.sellPrice = sellPrice;
        this.amount = amount;
        this.owner = owner;
        background = new CInventory(6*9, "Shop");

        int[] orangePanesIndex = {1,2,3,5,6,7,9,17,36,44,46,47,48,50,51,52};
        int[] whitePanesIndex = {0,4,8,18,26,27,35,45,49,53};
        for (int i: orangePanesIndex){
            background.addElement(new CItem(new ItemCreator(Material.ORANGE_STAINED_GLASS_PANE, 0)
                    .setName("§f")).setSlot(i));
        }
        for (int i: whitePanesIndex){
            background.addElement(new CItem(new ItemCreator(Material.WHITE_STAINED_GLASS_PANE, 0)
                    .setName("§f")).setSlot(i));
        }
    }

    public void updateShop(){
        if (shopDisplayItem != null) shopDisplayItem.remove();
        spawnDisplayItem();
        spawnHorse();
        updateHologram();
    }

    private void spawnDisplayItem() {
        ItemStack i = this.item.clone();
        shopDisplayItem = Objects.requireNonNull(location.getWorld()).dropItem(location, i);
        shopDisplayItem.setInvulnerable(true);
        shopDisplayItem.setVelocity(new Vector(0, 0, 0));
        shopDisplayItem.setGravity(false);
        shopDisplayItem.setPickupDelay(Integer.MAX_VALUE);
        shopDisplayItem.setOwner(owner);
        shopDisplayItem.setCustomName("Shop display item");
        shopDisplayItem.setCustomNameVisible(false);
        shopDisplayItem.setPersistent(true);
    }

    private void spawnHorse() {
        horse = (Horse) Objects.requireNonNull(location.getWorld()).spawnEntity(location, EntityType.HORSE);
        horse.setInvisible(true);
        horse.setInvulnerable(true);
        horse.setCustomName("Horse Shop");
        horse.setCustomNameVisible(false);
        horse.setAI(false);
        horse.setAdult();
        horse.setSilent(true);
    }

    private void updateHologram() {
        if (hologram != null) hologram.setLineAtIndex(String.format("Stock: %s", amount),0)
                .setLineAtIndex(String.format(ChatColor.YELLOW + "Achat: %s", buyPrice),1)
                .setLineAtIndex(String.format(ChatColor.GREEN + "Vente: %s", sellPrice),2);
        else hologram = P.getInstance().getHologramManager().createHologram("Shop", location,
                Arrays.asList(String.format("Stock: %s", amount),
                        String.format(ChatColor.YELLOW + "Achat: %s", buyPrice),
                        String.format(ChatColor.GREEN + "Vente: %s", sellPrice)));
        if (buyPrice == 0) hologram.removeLine(1);
        if (sellPrice == 0) hologram.removeLine(2);
    }

    public void openShop (Player player){
        CInventory copy = getShopInventory();
        if (player.getUniqueId().equals(owner)){
            copy.addElement(new CItem(new ItemCreator(Material.REPEATER,0)
                    .setName("Ouvrir le menu d'édition de shop")).setSlot(53)
                    .addEvent((inventoryRepresentation, itemRepresentation, p, clickContext) -> openEditorShop(p)));
        }
        copy.addElement(new CItem(new ItemCreator(item)).setSlot(12));
        copy.addElement(new CItem(new ItemCreator(Material.CHEST,0).setAmount(amount)
                .addLore(String.format("Quantité disponible: %s%s",ChatColor.YELLOW,amount))).setSlot(12));
        copy.addElement(new CItem(new ItemCreator(Material.PLAYER_HEAD,0)
                .setOwner(Objects.requireNonNull(Bukkit.getPlayer(owner)).getDisplayName())).setSlot(40));
        copy.open(player);
    }

    private void sellItem(int i, Player player) {
        if (player.getInventory().containsAtLeast(item,i)){
            for (int index = 0; index < i; index++) {
                player.getInventory().removeItem(item);
            }
            amount += i;
            //pay the player and withdraw the owner
            UserAccount sellerAccount = UserAccount.getAccount(player.getUniqueId());
            sellerAccount.getBankAccount().addAmount(sellPrice);
            UserAccount shopperAccount = UserAccount.getAccount(owner);
            shopperAccount.getBankAccount().removeAmount(sellPrice);
        }
        openShop(player);
    }

    private void buyItem(int quantity, Player player){
        if (quantity > amount) return;
        int available = 0;
        for (ItemStack itemStack : player.getInventory().getStorageContents()){
            if (itemStack.getType().equals(Material.AIR)) available += item.getMaxStackSize();
            else if (itemStack.isSimilar(item)) available += item.getMaxStackSize() - itemStack.getAmount();
        }
        if (available >= quantity){
            for (int i = 0; i < quantity; i++) {
                player.getInventory().addItem(item);
            }
            amount -= quantity;
            //pay the owner and withdraw the player
            UserAccount buyerAccount = UserAccount.getAccount(player.getUniqueId());
            buyerAccount.getBankAccount().removeAmount(buyPrice);
            UserAccount shopperAccount = UserAccount.getAccount(owner);
            shopperAccount.getBankAccount().addAmount(buyPrice);
        }
        openShop(player);
    }

    private void openEditorShop(Player player){
        CInventory copy = background;

        copy.addElement(new CItem(new ItemCreator(Material.WHITE_BANNER,0)
                .setName(ChatColor.YELLOW +"Diminuer le prix d'achat de 100$")).setSlot(19)
                .addEvent((inventoryRepresentation, itemRepresentation, p, clickContext) -> {
                    if (buyPrice - 100 >= 0) buyPrice -= 100;
                    updateHologram();
                }));
        copy.addElement(new CItem(new ItemCreator(Material.WHITE_BANNER,0)
                .setName(ChatColor.YELLOW +"Diminuer le prix d'achat de 10$")).setSlot(20)
                .addEvent((inventoryRepresentation, itemRepresentation, p, clickContext) -> {
                    if (buyPrice - 10 >= 0) buyPrice -= 10;
                    updateHologram();
                }));
        copy.addElement(new CItem(new ItemCreator(Material.WHITE_BANNER,0)
                .setName(ChatColor.YELLOW +"Diminuer le prix d'achat de 1$")).setSlot(21)
                .addEvent((inventoryRepresentation, itemRepresentation, p, clickContext) -> {
                    if (buyPrice - 1 >= 0) buyPrice -= 1;
                    updateHologram();
                }));
        copy.addElement(new CItem(new ItemCreator(Material.GOLD_NUGGET,0)
                        .setName(String.format("Prix d'achat actuel: %s$",buyPrice))).setSlot(22));
        copy.addElement(new CItem(new ItemCreator(Material.WHITE_BANNER,0)
                .setName(ChatColor.YELLOW +"Augmenter le prix d'achat de 1$")).setSlot(23)
                .addEvent((inventoryRepresentation, itemRepresentation, p, clickContext) -> {
                    buyPrice += 1;
                    updateHologram();
                }));
        copy.addElement(new CItem(new ItemCreator(Material.WHITE_BANNER,0)
                .setName(ChatColor.YELLOW +"Augmenter le prix d'achat de 10$")).setSlot(24)
                .addEvent((inventoryRepresentation, itemRepresentation, p, clickContext) -> {
                    buyPrice += 10;
                    updateHologram();
                }));
        copy.addElement(new CItem(new ItemCreator(Material.WHITE_BANNER,0)
                .setName(ChatColor.YELLOW +"Augmenter le prix d'achat de 100$")).setSlot(25)
                .addEvent((inventoryRepresentation, itemRepresentation, p, clickContext) -> {
                    buyPrice += 100;
                    updateHologram();
                }));

        copy.addElement(new CItem(new ItemCreator(Material.WHITE_BANNER,0)
                .setName(ChatColor.GREEN + "Diminuer le prix de vente de 100$")).setSlot(28)
                .addEvent((inventoryRepresentation, itemRepresentation, p, clickContext) -> {
                    if (sellPrice - 100 >= 0) sellPrice -= 100;
                    updateHologram();
                }));
        copy.addElement(new CItem(new ItemCreator(Material.WHITE_BANNER,0)
                .setName(ChatColor.GREEN + "Diminuer le prix de vente de 10$")).setSlot(29)
                .addEvent((inventoryRepresentation, itemRepresentation, p, clickContext) -> {
                    if (sellPrice - 10 >= 0) sellPrice -= 10;
                    updateHologram();
                }));
        copy.addElement(new CItem(new ItemCreator(Material.WHITE_BANNER,0)
                .setName(ChatColor.GREEN + "Diminuer le prix de vente de 1$")).setSlot(30)
                .addEvent((inventoryRepresentation, itemRepresentation, p, clickContext) -> {
                    if (sellPrice - 1 >= 0) sellPrice -= 1;
                    updateHologram();
                }));
        copy.addElement(new CItem(new ItemCreator(Material.IRON_NUGGET,0)
                        .setName(String.format(ChatColor.GREEN + "Prix de vente actuel: %s$",sellPrice))).setSlot(31));
        copy.addElement(new CItem(new ItemCreator(Material.WHITE_BANNER,0)
                .setName(ChatColor.GREEN + "Augmenter le prix de vente de 1$")).setSlot(32)
                .addEvent((inventoryRepresentation, itemRepresentation, p, clickContext) -> {
                    sellPrice += 1;
                    updateHologram();
                }));
        copy.addElement(new CItem(new ItemCreator(Material.WHITE_BANNER,0)
                .setName(ChatColor.GREEN + "Augmenter le prix de vente de 10$")).setSlot(33)
                .addEvent((inventoryRepresentation, itemRepresentation, p, clickContext) -> {
                    sellPrice += 10;
                    updateHologram();
                }));
        copy.addElement(new CItem(new ItemCreator(Material.WHITE_BANNER,0)
                .setName(ChatColor.GREEN + "Augmenter le prix de vente de 100$")).setSlot(34)
                .addEvent((inventoryRepresentation, itemRepresentation, p, clickContext) -> {
                    sellPrice += 100;
                    updateHologram();
                }));

        copy.addElement(new CItem(new ItemCreator(item)).setSlot(13)
                .addEvent((inventoryRepresentation, itemRepresentation, p, clickContext) -> {
                    if (clickContext.getInventoryAction().equals(Action.SWAP_WITH_CURSOR))
                        if (amount == 0) {
                            item = new ItemCreator(p.getItemOnCursor()).setAmount(1).getItem();
                        }
                }));

        copy.addElement(new CItem(new ItemCreator(Material.CHEST,0).setName(String.format("Quantité: %s",amount))
                .setAmount(amount)).setSlot(40)
                .addEvent((inventoryRepresentation, itemRepresentation, p, clickContext) -> {
                    if (clickContext.getInventoryAction().equals(Action.SWAP_WITH_CURSOR))
                        if (p.getItemOnCursor().isSimilar(item)){
                            amount += p.getItemOnCursor().getAmount();
                            p.getInventory().remove(p.getItemOnCursor());
                        }
                    else if (clickContext.getClickType().equals(ClickType.LEFT)){
                        if (p.getInventory().containsAtLeast(item,64)){
                            p.getInventory().remove(new ItemCreator(item).setAmount(64).getItem());
                            amount += 64;
                        }
                    }
                    else if (clickContext.getClickType().equals(ClickType.SHIFT_LEFT)){
                        Arrays.stream(p.getInventory().getStorageContents())
                                .filter(itemStack -> itemStack.isSimilar(item))
                                .forEach(itemStack -> amount += itemStack.getAmount());
                        p.getInventory().remove(item);
                    }
                    else if (clickContext.getClickType().equals(ClickType.SHIFT_RIGHT)){
                        Arrays.stream(p.getInventory().getStorageContents())
                                .filter(itemStack -> itemStack.isSimilar(item) || itemStack.getType().equals(Material.AIR)).forEach(itemStack -> {
                            if (itemStack.getType().equals(Material.AIR)) {
                                if (amount - item.getMaxStackSize() >= 0) {
                                    amount -= item.getMaxStackSize();
                                    p.getInventory().addItem(new ItemCreator(item).setAmount(item.getMaxStackSize()).getItem());
                                }
                                else {
                                    amount -= amount;
                                    p.getInventory().addItem(new ItemCreator(item).setAmount(amount).getItem());
                                }
                            }
                            else {
                                if (amount - item.getMaxStackSize()+1 - itemStack.getAmount() >= 0) {
                                    amount -= item.getMaxStackSize()+1 - itemStack.getAmount();
                                    p.getInventory().addItem(new ItemCreator(item).setAmount(item.getMaxStackSize()+1 - itemStack.getAmount()).getItem());
                                }
                            }
                        });
                    }
                }));
        copy.open(player);
    }

    public Location getLocation() {
        return location;
    }

    public Horse getHorse() {
        return horse;
    }

    public CInventory getShopInventory() {
        CInventory copy = background;
        if (buyPrice != 0){
            copy.addElement(new CItem(new ItemCreator(Material.GOLD_NUGGET,0)
                    .setName(ChatColor.YELLOW + "Acheter x1")).setSlot(20)
                    .addEvent((inventoryRepresentation, itemRepresentation, p, clickContext) -> buyItem(1, p)));
            copy.addElement(new CItem(new ItemCreator(Material.GOLD_INGOT,0)
                    .setName(ChatColor.YELLOW + "Acheter x16")).setSlot(22)
                    .addEvent((inventoryRepresentation, itemRepresentation, p, clickContext) -> buyItem(-16, p)));
            copy.addElement(new CItem(new ItemCreator(Material.GOLD_BLOCK,0)
                    .setName(ChatColor.YELLOW + "Acheter x64")).setSlot(24)
                    .addEvent((inventoryRepresentation, itemRepresentation, p, clickContext) -> buyItem(-64, p)));
        }
        if (sellPrice != 0){
            copy.addElement(new CItem(new ItemCreator(Material.IRON_NUGGET,0)
                    .setName(ChatColor.GREEN + "Vendre x1")).setSlot(29)
                    .addEvent((inventoryRepresentation, itemRepresentation, p, clickContext) -> sellItem(1, p)));
            copy.addElement(new CItem(new ItemCreator(Material.IRON_INGOT,0)
                    .setName(ChatColor.GREEN + "Vendre x16")).setSlot(31)
                    .addEvent((inventoryRepresentation, itemRepresentation, p, clickContext) -> sellItem(16, p)));
            copy.addElement(new CItem(new ItemCreator(Material.IRON_BLOCK,0)
                    .setName(ChatColor.GREEN + "Vendre x64")).setSlot(33)
                    .addEvent((inventoryRepresentation, itemRepresentation, p, clickContext) -> sellItem(64, p)));
        }

        return copy;
    }

    public Item getShopDisplayItem() {
        return shopDisplayItem;
    }

    public ItemStack getItem() {
        return item;
    }

    public int getBuyPrice() {
        return buyPrice;
    }

    public int getSellPrice() {
        return sellPrice;
    }

    public int getAmount() {
        return amount;
    }

    public UUID getOwner() {
        return owner;
    }

}

