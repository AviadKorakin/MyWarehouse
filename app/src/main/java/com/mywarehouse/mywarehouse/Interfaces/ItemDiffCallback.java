package com.mywarehouse.mywarehouse.Interfaces;

import androidx.recyclerview.widget.DiffUtil;

import com.mywarehouse.mywarehouse.Models.Item;

import java.util.List;

public class ItemDiffCallback extends DiffUtil.Callback {

    private final List<Item> oldList;
    private final List<Item> newList;

    public ItemDiffCallback(List<Item> oldList, List<Item> newList) {
        this.oldList = oldList;
        this.newList = newList;
    }

    @Override
    public int getOldListSize() {
        return oldList.size();
    }

    @Override
    public int getNewListSize() {
        return newList.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        return oldList.get(oldItemPosition).getBarcode().equals(newList.get(newItemPosition).getBarcode());
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        return oldList.get(oldItemPosition).equals(newList.get(newItemPosition));
    }
}
