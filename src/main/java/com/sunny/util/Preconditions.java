package com.sunny.util;

import javax.annotation.Nullable;

public class Preconditions {

    public static void main(String[] args) {
        checkArgument(false);
    }

    public static <T> T checkNotNull(T reference) {
        if (reference == null) {
            throw new NullPointerException();
        }
        return reference;
    }

    public static <T> T checkNotNull(T reference, @Nullable String errorMessage) {
        if (reference == null) {
            throw new NullPointerException(String.valueOf(errorMessage));
        }
        return reference;
    }

    public static <T> T checkNotNull(T reference,
                                     @Nullable String errorMessageTemplate,
                                     @Nullable Object... errorMessageArgs) {

        if (reference == null) {
            throw new NullPointerException(format(errorMessageTemplate, errorMessageArgs));
        }
        return reference;
    }

    // ------------------------------------------------------------------------
    //  Boolean Condition Checking (Argument)
    // ------------------------------------------------------------------------
    public static void checkArgument(boolean condition) {
        if (!condition) {
            throw new IllegalArgumentException();
        }
    }

    public static void checkArgument(boolean condition, @Nullable Object errorMessage) {
        if (!condition) {
            throw new IllegalArgumentException(String.valueOf(errorMessage));
        }
    }

    public static void checkArgument(boolean condition,
                                     @Nullable String errorMessageTemplate,
                                     @Nullable Object... errorMessageArgs) {

        if (!condition) {
            throw new IllegalArgumentException(format(errorMessageTemplate, errorMessageArgs));
        }
    }

    // ------------------------------------------------------------------------
    //  Boolean Condition Checking (State)
    // ------------------------------------------------------------------------

    public static void checkState(boolean condition) {
        if (!condition) {
            throw new IllegalStateException();
        }
    }

    public static void checkState(boolean condition, @Nullable Object errorMessage) {
        if (!condition) {
            throw new IllegalStateException(String.valueOf(errorMessage));
        }
    }

    public static void checkState(boolean condition,
                                  @Nullable String errorMessageTemplate,
                                  @Nullable Object... errorMessageArgs) {

        if (!condition) {
            throw new IllegalStateException(format(errorMessageTemplate, errorMessageArgs));
        }
    }

    public static void checkElementIndex(int index, int size) {
        checkArgument(size >= 0, "Size was negative.");
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
    }

    public static void checkElementIndex(int index, int size, @Nullable String errorMessage) {
        checkArgument(size >= 0, "Size was negative.");
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException(String.valueOf(errorMessage) + " Index: " + index + ", Size: " + size);
        }
    }

    // ------------------------------------------------------------------------
    //  Utilities
    // ------------------------------------------------------------------------

    private static String format(@Nullable String template, @Nullable Object... args) {
        final int numArgs = args == null ? 0 : args.length;
        template = String.valueOf(template); // null -> "null"

        // start substituting the arguments into the '%s' placeholders
        StringBuilder builder = new StringBuilder(template.length() + 16 * numArgs);
        int templateStart = 0;
        int i = 0;
        while (i < numArgs) {
            int placeholderStart = template.indexOf("%s", templateStart);
            if (placeholderStart == -1) {
                break;
            }
            builder.append(template.substring(templateStart, placeholderStart));
            builder.append(args[i++]);
            templateStart = placeholderStart + 2;
        }
        builder.append(template.substring(templateStart));

        // if we run out of placeholders, append the extra args in square braces
        if (i < numArgs) {
            builder.append(" [");
            builder.append(args[i++]);
            while (i < numArgs) {
                builder.append(", ");
                builder.append(args[i++]);
            }
            builder.append(']');
        }

        return builder.toString();
    }

    // ------------------------------------------------------------------------

    /** Private constructor to prevent instantiation. */
    private Preconditions() {}
}
