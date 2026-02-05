package top.ilov.mcmods.teamprojecte.integration.xaero;

import top.ilov.mcmods.teamprojecte.mixin.TPRMixinPlugin;
import lombok.Getter;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.capabilities.PECapabilities;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import xaero.hud.minimap.info.InfoDisplay;
import xaero.hud.minimap.info.InfoDisplay.Builder;
import xaero.hud.minimap.info.codec.InfoDisplayCommonStateCodecs;
import xaero.hud.minimap.info.widget.InfoDisplayCommonWidgetFactories;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Locale;

public class MinimapDisplays {

    private static final BigInteger THOUSAND = BigInteger.valueOf(1000);
    private static final BigInteger TWO = BigInteger.valueOf(2);
    private static final BigInteger RATE_HIGH_THRESHOLD = BigInteger.valueOf(1_000_000);

    private static BigInteger lastEmc = null;
    private static long lastSampleMs = 0;
    private static final BigInteger[] rateHistory = new BigInteger[]{BigInteger.ZERO, BigInteger.ZERO};

    public static final Builder<Boolean> PE_EMC_INFO_BUILDER;
    public static InfoDisplay<Boolean> PE_EMC;

    static {
        Minecraft mc = Minecraft.getInstance();
        Builder<Boolean> builder = Builder.begin();

        PE_EMC_INFO_BUILDER = builder
                .setId("teamper_pe_emc")
                .setName(Component.translatable("xaerominimap.teamprojecte_reborn.infodisplay.pe_emc"))
                .setDefaultState(true)
                .setCodec(InfoDisplayCommonStateCodecs.BOOLEAN)
                .setWidgetFactory(InfoDisplayCommonWidgetFactories.OFF_ON)
                .setCompiler((displayInfo, compiler, session, availableWidth, playerPos) -> {
                    if (!Boolean.TRUE.equals(displayInfo.getEffectiveState())) {
                        return;
                    }
                    if (TPRMixinPlugin.CONFIG == null || !TPRMixinPlugin.CONFIG.isEnableXaeroMinimapEMCDisplay()) {
                        return;
                    }
                    if (mc.player == null) {
                        return;
                    }

                    IKnowledgeProvider provider = mc.player.getCapability(PECapabilities.KNOWLEDGE_CAPABILITY);
                    if (provider == null) {
                        return;
                    }

                    BigInteger emc = provider.getEmc();
                    boolean showFull = TPRMixinPlugin.CONFIG.isEnableXaeroMinimapEMCDisplayHoldShiftShowFull() && Screen.hasShiftDown();
                    String emcText = showFull ? formatFull(emc) : formatShort(emc);

                    MutableComponent line = Component.literal("EMC: ")
                            .withStyle(ChatFormatting.GRAY)
                            .append(Component.literal(emcText).withStyle(ChatFormatting.LIGHT_PURPLE));

                    if (TPRMixinPlugin.CONFIG.isEnableXaeroMinimapEMCDisplayRate()) {
                        BigInteger rate = getAverageRatePerSecond(emc);
                        if (rate.signum() != 0) {
                            ChatFormatting rateColor = getRateColor(rate);
                            String rateValue = showFull ? formatFull(rate.abs()) : formatShort(rate.abs());
                            String rateText = (rate.signum() > 0 ? "+" : "-") + rateValue + "/s";
                            line.append(Component.literal(" "))
                                    .append(Component.literal(rateText).withStyle(rateColor));
                        }
                    }

                    compiler.addLine(line);
                });
    }

    private static BigInteger getAverageRatePerSecond(BigInteger currentEmc) {
        long now = System.currentTimeMillis();
        if (lastEmc == null) {
            lastEmc = currentEmc;
            lastSampleMs = now;
            rateHistory[0] = BigInteger.ZERO;
            rateHistory[1] = BigInteger.ZERO;
            return BigInteger.ZERO;
        }

        long dtMs = now - lastSampleMs;
        if (dtMs < 1000) {
            return rateHistory[0].add(rateHistory[1]).divide(TWO);
        }

        BigInteger dt = BigInteger.valueOf(dtMs);
        BigInteger delta = currentEmc.subtract(lastEmc);
        BigInteger rate = delta.multiply(THOUSAND).divide(dt);

        rateHistory[1] = rateHistory[0];
        rateHistory[0] = rate;
        lastEmc = currentEmc;
        lastSampleMs = now;

        return rateHistory[0].add(rateHistory[1]).divide(TWO);
    }

    private static ChatFormatting getRateColor(BigInteger rate) {
        int sign = rate.signum();
        BigInteger abs = rate.abs();
        if (sign > 0) {
            return abs.compareTo(RATE_HIGH_THRESHOLD) >= 0 ? ChatFormatting.GREEN : ChatFormatting.BLUE;
        } else if (sign < 0) {
            return abs.compareTo(RATE_HIGH_THRESHOLD) >= 0 ? ChatFormatting.RED : ChatFormatting.YELLOW;
        }
        return ChatFormatting.WHITE;
    }

    private static String formatFull(BigInteger value) {
        return NumberFormat.getNumberInstance(Locale.US).format(value);
    }

    private static String formatShort(BigInteger value) {
        if (value.signum() < 0) {
            return "-" + formatShort(value.negate());
        }
        if (value.compareTo(BigInteger.valueOf(1000)) < 0) {
            return value.toString();
        }

        BigDecimal decimal = new BigDecimal(value);
        NumberName name = NumberName.findName(decimal);

        BigDecimal scaled = decimal.divide(name.getBigDecimalValue(), 2, RoundingMode.DOWN);
        String s = scaled.stripTrailingZeros().toPlainString();
        return s + name.getShortName();
    }

    @Getter
    private enum NumberName {
        THOUSAND(1e3, "K"),
        MILLION(1e6, "M"),
        BILLION(1e9, "B"),
        TRILLION(1e12, "T"),
        QUADRILLION(1e15, "Qa"),
        QUINTILLION(1e18, "Qi"),
        SEXTILLION(1e21, "Sx"),
        SEPTILLION(1e24, "Sp"),
        OCTILLION(1e27, "O"),
        NONILLION(1e30, "N"),
        DECILLION(1e33, "D"),
        UNDECILLION(1e36, "U"),
        DUODECILLION(1e39, "Du"),
        TREDECILLION(1e42, "Tr"),
        QUATTUORDECILLION(1e45, "Qt"),
        QUINDECILLION(1e48, "Qd"),
        SEXDECILLION(1e51, "Sd"),
        SEPTENDECILLION(1e54, "St"),
        OCTODECILLION(1e57, "Oc"),
        NOVEMDECILLION(1e60, "No");

        private static final NumberName[] VALUES = values();

        private final BigDecimal bigDecimalValue;
        private final String shortName;

        NumberName(double value, String shortName) {
            this.bigDecimalValue = new BigDecimal(String.valueOf(value));
            this.shortName = shortName;
        }

        public static NumberName findName(BigDecimal value) {
            return Arrays.stream(VALUES)
                    .filter(v -> value.compareTo(v.getBigDecimalValue()) >= 0)
                    .reduce((first, second) -> second)
                    .orElse(null);
        }
    }

}
