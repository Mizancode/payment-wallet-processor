package com.vvp_healthcare_evolution_pvt_ltd.payment_wallet_processor.service;

import com.vvp_healthcare_evolution_pvt_ltd.payment_wallet_processor.dto.TransactionRequest;
import com.vvp_healthcare_evolution_pvt_ltd.payment_wallet_processor.dto.TransactionResponse;
import com.vvp_healthcare_evolution_pvt_ltd.payment_wallet_processor.entity.Transaction;
import com.vvp_healthcare_evolution_pvt_ltd.payment_wallet_processor.entity.TransactionStatus;
import com.vvp_healthcare_evolution_pvt_ltd.payment_wallet_processor.entity.TransactionType;
import com.vvp_healthcare_evolution_pvt_ltd.payment_wallet_processor.entity.Wallet;
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
        var existingTransaction = transactionRepository.findByTransactionId(request.getTransactionId());
        if (existingTransaction.isPresent()) {
            TransactionResponse response = new TransactionResponse();
            response.setTransactionId(request.getTransactionId());
            response.setMessage("Transaction already processed");
            response.setStatus(existingTransaction.get().getStatus());
            response.setRemainingBalance(null);
            return response;
        }
        Wallet wallet = walletRepository.findByUserId(request.getUserId())
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found for user: " + request.getUserId()));
        if(request.getType()== TransactionType.DEBIT){
            if (wallet.getBalance().compareTo(request.getAmount()) < 0) {
                throw new InsufficientFundsException("Insufficient funds");
            }
            BigDecimal newBalance = wallet.getBalance().subtract(request.getAmount());
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

        if(request.getType()==TransactionType.CREDIT){
            BigDecimal newBalance = wallet.getBalance().add(request.getAmount());
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
        throw new IllegalArgumentException("Unsupported transaction type");
    }
}
