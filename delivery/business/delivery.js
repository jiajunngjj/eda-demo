function process(event) {

    console.log("got it "+event);
    var order = JSON.parse(event);
    console.log("got it "+order.deliveryStatus);
    if (order.deliveryStatus == DeliveryStatus.NEW.value) {
        console.log("NEW EVENT");
        order.deliveryStatus = DeliveryStatus.IN_PROGRESS.value;
    }
    console.log("returning "+order);
    return order;
}



const DeliveryStatus = Object.freeze({
    NEW : { label: "New", value:"NEW" },
    CONFIRMED: { label: "Confirmed", value: "CONFIRMED" },
    UPDATED : { label: "Updated", value: "UPDATED" },
    REVERTED : { label: "Reverted", value: "REVERTED"},
    NO_SCHEDULE : { label: "No Schedule", value: "NO_SCHEDULE"},
    IN_PROGRESS : { label: "Processing", value: "IN_PROGRESS"},
    ERROR_PROCESSING : {label: "Error Processing Order", value: "ERROR_PROCESSING" }   
  });
exports.process = process;
exports.DeliveryStatus = DeliveryStatus;