package com.lota.LotaGuide.screen;

import com.lota.LotaGuide.LotaGuide;
import com.lota.LotaGuide.client.ImageCache;
import com.lota.LotaGuide.data.ImageBookData;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.io.File;

/**
 * Screen for editing an Image Book.
 */
@OnlyIn(Dist.CLIENT)
public class ImageBookEditScreen extends Screen {
    private final Player player;
    private final ItemStack bookStack;
    private final InteractionHand hand;
    private final ImageBookData bookData;
    private ImageBookLayout layout;
    
    private int currentPage = 0;
    private EditBox urlField;
    private MultiLineEditBox textField;
    private Button browseButton;
    private Button imageToggleButton;
    private Button prevPageButton;
    private Button nextPageButton;
    private Button addPageButton;
    private Button removePageButton;
    private Button signButton;
    private Button cancelButton;
    
    public ImageBookEditScreen(Player player, ItemStack bookStack, InteractionHand hand, ImageBookData bookData) {
        super(Component.empty());
        this.player = player;
        this.bookStack = bookStack;
        this.hand = hand;
        this.bookData = bookData;
    }
    
    @Override
    protected void init() {
        super.init();

        this.layout = ImageBookLayout.forEdit(this.width, this.height, isCurrentPageImageVisible());

        this.urlField = new TransparentEditBox(this.font, layout.urlFieldX, layout.urlFieldY, layout.urlFieldWidth,
            20, Component.empty());
        this.urlField.setMaxLength(500);
        this.urlField.setHint(Component.translatable("screen.lotaguide.image_book.source_hint"));
        this.urlField.setResponder(this::onUrlChanged);
        this.addRenderableWidget(this.urlField);

        this.browseButton = Button.builder(Component.translatable("screen.lotaguide.image_book.browse"),
            button -> openLocalImagePicker())
            .bounds(layout.browseButtonX, layout.browseButtonY, ImageBookLayout.browseButtonWidth(), 20)
            .build();
        this.addRenderableWidget(this.browseButton);

        this.imageToggleButton = Button.builder(Component.translatable("screen.lotaguide.image_book.hide_image"),
            button -> toggleImageVisibility())
            .bounds(layout.imageToggleButtonX, layout.imageToggleButtonY, ImageBookLayout.imageToggleButtonWidth(), 20)
            .build();
        this.addRenderableWidget(this.imageToggleButton);

        this.addPageButton = Button.builder(Component.literal("+"), button -> addPage())
            .bounds(layout.addPageButtonX, layout.topRowY, ImageBookLayout.smallButtonWidth(), 20)
            .build();
        this.addRenderableWidget(this.addPageButton);

        this.removePageButton = Button.builder(Component.literal("-"), button -> removePage())
            .bounds(layout.removePageButtonX, layout.topRowY, ImageBookLayout.smallButtonWidth(), 20)
            .build();
        this.addRenderableWidget(this.removePageButton);

        this.textField = new TransparentMultiLineEditBox(this.font, layout.textX, layout.textY, layout.textWidth,
            layout.textHeight, Component.empty(), Component.empty());
        this.textField.setCharacterLimit(1000);
        this.textField.setValueListener(this::onTextChanged);
        this.addRenderableWidget(this.textField);

        int bottomRowY = layout.bottomRowY;
        this.prevPageButton = Button.builder(Component.literal("<"), button -> previousPage())
            .bounds(layout.prevButtonX, bottomRowY, ImageBookLayout.navButtonWidth(), 20)
            .build();
        this.addRenderableWidget(this.prevPageButton);

        this.signButton = Button.builder(Component.translatable("screen.lotaguide.image_book.sign"),
            button -> signBook())
            .bounds(layout.centerButtonX, bottomRowY, ImageBookLayout.actionButtonWidth(), 20)
            .build();
        this.addRenderableWidget(this.signButton);

        this.nextPageButton = Button.builder(Component.literal(">"), button -> nextPage())
            .bounds(layout.nextButtonX, bottomRowY, ImageBookLayout.navButtonWidth(), 20)
            .build();
        this.addRenderableWidget(this.nextPageButton);

        loadPageData();
        updateNavigationButtons();
        updateLayout();
    }
    
