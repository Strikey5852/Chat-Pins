package com.strikey.chat_pins.fabric.client.mixin;

import com.strikey.chat_pins.fabric.client.accessor.LineAccessor;
import net.minecraft.client.GuiMessage;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(GuiMessage.Line.class)
public abstract class LineMixin implements LineAccessor {

    @Shadow
    private FormattedCharSequence content;

    @Override
    public FormattedCharSequence chat_pins$content() {
        return content;
    }
}
