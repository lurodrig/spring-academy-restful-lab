package spring.academy.restful.web;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import spring.academy.restful.accounts.AccountManager;
import spring.academy.restful.common.money.Percentage;
import spring.academy.restful.rewards.internal.account.Account;
import spring.academy.restful.rewards.internal.account.Beneficiary;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * A controller handling requests for CRUD operations on Accounts and their
 * Beneficiaries.
 */
@RestController
public class AccountController {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private AccountManager accountManager;

    /**
     * Creates a new AccountController with a given account manager.
     */
    public AccountController(AccountManager accountManager) {
        this.accountManager = accountManager;
    }

    /**
     * Provide a list of all accounts.
     */
    @GetMapping(value = "/accounts")
    public List<Account> accountSummary() {
        return accountManager.getAllAccounts();
    }

    /**
     * Provide the details of an account with the given id.
     */
    @PostAuthorize("returnObject.name == authentication.name")
    @GetMapping(value = "/accounts/{id}")
    public Account accountDetails(@PathVariable int id) {
        return retrieveAccount(id);
    }

    /**
     * Creates a new Account, setting its URL as the Location header on the
     * response.
     */
    @PostMapping(value = "/accounts")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<Void> createAccount(@RequestBody Account newAccount) {
        // Saving the account also sets its entity Id
        Account account = accountManager.save(newAccount);

        // Return a ResponseEntity - it will be used to build the
        // HttpServletResponse.
        return entityWithLocation(account.getEntityId());
    }

    @DeleteMapping(value = "/accounts/{accountId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeAccount(@PathVariable Long accountId) {
        accountManager.removeAccount(accountId);
    }

    /**
     * Return a response with the location of the new resource.
     * <p>
     * Suppose we have just received an incoming URL of, say,
     * http://localhost:8080/accounts and resourceId is "1111".
     * Then the URL of the new resource will be
     * http://localhost:8080/accounts/1111.
     */
    private ResponseEntity<Void> entityWithLocation(Object resourceId) {

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{resourceId}")
                .buildAndExpand(resourceId).toUri();
        return ResponseEntity.created(location).build();
    }

    /**
     * Returns the Beneficiary with the given name for the Account with the
     * given id.
     */
    @GetMapping(value = "/accounts/{accountId}/beneficiaries/{beneficiaryName}")
    public ResponseEntity<Object> getBeneficiary(@PathVariable("accountId") int accountId,
                                                 @PathVariable("beneficiaryName") String beneficiaryName) {
        return new ResponseEntity<>(retrieveAccount(accountId).getBeneficiary(beneficiaryName),HttpStatus.OK);
    }

    /**
     * Adds a Beneficiary with the given name to the Account with the given id,
     * setting its URL as the Location header on the response.
     */
    @PostMapping(value = "/accounts/{accountId}/beneficiaries")
    public ResponseEntity<Void> addBeneficiary(@PathVariable Long accountId, @RequestBody String beneficiaryName) {

        accountManager.addBeneficiary(accountId, beneficiaryName);

        return entityWithLocation(beneficiaryName);  // Modify this to return something
    }

    /**
     * Removes the Beneficiary with the given name from the Account with the
     * given id.
     */
    @DeleteMapping(value = "/accounts/{accountId}/beneficiaries/{beneficiaryName}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeBeneficiary(@PathVariable Long accountId, @PathVariable String beneficiaryName) {
        Account account = accountManager.getAccount(accountId);
        if (account == null) {
            throw new IllegalArgumentException("No such account with id " + accountId);
        }
        Beneficiary b = account.getBeneficiary(beneficiaryName);

        // If there are more than one beneficiary and the one that we are deleting has one that is not 0,
        // we need to reset the allocation percentages.
        if (account.getBeneficiaries().size() != 1 && (!b.getAllocationPercentage().equals(Percentage.zero()))) {
            resetAllocationPercentages(accountId, beneficiaryName, account, b);
        }

        accountManager.removeBeneficiary(accountId, beneficiaryName, new HashMap<String, Percentage>());
    }

    @PreAuthorize("@authz.isOwnerOfTheAccount(#accountId)")
    @PutMapping(value = "/accounts/{accountId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateBeneficiaryAllocationPercentages(@RequestBody Map<String, Percentage> allocationPercentages,
                                                       @PathVariable Long accountId) {
        accountManager.updateBeneficiaryAllocationPercentages(accountId, allocationPercentages);
    }

    /**
     * Maps IllegalArgumentExceptions to a 404 Not Found HTTP status code.
     */
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler({IllegalArgumentException.class})
    public void handleNotFound(Exception ex) {
        logger.error("Exception is: ", ex);
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler({DataIntegrityViolationException.class})
    public void handleConflict(Exception ex) {
        logger.error("Exception is: ", ex);
    }


    /**
     * Finds the Account with the given id, throwing an IllegalArgumentException
     * if there is no such Account.
     */
    private Account retrieveAccount(long accountId) throws IllegalArgumentException {
        Account account = accountManager.getAccount(accountId);
        if (account == null) {
            throw new IllegalArgumentException("No such account with id " + accountId);
        }
        return account;
    }

    private void resetAllocationPercentages(Long accountId, String beneficiaryName, Account account, Beneficiary b) {
        int newNumberOfBeneficiaries = account.getBeneficiaries().size() - 1;
        BigDecimal removedAllocationPercentage = b.getAllocationPercentage().asBigDecimal();
        BigDecimal allocationAmountToDistribute = removedAllocationPercentage
                .divide(BigDecimal.valueOf(newNumberOfBeneficiaries), RoundingMode.DOWN);
        Map<String, Percentage> allocationPercentages = new HashMap<String, Percentage>();
        BigDecimal totalOfRebalancedBeneficiariesPercentage = new BigDecimal(0);
        for (Beneficiary beneficiary : account.getBeneficiaries()) {
            if (!beneficiaryName.equals(beneficiary.getName())) {
                Percentage newPercentage = beneficiary.getAllocationPercentage()
                        .add(Percentage.valueOf(allocationAmountToDistribute.toString()));
                allocationPercentages.put(beneficiary.getName(), newPercentage);
                totalOfRebalancedBeneficiariesPercentage = totalOfRebalancedBeneficiariesPercentage.add(newPercentage.asBigDecimal());
            }
        }
        String randomName = (String) allocationPercentages.keySet().toArray()[new Random().nextInt(allocationPercentages.keySet().toArray().length)];
        allocationPercentages.put(randomName, allocationPercentages.get(randomName)
                .add(new Percentage(BigDecimal.valueOf(1L).subtract(totalOfRebalancedBeneficiariesPercentage))));
        accountManager.updateBeneficiaryAllocationPercentages(accountId, allocationPercentages);
    }

}
