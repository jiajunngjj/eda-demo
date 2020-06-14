package com.redhat.app.inventory.status;

import com.google.gson.annotations.SerializedName;

public enum OrderStatus {
    @SerializedName("NEW")
    NEW("New"),
    @SerializedName("IN_PROGRESS")
    IN_PROGRESS("In Progress"),
    @SerializedName("CANCELLED")
    CANCELLED("Cancelled"),
    @SerializedName("CANCELLED_TIMEOUT")
    CANCELLED_TIMEOUT("Timeout"),
    @SerializedName("CANCELLED_OUT_OF_STOCK")
    CANCELLED_NO_STOCK("Out Of Stock"),
    @SerializedName("CANCELLING")
    CANCELLING("Cancelling"),
    @SerializedName("ERROR_PROCESSING")
    ERROR_PROCESSING("Error Processing Order"),    
    @SerializedName("CONFIRMED")
    CONFIRMED("Confirmed");
    public final String label;

    private OrderStatus(String label) {
        this.label = label;
    }
}