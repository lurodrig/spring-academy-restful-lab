package spring.academy.restful.accounts.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import spring.academy.restful.common.money.Percentage;
import spring.academy.restful.jwt.Constants;
import spring.academy.restful.jwt.TokenGenerator;
import spring.academy.restful.rewards.internal.account.Account;
import spring.academy.restful.rewards.internal.account.Beneficiary;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AccountIntegrationTests {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_AUTHENTICATION = "Bearer ";

    @Autowired
    private final TestRestTemplate restTemplate = new TestRestTemplate();

    private final Random random = new Random();

    @Autowired
    private JwtEncoder jwtEncoder;

    private TokenGenerator tokenGenerator;

    @BeforeEach
    void setup() {
        tokenGenerator = new TokenGenerator(jwtEncoder);
    }

    @Test
    public void shouldListAccounts() {
        ResponseEntity<?> response = makeAuthenticatedHttpRequest(
                "/accounts",
                HttpMethod.GET,
                Account[].class,
                null,
                List.of(MediaType.APPLICATION_JSON),
                Constants.EMPTY_BUILDER_COMSUMER
        );
        assertNotNull(response);
        Account[] accounts = (Account[]) response.getBody();

        assertNotNull(accounts);
        assertTrue(accounts.length >= 21);
        assertEquals("Keith and Keri Donald", accounts[0].getName());
        assertEquals(2, accounts[0].getBeneficiaries().size());
        assertEquals(Percentage.valueOf("50%"), accounts[0].getBeneficiary("Annabelle").getAllocationPercentage());
    }

    @Test
    public void shouldGetAccount() {
        ResponseEntity<?> response = makeAuthenticatedHttpRequest(
                "/accounts/0",
                HttpMethod.GET,
                Account.class,
                null,
                List.of(MediaType.APPLICATION_JSON),
                Constants.EMPTY_BUILDER_COMSUMER
        );
        assertNotNull(response);
        Account account = (Account) response.getBody();

        assertNotNull(account);
        assertEquals("Keith and Keri Donald", account.getName());
        assertEquals(2, account.getBeneficiaries().size());
        assertEquals(Percentage.valueOf("50%"), account.getBeneficiary("Annabelle").getAllocationPercentage());
        assertEquals(Percentage.valueOf("50%"), account.getBeneficiary("Corgan").getAllocationPercentage());

    }

    @Test
    public void shouldCreateAccount() throws URISyntaxException {
        // Use a unique number to avoid conflicts
        String number = String.format("12345%4d", random.nextInt(10000));
        Account account = new Account(number, "John Doe");
        account.addBeneficiary("Jane Doe");

        createAccount(account);
    }

    @Test
    public void serverShouldRespondsWithConflictStatusWhenConflictingAccountNumber() throws URISyntaxException {
        String number = "123123123";
        Account account1 = new Account(number, "John Doe");
        account1.addBeneficiary("Jane Doe");
        createAccount(account1);
        Account account2 = new Account(number, "Federico Martillo");
        account2.addBeneficiary("Enriqueta Lapuerta");
        assertEquals(HttpStatus.CONFLICT, createAccount(account2).getStatusCode());
    }

    @Test
    public void shouldAddAndDeleteBeneficiaryWithoutResettingAllocationPercentages() {
        // perform both add and delete to avoid issues with side effects
        String beneficiaryName = "David";
        Long accountId = 1L;
        addAndDeleteBeneficiary(beneficiaryName, accountId);
    }

    @Test
    public void shouldDeleteBeneficiaryAndResetAllocationPercentages() {
        String beneficiaryName = "Antolin";
        Long accountId = 3L;
        ResponseEntity<?> response = makeAuthenticatedHttpRequest(
                "/accounts/" + accountId + "/beneficiaries/" + beneficiaryName,
                HttpMethod.DELETE,
                Void.class,
                null,
                List.of(MediaType.APPLICATION_JSON),
                Constants.EMPTY_BUILDER_COMSUMER
        );
        assertNotNull(response);

        response = makeAuthenticatedHttpRequest(
                "/accounts/{accountId}",
                HttpMethod.GET,
                Account.class,
                null,
                List.of(MediaType.APPLICATION_JSON),
                Constants.EMPTY_BUILDER_COMSUMER,
                accountId
        );
        assertNotNull(response);
        Account account = (Account) response.getBody();

        assertNotNull(account);
        assertTotalPercentageIsCorrect(account, "100%");
        // Add it again or next run will fail
        response = makeAuthenticatedHttpRequest(
                "/accounts/{accountId}/beneficiaries",
                HttpMethod.POST,
                Object.class,
                beneficiaryName,
                List.of(MediaType.APPLICATION_JSON),
                Constants.EMPTY_BUILDER_COMSUMER,
                accountId
        );
        assertNotNull(response);
        URI beneficiaryLocation = URI.create(Objects.requireNonNull(response.getHeaders().get(HttpHeaders.LOCATION)).getFirst());
        assertNotNull(beneficiaryLocation);

        Map<String, Percentage> allocationPercentages = new HashMap<String, Percentage>();
        allocationPercentages.put("Antolin", Percentage.valueOf("20%"));
        allocationPercentages.put("Argus", Percentage.valueOf("30%"));
        allocationPercentages.put("Gian", Percentage.valueOf("35%"));
        allocationPercentages.put("Argeo", Percentage.valueOf("15%"));

        makeAuthenticatedHttpRequest(
                "/accounts/{accountId}",
                HttpMethod.PUT,
                Void.class,
                allocationPercentages,
                List.of(MediaType.APPLICATION_JSON),
                Constants.EMPTY_BUILDER_COMSUMER,
                accountId
        );
        assertNotNull(response);


        response = makeAuthenticatedHttpRequest(
                "/accounts/{accountId}",
                HttpMethod.GET,
                Account.class,
                null,
                List.of(MediaType.APPLICATION_JSON),
                Constants.EMPTY_BUILDER_COMSUMER,
                account.getEntityId()
        );
        assertNotNull(response);
        account = (Account) response.getBody();
        assertNotNull(account);
        assertTotalPercentageIsCorrect(account, "100%");
    }

    private ResponseEntity<?> createAccount(Account account) throws URISyntaxException {
        ResponseEntity<?> response = makeAuthenticatedHttpRequest(
                "/accounts",
                HttpMethod.POST,
                Account.class,
                account,
                List.of(MediaType.APPLICATION_JSON),
                Constants.EMPTY_BUILDER_COMSUMER
        );

        assertNotNull(response);
        // If the status is not CREATED do not bother continuing processing the response, something
        // wrong has happened, probably a 409, lets the caller of the method deal with it
        if (response.getStatusCode() == HttpStatus.CREATED) {
            response = makeAuthenticatedHttpRequest(
                    String.valueOf(new URI(Objects.requireNonNull(response.getHeaders().get("Location")).getFirst())),
                    HttpMethod.GET,
                    Account.class,
                    null,
                    List.of(MediaType.APPLICATION_JSON),
                    Constants.EMPTY_BUILDER_COMSUMER
            );

            assertNotNull(response);
            Account retrievedAccount = (Account) response.getBody();
            assertNotNull(retrievedAccount);
            assertEquals(account.getNumber(), retrievedAccount.getNumber());

            Beneficiary accountBeneficiary = account.getBeneficiaries().iterator().next();
            Beneficiary retrievedAccountBeneficiary = retrievedAccount.getBeneficiaries().iterator().next();

            assertEquals(accountBeneficiary.getName(), retrievedAccountBeneficiary.getName());
            assertNotNull(retrievedAccount.getEntityId());
        }
        return response;
    }

    private ResponseEntity<?> makeAuthenticatedHttpRequest(
            String url,
            HttpMethod httpMethod,
            Class responseClass,
            Object request,
            List<MediaType> acceptedMediaTypes,
            Consumer<JwtClaimsSet.Builder> consumer,
            Object... urlPathVariable) {
        HttpHeaders httpRequestHeaders = new HttpHeaders();
        httpRequestHeaders.add(AUTHORIZATION_HEADER, BEARER_AUTHENTICATION + tokenGenerator.generate(consumer));
        if (acceptedMediaTypes != null) {
            httpRequestHeaders.setAccept(acceptedMediaTypes);
        }

        HttpEntity<?> httpRequestEntity = request == null ? new HttpEntity<>(httpRequestHeaders) : new HttpEntity<>(request, httpRequestHeaders);

        return restTemplate.exchange(url, httpMethod, httpRequestEntity, responseClass, urlPathVariable);
    }

    private static void assertTotalPercentageIsCorrect(Account account, String totalPercentage) {
        Optional<Percentage> total = account.getBeneficiaries().stream().map(b -> b.getAllocationPercentage()).reduce(Percentage::add);
        assertEquals(Percentage.valueOf(totalPercentage), total.get(), "Total percentage of the beneficiaries should be " + totalPercentage);
    }

    private void addAndDeleteBeneficiary(String beneficiaryName, Long accountId) {
        // Crete the beneficiary, it will belong to the account with accountId
        ResponseEntity<?> response = makeAuthenticatedHttpRequest(
                "/accounts/{accountId}/beneficiaries",
                HttpMethod.POST,
                Object.class,
                beneficiaryName,
                List.of(MediaType.APPLICATION_JSON),
                Constants.EMPTY_BUILDER_COMSUMER,
                accountId
        );
        assertNotNull(response);
        URI beneficiaryLocation = URI.create(Objects.requireNonNull(response.getHeaders().get(HttpHeaders.LOCATION)).getFirst());

        // Check the beneficiary has been created
        response = makeAuthenticatedHttpRequest(
                beneficiaryLocation.toString(),
                HttpMethod.GET,
                Beneficiary.class,
                null,
                List.of(MediaType.APPLICATION_JSON),
                Constants.EMPTY_BUILDER_COMSUMER
        );
        assertNotNull(response);
        Beneficiary newBeneficiary = (Beneficiary) response.getBody();
        assertNotNull(newBeneficiary);
        assertEquals(beneficiaryName, newBeneficiary.getName());

        // Remove the beneficiary
        response = makeAuthenticatedHttpRequest(
                beneficiaryLocation.toString(),
                HttpMethod.DELETE,
                Void.class,
                null,
                List.of(MediaType.APPLICATION_JSON),
                Constants.EMPTY_BUILDER_COMSUMER
        );
        assertNotNull(response);
        assertEquals(HttpStatus.NO_CONTENT,response.getStatusCode());

        // Check the deleted beneficiary can not be found
        response = makeAuthenticatedHttpRequest(
                beneficiaryLocation.toString(),
                HttpMethod.GET,
                Beneficiary.class,
                beneficiaryName,
                List.of(MediaType.APPLICATION_JSON),
                Constants.EMPTY_BUILDER_COMSUMER
        );
        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

}
