package com.redhat.app.order.status;

import com.google.gson.annotations.SerializedName;

public enum TransactionStatus {
    @SerializedName("NEW")
    NEW("new"),
    @SerializedName("CANCELLED")
    CANCELLED("Cancelled"),
    @SerializedName("COMPLETED")
    COMPLETED("Completed");

    public final String label;

    private TransactionStatus(String label){

        this.label = label;
    }
}