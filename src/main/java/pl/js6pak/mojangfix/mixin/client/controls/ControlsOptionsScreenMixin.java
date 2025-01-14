/*
 * Copyright (C) 2022 js6pak
 *
 * This file is part of MojangFix.
 *
 * MojangFix is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free Software Foundation, version 3.
 *
 * MojangFix is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with MojangFix. If not, see <https://www.gnu.org/licenses/>.
 */

package pl.js6pak.mojangfix.mixin.client.controls;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.options.ControlsOptionsScreen ;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.OptionButtonWidget;
import net.minecraft.client.options.GameOptions;
import net.minecraft.client.options.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.js6pak.mojangfix.client.gui.CallbackButtonWidget;
import pl.js6pak.mojangfix.client.gui.ControlsListWidget;
import pl.js6pak.mojangfix.mixinterface.KeyBindingAccessor;

@Mixin(ControlsOptionsScreen.class)
public class ControlsOptionsScreenMixin extends Screen {
    @Shadow
    private GameOptions options;

    @Unique
    private ControlsListWidget controlsList;

    @Inject(method = "init", at = @At("HEAD"))
    private void onInit(CallbackInfo ci) {
        this.controlsList = new ControlsListWidget((ControlsOptionsScreen) (Object) this, minecraft, options);
    }

    @Redirect(method = "init", at = @At(value = "NEW", target = "net/minecraft/client/gui/widget/OptionButtonWidget"))
    private OptionButtonWidget redirectOptionButtonWidget(int id, int x, int y, int width, int height, String text) {
        OptionButtonWidget editButton = new OptionButtonWidget(id, -1, -1, width, height, text);
        CallbackButtonWidget resetButton = new CallbackButtonWidget(-1, -1, 50, 20, "Reset", (button) -> {
            KeyBinding keyBinding = this.options.keyBindings[id];
            keyBinding.keyCode = ((KeyBindingAccessor) keyBinding).getDefaultKeyCode();
            ((ButtonWidget) this.buttons.get(id)).message = this.options.getKeyBindingName(id);
        });
        controlsList.getButtons().put(this.options.keyBindings[id], new ControlsListWidget.KeyBindingEntry(editButton, resetButton));
        return editButton;
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void afterInit(CallbackInfo ci) {
        for (ControlsListWidget.KeyBindingEntry entry : controlsList.getButtons().values()) {
            this.buttons.add(entry.getResetButton());
        }
    }

    @Unique
    private ButtonWidget doneButton;

    @Redirect(method = "init", at = @At(value = "NEW", target = "net/minecraft/client/gui/widget/ButtonWidget"))
    private ButtonWidget redirectDoneButton(int id, int x, int y, String text) {
        return doneButton = new ButtonWidget(id, x, this.height - 30, text);
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/options/ControlsOptionsScreen;renderBackground()V", shift = At.Shift.AFTER))
    private void onRender(int mouseX, int mouseY, float delta, CallbackInfo ci) {
        this.controlsList.render(mouseX, mouseY, delta);
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/options/ControlsOptionsScreen;getControlsListX()I"), cancellable = true)
    private void onDrawButtons(int mouseX, int mouseY, float delta, CallbackInfo ci) {
        doneButton.render(this.minecraft, mouseX, mouseY);
        ci.cancel();
    }
}
