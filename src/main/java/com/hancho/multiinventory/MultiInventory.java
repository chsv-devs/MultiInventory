package com.hancho.multiinventory;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.event.entity.EntityLevelChangeEvent;
import cn.nukkit.inventory.PlayerInventory;
import cn.nukkit.item.Item;
import cn.nukkit.level.Level;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.Binary;
import cn.nukkit.utils.Config;
import cn.nukkit.utils.MainLogger;
import cn.nukkit.utils.Utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

public class MultiInventory extends PluginBase implements Listener {
    public HashSet<String> queue = new HashSet<>();
    public HashSet<String> enabledLevel;
    public String defInvPath;
    public String multiInvPath;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.defInvPath = this.getDataFolder().getAbsolutePath() + "/def/";
        this.multiInvPath = this.getDataFolder().getAbsolutePath() + "/multi/";
        this.enabledLevel = (HashSet<String>) this.getConfig().get("enabledLevel");

        this.getServer().getPluginManager().registerEvents(this, this);
    }

    public static String itemToString(Item item) {
        String hex = item.hasCompoundTag() ? Binary.bytesToHexString(item.getCompoundTag()) : "00";
        StringBuilder sb = new StringBuilder()
                .append(item.getId())
                .append("_")
                .append(item.getDamage())
                .append("_")
                .append(hex);
        if(!item.getCustomName().isEmpty()){
            sb.append("_").append(item.getCustomName());
        }
        return sb.toString();
    }

    public static Item StringToItem(String s){
        // id_meta_tag_customName
        String[] strings = s.split("_");
        Item item = Item.get(Integer.parseInt(strings[0]), Integer.parseInt(strings[1]));
        if(!strings[2].equals("00")){
            item.setCompoundTag(Utils.parseHexBinary(strings[2]));
        }
        if(strings.length > 3){
            item.setCustomName(strings[3]);
        }
        return item;
    }

    public InventoryData getMultiInv(Level level, String playerName){
        return this.getInv(this.multiInvPath + level.getName() + "/" + playerName + ".yml");
    }

    public InventoryData getDefInv(String playerName){
        return this.getInv(this.defInvPath + playerName + ".yml");
    }

    private InventoryData getInv(String path){
        Config data = new Config(path);
        LinkedHashMap<Integer, String> invData = data.get("inv", new LinkedHashMap<>());
        LinkedHashMap<Integer, String> armorData = data.get("armor", new LinkedHashMap<>());
        LinkedHashMap<Integer, String> offHandData = data.get("offHand", new LinkedHashMap<>());

        LinkedHashMap<Integer, Item> invItems = new LinkedHashMap<>();
        LinkedHashMap<Integer, Item> offHandItems = new LinkedHashMap<>();
        Item[] armorItems = new Item[4];

        invData.forEach((k, i) -> invItems.put(k, StringToItem(i)));
        offHandData.forEach((k, i) -> offHandItems.put(k, StringToItem(i)));
        for(int i = 0; i < 4; i++){
            String stringItem = armorData.get(i);
            if(stringItem == null){
                armorItems[i] = null;
            }else{
                armorItems[i] = StringToItem(stringItem);
            }
        }


        return new InventoryData(invItems, offHandItems, armorItems);
    }

    public void saveMultiInv(Level level, String playerName, PlayerInventory inv, Map<Integer, Item> offHand){
        this.saveInv(this.multiInvPath + level.getName() + "/" + playerName + ".yml", inv, offHand);
    }

    public void saveDefInv(String playerName, PlayerInventory inv, Map<Integer, Item> offHand){
        this.saveInv(this.defInvPath + playerName + ".yml", inv, offHand);
    }

    public void saveInv(String path, PlayerInventory inv, Map<Integer, Item> offHand){
        LinkedHashMap<Integer, String> invItem = new LinkedHashMap<>();
        LinkedHashMap<Integer, String> armorItem = new LinkedHashMap<>();
        LinkedHashMap<Integer, String> offHandItem = new LinkedHashMap<>();
        Config data = new Config(path);


        inv.getContents().forEach((k, i) -> {
            invItem.put(k, itemToString(i));
        });
        Item[] armorContents = inv.getArmorContents();
        for(int i = 0; i < 4; i++) {
            if(armorContents[i] == null) continue;
            armorItem.put(i,itemToString(armorContents[i]));
        }
        offHand.forEach((k, i) -> {
            offHandItem.put(k, itemToString(i));
        });

        data.set("inv", invItem);
        data.set("armor", armorItem);
        data.set("offHand", offHandItem);
        data.save();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onLevelChange(EntityLevelChangeEvent ev){
        if(!(ev.getEntity() instanceof Player)) return;
        Player player = (Player) ev.getEntity();
        Level from = ev.getOrigin();
        Level target = ev.getTarget();

        boolean toDef;
        if((toDef = this.enabledLevel.contains(from.getName())) || this.enabledLevel.contains(target.getName())){
            if(this.queue.contains(player.getName())){
                player.sendTitle("§c잠시만요!", "서버가 작업을 처리중입니다.", 20, 40, 20);
                ev.setCancelled();
                return;
            }

            this.queue.add(player.getName());
            player.sendTitle("처리중", "잠시만 기다려주세요", 1, 30, 1);

            PlayerInventory clonedInv = new PlayerInventory(player);
            PlayerInventory playerInv = player.getInventory();
            clonedInv.setContents(playerInv.getContents());
            clonedInv.setArmorContents(playerInv.getArmorContents());
            Map<Integer, Item> offHand = player.getInventory().getHolder().getOffhandInventory().getContents();
            playerInv.clearAll();
            Thread thread = new Thread(() -> {
                if(toDef) this.saveMultiInv(from, player.getName(), clonedInv, offHand);
                else this.saveDefInv(player.getName(), clonedInv, offHand);

                InventoryData invData;
                if(toDef) invData = getDefInv(player.getName());
                else invData = getMultiInv(target, player.getName());
                playerInv.setContents(invData.getInv());
                playerInv.setArmorContents(invData.getArmor());
                playerInv.getHolder().getOffhandInventory().setContents(invData.getOffHand());
                this.getServer().getLogger().info(playerInv.getHolder().getOffhandInventory().getSize() + "");
                queue.remove(player.getName());
                player.sendTitle("§o이동완료", "§d기존 인벤토리가 저장되었어요", 1, 15, 1);
            });
            thread.start();
            //ev.setCancelled(true);
        }
    }
}
