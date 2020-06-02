package com.redhat.app.inventory;

public class InventoryException extends Exception {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
    
    public InventoryException(String message){
        super(message);
    }
}