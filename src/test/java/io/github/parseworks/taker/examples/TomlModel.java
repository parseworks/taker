package io.github.parseworks.taker.examples;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class TomlModel {

    private TomlModel() {
    }

    sealed interface TomlValue permits TomlString, TomlInteger, TomlFloat, TomlBoolean, TomlDate, TomlArray, TomlTable {
    }

    record TomlString(String value) implements TomlValue {
    }

    record TomlInteger(long value) implements TomlValue {
    }

    record TomlFloat(double value) implements TomlValue {
    }

    record TomlBoolean(boolean value) implements TomlValue {
    }

    record TomlDate(LocalDate value) implements TomlValue {
    }

    record TomlArray(List<TomlValue> items) implements TomlValue {
        TomlArray {
            items = List.copyOf(items);
        }
    }

    static final class TomlDocument {
        private final TomlTable root = new TomlTable();

        TomlValue get(String first, String... rest) {
            ArrayList<String> path = new ArrayList<>(1 + rest.length);
            path.add(first);
            path.addAll(List.of(rest));
            return root.get(path);
        }

        TomlTable table(List<String> path) {
            return root.table(path);
        }

        boolean canCreateTable(List<String> path) {
            return root.canCreateTable(path);
        }

        String put(List<String> path, TomlValue value) {
            return root.put(path, value);
        }
    }

    static final class TomlTable implements TomlValue {
        private final Map<String, TomlValue> fields = new LinkedHashMap<>();

        TomlValue get(String key) {
            return fields.get(key);
        }

        TomlValue get(List<String> path) {
            TomlTable current = this;
            for (int i = 0; i < path.size() - 1; i++) {
                TomlValue next = current.fields.get(path.get(i));
                if (!(next instanceof TomlTable table)) {
                    return null;
                }
                current = table;
            }
            return current.fields.get(path.getLast());
        }

        TomlTable table(List<String> path) {
            TomlTable current = this;
            for (String part : path) {
                TomlValue next = current.fields.get(part);
                if (next == null) {
                    TomlTable table = new TomlTable();
                    current.fields.put(part, table);
                    current = table;
                } else if (next instanceof TomlTable table) {
                    current = table;
                } else {
                    throw new IllegalArgumentException("TOML path conflicts with scalar key: " + part);
                }
            }
            return current;
        }

        boolean canCreateTable(List<String> path) {
            TomlTable current = this;
            for (String part : path) {
                TomlValue next = current.fields.get(part);
                if (next == null) {
                    return true;
                }
                if (!(next instanceof TomlTable table)) {
                    return false;
                }
                current = table;
            }
            return true;
        }

        String put(List<String> path, TomlValue value) {
            if (!canCreateTable(path.subList(0, path.size() - 1))) {
                return String.join(".", path);
            }
            TomlTable table = table(path.subList(0, path.size() - 1));
            String key = path.getLast();
            if (table.fields.containsKey(key)) {
                return String.join(".", path);
            }
            table.fields.put(key, value);
            return null;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof TomlTable table && fields.equals(table.fields);
        }

        @Override
        public int hashCode() {
            return fields.hashCode();
        }

        @Override
        public String toString() {
            return fields.toString();
        }
    }
}
