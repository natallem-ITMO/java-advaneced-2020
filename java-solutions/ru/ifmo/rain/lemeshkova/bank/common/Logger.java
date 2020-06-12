package ru.ifmo.rain.lemeshkova.bank.common;

import java.io.PrintStream;

public class Logger {

    private static final PrintStream OUT_STREAM = System.out;

    private static final boolean SILENT = false;

    private static final boolean SILENT_ADDITIONAL = true;

    private static final PrintStream ERR_STREAM = System.err;

    private static final boolean PRINT_STACK_TRACE = false;

    public static void info(final String... message) {
        if (!SILENT) {
            OUT_STREAM.println(String.join(" ", message));
        }
    }

    public static void additionalInfo(final String... message) {
        if (!SILENT_ADDITIONAL) {
            OUT_STREAM.println(String.join(" ", message));
        }
    }

    public static void error(final Exception e, final String... message) {
        if (!SILENT) {
            if (PRINT_STACK_TRACE) {
                e.printStackTrace();
            }
            String errorMessage = String.join(" ", message);
            if (!errorMessage.isBlank()) {
                errorMessage += ": ";
            }
            ERR_STREAM.println(errorMessage + e.getMessage());
        }
    }

}
