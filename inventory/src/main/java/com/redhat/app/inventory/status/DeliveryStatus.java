package com.redhat.app.inventory.status;

import com.google.gson.annotations.SerializedName;

public enum DeliveryStatus {
    @SerializedName("NEW")
    NEW("New"),
    @SerializedName("UPDATED")
    UPDATED("Updated"),
    @SerializedName("REVERTED")
    REVERTED("Reverted"),
    @SerializedName("NO_SCHEDULE")
    NO_SCHEDULE("No Schedule"),
    @SerializedName("IN_PROGRESS")
    IN_PROGRESS("Processing"),
    @SerializedName("ERROR_PROCESSING")
    ERROR_PROCESSING("Error Processing Order"),
    @SerializedName("CONFIRMED")
    CONFIRMED("Confirmed");

    public final String label;

    private DeliveryStatus(String label) {
        this.label = label;
    }
}