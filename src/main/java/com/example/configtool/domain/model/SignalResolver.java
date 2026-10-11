package com.example.configtool.domain.model;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Compatibility adapter that infers peripheral ownership from legacy function names.
 * Raw identifiers remain unchanged in processor definition files. New processor definitions should
 * prefer explicit peripheral metadata because naming conventions cannot describe every signal type.
 */
public final class SignalResolver {

    private static final Pattern INDEXED_SIGNAL = Pattern.compile(
            "^(LPUART|CAN|SPI|ADC|I2C|TIMER|PWM)(\\d+)_(.+)$");
    private static final Pattern FLEXIO = Pattern.compile("^FLEXIO_D\\d+$");
    private static final Pattern EXTERNAL_INTERRUPT = Pattern.compile("^EXT_INT\\d+$");
    private static final Pattern TRACE = Pattern.compile("^TRACE_.+$");

    private SignalResolver() {
    }

    /**
     * @param function raw pin-function identifier
     * @return inferred signal metadata, or empty when no legacy convention matches
     */
    public static Optional<SignalDescriptor> resolve(String function) {
        if (function == null || function.isBlank() || "GPIO".equals(function)) {
            return Optional.empty();
        }

        Matcher matcher = INDEXED_SIGNAL.matcher(function);
        if (matcher.matches()) {
            String prefix = matcher.group(1);
            String index = matcher.group(2);
            PeripheralCategory category = switch (prefix) {
                case "LPUART" -> PeripheralCategory.UART;
                case "CAN" -> PeripheralCategory.CAN;
                case "SPI" -> PeripheralCategory.SPI;
                case "ADC" -> PeripheralCategory.ADC;
                case "I2C" -> PeripheralCategory.I2C;
                case "TIMER" -> PeripheralCategory.TIMER;
                case "PWM" -> PeripheralCategory.PWM;
                default -> PeripheralCategory.OTHER;
            };
            return Optional.of(new SignalDescriptor(prefix + index, category, function));
        }

        if ("I2C_SCL".equals(function)) {
            return Optional.of(new SignalDescriptor("I2C0", PeripheralCategory.I2C, function));
        }
        if (function.startsWith("ETH_")) {
            return Optional.of(new SignalDescriptor("ETHERNET0", PeripheralCategory.ETHERNET, function));
        }
        if (FLEXIO.matcher(function).matches()) {
            return Optional.of(new SignalDescriptor("FLEXIO0", PeripheralCategory.FLEXIO, function));
        }
        if (EXTERNAL_INTERRUPT.matcher(function).matches()) {
            return Optional.of(new SignalDescriptor("EXTINT0", PeripheralCategory.EXT_INTERRUPT, function));
        }
        if (TRACE.matcher(function).matches()) {
            return Optional.of(new SignalDescriptor("TRACE0", PeripheralCategory.TRACE, function));
        }
        return Optional.empty();
    }
}
