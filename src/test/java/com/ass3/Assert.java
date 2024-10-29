package com.ass3;

public class Assert {
    public static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError("Assertion failed: " + message);
        }
    }

    public static void assertFalse(boolean condition, String message) {
        if (condition) {
            throw new AssertionError("Negative assertion failed: " + message);
        }
    }

    public static void assertEquals(Object expected, Object actual, String message) {
        if (!expected.equals(actual)) {
            throw new AssertionError("Assertion failed: " + message + ". Expected: " + expected + ", but got: " + actual);
        }
    }
}

