package com.vvp_healthcare_evolution_pvt_ltd.payment_wallet_processor.exception;

public class InsufficientFundsException extends RuntimeException {

    public InsufficientFundsException(String message){
        super(message);
    }

    public InsufficientFundsException(String message,Throwable cause){
        super(message,cause);
    }
}
