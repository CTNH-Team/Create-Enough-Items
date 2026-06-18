package com.moguang.cei.mixin.emi;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import com.moguang.cei.utils.emi.collapsible.CEICollapsibleGroups;
import com.moguang.cei.utils.emi.duplicate.CEIDuplicateRecipeScreen;
import com.moguang.cei.utils.emi.duplicate.CEIDuplicateRecipes;
import com.moguang.cei.utils.emi.featured.CEIFeaturedRecipeScreen;
import com.moguang.cei.utils.emi.featured.CEIFeaturedRecipes;
import com.moguang.cei.utils.emi.search.CEIAssociatedSearch;
import com.moguang.cei.utils.emi.search.CEIAssociatedSearchRecipeScreen;
import com.moguang.cei.utils.emi.voltage.CEIVoltageRecipeFilter;
import com.moguang.cei.utils.emi.voltage.CEIVoltageRecipeScreen;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStackInteraction;
import dev.emi.emi.config.SidebarType;
import dev.emi.emi.runtime.EmiDrawContext;
import dev.emi.emi.runtime.EmiSidebars;
import dev.emi.emi.screen.EmiScreenBase;
import dev.emi.emi.screen.EmiScreenManager;
import dev.emi.emi.screen.widget.EmiSearchWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * 处理 EMI 折叠组的输入：单组切换、全部切换按钮和列表重建。
 */
@Mixin(value = EmiScreenManager.class, remap = false)
public class EmiScreenManagerInputMixin {

    /** EMI 原生搜索框实例，用于把 G 按钮定位到搜索框右侧。 */
    @Shadow
    public static EmiSearchWidget search;

    /** G 按钮边长，和 EMI 侧栏单格尺寸保持接近。 */
    @Unique
    private static final int TOGGLE_BUTTON_SIZE = 16;

    /** G 按钮与搜索框之间的水平间距。 */
    @Unique
    private static final int TOGGLE_BUTTON_GAP = 4;

    /** G 按钮左上角 X 坐标；-1 表示当前没有可点击按钮。 */
    @Unique
    private static int cei$toggleBtnX = -1;

    /** G 按钮左上角 Y 坐标；-1 表示当前没有可点击按钮。 */
    @Unique
    private static int cei$toggleBtnY = -1;

    /** 鼠标当前是否悬停在 G 按钮上，用于按钮高亮和 tooltip。 */
    @Unique
    private static boolean cei$hoveredToggleBtn = false;

    @Unique
    private static final int FEATURED_BUTTON_WIDTH = 56;

    @Unique
    private static final int FEATURED_BUTTON_HEIGHT = 16;

    @Unique
    private static final int ASSOCIATED_SEARCH_BUTTON_WIDTH = 56;

    @Unique
    private static final int ASSOCIATED_SEARCH_BUTTON_HEIGHT = 16;

    @Unique
    private static final int DUPLICATE_BUTTON_WIDTH = 56;

    @Unique
    private static final int DUPLICATE_BUTTON_HEIGHT = 16;

    @Unique
    private static final int VOLTAGE_BUTTON_WIDTH = 32;

    @Unique
    private static final int VOLTAGE_BUTTON_HEIGHT = 16;

