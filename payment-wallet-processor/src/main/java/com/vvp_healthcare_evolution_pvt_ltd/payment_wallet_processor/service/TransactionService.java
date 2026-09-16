package com.vvp_healthcare_evolution_pvt_ltd.payment_wallet_processor.service;

import com.vvp_healthcare_evolution_pvt_ltd.payment_wallet_processor.dto.TransactionRequest;
import com.vvp_healthcare_evolution_pvt_ltd.payment_wallet_processor.dto.TransactionResponse;
import com.vvp_healthcare_evolution_pvt_ltd.payment_wallet_processor.entity.Transaction;
import com.vvp_healthcare_evolution_pvt_ltd.payment_wallet_processor.entity.TransactionStatus;
import com.vvp_healthcare_evolution_pvt_ltd.payment_wallet_processor.entity.TransactionType;
import com.vvp_healthcare_evolution_pvt_ltd.payment_wallet_processor.entity.Wallet;
import com.vvp_healthcare_evolution_pvt_ltd.payment_wallet_processor.exception.DuplicateTransactionException;
import com.vvp_healthcare_evolution_pvt_ltd.payment_wallet_processor.exception.InsufficientFundsException;
import com.vvp_healthcare_evolution_pvt_ltd.payment_wallet_processor.exception.WalletNotFoundException;
import com.vvp_healthcare_evolution_pvt_ltd.payment_wallet_processor.repository.TransactionRepository;
import com.vvp_healthcare_evolution_pvt_ltd.payment_wallet_processor.repository.WalletRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;

    public TransactionService(TransactionRepository transactionRepository, WalletRepository walletRepository) {
        this.transactionRepository = transactionRepository;
        this.walletRepository = walletRepository;
    }

    @Transactional
    public TransactionResponse processTransaction(@Valid TransactionRequest request) {
        Wallet wallet = walletRepository.findByUserId(request.getUserId()).orElseThrow(() ->
                        new WalletNotFoundException("Wallet not found for user: " + request.getUserId()));

        var existingTransaction = transactionRepository.findByTransactionId(request.getTransactionId());

        if (existingTransaction.isPresent()) {
            throw new DuplicateTransactionException(
                    "Transaction already processed: "
                            + request.getTransactionId()
            );
        }

        BigDecimal newBalance;

        if(request.getType()== TransactionType.DEBIT){
            if (wallet.getBalance().compareTo(request.getAmount()) < 0) {
                throw new InsufficientFundsException("Insufficient funds");
            }
            newBalance= wallet.getBalance().subtract(request.getAmount());
        }else if(request.getType()==TransactionType.CREDIT){
            newBalance= wallet.getBalance().subtract(request.getAmount());
        }else{
            throw new IllegalArgumentException("Unsupported transaction type");
        }
        wallet.setBalance(newBalance);
        walletRepository.save(wallet);
        Transaction transaction=new Transaction();
        transaction.setTransactionId(request.getTransactionId());
        transaction.setAmount(request.getAmount());
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setType(request.getType());
        transaction.setUserId(request.getUserId());
        transactionRepository.save(transaction);
        TransactionResponse response = new TransactionResponse();
        response.setTransactionId(request.getTransactionId());
        response.setMessage("Transaction processed successfully");
        response.setStatus(TransactionStatus.SUCCESS);
        response.setRemainingBalance(newBalance);
        return response;
    }
}
