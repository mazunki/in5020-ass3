package com.ass3.p2p;

import java.util.Objects;

public class DataEntry {
    private final int key;
    private final Object value;

    public DataEntry(int key, Object value) {
        this.key = key;
        this.value = value;
    }

    public int getKey() {
        return key;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "DataEntry{key=" + key + ", value=" + value + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DataEntry)) return false;
        DataEntry dataEntry = (DataEntry) o;
        return key == dataEntry.key && Objects.equals(value, dataEntry.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, value);
    }
}

