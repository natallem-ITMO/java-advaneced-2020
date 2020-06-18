package ru.ifmo.rain.lemeshkova.hello;

import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.function.Predicate;

public class HelloUtils {

    public static final Charset CHARSET = StandardCharsets.UTF_8;

    public static int parseIntArgument(String[] args, int i, String expected) {
        final String ERROR_PREFIX = "HelloUtils";
        try {
            int result = Integer.parseInt(args[i]);
            if (result <= 0) {
                throw HelloUtils.error(ERROR_PREFIX, String.format(
                        "Invalid argument №%d. Expected positive integer for %s, found %d", i + 1, expected, result));
            }
            return result;
        } catch (NumberFormatException ex) {
            throw HelloUtils.error(ERROR_PREFIX, String.format("Invalid argument №%d. Expected %s, found %s", i + 1,
                    expected, args[i]), ex);
        }
    }

    public static HelloUDPException error(String where, String message, Throwable cause) {
        return new HelloUDPException("{" + where + "}" + message, cause);
    }

    public static HelloUDPException error(String where, String message) {
        return new HelloUDPException("{" + where + "}" + message);
    }

    public static void addPrefix(byte[] buff, byte[] prefix) {
        for (int i = 0; i < prefix.length; i++) {
            buff[i] = prefix[i];
        }
    }

    public static void shiftData(byte[] data, int shift, int dataSize) {
        for (int i = Integer.min(data.length - shift, dataSize); i >= 0; i--) {
            data[i + shift] = data[i];
        }
    }

    public static void putNumber(int number, ByteBuffer buffer) {
        int ten = 1;
        while (ten * 10 <= number) {
            ten *= 10;
        }
        while (ten > 0) {
            buffer.put((byte) ('0' + number / ten));
            number %= ten;
            ten /= 10;
        }
    }

    public static void printBuffer(PrintStream printStream, ByteBuffer buffer) {
        for (int i = 0; i < buffer.limit(); i++) {
            printStream.print((char) buffer.get(i));
        }
    }

    public static boolean checkNumberInBuffer(byte[] buffer, int l, int r, int number) {
        if (numberLength(number) != r - l) {
            return false;
        }
        for (int i = r - 1; i >= l; i--) {
            if ((byte) '0' + number % 10 != buffer[i]) {
                return false;
            }
            number /= 10;
        }
        return true;
    }

    public static boolean checkValidByteArrayResponse(byte[] array, int threadNum, int requestNum, int length) {
        int endPosition = checkFirstNumber(array, 0, threadNum, length);
        if (endPosition == -1) {
            return false;
        }
        endPosition = checkFirstNumber(array, endPosition, requestNum, length);
        if (endPosition == -1) {
            return false;
        }
        return findFirstPosition(array, endPosition, Character::isDigit, length) == length;
    }

    private static int numberLength(int number) {
        if (number == 0) return 1;
        int length = 0;
        long temp = 1;
        while (temp <= number) {
            length++;
            temp *= 10;
        }
        return length;
    }

    private static int checkFirstNumber(byte[] array, int startPosition, int numberToCompare, int length) {
        int l = findFirstPosition(array, startPosition, Character::isDigit, length);
        int r = findFirstPosition(array, l, Predicate.not(Character::isDigit), length);
        return (checkNumberInBuffer(array, l, r, numberToCompare)) ? r : -1;
    }

    private static int findFirstPosition(byte[] array, int position, Predicate<Byte> predicate, int length) {
        while (position < length && !predicate.test(array[position])) {
            ++position;
        }
        return position;
    }
}