    private void loadPageData() {
        ImageBookData.Page page = bookData.getPage(currentPage);
        if (page != null) {
            this.urlField.setValue(page.getImageUrl());
            this.textField.setValue(page.getText());
        }
    }

    private boolean isCurrentPageImageVisible() {
        ImageBookData.Page page = bookData.getPage(currentPage);
        return page == null || page.isImageVisible();
    }

    private void updateLayout() {
        boolean imageVisible = isCurrentPageImageVisible();
        this.layout = ImageBookLayout.forEdit(this.width, this.height, imageVisible);

        this.urlField.setX(layout.urlFieldX);
        this.urlField.setY(layout.urlFieldY);
        this.urlField.setWidth(layout.urlFieldWidth);
        this.urlField.setHeight(20);

        this.browseButton.setX(layout.browseButtonX);
        this.browseButton.setY(layout.browseButtonY);

        this.imageToggleButton.setX(layout.imageToggleButtonX);
        this.imageToggleButton.setY(layout.imageToggleButtonY);
        this.imageToggleButton.setWidth(ImageBookLayout.imageToggleButtonWidth());
        this.imageToggleButton.setMessage(Component.translatable(imageVisible
            ? "screen.lotaguide.image_book.hide_image"
            : "screen.lotaguide.image_book.show_image"));

        this.addPageButton.setX(layout.addPageButtonX);
        this.addPageButton.setY(layout.topRowY);

        this.removePageButton.setX(layout.removePageButtonX);
        this.removePageButton.setY(layout.topRowY);

        this.textField.setX(layout.textX);
        this.textField.setY(layout.textY);
        this.textField.setWidth(layout.textWidth);
        this.textField.setHeight(layout.textHeight);

        this.prevPageButton.setX(layout.prevButtonX);
        this.prevPageButton.setY(layout.bottomRowY);

        this.signButton.setX(layout.centerButtonX);
        this.signButton.setY(layout.bottomRowY);

        this.nextPageButton.setX(layout.nextButtonX);
        this.nextPageButton.setY(layout.bottomRowY);
    }
    
    private void saveCurrentPage() {
        ImageBookData.Page page = bookData.getPage(currentPage);
        if (page != null) {
            page.setImageUrl(this.urlField.getValue());
            page.setText(this.textField.getValue());
        }
    }
    
    private void updateNavigationButtons() {
        this.prevPageButton.active = currentPage > 0;
        this.nextPageButton.active = currentPage < bookData.getPageCount() - 1;
        this.removePageButton.active = bookData.getPageCount() > 1;
    }
    
    private void onUrlChanged(String url) {
        ImageBookData.Page page = bookData.getPage(currentPage);
        if (page != null) {
            page.setImageUrl(url);
        }
    }

    private void openLocalImagePicker() {
        String currentValue = this.urlField != null ? this.urlField.getValue() : "";
        String defaultPath = "";
        if (currentValue != null && !currentValue.isEmpty()) {
            File selectedFile = new File(currentValue);
            if (selectedFile.isFile()) {
                defaultPath = selectedFile.getAbsolutePath();
            }
        }

        String selectedPath = TinyFileDialogs.tinyfd_openFileDialog(
            "Select a local image",
            defaultPath,
            null,
            "Image files",
            false
        );

        if (selectedPath != null && !selectedPath.isEmpty()) {
            String normalizedPath = new File(selectedPath).getAbsolutePath();
            Minecraft.getInstance().execute(() -> this.urlField.setValue(normalizedPath));
        }
    }

    private void onTextChanged(String text) {
        ImageBookData.Page page = bookData.getPage(currentPage);
        if (page != null) {
            page.setText(text);
        }
    }
    
    private void previousPage() {
        if (currentPage > 0) {
            saveCurrentPage();
            currentPage--;
            loadPageData();
            updateNavigationButtons();
            updateLayout();
        }
    }
    
    private void nextPage() {
        if (currentPage < bookData.getPageCount() - 1) {
            saveCurrentPage();
            currentPage++;
            loadPageData();
            updateNavigationButtons();
            updateLayout();
        }
    }
    
