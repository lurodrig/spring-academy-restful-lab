package spring.academy.restful.web;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    // TODO-02: Review the code that performs the following
    // a. Respond to GET /accounts
    // b. Return a List<Account> to be converted to the response body
    // - Access http://localhost:8080/accounts using a browser or curl
    //   and verify that you see the list of accounts in JSON format.
    @GetMapping(value = "/accounts")
    public List<Account> accountSummary() {
        return accountManager.getAllAccounts();
    }

    /**
     * Provide the details of an account with the given id.
     */
    // TODO-04: Review the code that performs the following
    // a. Respond to GET /accounts/{accountId}
    // b. Return an Account to be converted to the response body
    // - Access http://localhost:8080/accounts/0 using a browser or curl
    //   and verify that you see the account detail in JSON format
    @GetMapping(value = "/accounts/{id}")
    public Account accountDetails(@PathVariable int id) {
        return retrieveAccount(id);
    }

    /**
     * Creates a new Account, setting its URL as the Location header on the
     * response.
     */
    // TODO-06: Complete this method. Add annotations to:
    // a. Respond to POST /accounts requests
    // b. Use a proper annotation for creating an Account object from the request
    @PostMapping(value = "/accounts")
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

        // TODO-07: Set the 'location' header on a Response to URI of
        //          the newly created resource and return it.
        // a. You will need to use 'ServletUriComponentsBuilder' and
        //     'ResponseEntity' to implement this - Use ResponseEntity.created(..)
        // b. Refer to the POST example in the slides for more information
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
        try {
            return new ResponseEntity<>(retrieveAccount(accountId).getBeneficiary(beneficiaryName),
                    HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Adds a Beneficiary with the given name to the Account with the given id,
     * setting its URL as the Location header on the response.
     */
    // TODO-10: Complete this method. Add annotations to:
    // a. Respond to a POST /accounts/{accountId}/beneficiaries
    // b. Extract a beneficiary name from the incoming request
    // c. Indicate a "201 Created" status
    @PostMapping(value = "/accounts/{accountId}/beneficiaries")
    public ResponseEntity<Void> addBeneficiary(@PathVariable Long accountId, @RequestBody String beneficiaryName) {

        // TODO-11: Create a ResponseEntity containing the location of the newly
        // created beneficiary.
        // a. Use accountManager's addBeneficiary method to add a beneficiary to an account
        // b. Use the entityWithLocation method - like we did for createAccount().
        accountManager.addBeneficiary(accountId, beneficiaryName);

        return entityWithLocation(beneficiaryName);  // Modify this to return something
    }

    /**
     * Removes the Beneficiary with the given name from the Account with the
     * given id.
     */
    // TODO-12: Complete this method by adding the appropriate annotations to:
    // a. Respond to a DELETE to /accounts/{accountId}/beneficiaries/{beneficiaryName}
    // b. Indicate a "204 No Content" status
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
        // just return empty 404
    }

    // TODO-17 (Optional): Add a new exception-handling method
    // - It should map DataIntegrityViolationException to a 409 Conflict status code.
    // - Use the handleNotFound method above for guidance.
    // - Consult the lab document for further instruction
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
