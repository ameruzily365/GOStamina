package org.go.gostamina.config;

public record ActionSettings(boolean enabled, ActionType type, int amount, int intervalSeconds) {
    public enum ActionType { INSTANT, CONTINUOUS }
}
