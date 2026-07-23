package de.destenylp.xBotenyy.discordbot.util;

import java.util.function.Consumer;

public sealed interface FieldEdit<T> {
    record NotProvided<T>() implements FieldEdit<T> {
    }

    record Clear<T>() implements FieldEdit<T> {
    }

    record Value<T>(T value) implements FieldEdit<T> {
    }

    static <T> FieldEdit<T> notProvided() {
        return new NotProvided<>();
    }

    static <T> FieldEdit<T> clear() {
        return new Clear<>();
    }

    static <T> FieldEdit<T> value(T value) {
        return new Value<>(value);
    }

    default void applyTo(Consumer<T> setter) {
        switch (this) {
            case NotProvided<T> ignored -> {
            }
            case Clear<T> ignored -> setter.accept(null);
            case Value<T> provided -> setter.accept(provided.value());
        }
    }
}
