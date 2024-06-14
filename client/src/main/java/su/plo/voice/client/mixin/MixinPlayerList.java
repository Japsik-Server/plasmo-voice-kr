package su.plo.voice.client.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import su.plo.lib.mod.server.ModServerLib;

//#if MC>=12100
//$$ import net.minecraft.world.entity.Entity;
//#endif

@Mixin(PlayerList.class)
public abstract class MixinPlayerList {

    @Inject(method = "respawn", at = @At("RETURN"))
    //#if MC>=12100
    //$$ private void onRespawn(ServerPlayer serverPlayer, boolean bl, Entity.RemovalReason removalReason, CallbackInfoReturnable<ServerPlayer> cir) {
    //#else
    private void onRespawn(ServerPlayer serverPlayer, boolean bl, CallbackInfoReturnable<ServerPlayer> cir) {
        //#endif
        ServerPlayer newPlayer = cir.getReturnValue();
        ModServerLib.INSTANCE.getPlayerByInstance(newPlayer);
    }
}
