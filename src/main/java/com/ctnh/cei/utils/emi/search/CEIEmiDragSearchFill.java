package com.ctnh.cei.utils.emi.search;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;

import com.ctnh.cei.mixin.accessor.EditBoxAccessor;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.screen.EmiScreenManager;

import java.util.HashSet;
import java.util.Set;

/** Copies the display name of a dragged EMI stack into vanilla EditBox-based search fields. */
public class CEIEmiDragSearchFill {

    private static final int DRAG_SEARCH_HIGHLIGHT_COLOR = 0x8000FF00;

    public static boolean dropNameIntoHoveredTextField(Screen screen, EmiIngredient dragged, int mouseX, int mouseY) {
        if (screen == null || dragged == null || dragged.isEmpty()) return false;

        SearchFieldTarget target = findHoveredTarget(screen, mouseX, mouseY, new HashSet<>());
        if (target == null) return false;

        String name = getDraggedName(dragged);
        if (name.isBlank()) return false;

        EditBox textField = target.textField();
        textField.setValue(name);
        textField.setFocused(true);
        textField.moveCursorToEnd();
        screen.setFocused(textField);
        return true;
    }

    public static void renderSearchFieldHighlights(GuiGraphics graphics) {
        Screen screen = Minecraft.getInstance().screen;
        if (screen == null || graphics == null || EmiScreenManager.draggedStack.isEmpty()) return;

        for (SearchFieldTarget target : collectTargets(screen, new HashSet<>())) {
            Rect2i area = target.area();
            graphics.fill(area.getX(), area.getY(),
                    area.getX() + area.getWidth(),
                    area.getY() + area.getHeight(),
                    DRAG_SEARCH_HIGHLIGHT_COLOR);
        }
    }

    private static SearchFieldTarget findHoveredTarget(GuiEventListener listener, int mouseX, int mouseY,
                                                       Set<GuiEventListener> visited) {
        if (listener == null || !visited.add(listener)) return null;

        if (listener instanceof EditBox editBox && editBox.visible) {
            SearchFieldTarget target = createTarget(editBox);
            if (target.area().contains(mouseX, mouseY)) {
                return target;
            }
        }

        if (listener instanceof ContainerEventHandler container) {
            for (GuiEventListener child : container.children()) {
                SearchFieldTarget found = findHoveredTarget(child, mouseX, mouseY, visited);
                if (found != null) return found;
            }
        }

        return null;
    }

    private static Set<SearchFieldTarget> collectTargets(GuiEventListener listener, Set<GuiEventListener> visited) {
        Set<SearchFieldTarget> targets = new HashSet<>();
        collectTargets(listener, visited, targets);
        return targets;
    }

    private static void collectTargets(GuiEventListener listener, Set<GuiEventListener> visited,
                                       Set<SearchFieldTarget> targets) {
        if (listener == null || !visited.add(listener)) return;

        if (listener instanceof EditBox editBox && editBox.visible) {
            targets.add(createTarget(editBox));
        }

        if (listener instanceof ContainerEventHandler container) {
            for (GuiEventListener child : container.children()) {
                collectTargets(child, visited, targets);
            }
        }
    }

    private static SearchFieldTarget createTarget(EditBox editBox) {
        return new SearchFieldTarget(editBox, getVisualArea(editBox));
    }

    private static Rect2i getVisualArea(EditBox editBox) {
        if (isAeTextField(editBox)) {
            int padding = 2;
            int fontPad = Minecraft.getInstance().font.width("_");
            return new Rect2i(editBox.getX() - padding, editBox.getY() - padding,
                    editBox.getWidth() + padding * 2 + fontPad,
                    editBox.getHeight() + padding * 2);
        }

        int x = editBox.getX();
        int y = editBox.getY();
        int width = editBox.getWidth();
        int height = editBox.getHeight();

        if (((EditBoxAccessor) editBox).cei$isBordered()) {
            x -= 1;
            y -= 1;
            width += 2;
            height += 2;
        }

        return new Rect2i(x, y, width, height);
    }

    private static boolean isAeTextField(EditBox editBox) {
        Class<?> type = editBox.getClass();
        while (type != null) {
            if ("appeng.client.gui.widgets.AETextField".equals(type.getName())) {
                return true;
            }
            type = type.getSuperclass();
        }
        return false;
    }

    private static String getDraggedName(EmiIngredient dragged) {
        for (EmiStack stack : dragged.getEmiStacks()) {
            if (!stack.isEmpty()) {
                return stack.getName().getString();
            }
        }
        return "";
    }

    private record SearchFieldTarget(EditBox textField, Rect2i area) {}
}