    /** 处理 G 按钮点击，以及 Alt + 左键切换单个分组。 */
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private static void cei$handleMouseClicked(double mouseX, double mouseY, int button,
                                               CallbackInfoReturnable<Boolean> cir) {
        if (button == 0 && cei$clickFeaturedRecipeButton((int) mouseX, (int) mouseY)) {
            cir.setReturnValue(true);
            return;
        }
        if (button == 0 && cei$clickAssociatedSearchButton((int) mouseX, (int) mouseY)) {
            cir.setReturnValue(true);
            return;
        }
        if (button == 0 && cei$clickDuplicateRecipeButton((int) mouseX, (int) mouseY)) {
            cir.setReturnValue(true);
            return;
        }
        if ((button == 0 || button == 1) && cei$clickVoltageRecipeButton((int) mouseX, (int) mouseY, button)) {
            cir.setReturnValue(true);
            return;
        }

        if (CEICollapsibleGroups.needsRebuild()) return;
        if (!CEICollapsibleGroups.hasGroups()) return;

        int mx = (int) mouseX;
        int my = (int) mouseY;

        if (cei$toggleBtnX >= 0 && cei$toggleBtnY >= 0 && mx >= cei$toggleBtnX &&
                mx < cei$toggleBtnX + TOGGLE_BUTTON_SIZE && my >= cei$toggleBtnY &&
                my < cei$toggleBtnY + TOGGLE_BUTTON_SIZE) {
            if (button == 0) {
                CEICollapsibleGroups.toggleAll(false);
            } else if (button == 1) {
                CEICollapsibleGroups.toggleAll(true);
            }
            EmiScreenManager.repopulatePanels(SidebarType.INDEX);
            cir.setReturnValue(true);
            return;
        }

        if (button == 0 && Screen.hasAltDown()) {
            EmiStackInteraction interaction = EmiScreenManager.getHoveredStack(mx, my, false);
            EmiIngredient hovered = interaction.getStack();
            if (!hovered.isEmpty()) {
                if (CEICollapsibleGroups.toggleGroup(hovered)) {
                    EmiScreenManager.repopulatePanels(SidebarType.INDEX);
                    cir.setReturnValue(true);
                }
            }
        }
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private static void cei$handleMouseScrolled(double mouseX, double mouseY, double amount,
                                                CallbackInfoReturnable<Boolean> cir) {
        int delta = amount > 0 ? -1 : amount < 0 ? 1 : 0;
        if (delta != 0 && cei$scrollVoltageRecipeButton((int) mouseX, (int) mouseY, delta)) {
            cir.setReturnValue(true);
        }
    }

    /** 在搜索框右侧绘制 G 按钮：左键智能切换，右键全部折叠。 */
    @Inject(method = "renderWidgets", at = @At("TAIL"))
    private static void cei$renderToggleButton(EmiDrawContext context, int mouseX, int mouseY,
                                               float delta, EmiScreenBase base,
                                               CallbackInfo ci) {
        cei$renderFeaturedRecipeButton(context.raw(), mouseX, mouseY);
        cei$renderAssociatedSearchButton(context.raw(), mouseX, mouseY);
        cei$renderDuplicateRecipeButton(context.raw(), mouseX, mouseY);
        cei$renderVoltageRecipeButtons(context.raw(), mouseX, mouseY);

        if (CEICollapsibleGroups.needsRebuild()) return;
        if (!CEICollapsibleGroups.hasGroups()) {
            cei$toggleBtnX = -1;
            cei$toggleBtnY = -1;
            cei$hoveredToggleBtn = false;
            return;
        }

        if (base == null || search == null) return;
        cei$toggleBtnX = search.getX() + search.getWidth() + TOGGLE_BUTTON_GAP;
        cei$toggleBtnY = search.getY();

        int x = cei$toggleBtnX;
        int y = cei$toggleBtnY;

        GuiGraphics graphics = context.raw();

        cei$hoveredToggleBtn = mouseX >= x && mouseX < x + TOGGLE_BUTTON_SIZE && mouseY >= y &&
                mouseY < y + TOGGLE_BUTTON_SIZE;

        int bgColor = cei$hoveredToggleBtn ? 0xFF444444 : 0xFF333333;
        graphics.fill(x, y, x + TOGGLE_BUTTON_SIZE, y + TOGGLE_BUTTON_SIZE, bgColor);

        int borderColor = cei$hoveredToggleBtn ? 0xFF888888 : 0xFF555555;
        graphics.fill(x, y, x + TOGGLE_BUTTON_SIZE, y + 1, borderColor);
        graphics.fill(x, y + TOGGLE_BUTTON_SIZE - 1, x + TOGGLE_BUTTON_SIZE, y + TOGGLE_BUTTON_SIZE, borderColor);
        graphics.fill(x, y, x + 1, y + TOGGLE_BUTTON_SIZE, borderColor);
        graphics.fill(x + TOGGLE_BUTTON_SIZE - 1, y, x + TOGGLE_BUTTON_SIZE, y + TOGGLE_BUTTON_SIZE, borderColor);

        int collapsedCount = CEICollapsibleGroups.collapsedGroupCount();
        int textColor = collapsedCount > 0 ? 0xFF88FF88 : 0xFF888888;
        graphics.drawString(Minecraft.getInstance().font, "G", x + 4, y + 4, textColor, false);

        if (cei$hoveredToggleBtn) {
            int totalCount = CEICollapsibleGroups.totalGroupCount();
            if (collapsedCount > 0) {
                graphics.renderComponentTooltip(Minecraft.getInstance().font,
                        List.of(
                                Component.translatable("cei.emi.collapsible.button.expand_all", collapsedCount),
                                Component.translatable("cei.emi.collapsible.button.collapse_all.right_click")),
                        x, y + TOGGLE_BUTTON_SIZE + 4);
            } else {
                graphics.renderComponentTooltip(Minecraft.getInstance().font,
                        List.of(
                                Component.translatable("cei.emi.collapsible.button.collapse_all", totalCount),
                                Component.translatable("cei.emi.collapsible.button.collapse_all.right_click")),
                        x, y + TOGGLE_BUTTON_SIZE + 4);
            }
        }
    }

    @Unique
    private static boolean cei$clickFeaturedRecipeButton(int mouseX, int mouseY) {
        Screen screen = Minecraft.getInstance().screen;
        if (!(screen instanceof CEIFeaturedRecipeScreen featuredScreen)) return false;
        if (!cei$isFeaturedRecipeButtonHovered(featuredScreen, mouseX, mouseY)) return false;

        featuredScreen.cei$toggleFeaturedRecipes();
        return true;
    }

    @Unique
    private static boolean cei$clickAssociatedSearchButton(int mouseX, int mouseY) {
        Screen screen = Minecraft.getInstance().screen;
        if (!(screen instanceof CEIAssociatedSearchRecipeScreen associatedScreen)) return false;
        if (!cei$isAssociatedSearchButtonHovered(associatedScreen, mouseX, mouseY)) return false;

        associatedScreen.cei$toggleAssociatedSearch();
        return true;
    }

    @Unique
    private static boolean cei$clickDuplicateRecipeButton(int mouseX, int mouseY) {
        Screen screen = Minecraft.getInstance().screen;
        if (!(screen instanceof CEIDuplicateRecipeScreen duplicateScreen)) return false;
        if (!cei$isDuplicateRecipeButtonHovered(duplicateScreen, mouseX, mouseY)) return false;

        duplicateScreen.cei$toggleDuplicateRecipes();
        return true;
    }

    @Unique
    private static boolean cei$clickVoltageRecipeButton(int mouseX, int mouseY, int button) {
        Screen screen = Minecraft.getInstance().screen;
        if (!(screen instanceof CEIVoltageRecipeScreen voltageScreen)) return false;

        int delta = button == 1 ? -1 : 1;
        if (cei$isVoltageMinButtonHovered(voltageScreen, mouseX, mouseY)) {
            voltageScreen.cei$adjustVoltageMinTier(delta);
            return true;
        }
        if (cei$isVoltageMaxButtonHovered(voltageScreen, mouseX, mouseY)) {
            voltageScreen.cei$adjustVoltageMaxTier(delta);
            return true;
        }
        return false;
    }

    @Unique
    private static boolean cei$scrollVoltageRecipeButton(int mouseX, int mouseY, int delta) {
        Screen screen = Minecraft.getInstance().screen;
        if (!(screen instanceof CEIVoltageRecipeScreen voltageScreen)) return false;

        if (cei$isVoltageMinButtonHovered(voltageScreen, mouseX, mouseY)) {
            voltageScreen.cei$adjustVoltageMinTier(delta);
            return true;
        }
        if (cei$isVoltageMaxButtonHovered(voltageScreen, mouseX, mouseY)) {
            voltageScreen.cei$adjustVoltageMaxTier(delta);
            return true;
        }
        return false;
    }

    @Unique
    private static void cei$renderFeaturedRecipeButton(GuiGraphics graphics, int mouseX, int mouseY) {
        Screen screen = Minecraft.getInstance().screen;
        if (!(screen instanceof CEIFeaturedRecipeScreen featuredScreen)) return;

        int x = featuredScreen.cei$getFeaturedButtonX();
        int y = featuredScreen.cei$getFeaturedButtonY();
        boolean hovered = cei$isFeaturedRecipeButtonHovered(featuredScreen, mouseX, mouseY);
        boolean hidden = CEIFeaturedRecipes.isEnabled();

        int bgColor = hovered ? 0xFF444444 : 0xFF333333;
        int borderColor = hidden ? 0xFFCC7755 : 0xFF55CC77;
        int textColor = hidden ? 0xFFFFCCAA : 0xFF88FF88;

        graphics.fill(x, y, x + FEATURED_BUTTON_WIDTH, y + FEATURED_BUTTON_HEIGHT, bgColor);
        graphics.fill(x, y, x + FEATURED_BUTTON_WIDTH, y + 1, borderColor);
        graphics.fill(x, y + FEATURED_BUTTON_HEIGHT - 1, x + FEATURED_BUTTON_WIDTH,
                y + FEATURED_BUTTON_HEIGHT, borderColor);
        graphics.fill(x, y, x + 1, y + FEATURED_BUTTON_HEIGHT, borderColor);
        graphics.fill(x + FEATURED_BUTTON_WIDTH - 1, y, x + FEATURED_BUTTON_WIDTH,
                y + FEATURED_BUTTON_HEIGHT, borderColor);

        String text = Component
                .translatable(
                        hidden ? "cei.emi.recycling_recipes.button.hidden" : "cei.emi.recycling_recipes.button.visible")
                .getString();
        int textX = x + (FEATURED_BUTTON_WIDTH - Minecraft.getInstance().font.width(text)) / 2;
        graphics.drawString(Minecraft.getInstance().font, text, textX, y + 4, textColor, false);

        if (hovered) {
            graphics.renderComponentTooltip(Minecraft.getInstance().font,
                    List.of(Component.translatable(hidden ? "cei.emi.recycling_recipes.tooltip.hidden" :
                            "cei.emi.recycling_recipes.tooltip.visible")),
                    mouseX,
                    mouseY);
        }
    }

    @Unique
    private static void cei$renderAssociatedSearchButton(GuiGraphics graphics, int mouseX, int mouseY) {
        Screen screen = Minecraft.getInstance().screen;
        if (!(screen instanceof CEIAssociatedSearchRecipeScreen associatedScreen)) return;

        int x = associatedScreen.cei$getAssociatedSearchButtonX();
        int y = associatedScreen.cei$getAssociatedSearchButtonY();
        boolean hovered = cei$isAssociatedSearchButtonHovered(associatedScreen, mouseX, mouseY);
        boolean enabled = CEIAssociatedSearch.isEnabled();

        int bgColor = hovered ? 0xFF444444 : 0xFF333333;
        int borderColor = enabled ? 0xFF55CC77 : hovered ? 0xFF888888 : 0xFF555555;
        int textColor = enabled ? 0xFF88FF88 : 0xFFE0E0E0;

        graphics.fill(x, y, x + ASSOCIATED_SEARCH_BUTTON_WIDTH, y + ASSOCIATED_SEARCH_BUTTON_HEIGHT, bgColor);
        graphics.fill(x, y, x + ASSOCIATED_SEARCH_BUTTON_WIDTH, y + 1, borderColor);
        graphics.fill(x, y + ASSOCIATED_SEARCH_BUTTON_HEIGHT - 1, x + ASSOCIATED_SEARCH_BUTTON_WIDTH,
                y + ASSOCIATED_SEARCH_BUTTON_HEIGHT, borderColor);
        graphics.fill(x, y, x + 1, y + ASSOCIATED_SEARCH_BUTTON_HEIGHT, borderColor);
        graphics.fill(x + ASSOCIATED_SEARCH_BUTTON_WIDTH - 1, y, x + ASSOCIATED_SEARCH_BUTTON_WIDTH,
                y + ASSOCIATED_SEARCH_BUTTON_HEIGHT, borderColor);

        String text = Component.translatable("cei.emi.associated_search.button").getString();
        int textX = x + (ASSOCIATED_SEARCH_BUTTON_WIDTH - Minecraft.getInstance().font.width(text)) / 2;
        graphics.drawString(Minecraft.getInstance().font, text, textX, y + 4, textColor, false);

        if (hovered) {
            Component tooltip = Component.translatable(enabled ? "cei.emi.associated_search.tooltip.enabled" :
                    "cei.emi.associated_search.tooltip.disabled");
            graphics.renderComponentTooltip(Minecraft.getInstance().font, List.of(tooltip), mouseX, mouseY);
        }
    }

    @Unique
    private static void cei$renderDuplicateRecipeButton(GuiGraphics graphics, int mouseX, int mouseY) {
        Screen screen = Minecraft.getInstance().screen;
        if (!(screen instanceof CEIDuplicateRecipeScreen duplicateScreen)) return;

        int x = duplicateScreen.cei$getDuplicateRecipeButtonX();
        int y = duplicateScreen.cei$getDuplicateRecipeButtonY();
        boolean hovered = cei$isDuplicateRecipeButtonHovered(duplicateScreen, mouseX, mouseY);
        boolean hidden = CEIDuplicateRecipes.isHidden();

        int bgColor = hovered ? 0xFF444444 : 0xFF333333;
        int borderColor = hidden ? 0xFFCC7755 : 0xFF55CC77;
        int textColor = hidden ? 0xFFFFCCAA : 0xFF88FF88;

        graphics.fill(x, y, x + DUPLICATE_BUTTON_WIDTH, y + DUPLICATE_BUTTON_HEIGHT, bgColor);
        graphics.fill(x, y, x + DUPLICATE_BUTTON_WIDTH, y + 1, borderColor);
        graphics.fill(x, y + DUPLICATE_BUTTON_HEIGHT - 1, x + DUPLICATE_BUTTON_WIDTH,
                y + DUPLICATE_BUTTON_HEIGHT, borderColor);
        graphics.fill(x, y, x + 1, y + DUPLICATE_BUTTON_HEIGHT, borderColor);
        graphics.fill(x + DUPLICATE_BUTTON_WIDTH - 1, y, x + DUPLICATE_BUTTON_WIDTH,
                y + DUPLICATE_BUTTON_HEIGHT, borderColor);

        String text = Component
                .translatable(
                        hidden ? "cei.emi.duplicate_recipes.button.hidden" : "cei.emi.duplicate_recipes.button.visible")
                .getString();
        int textX = x + (DUPLICATE_BUTTON_WIDTH - Minecraft.getInstance().font.width(text)) / 2;
        graphics.drawString(Minecraft.getInstance().font, text, textX, y + 4, textColor, false);

        if (hovered) {
            graphics.renderComponentTooltip(Minecraft.getInstance().font,
                    List.of(Component.translatable(hidden ? "cei.emi.duplicate_recipes.tooltip.hidden" :
                            "cei.emi.duplicate_recipes.tooltip.visible")),
                    mouseX,
                    mouseY);
        }
    }

    @Unique
    private static void cei$renderVoltageRecipeButtons(GuiGraphics graphics, int mouseX, int mouseY) {
        Screen screen = Minecraft.getInstance().screen;
        if (!(screen instanceof CEIVoltageRecipeScreen voltageScreen)) return;

        int minX = voltageScreen.cei$getVoltageMinButtonX();
        int minY = voltageScreen.cei$getVoltageMinButtonY();
        int maxX = voltageScreen.cei$getVoltageMaxButtonX();
        int maxY = voltageScreen.cei$getVoltageMaxButtonY();
        boolean minHovered = cei$isVoltageMinButtonHovered(voltageScreen, mouseX, mouseY);
        boolean maxHovered = cei$isVoltageMaxButtonHovered(voltageScreen, mouseX, mouseY);

        cei$drawVoltageButton(graphics, minX, minY, CEIVoltageRecipeFilter.getMinTierName(), minHovered);
        cei$drawVoltageButton(graphics, maxX, maxY, CEIVoltageRecipeFilter.getMaxTierName(), maxHovered);

        String arrow = "->";
        int arrowX = minX + VOLTAGE_BUTTON_WIDTH +
                (maxX - minX - VOLTAGE_BUTTON_WIDTH - Minecraft.getInstance().font.width(arrow)) / 2;
        graphics.drawString(Minecraft.getInstance().font, arrow, arrowX, minY + 4, 0xFFE0E0E0, false);
    }

    @Unique
    private static void cei$drawVoltageButton(GuiGraphics graphics, int x, int y, String text, boolean hovered) {
        int bgColor = hovered ? 0xFF444444 : 0xFF333333;
        int borderColor = 0xFF55AAEE;
        int textColor = 0xFFAAE0FF;

        graphics.fill(x, y, x + VOLTAGE_BUTTON_WIDTH, y + VOLTAGE_BUTTON_HEIGHT, bgColor);
        graphics.fill(x, y, x + VOLTAGE_BUTTON_WIDTH, y + 1, borderColor);
        graphics.fill(x, y + VOLTAGE_BUTTON_HEIGHT - 1, x + VOLTAGE_BUTTON_WIDTH,
                y + VOLTAGE_BUTTON_HEIGHT, borderColor);
        graphics.fill(x, y, x + 1, y + VOLTAGE_BUTTON_HEIGHT, borderColor);
        graphics.fill(x + VOLTAGE_BUTTON_WIDTH - 1, y, x + VOLTAGE_BUTTON_WIDTH,
                y + VOLTAGE_BUTTON_HEIGHT, borderColor);

        int textX = x + (VOLTAGE_BUTTON_WIDTH - Minecraft.getInstance().font.width(text)) / 2;
        graphics.drawString(Minecraft.getInstance().font, text, textX, y + 4, textColor, false);
    }

    @Unique
    private static boolean cei$isFeaturedRecipeButtonHovered(CEIFeaturedRecipeScreen screen, int mouseX, int mouseY) {
        int x = screen.cei$getFeaturedButtonX();
        int y = screen.cei$getFeaturedButtonY();
        return mouseX >= x && mouseX < x + FEATURED_BUTTON_WIDTH &&
                mouseY >= y && mouseY < y + FEATURED_BUTTON_HEIGHT;
    }

    @Unique
    private static boolean cei$isAssociatedSearchButtonHovered(CEIAssociatedSearchRecipeScreen screen,
                                                               int mouseX, int mouseY) {
        int x = screen.cei$getAssociatedSearchButtonX();
        int y = screen.cei$getAssociatedSearchButtonY();
        return mouseX >= x && mouseX < x + ASSOCIATED_SEARCH_BUTTON_WIDTH &&
                mouseY >= y && mouseY < y + ASSOCIATED_SEARCH_BUTTON_HEIGHT;
    }

    @Unique
    private static boolean cei$isDuplicateRecipeButtonHovered(CEIDuplicateRecipeScreen screen,
                                                              int mouseX, int mouseY) {
        int x = screen.cei$getDuplicateRecipeButtonX();
        int y = screen.cei$getDuplicateRecipeButtonY();
        return mouseX >= x && mouseX < x + DUPLICATE_BUTTON_WIDTH &&
                mouseY >= y && mouseY < y + DUPLICATE_BUTTON_HEIGHT;
    }

    @Unique
    private static boolean cei$isVoltageMinButtonHovered(CEIVoltageRecipeScreen screen, int mouseX, int mouseY) {
        int x = screen.cei$getVoltageMinButtonX();
        int y = screen.cei$getVoltageMinButtonY();
        return mouseX >= x && mouseX < x + VOLTAGE_BUTTON_WIDTH &&
                mouseY >= y && mouseY < y + VOLTAGE_BUTTON_HEIGHT;
    }

    @Unique
    private static boolean cei$isVoltageMaxButtonHovered(CEIVoltageRecipeScreen screen, int mouseX, int mouseY) {
        int x = screen.cei$getVoltageMaxButtonX();
        int y = screen.cei$getVoltageMaxButtonY();
        return mouseX >= x && mouseX < x + VOLTAGE_BUTTON_WIDTH &&
                mouseY >= y && mouseY < y + VOLTAGE_BUTTON_HEIGHT;
    }

    /** EMI 刷新搜索来源时，用 INDEX 完整列表重建分组。 */
    @Inject(method = "getSearchSource", at = @At("RETURN"))
    private static void cei$rebuildOnSearch(CallbackInfoReturnable<List<? extends EmiIngredient>> cir) {
        if (CEICollapsibleGroups.needsRebuild()) {
            List<? extends EmiIngredient> source = EmiSidebars.getStacks(SidebarType.INDEX);
            if (source != null && !source.isEmpty()) {
                CEICollapsibleGroups.rebuild(source);
            }
        }
    }

    /** EMI 开关或重载后，下一次搜索刷新时重新扫描列表。 */
    @Inject(method = "toggleVisibility", at = @At("HEAD"))
    private static void cei$markDirtyOnToggle(boolean notify, CallbackInfo ci) {
        CEICollapsibleGroups.markDirty();
    }
}
