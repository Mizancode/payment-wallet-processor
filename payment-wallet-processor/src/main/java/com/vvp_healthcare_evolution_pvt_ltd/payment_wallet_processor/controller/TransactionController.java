package com.vvp_healthcare_evolution_pvt_ltd.payment_wallet_processor.controller;

import com.vvp_healthcare_evolution_pvt_ltd.payment_wallet_processor.dto.TransactionRequest;
import com.vvp_healthcare_evolution_pvt_ltd.payment_wallet_processor.dto.TransactionResponse;
import com.vvp_healthcare_evolution_pvt_ltd.payment_wallet_processor.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/transactions")
public class TransactionController {

    private final TransactionService service;

    public TransactionController(TransactionService service) {
        this.service = service;
    }

    @PostMapping("/process")
    public ResponseEntity<TransactionResponse> processTransaction(@Valid @RequestBody TransactionRequest request){
        return ResponseEntity.ok(service.processTransaction(request));
    }
}
