package com.moguang.cei.utils.emi.search;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;

import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;

import java.util.HashSet;
import java.util.Set;

/** Copies the display name of a dragged EMI stack into vanilla EditBox-based search fields. */
public class CEIEmiDragSearchFill {

    public static boolean dropNameIntoHoveredTextField(Screen screen, EmiIngredient dragged, int mouseX, int mouseY) {
        if (screen == null || dragged == null || dragged.isEmpty()) return false;

        EditBox textField = findHoveredTextField(screen, mouseX, mouseY, new HashSet<>());
        if (textField == null) return false;

        String name = getDraggedName(dragged);
        if (name.isBlank()) return false;

        textField.setValue(name);
        textField.setFocused(true);
        textField.moveCursorToEnd();
        screen.setFocused(textField);
        return true;
    }

    private static EditBox findHoveredTextField(GuiEventListener listener, int mouseX, int mouseY,
                                                Set<GuiEventListener> visited) {
        if (listener == null || !visited.add(listener)) return null;

        if (listener instanceof EditBox editBox && editBox.isMouseOver(mouseX, mouseY)) {
            return editBox;
        }

        if (listener instanceof ContainerEventHandler container) {
            for (GuiEventListener child : container.children()) {
                EditBox found = findHoveredTextField(child, mouseX, mouseY, visited);
                if (found != null) return found;
            }
        }

        return null;
    }

    private static String getDraggedName(EmiIngredient dragged) {
        for (EmiStack stack : dragged.getEmiStacks()) {
            if (!stack.isEmpty()) {
                return stack.getName().getString();
            }
        }
        return "";
    }
}
