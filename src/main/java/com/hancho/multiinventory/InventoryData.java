package com.hancho.multiinventory;

import cn.nukkit.item.Item;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class InventoryData {
    private LinkedHashMap<Integer, Item> inv, offHand;
    private Item[] armor;

    public InventoryData(LinkedHashMap<Integer, Item> inv, LinkedHashMap<Integer, Item> offHand, Item[] armor){
        this.inv = inv;
        this.armor = armor;
        this.offHand = offHand;
    }

    public LinkedHashMap<Integer, Item> getInv() {
        return inv;
    }

    public void setInv(LinkedHashMap<Integer, Item> inv) {
        this.inv = inv;
    }

    public LinkedHashMap<Integer, Item> getOffHand() {
        return offHand;
    }

    public void setOffHand(LinkedHashMap<Integer, Item> offHand) {
        this.offHand = offHand;
    }

    public Item[] getArmor() {
        return armor;
    }

    public void setArmor(Item[] armor) {
        this.armor = armor;
    }
}
