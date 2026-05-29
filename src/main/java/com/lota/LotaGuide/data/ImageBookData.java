package com.lota.LotaGuide.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Data structure for Image Book pages.
 * Handles serialization/deserialization to NBT.
 */
public class ImageBookData {
    public static final String TAG_AUTHOR = "author";
    public static final String TAG_AUTHOR_NAME = "authorName";
    public static final String TAG_PAGES = "pages";
    public static final String TAG_IMAGE_URL = "imageUrl";
    public static final String TAG_TEXT = "text";
    public static final String TAG_FONT_SIZE = "fontSize";
    
    public static final int DEFAULT_FONT_SIZE = 12;
    public static final int MIN_FONT_SIZE = 8;
    public static final int MAX_FONT_SIZE = 24;
    
    private UUID authorUUID;
    private String authorName;
    private final List<Page> pages;
    
    public ImageBookData() {
        this.pages = new ArrayList<>();
        this.pages.add(new Page());
    }
    
    public static class Page {
        private String imageUrl;
        private String text;
        private int fontSize;
        
        public Page() {
            this.imageUrl = "";
            this.text = "";
            this.fontSize = DEFAULT_FONT_SIZE;
        }
        
        public Page(String imageUrl, String text, int fontSize) {
            this.imageUrl = imageUrl != null ? imageUrl : "";
            this.text = text != null ? text : "";
            this.fontSize = Math.max(MIN_FONT_SIZE, Math.min(MAX_FONT_SIZE, fontSize));
        }
        
        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl != null ? imageUrl : ""; }
        
        public String getText() { return text; }
        public void setText(String text) { this.text = text != null ? text : ""; }
        
        public int getFontSize() { return fontSize; }
        public void setFontSize(int fontSize) { 
            this.fontSize = Math.max(MIN_FONT_SIZE, Math.min(MAX_FONT_SIZE, fontSize)); 
        }
        
        public void increaseFontSize() {
            setFontSize(fontSize + 2);
        }
        
        public void decreaseFontSize() {
            setFontSize(fontSize - 2);
        }
        
        public CompoundTag toNBT() {
            CompoundTag tag = new CompoundTag();
            tag.putString(TAG_IMAGE_URL, imageUrl);
            tag.putString(TAG_TEXT, text);
            tag.putInt(TAG_FONT_SIZE, fontSize);
            return tag;
        }
        
        public static Page fromNBT(CompoundTag tag) {
            return new Page(
                tag.getString(TAG_IMAGE_URL),
                tag.getString(TAG_TEXT),
                tag.getInt(TAG_FONT_SIZE)
            );
        }
    }
    
    public UUID getAuthorUUID() { return authorUUID; }
    public void setAuthorUUID(UUID uuid) { this.authorUUID = uuid; }
    
    public String getAuthorName() { return authorName; }
    public void setAuthorName(String name) { this.authorName = name; }
    
    public boolean isAuthor(UUID playerUUID) {
        return authorUUID != null && authorUUID.equals(playerUUID);
    }
    
    public List<Page> getPages() { return pages; }
    
    public Page getPage(int index) {
        if (index >= 0 && index < pages.size()) {
            return pages.get(index);
        }
        return null;
    }
    
    public int getPageCount() { return pages.size(); }
    
    public void addPage() {
        pages.add(new Page());
    }
    
    public void removePage(int index) {
        if (pages.size() > 1 && index >= 0 && index < pages.size()) {
            pages.remove(index);
        }
    }
    
    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        
        if (authorUUID != null) {
            tag.putUUID(TAG_AUTHOR, authorUUID);
        }
        if (authorName != null) {
            tag.putString(TAG_AUTHOR_NAME, authorName);
        }
        
        ListTag pagesTag = new ListTag();
        for (Page page : pages) {
            pagesTag.add(page.toNBT());
        }
        tag.put(TAG_PAGES, pagesTag);
        
        return tag;
    }
    
    public static ImageBookData fromNBT(CompoundTag tag) {
        ImageBookData data = new ImageBookData();
        data.pages.clear();
        
        if (tag.hasUUID(TAG_AUTHOR)) {
            data.authorUUID = tag.getUUID(TAG_AUTHOR);
        }
        if (tag.contains(TAG_AUTHOR_NAME)) {
            data.authorName = tag.getString(TAG_AUTHOR_NAME);
        }
        
        if (tag.contains(TAG_PAGES)) {
            ListTag pagesTag = tag.getList(TAG_PAGES, Tag.TAG_COMPOUND);
            for (int i = 0; i < pagesTag.size(); i++) {
                data.pages.add(Page.fromNBT(pagesTag.getCompound(i)));
            }
        }
        
        if (data.pages.isEmpty()) {
            data.pages.add(new Page());
        }
        
        return data;
    }
    
    public void saveToStack(ItemStack stack) {
        stack.getOrCreateTag().put("ImageBookData", toNBT());
    }
    
    public static ImageBookData loadFromStack(ItemStack stack) {
        if (stack.hasTag() && stack.getTag().contains("ImageBookData")) {
            return fromNBT(stack.getTag().getCompound("ImageBookData"));
        }
        return new ImageBookData();
    }
}
