package stardust.cc.mixin.client;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.lwjgl.glfw.GLFW;

@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeInventoryScreenMixin extends AbstractContainerScreen<AbstractContainerMenu> {
    @Unique private static final int[] CC_SLOT_X = { 172, 133, 151, 133, 151 };
    @Unique private static final int[] CC_SLOT_Y = {  20,  10,  10,  28,  28 };

    @Shadow private static CreativeModeTab selectedTab;

    public CreativeInventoryScreenMixin(AbstractContainerMenu handler) {
        super(handler, null, null);
    }

    // Move crafting slots from offscreen
    @Inject(method = "selectTab", at = @At("TAIL"))
    private void onSelectTab(CreativeModeTab tab, CallbackInfo ci) {
        if (!tab.getType().equals(CreativeModeTab.Type.INVENTORY)) return;
        if (menu.slots.size() < 5) return;

        for (int i = 0; i < 5; i++) {
            Slot slot = menu.slots.get(i);
            slot.x = CC_SLOT_X[i];
            slot.y = CC_SLOT_Y[i];
        }
    }

    // Delete key clears the hovered crafting slot
    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void onKeyPressed(KeyEvent input, CallbackInfoReturnable<Boolean> cir) {
        if (!selectedTab.getType().equals(CreativeModeTab.Type.INVENTORY)) return;
        if (input.key() != GLFW.GLFW_KEY_DELETE) return;

        LocalPlayer player = minecraft.player;
        if (player == null) return;

        Slot hovered = hoveredSlot;
        if (hovered == null) return;

        int index = menu.slots.indexOf(hovered);
        if (index < 1 || index > 4 || !hovered.hasItem()) return;

        hovered.set(ItemStack.EMPTY);
        player.connection.send(new ServerboundSetCreativeModeSlotPacket(index, ItemStack.EMPTY));

        cir.setReturnValue(true);
    }
}