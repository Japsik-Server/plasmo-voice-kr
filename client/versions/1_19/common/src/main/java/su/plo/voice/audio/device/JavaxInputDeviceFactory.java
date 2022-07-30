package su.plo.voice.audio.device;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.plo.voice.api.PlasmoVoiceClient;
import su.plo.voice.api.audio.device.AudioDevice;
import su.plo.voice.api.audio.device.DeviceException;
import su.plo.voice.api.audio.device.DeviceFactory;
import su.plo.voice.api.audio.device.Params;

import javax.sound.sampled.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.google.common.base.Preconditions.checkNotNull;

public final class JavaxInputDeviceFactory implements DeviceFactory {

    private final PlasmoVoiceClient client;

    public JavaxInputDeviceFactory(PlasmoVoiceClient client) {
        this.client = client;
    }

    @Override
    public CompletableFuture<AudioDevice> openDevice(@NotNull AudioFormat format, @Nullable String deviceName, @NotNull Params params) throws DeviceException {
        checkNotNull(format, "format cannot be null");
        checkNotNull(params, "params cannot be null");

        AudioDevice device = new JavaxInputDevice(client, deviceName);
        return device.open(format, params);
    }

    @Override
    public String getDefaultDeviceName() {
        return getDeviceNames().iterator().next();
    }

    @Override
    public Collection<String> getDeviceNames() {
        List<String> devices = new ArrayList<>();
        Mixer.Info[] mixers = AudioSystem.getMixerInfo();

        for (Mixer.Info mixerInfo : mixers) {
            Mixer mixer = AudioSystem.getMixer(mixerInfo);
            Line.Info lineInfo = new Line.Info(TargetDataLine.class);

            if (mixer.isLineSupported(lineInfo)) {
                devices.add(mixerInfo.getName());
            }
        }

        return devices;
    }

    @Override
    public String getType() {
        return "JAVAX_INPUT";
    }
}