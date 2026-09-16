package com.vvp_healthcare_evolution_pvt_ltd.payment_wallet_processor.exception;

public class DuplicateTransactionException extends RuntimeException{

    public DuplicateTransactionException(String message) {
        super(message);
    }
}
