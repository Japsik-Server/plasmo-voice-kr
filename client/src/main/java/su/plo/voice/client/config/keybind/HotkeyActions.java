package su.plo.voice.client.config.keybind;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import su.plo.config.entry.ConfigEntry;
import su.plo.lib.api.chat.MinecraftTextComponent;
import su.plo.lib.mod.client.render.RenderUtil;
import su.plo.voice.api.client.PlasmoVoiceClient;
import su.plo.voice.api.client.config.keybind.KeyBinding;
import su.plo.voice.api.client.config.keybind.KeyBindings;
import su.plo.voice.client.config.VoiceClientConfig;
import su.plo.voice.proto.packets.tcp.serverbound.PlayerStatePacket;
import gg.essential.universal.UChat;

@RequiredArgsConstructor
public final class HotkeyActions {

    private final PlasmoVoiceClient voiceClient;
    private final KeyBindings keyBindings;
    private final VoiceClientConfig config;

    public void register() {
        setHotkeyAction(
                "key.plasmovoice.general.mute_microphone",
                createConfigToggleAction(config.getVoice().getMicrophoneDisabled())
        );
        config.getVoice().getMicrophoneDisabled().addChangeListener((value) -> sendPlayerStatePacket());

        setHotkeyAction(
                "key.plasmovoice.general.disable_voice",
                createConfigToggleAction(config.getVoice().getDisabled())
        );
        config.getVoice().getDisabled().addChangeListener((value) -> sendPlayerStatePacket());

        setHotkeyAction(
                "key.plasmovoice.occlusion.toggle",
                createKeyDownAction(() -> {
                    ConfigEntry<Boolean> entry = config.getVoice().getSoundOcclusion();
                    entry.set(!entry.value());

                    UChat.actionBar(RenderUtil.getTextConverter().convert(
                            MinecraftTextComponent.translatable(
                                    "message.plasmovoice.occlusion_changed",
                                    entry.value()
                                            ? MinecraftTextComponent.translatable("message.plasmovoice.on")
                                            : MinecraftTextComponent.translatable("message.plasmovoice.off")
                            )
                    ));
                })
        );
    }

    private KeyBinding.OnPress createConfigToggleAction(ConfigEntry<Boolean> entry) {
        return createKeyDownAction(() -> entry.set(!entry.value()));
    }

    private KeyBinding.OnPress createKeyDownAction(Runnable runnable) {
        return (action) -> {
            if (action != KeyBinding.Action.DOWN) return;
            runnable.run();
        };
    }

    private void setHotkeyAction(@NotNull String name, @NotNull KeyBinding.OnPress onPress) {
        keyBindings
                .getKeyBinding(name)
                .ifPresent(hotkey -> hotkey.addPressListener(onPress));
    }

    private void sendPlayerStatePacket() {
        voiceClient.getServerConnection()
                .ifPresent((connection) ->
                        connection.sendPacket(new PlayerStatePacket(
                                config.getVoice().getDisabled().value(),
                                config.getVoice().getMicrophoneDisabled().value()
                        ))
                );
    }
}
