package su.plo.voice.api.audio.device;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.sound.sampled.AudioFormat;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

// todo: doc
public interface DeviceFactory {

    /**
     * Opens a new device
     *
     * @param deviceName the device name
     * @param params device params, may be different depending on DeviceFactory
     *
     * @throws DeviceException if device cannot be open
     */
    CompletableFuture<AudioDevice> openDevice(@NotNull AudioFormat format, @Nullable String deviceName, @NotNull Params params) throws DeviceException;

    /**
     * Gets the default device name
     *
     * @return the default device name
     */
    String getDefaultDeviceName();

    /**
     * Gets all device names
     *
     * @return device names
     */
    Collection<String> getDeviceNames();

    /**
     * Gets the device's factory type, should be unique
     *
     * @return the device's factory type
     */
    String getType();
}