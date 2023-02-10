package jss.notfine;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.relauncher.Side;
import jss.notfine.core.LoadNotFineMenuButtons;
import jss.notfine.core.NotFineSettings;
import net.minecraftforge.common.MinecraftForge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(
    modid = NotFine.MODID,
    name = NotFine.NAME,
    version = NotFine.VERSION,
    acceptableRemoteVersions = "*"
)
public class NotFine {
    public static final String MODID = "notfine";
    public static final String NAME = "NotFine";
    public static final String VERSION = "GRADLETOKEN_VERSION";
    public static final Logger logger = LogManager.getLogger(NotFine.MODID);

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        //No mcmod.info? Shut.
        event.getModMetadata().autogenerated = false;

        if(event.getSide() == Side.CLIENT) {
            NotFineSettings.loadSettings();

            FMLCommonHandler.instance().bus().register(LoadNotFineMenuButtons.INSTANCE);
            MinecraftForge.EVENT_BUS.register(LoadNotFineMenuButtons.INSTANCE);
        }
    }

}
