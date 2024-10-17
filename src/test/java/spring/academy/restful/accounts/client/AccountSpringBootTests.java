package spring.academy.restful.accounts.client;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import spring.academy.restful.common.money.Percentage;
import spring.academy.restful.config.JwtConfig;
import spring.academy.restful.jwt.utils.TokenGenerator;
import spring.academy.restful.rewards.internal.account.Account;
import spring.academy.restful.rewards.internal.account.Beneficiary;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Import({JwtConfig.class})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AccountSpringBootTests {

    @Autowired
    private final TestRestTemplate restTemplate = new TestRestTemplate();

    private final Random random = new Random();

    @Autowired
    private JwtEncoder jwtEncoder;

    private final TokenGenerator tokenGenerator = new TokenGenerator(jwtEncoder);

    @Test
    public void listAccounts() {

        Account[] accounts = restTemplate.getForObject("/accounts", Account[].class);

        assertNotNull(accounts);
        assertTrue(accounts.length >= 21);
        assertEquals("Keith and Keri Donald", accounts[0].getName());
        assertEquals(2, accounts[0].getBeneficiaries().size());
        assertEquals(Percentage.valueOf("50%"), accounts[0].getBeneficiary("Annabelle").getAllocationPercentage());
    }

    @Test
    public void getAccount() {

        Account account = restTemplate.getForObject("/accounts/{accountId}", Account.class, 0); // Modify this line to use the restTemplate

        assertNotNull(account);
        assertEquals("Keith and Keri Donald", account.getName());
        assertEquals(2, account.getBeneficiaries().size());
        assertEquals(Percentage.valueOf("50%"), account.getBeneficiary("Annabelle").getAllocationPercentage());
        assertEquals(Percentage.valueOf("50%"), account.getBeneficiary("Corgan").getAllocationPercentage());

    }

    @Test
    public void createAccount() throws URISyntaxException {
        // Use a unique number to avoid conflicts
        String number = String.format("12345%4d", random.nextInt(10000));
        Account account = new Account(number, "John Doe");
        account.addBeneficiary("Jane Doe");

        callCreateAccount(account);
    }

    private ResponseEntity<Account> callCreateAccount(Account account) throws URISyntaxException {
        ResponseEntity<Account> response = restTemplate.postForEntity("/accounts", account, Account.class);
        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        // If the status is not CREATED do not bother continuing processing the response, something
        // wrong has happened, probably a 409, lets the caller of the method deal with it

        Account retrievedAccount = restTemplate.getForObject(new URI(Objects.requireNonNull(response.getHeaders().get("Location")).getFirst()), Account.class);
        assertNotNull(retrievedAccount);
        assertEquals(account.getNumber(), retrievedAccount.getNumber());

        Beneficiary accountBeneficiary = account.getBeneficiaries().iterator().next();
        Beneficiary retrievedAccountBeneficiary = retrievedAccount.getBeneficiaries().iterator().next();

        assertEquals(accountBeneficiary.getName(), retrievedAccountBeneficiary.getName());
        assertNotNull(retrievedAccount.getEntityId());

        return response;
    }

    @Test
    public void serverRespondsWithConflictStatusWhenConflictingAccountNumber() throws URISyntaxException {
        String number = "123123123";
        Account account1 = new Account(number, "John Doe");
        account1.addBeneficiary("Jane Doe");
        callCreateAccount(account1);
        Account account2 = new Account(number, "Federico Martillo");
        account2.addBeneficiary("Enriqueta Lapuerta");
        assertEquals(HttpStatus.CONFLICT, callCreateAccount(account2).getStatusCode());
    }

    @Test
    public void addAndDeleteBeneficiaryWithoutResettingAllocationPercentages() {
        // perform both add and delete to avoid issues with side effects
        String beneficiaryName = "David";
        Long accountId = 1L;
        addAndDeleteBeneficiary(beneficiaryName, accountId);
    }

    @Test
    public void deleteBeneficiaryAndResetAllocationPercentages() {
        String beneficiaryName = "Antolin";
        Long accountId = 3L;
        restTemplate.delete("/accounts/" + accountId + "/beneficiaries/" + beneficiaryName);
        Account account = restTemplate.getForObject("/accounts/{accountId}", Account.class, accountId);
        assertNotNull(account);
        assertTotalPercentageIsCorrect(account, "100%");
        // Add it again or next run will fail
        URI beneficiaryLocation = restTemplate.postForLocation("/accounts/{accountId}/beneficiaries", beneficiaryName, accountId);
        assertNotNull(beneficiaryLocation);
        Map<String, Percentage> allocationPercentages = new HashMap<String, Percentage>();
        allocationPercentages.put("Antolin", Percentage.valueOf("20%"));
        allocationPercentages.put("Argus", Percentage.valueOf("30%"));
        allocationPercentages.put("Gian", Percentage.valueOf("35%"));
        allocationPercentages.put("Argeo", Percentage.valueOf("15%"));
        restTemplate.put("/accounts/{accountId}", allocationPercentages, accountId);
        account = restTemplate.getForObject("/accounts/{accountId}", Account.class, account.getEntityId());
        assertNotNull(account);
        assertTotalPercentageIsCorrect(account, "100%");
    }

    private static void assertTotalPercentageIsCorrect(Account account, String totalPercentage) {
        Optional<Percentage> total = account.getBeneficiaries().stream()
                .map(b -> b.getAllocationPercentage())
                .reduce(Percentage::add);
        assertEquals(Percentage.valueOf(totalPercentage), total.get(), "Total percentage of the beneficiaries should be " + totalPercentage);
    }

    private void addAndDeleteBeneficiary(String beneficiaryName, Long accountId) {
        URI beneficiaryLocation = restTemplate.postForLocation("/accounts/{accountId}/beneficiaries", beneficiaryName, accountId);
        assertNotNull(beneficiaryLocation);

        Beneficiary newBeneficiary = restTemplate.getForObject(beneficiaryLocation, Beneficiary.class); // Modify this line to use the restTemplate
        assertNotNull(newBeneficiary);
        assertEquals(beneficiaryName, newBeneficiary.getName());

        restTemplate.delete(beneficiaryLocation);

        ResponseEntity<Beneficiary> response = restTemplate.getForEntity(beneficiaryLocation, Beneficiary.class);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

}
