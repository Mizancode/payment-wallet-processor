package com.vvp_healthcare_evolution_pvt_ltd.payment_wallet_processor.exception;

public class DataIntegrityViolationException extends RuntimeException{

    public DataIntegrityViolationException(String message){
        super(message);
    }

    public DataIntegrityViolationException(String message,Throwable cause){
        super(message,cause);
    }
}
