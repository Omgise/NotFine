package jss.notfine.core;

import cpw.mods.fml.client.FMLClientHandler;
import jss.notfine.NotFine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.MathHelper;

import java.io.*;

public class NotFineSettings {

    public static int minimumFarPlaneDistance = 4;

    private static File settingsFile;

    public static void loadSettings() {
        if (FMLClientHandler.instance().isLoading()) {
            return;
        }

        if(settingsFile == null) {
            Minecraft mc = Minecraft.getMinecraft();
            settingsFile = new File(mc.mcDataDir + "optionsGraphics.txt");
        }

        if(!settingsFile.exists()) {
            return;
        }


    }

    public static void saveSettings() {
        if(settingsFile == null) {
            return;
        }


    }

    private static void settingUpdated(Settings setting) {
        Minecraft mc = Minecraft.getMinecraft();
        switch(setting) {
            case CLOUD_HEIGHT:
            case MODE_CLOUDS:
            case RENDER_DISTANCE_CLOUDS:
                if(Settings.MODE_CLOUDS.value != 2f) {
                    minimumFarPlaneDistance = (int)(16f * Settings.RENDER_DISTANCE_CLOUDS.value);
                    minimumFarPlaneDistance += Math.abs(Settings.CLOUD_HEIGHT.value);
                    mc.gameSettings.clouds = true;
                } else {
                    minimumFarPlaneDistance = 128;
                    mc.gameSettings.clouds = false;
                }
                break;
            case MODE_LEAVES:
                mc.renderGlobal.loadRenderers();
                break;
            case TOTAL_STARS:
                //TODO: Reload stars.
                break;
        }
    }

    public enum Settings {
        CLOUD_HEIGHT(true, 128f, 64f, 384f, 16f),
        CLOUD_SCALE(true, 1f, 0.5f, 4f, 0.5f),
        //-1 default, 0 fancy, 1 fast, 2 off
        MODE_CLOUDS(false,-1f, -1f, 2f, 1f),
        //0 OFF, 1 ON
        MODE_GLEAM_INV(false,1f, 0f, 1f, 1f),
        //0 OFF, 1 ON
        MODE_GLEAM_WORLD(false,1f, 0f, 1f, 1f),
        //-1 default, 0 fancy, 1 fast, 2 smart, 3 fast hybrid, 4 fancy hybrid
        MODE_LEAVES(false,-1f, -1f, 4f,1f),
        //0 OFF, 1 ON
        MODE_SKY(false,1f, 0f, 1f, 1f),
        PARTICLES_ENC_TABLE(true,1f, 0f, 16f, 1f),
        RENDER_DISTANCE_CLOUDS(true, 4f, 4f, 64f, 2f),
        TOTAL_STARS(true, 1500f, 0f, 32000f, 500f);

        public final boolean slider;
        public final float base;
        public final float minimum;
        public final float maximum;
        public final float step;
        private float value;

        Settings(boolean slider, float base, float minimum, float maximum, float step) {
            this.slider = slider;
            this.base = base;
            this.minimum = minimum;
            this.maximum = maximum;
            this.step = step;
            value = base;
        }

        public void setValue(float value) {
            value = MathHelper.clamp_float(value, minimum, maximum);
            if(step > 0f) {
                value = step * (float)Math.round(value / step);
            }
            if(this.value != value) {
                this.value = value;
                settingUpdated(this);
            }
        }

        public void setValueNormalized(float value) {
            setValue(minimum + (maximum - minimum) * MathHelper.clamp_float(value, 0f, 1f));
        }

        public void incrementValue() {
            value += step;
            if(value > maximum) {
                value = minimum;
            }
            settingUpdated(this);
        }

        public float getValue() {
            return value;
        }

        public float getValueNormalized() {
            return MathHelper.clamp_float((value - minimum) / (maximum - minimum), 0f, 1f);
        }

        public boolean isValueBase() {
            return value == base;
        }

        public String getLocalization() {
            String localized = I18n.format("options." + name().toLowerCase()) + ": ";
            if(slider) {
                if(step % 1f == 0f) {
                    localized += (int)value;
                } else {
                    localized += value;
                }
            } else if(step == 1f && minimum == 0f && maximum == 1f) {
                if(value == 0f) {
                    localized += I18n.format("options.off");
                } else {
                    localized += I18n.format("options.on");
                }
           } else {
                localized += I18n.format("options." + name().toLowerCase() + '.' + (int)value);
            }
            return localized;
        }

        public static Settings getSettingFromOrdinal(int ordinal) {
            Settings[] options = values();
            if(ordinal >= options.length | ordinal < 0) {
                return null;
            } else {
                return options[ordinal];
            }
        }

    }

}
