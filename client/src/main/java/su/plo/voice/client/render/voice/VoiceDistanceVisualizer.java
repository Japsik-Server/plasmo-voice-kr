package su.plo.voice.client.render.voice;

import com.google.common.collect.Maps;
import gg.essential.universal.UGraphics;
import gg.essential.universal.UMatrixStack;
import gg.essential.universal.UMinecraft;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.plo.lib.mod.client.render.RenderUtil;
import su.plo.voice.api.client.PlasmoVoiceClient;
import su.plo.voice.api.client.event.render.VoiceDistanceRenderEvent;
import su.plo.voice.api.client.render.DistanceVisualizer;
import su.plo.voice.api.event.EventSubscribe;
import su.plo.voice.client.config.VoiceClientConfig;
import su.plo.voice.client.event.render.LevelRenderEvent;
import su.plo.voice.client.render.ModCamera;
import su.plo.voice.proto.data.pos.Pos3d;

import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
public final class VoiceDistanceVisualizer implements DistanceVisualizer {

    private static final int SPHERE_STACK = 18;
    private static final int SPHERE_SLICE = 36;

    private final PlasmoVoiceClient voiceClient;
    private final VoiceClientConfig config;

    private final Map<UUID, VisualizeEntry> entries = Maps.newConcurrentMap();

//    private int color = 0x00a000;
//    private int alpha = 0;
//    private float radius = 8F;
//    private long lastChanged;

    @Override
    public synchronized void render(int radius, int color, @Nullable Pos3d position) {
        if (!config.getAdvanced().getVisualizeVoiceDistance().value()) return;
        // todo: legacy render distance
        if (radius < 2 || radius > UMinecraft.getSettings().renderDistance().get() * 16) return;

        VoiceDistanceRenderEvent event = new VoiceDistanceRenderEvent(this, radius, color);
        if (!voiceClient.getEventBus().call(event)) return;

        UUID key;
        if (position == null) {
            key = UUID.fromString("00000000-0000-0000-0000-000000000000");
        } else {
            key = UUID.randomUUID();
        }

        entries.put(key, new VisualizeEntry(
                color,
                radius,
                position != null ? new Vec3(position.getX(), position.getY(), position.getZ()) : null
        ));
    }

    @EventSubscribe
    public void onLevelRender(@NotNull LevelRenderEvent event) {
        for (Map.Entry<UUID, VisualizeEntry> entry : entries.entrySet()) {
            UUID key = entry.getKey();
            VisualizeEntry value = entry.getValue();

            if (value.alpha() == 0 || !config.getAdvanced().getVisualizeVoiceDistance().value()) {
                entries.remove(key);
                continue;
            }

            if (value.position() != null &&
                    event.getCamera().position().distanceTo(value.position()) > (UMinecraft.getSettings().renderDistance().get() * 16)
            ) {
                entries.remove(key);
                continue;
            }

            renderEntry(value, event.getStack(), event.getCamera());
        }
    }

    private void renderEntry(@NotNull VisualizeEntry entry,
                             @NotNull UMatrixStack stack,
                             @NotNull ModCamera camera) {
        if (System.currentTimeMillis() - entry.lastChanged() > 2000L) {
            entry.alpha(entry.alpha() - 5);
        }

        Vec3 center;
        if (entry.position() != null) {
            center = entry.position();
        } else {
            LocalPlayer clientPlayer = UMinecraft.getPlayer();
            if (clientPlayer == null) return;

            center = clientPlayer.position();
        }

        UGraphics buffer = UGraphics.getFromTessellator();

        // setup render
        RenderUtil.disableCull();
        UGraphics.enableDepth();
        UGraphics.depthMask(false);
        RenderUtil.polygonOffset(-3f, -3f);
        RenderUtil.enablePolygonOffset();
        UGraphics.depthFunc(515);

        UGraphics.enableBlend();
        UGraphics.tryBlendFuncSeparate(
                770, // SourceFactor.SRC_ALPHA
                771, // DestFactor.ONE_MINUS_SRC_ALPHA
                1, // SourceFactor.ONE
                0 // DestFactor.ZERO
        );
        UGraphics.color4f(1F, 1F, 1F, 1F);

        stack.push();
        RenderUtil.lineWidth(1f);

        stack.translate(
                center.x - camera.position().x,
                center.y - camera.position().y,
                center.z - camera.position().z
        );

        buffer.beginWithDefaultShader(
                UGraphics.DrawMode.TRIANGLE_STRIP,
                UGraphics.CommonVertexFormats.POSITION_COLOR
        );

        int r = (entry.color() >> 16) & 0xFF;
        int g = (entry.color() >> 8) & 0xFF;
        int b = entry.color() & 0xFF;

        float r0, r1, alpha0, alpha1, x0, x1, y0, y1, z0, z1, beta;
        float stackStep = (float) (Math.PI / SPHERE_STACK);
        float sliceStep = (float) (Math.PI / SPHERE_SLICE);
        for (int i = 0; i < SPHERE_STACK; ++i) {
            alpha0 = (float) (-Math.PI / 2 + i * stackStep);
            alpha1 = alpha0 + stackStep;
            r0 = (float) (entry.radius() * Math.cos(alpha0));
            r1 = (float) (entry.radius() * Math.cos(alpha1));

            y0 = (float) (entry.radius() * Math.sin(alpha0));
            y1 = (float) (entry.radius() * Math.sin(alpha1));

            for (int j = 0; j < (SPHERE_SLICE << 1); ++j) {
                beta = j * sliceStep;
                x0 = (float) (r0 * Math.cos(beta));
                x1 = (float) (r1 * Math.cos(beta));

                z0 = (float) (-r0 * Math.sin(beta));
                z1 = (float) (-r1 * Math.sin(beta));

                buffer.pos(stack, x0, y0, z0)
                        .color(r, g, b, entry.alpha())
                        .endVertex();
                buffer.pos(stack, x1, y1, z1)
                        .color(r, g, b, entry.alpha())
                        .endVertex();
            }
        }

        buffer.drawDirect();

        stack.pop();

        // cleanup render
        RenderUtil.polygonOffset(0f, 0f);
        RenderUtil.disablePolygonOffset();
        UGraphics.disableBlend();
        RenderUtil.defaultBlendFunc();
        UGraphics.disableDepth();
        RenderUtil.enableCull();
        UGraphics.depthMask(true);
    }

    @Data
    @Accessors(fluent = true)
    private static final class VisualizeEntry {

        private final int color;
        private final float radius;
        private final @Nullable Vec3 position;

        private int alpha = 150;
        private long lastChanged = System.currentTimeMillis();
    }
}
