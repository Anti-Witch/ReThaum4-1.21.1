package thaumcraft.init;

import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import thaumcraft.Thaumcraft;
import thaumcraft.common.lib.CommandThaumcraft;

@EventBusSubscriber(modid = Thaumcraft.MODID)
public class ModCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandThaumcraft.register(event.getDispatcher());
    }
}





