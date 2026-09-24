package com.fincore.ledger.api;

import com.fincore.ledger.domain.Account;
import com.fincore.ledger.domain.AccountType;
import com.fincore.ledger.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API for account management. This is the first controller in the
 * project — it gives the ledger an HTTP surface so other services (and
 * later the BFF) can interact with it without sharing the database.
 *
 * <p>Key Spring concepts at work here:
 * <ul>
 *   <li>{@code @RestController} = {@code @Controller} + {@code @ResponseBody}:
 *       every method return value is serialized to JSON automatically.</li>
 *   <li>{@code @Valid} on the request body triggers Bean Validation
 *       ({@code @NotBlank}, etc.) before your code runs.</li>
 *   <li>{@code @ResponseStatus} sets the HTTP status code on success.</li>
 * </ul>
 */
@RestController
@RequestMapping("/v1/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse createAccount(@Valid @RequestBody CreateAccountRequest request) {
        AccountType type = AccountType.valueOf(request.accountType().toUpperCase());
        Account account = accountService.createAccount(request.accountRef(), type, request.currency().toUpperCase());
        return AccountResponse.from(account);
    }

    @GetMapping("/{accountRef}/balance")
    public BalanceResponse getBalance(@PathVariable String accountRef) {
        Account account = accountService.getAccount(accountRef);
        return new BalanceResponse(
                accountRef,
                accountService.getBalance(accountRef),
                account.getCurrency()
        );
    }
}