    private void addPage() {
        saveCurrentPage();
        bookData.addPage();
        currentPage = bookData.getPageCount() - 1;
        loadPageData();
        updateNavigationButtons();
        updateLayout();
    }
    
    private void removePage() {
        if (bookData.getPageCount() > 1) {
            bookData.removePage(currentPage);
            if (currentPage >= bookData.getPageCount()) {
                currentPage = bookData.getPageCount() - 1;
            }
            loadPageData();
            updateNavigationButtons();
            updateLayout();
        }
    }

    private void toggleImageVisibility() {
        ImageBookData.Page page = bookData.getPage(currentPage);
        if (page != null) {
            page.toggleImageVisible();
            updateLayout();
        }
    }
    
    private void signBook() {
        saveCurrentPage();
        
        bookData.setAuthorUUID(player.getUUID());
        bookData.setAuthorName(player.getName().getString());
        
        ItemStack signedBook = new ItemStack(LotaGuide.SIGNED_IMAGE_BOOK.get());
        bookData.saveToStack(signedBook);
        
        player.setItemInHand(hand, signedBook);
        
        this.onClose();
    }
    
    @Override
    public void onClose() {
        saveCurrentPage();
        bookData.saveToStack(bookStack);
        super.onClose();
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);

        ImageBookData.Page page = bookData.getPage(currentPage);
        boolean imageVisible = page == null || page.isImageVisible();

        String pageIndicator = (currentPage + 1) + " / " + bookData.getPageCount();
        graphics.drawCenteredString(this.font, pageIndicator,
            layout.contentLeft + layout.contentWidth / 2, layout.pageIndicatorY + 5, 0xFFFFFF);

        String imageUrl = this.urlField.getValue();
        boolean hasImage = false;

        if (imageVisible && !imageUrl.isEmpty()) {
            ImageCache.CachedImage cachedImage = ImageCache.getInstance().getImage(imageUrl);

            if (cachedImage != null && !cachedImage.isError()) {
                ResourceLocation texture = cachedImage.getTexture(System.currentTimeMillis());
                if (texture != null) {
                    hasImage = true;
                    RenderSystem.setShaderTexture(0, texture);

                    int imgWidth = cachedImage.getWidth();
                    int imgHeight = cachedImage.getHeight();

                    float scale = Math.min(
                        (float) layout.imageWidth / imgWidth,
                        (float) layout.imageHeight / imgHeight
                    );
                    imgWidth = Math.max(1, (int) (imgWidth * scale));
                    imgHeight = Math.max(1, (int) (imgHeight * scale));

                    int renderX = layout.imageX + (layout.imageWidth - imgWidth) / 2;
                    int renderY = layout.imageY + (layout.imageHeight - imgHeight) / 2;

                    graphics.blit(texture, renderX, renderY, 0, 0, imgWidth, imgHeight,
                        imgWidth, imgHeight);
                }
            }
        }

        if (imageVisible) {
            if (!hasImage) {
                graphics.fill(layout.imageX, layout.imageY,
                    layout.imageX + layout.imageWidth, layout.imageY + layout.imageHeight,
                    0xFF333333);

                if (!imageUrl.isEmpty()) {
                    ImageCache.CachedImage cachedImage = ImageCache.getInstance().getImage(imageUrl);
                    if (cachedImage != null && cachedImage.isError()) {
                        graphics.drawCenteredString(this.font, cachedImage.getErrorMessage(),
                            layout.contentLeft + layout.contentWidth / 2,
                            layout.imageY + layout.imageHeight / 2, 0xFF5555);
                    } else if (ImageCache.getInstance().isLoading(imageUrl)) {
                        long dots = (System.currentTimeMillis() / 500) % 4;
                        String loading = "Loading" + ".".repeat((int) dots);
                        graphics.drawCenteredString(this.font, loading,
                            layout.contentLeft + layout.contentWidth / 2,
                            layout.imageY + layout.imageHeight / 2, 0xAAAAAA);
                    }
                } else {
                    graphics.drawCenteredString(this.font,
                        Component.translatable("screen.lotaguide.image_book.no_image").getString(),
                        layout.contentLeft + layout.contentWidth / 2,
                        layout.imageY + layout.imageHeight / 2, 0x888888);
                }
            }
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
