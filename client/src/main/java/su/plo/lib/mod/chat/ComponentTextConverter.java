package su.plo.lib.mod.chat;

import lombok.NonNull;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.plo.lib.api.chat.*;

import java.util.List;

//#if MC>=12005
//$$ import com.mojang.serialization.JsonOps;
//$$ import com.google.gson.*;
//$$ import net.minecraft.network.chat.ComponentSerialization;
//#endif

public final class ComponentTextConverter implements MinecraftTextConverter<Component> {

    //#if MC>=12005
    //$$ private static final Gson GSON = (new GsonBuilder()).disableHtmlEscaping().create();
    //#endif

    @Override // todo: legacy
    public @NotNull String convertToJson(@NotNull Component text) {
        //#if MC>=12005
        //$$ return GSON.toJson(
        //$$         ComponentSerialization.CODEC.encodeStart(JsonOps.INSTANCE, text)
        //$$                 .getOrThrow(JsonParseException::new)
        //$$ );
        //#else
        return Component.Serializer.toJson(text);
        //#endif
    }

    @Override // todo: legacy
    public Component convertFromJson(@NotNull String json) {
        //#if MC>=12005
        //$$ JsonElement jsonElement = JsonParser.parseString(json);
        //$$ return jsonElement == null ? null : ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, jsonElement)
        //$$         .getOrThrow(JsonParseException::new);
        //#else
        return Component.Serializer.fromJson(json);
        //#endif
    }

    @Override
    public Component convert(@NotNull MinecraftTextComponent text) {
        MutableComponent component;

        if (text instanceof MinecraftTranslatableText)
            component = convertTranslatable((MinecraftTranslatableText) text);
        else
            component = Component.literal(text.toString());

        // apply styles
        component = applyStyles(component, text.styles());

        // apply click event
        component = applyClickEvent(component, text.clickEvent());

        // apply hover event
        component = applyHoverEvent(component, text.hoverEvent());

        // add siblings
        for (MinecraftTextComponent sibling : text.siblings()) {
            component.append(convert(sibling));
        }

        return component;
    }

    private MutableComponent convertTranslatable(@NotNull MinecraftTranslatableText text) {
        Object[] args = new Object[text.getArgs().length];

        for (int i = 0; i < args.length; i++) {
            Object arg = text.getArgs()[i];

            if (arg instanceof MinecraftTextComponent) {
                args[i] = convert((MinecraftTextComponent) arg);
            } else {
                args[i] = arg;
            }
        }

        return Component.translatable(text.getKey(), args);
    }

    private MutableComponent applyClickEvent(@NotNull MutableComponent component,
                                             @Nullable MinecraftTextClickEvent clickEvent) {
        if (clickEvent == null) return component;

        component.setStyle(component.getStyle().withClickEvent(new ClickEvent(
                ClickEvent.Action.valueOf(clickEvent.action().name()),
                clickEvent.value()
        )));

        return component;
    }

    private MutableComponent applyHoverEvent(@NotNull MutableComponent component,
                                             @Nullable MinecraftTextHoverEvent hoverEvent) {
        if (hoverEvent == null) return component;

        // todo: waytoodank
        if (hoverEvent.action() == MinecraftTextHoverEvent.Action.SHOW_TEXT) {
            component.setStyle(component.getStyle().withHoverEvent(new HoverEvent(
                    HoverEvent.Action.SHOW_TEXT,
                    convert((MinecraftTextComponent) hoverEvent.value())
            )));
        }

        return component;
    }

    private MutableComponent applyStyles(@NonNull MutableComponent component,
                                         @NotNull List<MinecraftTextStyle> styles) {
        if (styles.isEmpty()) return component;

        // todo: legacy support
        component.setStyle(component.getStyle().applyFormats(
                styles.stream()
                        .map(this::convertStyle)
                        .toArray(ChatFormatting[]::new)
        ));

        return component;
    }

    private ChatFormatting convertStyle(@NotNull MinecraftTextStyle style) {
        return ChatFormatting.valueOf(style.name());
    }
}
