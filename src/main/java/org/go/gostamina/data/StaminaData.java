package org.go.gostamina.data;

import java.util.UUID;

public final class StaminaData {
    private final UUID uuid;
    private int currentStamina;
    private int baseMaxStamina;
    private int bonusMaxStamina;
    private boolean dirty;

    public StaminaData(UUID uuid, int currentStamina, int baseMaxStamina, int bonusMaxStamina) {
        this.uuid = uuid;
        this.baseMaxStamina = Math.max(0, baseMaxStamina);
        this.bonusMaxStamina = bonusMaxStamina;
        setCurrentStamina(currentStamina);
    }

    public UUID uuid() { return uuid; }
    public int currentStamina() { return currentStamina; }
    public int baseMaxStamina() { return baseMaxStamina; }
    public int bonusMaxStamina() { return bonusMaxStamina; }
    public int maximumStamina() { return Math.max(0, baseMaxStamina + bonusMaxStamina); }
    public boolean dirty() { return dirty; }
    public void clearDirty() { dirty = false; }

    public void setCurrentStamina(int value) {
        currentStamina = Math.max(0, Math.min(value, maximumStamina()));
        dirty = true;
    }

    public void addCurrentStamina(int amount) { setCurrentStamina(currentStamina + amount); }

    public void setBaseMaxStamina(int value) {
        baseMaxStamina = Math.max(0, value);
        setCurrentStamina(currentStamina);
        dirty = true;
    }

    public void addBonusMaxStamina(int amount) {
        bonusMaxStamina += amount;
        setCurrentStamina(currentStamina);
        dirty = true;
    }
}
