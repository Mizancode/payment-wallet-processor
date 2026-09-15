package com.vvp_healthcare_evolution_pvt_ltd.payment_wallet_processor.exception;

public class WalletNotFoundException extends RuntimeException {

    public WalletNotFoundException(String message){
        super(message);
    }

    public WalletNotFoundException(String message,Throwable cause){
        super(message,cause);
    }
}
