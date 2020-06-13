package com.redhat.app.order.status;

import com.google.gson.annotations.SerializedName;

public enum InventoryStatus {
    @SerializedName("NEW")
    NEW("New"),
    @SerializedName("UPDATED")
    UPDATED("Updated"),
    @SerializedName("REVERTED")
    REVERTED("Reverted"),
    @SerializedName("NO_STOCK")
    NO_STOCK("No Stock"),
    @SerializedName("CONFIRM")
    CONFIRMED("Confirmed");

    public final String label;

    private InventoryStatus(String label) {
        this.label = label;
    }
}