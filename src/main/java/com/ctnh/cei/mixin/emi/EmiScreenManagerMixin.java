package com.ctnh.cei.mixin.emi;

import com.gregtechceu.gtceu.common.data.GTItems;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.FluidEmiStack;
import dev.emi.emi.network.CreateItemC2SPacket;
import dev.emi.emi.network.EmiNetwork;
import dev.emi.emi.screen.EmiScreenManager;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = EmiScreenManager.class, remap = false)
public abstract class EmiScreenManagerMixin {

    @Redirect(method = "give",
              at = @At(value = "INVOKE",
                       target = "Ldev/emi/emi/api/stack/EmiStack;getItemStack()Lnet/minecraft/world/item/ItemStack;"))
    private static ItemStack allowFluidStack(EmiStack stack, @Share("realStack") LocalRef<ItemStack> realStackRef) {
        if (realStackRef.get() != null)
            return realStackRef.get();
        ItemStack realStack = ItemStack.EMPTY;
        if (stack.getItemStack().isEmpty() && stack instanceof FluidEmiStack fluidEmiStack) {
            if (fluidEmiStack.getKey() instanceof Fluid fluid) {
                var container = GTItems.FLUID_CELL.asStack();
                container.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).ifPresent(
                        p -> p.fill(new FluidStack(fluid, 1000, fluidEmiStack.getNbt()),
                                IFluidHandler.FluidAction.EXECUTE));
                realStack = container;
            }
        } else {
            realStack = stack.getItemStack();
        }
        realStackRef.set(realStack);
        return realStack;
    }

    @Inject(method = "mouseReleased",
            at = @At(value = "INVOKE",
                     target = "Ldev/emi/emi/screen/EmiScreenManager;deleteCursor(II)Z"),
            cancellable = true)
    private static void fillCursorContainerFromFluidStack(double mouseX, double mouseY, int button,
                                                          CallbackInfoReturnable<Boolean> cir) {
        AbstractContainerScreen<?> handled = EmiApi.getHandledScreen();
        if (handled == null) return;

        ItemStack cursor = handled.getMenu().getCarried();
        if (cursor.isEmpty() || cursor.getCount() != 1) return;

        EmiIngredient ingredient = EmiScreenManager.getHoveredStack((int) mouseX, (int) mouseY, false).getStack();
        if (ingredient.getEmiStacks().size() != 1 ||
                !(ingredient.getEmiStacks().get(0) instanceof FluidEmiStack fluidStack) ||
                !(fluidStack.getKey() instanceof Fluid fluid)) {
            return;
        }

        ItemStack filledContainer = cursor.copy();
        var capability = filledContainer.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM);
        if (!capability.isPresent()) return;

        capability.ifPresent(handler -> {
            if (handler.fill(new FluidStack(fluid, Integer.MAX_VALUE, fluidStack.getNbt()),
                    IFluidHandler.FluidAction.EXECUTE) > 0) {
                ItemStack result = handler.getContainer();
                handled.getMenu().setCarried(result);
                if (!(handled instanceof CreativeModeInventoryScreen) ||
                        !Minecraft.getInstance().player.getAbilities().instabuild) {
                    EmiNetwork.sendToServer(new CreateItemC2SPacket(1, result));
                }
            }
        });
        cir.setReturnValue(true);
    }

    @ModifyExpressionValue(method = "addWidgets",
                           at = @At(value = "FIELD",
                                    target = "Ldev/emi/emi/config/EmiConfig;centerSearchBar:Z",
                                    opcode = Opcodes.GETSTATIC))
    private static boolean disableCenterSearchBar(boolean original) {
        return false;
    }
}
