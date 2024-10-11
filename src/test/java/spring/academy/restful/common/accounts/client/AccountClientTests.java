package spring.academy.restful.common.accounts.client;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import spring.academy.restful.common.money.Percentage;
import spring.academy.restful.rewards.internal.account.Account;
import spring.academy.restful.rewards.internal.account.Beneficiary;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AccountClientTests {

    private static final String BASE_URL = "http://localhost:8080";

    private final RestTemplate restTemplate = new RestTemplate();
    private final Random random = new Random();

    @Test
//	@Disabled
    public void listAccounts() {
        // TODO-03: Run this test
        // - Remove the @Disabled on this test method.
        // - Then, use the restTemplate to retrieve an array containing all Account instances.
        // - Use BASE_URL to help define the URL you need: BASE_URL + "/..."
        // - Run the test and ensure that it passes.

        Account[] accounts = restTemplate.getForObject(BASE_URL + "/accounts", Account[].class);

        assertNotNull(accounts);
        assertTrue(accounts.length >= 21);
        assertEquals("Keith and Keri Donald", accounts[0].getName());
        assertEquals(2, accounts[0].getBeneficiaries().size());
        assertEquals(Percentage.valueOf("50%"), accounts[0].getBeneficiary("Annabelle").getAllocationPercentage());
    }

    @Test
//	@Disabled
    public void getAccount() {
        // TODO-05: Run this test
        // - Remove the @Disabled on this test method.
        // - Then, use the restTemplate to retrieve the Account with id 0 using a URI template
        // - Run the test and ensure that it passes.
        Account account = restTemplate.getForObject(BASE_URL + "/accounts/{accountId}", Account.class, 0); // Modify this line to use the restTemplate

        assertNotNull(account);
        assertEquals("Keith and Keri Donald", account.getName());
        assertEquals(2, account.getBeneficiaries().size());
        assertEquals(Percentage.valueOf("50%"), account.getBeneficiary("Annabelle").getAllocationPercentage());
        assertEquals(Percentage.valueOf("50%"), account.getBeneficiary("Corgan").getAllocationPercentage());

    }

    @Test
//	@Disabled
    public void createAccount() {
        // Use a unique number to avoid conflicts
        String number = String.format("12345%4d", random.nextInt(10000));
        Account account = new Account(number, "John Doe");
        account.addBeneficiary("Jane Doe");

        //	TODO-08: Create a new Account
        //	- Remove the @Disabled on this test method.
        //	- Create a new Account by POSTing to the right URL and
        //    store its location in a variable
        //  - Note that 'RestTemplate' has two methods for this.
        //  - Use the one that returns the location of the newly created
        //    resource and assign that to a variable.
        callCreateAccount(account);
    }

    private URI callCreateAccount(Account account) {
        URI newAccountLocation = restTemplate.postForLocation(BASE_URL + "/accounts", account);
        assertNotNull(newAccountLocation);
        //	TODO-09: Retrieve the Account you just created from
        //	         the location that was returned.
        //	- Run this test, then. Make sure the test succeeds.
        Account retrievedAccount = restTemplate.getForObject(newAccountLocation, Account.class);
        assertNotNull(retrievedAccount);
        assertEquals(account.getNumber(), retrievedAccount.getNumber());

        Beneficiary accountBeneficiary = account.getBeneficiaries().iterator().next();
        Beneficiary retrievedAccountBeneficiary = retrievedAccount.getBeneficiaries().iterator().next();

        assertEquals(accountBeneficiary.getName(), retrievedAccountBeneficiary.getName());
        assertNotNull(retrievedAccount.getEntityId());
        return newAccountLocation;
    }

    @Test
    public void serverRespondsWithConflictStatusWhenConflictingAccountNumber() {
        String number = "123123123";
        Account account1 = new Account(number, "John Doe");
        account1.addBeneficiary("Jane Doe");
        URI account1Location = callCreateAccount(account1);
        Account account2 = new Account(number, "Federico Martillo");
        account2.addBeneficiary("Enrica La Puerca");
        assertThrows(HttpClientErrorException.class, () -> {
            callCreateAccount(account2);
        });
        // Delete account1, if not test will fail if you re-run it
        restTemplate.delete(account1Location);
        assertThrows(HttpClientErrorException.class, () -> {
            restTemplate.getForEntity(BASE_URL + "/accounts/" + account1.getEntityId(), Account.class);
        });
    }

    @Test
//	@Disabled
    public void addAndDeleteBeneficiaryWithoutResettingAllocationPercentages() {
        // perform both add and delete to avoid issues with side effects

        // TODO-13: Create a new Beneficiary
        // - Remove the @Disabled on this test method.
        // - Create a new Beneficiary called "David" for the account with id 1
        //	 (POST the String "David" to the "/accounts/{accountId}/beneficiaries" URL).
        // - Store the returned location URI in a variable.
        String beneficiaryName = "David";
        Long accountId = 1L;
        addAndDeleteBeneficiary(beneficiaryName, accountId);
    }

    @Test
    public void deleteBeneficiaryAndResetAllocationPercentages() {
        String beneficiaryName = "Antolin";
        Long accountId = 3L;
        restTemplate.delete(BASE_URL + "/accounts/" + accountId + "/beneficiaries/" + beneficiaryName);
        Account account = restTemplate.getForObject(BASE_URL + "/accounts/{accountId}", Account.class, accountId);
        assertNotNull(account);
        assertTotalPercentageIsCorrect(account, "100%");
        // Add it again or next run will fail
        URI beneficiaryLocation = restTemplate.postForLocation(BASE_URL + "/accounts/{accountId}/beneficiaries", beneficiaryName, accountId);
        assertNotNull(beneficiaryLocation);
        Map<String, Percentage> allocationPercentages = new HashMap<String, Percentage>();
        allocationPercentages.put("Antolin", Percentage.valueOf("20%"));
        allocationPercentages.put("Argus", Percentage.valueOf("30%"));
        allocationPercentages.put("Gian", Percentage.valueOf("35%"));
        allocationPercentages.put("Argeo", Percentage.valueOf("15%"));
        restTemplate.put(BASE_URL + "/accounts/{accountId}",allocationPercentages, accountId);
        account = restTemplate.getForObject(BASE_URL + "/accounts/{accountId}",Account.class,account.getEntityId());
        assertNotNull(account);
        assertTotalPercentageIsCorrect(account,"100%");
    }

    private static void assertTotalPercentageIsCorrect(Account account, String totalPercentage) {
        Optional<Percentage> total = account.getBeneficiaries().stream()
                .map(b -> b.getAllocationPercentage())
                .reduce(Percentage::add);
        assertEquals(Percentage.valueOf(totalPercentage), total.get(), "Total percentage of the beneficiaries should be " + totalPercentage);
    }

    private void addAndDeleteBeneficiary(String beneficiaryName, Long accountId) {
        URI beneficiaryLocation = restTemplate.postForLocation(BASE_URL + "/accounts/{accountId}/beneficiaries", beneficiaryName, accountId);
        assertNotNull(beneficiaryLocation);

        // TODO-14: Retrieve the Beneficiary you just created from the location that was returned
        Beneficiary newBeneficiary = restTemplate.getForObject(beneficiaryLocation, Beneficiary.class); // Modify this line to use the restTemplate
        assertNotNull(newBeneficiary);
        assertEquals(beneficiaryName, newBeneficiary.getName());
//
//		// TODO-15: Delete the newly created Beneficiary
        restTemplate.delete(beneficiaryLocation);
//
        HttpClientErrorException httpClientErrorException = assertThrows(HttpClientErrorException.class, () -> {
            System.out.println("You SHOULD get the exception \"No such beneficiary with name '" + beneficiaryName + "'\" in the server.");
//
//			// TODO-16: Try to retrieve the newly created Beneficiary again.
//			// - Run this test, then. It should pass because we expect a 404 Not Found
//			//   If not, it is likely your delete in the previous step
//			//   was not successful.
            restTemplate.getForEntity(beneficiaryLocation, Beneficiary.class);
        });
        assertEquals("404 : \"No such beneficiary with name '" + beneficiaryName + "'\"", httpClientErrorException.getMessage());
    }

}
