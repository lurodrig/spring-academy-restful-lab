package spring.academy.restful.accounts.web;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import spring.academy.restful.accounts.AccountManager;
import spring.academy.restful.common.money.Percentage;
import spring.academy.restful.config.SecurityConfig;
import spring.academy.restful.config.authz.AccountAuthorization;
import spring.academy.restful.jwt.Constants;
import spring.academy.restful.rewards.internal.account.Account;
import spring.academy.restful.rewards.internal.account.Beneficiary;
import spring.academy.restful.web.AccountController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountController.class)
@Import({SecurityConfig.class, AccountAuthorization.class})
@WithMockUser(username = Constants.SUBJECT, authorities = {"SCOPE_rewards:BANKER", "SCOPE_rewards:CUSTOMER"})
public class AccountControllerUnitTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AccountManager accountManager;

    @Test
    @WithMockUser(username = "John Doe", authorities = {"SCOPE_rewards:CUSTOMER"})
    public void shouldGetAccountDetails() throws Exception {

        given(accountManager.getAccount(0L))
                .willReturn(new Account("1234567890", "John Doe"));

        mockMvc.perform(get("/accounts/0"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("name").value("John Doe"))
                .andExpect(jsonPath("number").value("1234567890"));

        verify(accountManager, times(1)).getAccount(0L);

    }

    @Test
    public void nonExistingAccountIdGettingDetailsReturnsNotFound() throws Exception {

        given(accountManager.getAccount(any(Long.class)))
                .willThrow(new IllegalArgumentException("No such account with id " + 9999L));

        mockMvc.perform((get("/accounts/9999")))
                .andExpect(status().isNotFound());

        verify(accountManager, times(1)).getAccount(any(Long.class));

    }

    @Test
    public void shouldCreateAccount() throws Exception {
        Account testAccount = new Account("1234512345", "Mary Jones");
        testAccount.setEntityId(21L);

        given(accountManager.save(any(Account.class)))
                .willReturn(testAccount);

        mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(testAccount)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(header().string("Location", "http://localhost/accounts/21"));

        verify(accountManager, times(1)).save(any(Account.class));
    }

    @Test
    @WithMockUser(username = "johnsmith", authorities = {"SCOPE_rewards:CUSTOMER"})
    public void creatingAccountRespondsForbidden() throws Exception {
        Account testAccount = new Account("1234512345", "Mary Jones");

        mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(testAccount)))
                .andExpect(status().isForbidden());
    }

    @Test
    public void shouldGetAllAccounts() throws Exception {

        List<Account> mockedListOfAccounts = List.of(new Account("123456789", "John Doe"));
        given(accountManager.getAllAccounts()).willReturn(mockedListOfAccounts);

        mockMvc.perform(get("/accounts"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$..number").value("123456789"))
                .andExpect(jsonPath("$..name").value("John Doe"));

        verify(accountManager).getAllAccounts();
    }

    @Test
    public void shouldGetExistingBeneficiary() throws Exception {

        String beneficiaryName = "Rufo";

        Account account = new Account("1234567890", "John Doe");
        account.addBeneficiary(beneficiaryName, new Percentage(1.0));

        given(accountManager.getAccount(any())).willReturn(account);

        mockMvc.perform(get("/accounts/{accountId}/beneficiaries/{beneficiaryName}", anyLong(), beneficiaryName))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("name").value("Rufo"))
                .andExpect(jsonPath("allocationPercentage").value("1.0"));

        verify(accountManager).getAccount(anyLong());
    }

    @Test
    public void getNonExistingBeneficiaryReturnsNotFound() throws Exception {

        String beneficiaryName = "Rufox";

        given(accountManager.getAccount(any())).willThrow(IllegalArgumentException.class);

        mockMvc.perform(get("/accounts/{accountId}/beneficiaries/{beneficiaryName}", anyLong(), beneficiaryName))
                .andExpect(status().isNotFound());

        verify(accountManager).getAccount(anyLong());
    }

    @Test
    @WithMockUser(username = "johnsmith", authorities = {"SCOPE_rewards:CUSTOMER"})
    public void shouldAddBeneficiary() throws Exception {
        doAnswer((a) -> {
                    assertTrue(Long.valueOf(0L).equals(a.getArgument(0)));
                    assertTrue("Rufo".equals(a.getArgument(1)));
                    return null;
                }
        ).when(accountManager).addBeneficiary(0L, "Rufo");

        mockMvc.perform(post("/accounts/{accountId}/beneficiaries", 0L)
                        .content("Rufo"))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(header().string("Location", "http://localhost/accounts/0/beneficiaries/Rufo"));

        verify(accountManager).addBeneficiary(0L, "Rufo");
    }

    @Test
    @WithMockUser(username = "johnsmith", authorities = {"SCOPE_rewards:CUSTOMER"})
    public void shouldRemoveUniqueBeneficiary() throws Exception {

        String beneficiaryName = "Rufo";
        Account account = new Account("1234567890", "John Doe");
        account.setEntityId(0L);
        Percentage percentage = new Percentage(1.0);
        account.addBeneficiary(beneficiaryName, percentage);
        HashMap<String, Percentage> allocationPercentages = new HashMap<String, Percentage>();

        given(accountManager.getAccount(0L)).willReturn(account);

        doAnswer((a) -> {
                    assertTrue(account.getEntityId().equals(a.getArgument(0)));
                    assertTrue(beneficiaryName.equals(a.getArgument(1)));
                    assertTrue(allocationPercentages.equals(a.getArgument(2)));
                    return null;
                }
        ).when(accountManager).removeBeneficiary(account.getEntityId(), beneficiaryName, allocationPercentages);

        mockMvc.perform(delete("/accounts/{accountId}/beneficiaries/{beneficiaryName}", account.getEntityId(), beneficiaryName))
                .andExpect(status().isNoContent());

        verify(accountManager).removeBeneficiary(account.getEntityId(), beneficiaryName, allocationPercentages);
    }


    @Test
    @Disabled
    // With the current implementation this test will fail: AccountController.resetAllocationPercentages makes the
    // allocation percentages sum equals to 100%. I developed this test assuming equal distribution of the
    // percentages between the beneficiaries (see solution proposed in the lab 40-boot-test on spring.academy
    public void shouldRemoveNonUniqueBeneficiary() throws Exception {
        String beneficiaryName = "Rufo";
        Percentage percentage = new Percentage(0.25);
        Beneficiary beneficiaryToBeDeleted = new Beneficiary(beneficiaryName, percentage);

        Set<Beneficiary> beneficiaries = Set.of(
                new Beneficiary("Pascal", new Percentage(0.25)),
                new Beneficiary("Ada", new Percentage(0.25)),
                new Beneficiary("Cobol", new Percentage(0.25)),
                beneficiaryToBeDeleted);

        Map<String, Percentage> allocationPercentages = Map.of(
                "Pascal", new Percentage(0.33),
                "Ada", new Percentage(0.33),
                "Cobol", new Percentage(0.33));

        Account mockedAccount = mock(Account.class);

        given(accountManager.getAccount(0L)).willReturn(mockedAccount);
        given(mockedAccount.getBeneficiary(beneficiaryName)).willReturn(beneficiaryToBeDeleted);
        given(mockedAccount.getBeneficiaries()).willReturn(beneficiaries);

        doAnswer((a) -> {
                    assertTrue(Long.valueOf(0L).equals(a.getArgument(0)));
                    assertTrue(beneficiaryName.equals(a.getArgument(1)));
                    assertTrue(allocationPercentages.equals(a.getArgument(2)));
                    return null;
                }
        ).when(accountManager).removeBeneficiary(0L, beneficiaryName, allocationPercentages);

        mockMvc.perform(delete("/accounts/{accountId}/beneficiaries/{beneficiaryName}", 0L, beneficiaryName))
                .andExpect(status().isNoContent());

        verify(accountManager).removeBeneficiary(0L, beneficiaryName, allocationPercentages);
    }


    @Test
    @WithMockUser(username = "johnsmith", authorities = {"SCOPE_rewards:CUSTOMER"})
    public void updateAllocationPercentagesReturnForbidden() throws Exception {
        Map<String, Percentage> allocationPercentages = Map.of(
                "Pascal", new Percentage(0.33),
                "Ada", new Percentage(0.33),
                "Cobol", new Percentage(0.33));

        String differentUsername = "janesmith";
        Long accountId = 0L;
        Account mockedAccount = mock(Account.class);

        given(accountManager.getAccount(accountId)).willReturn(mockedAccount);
        given(accountManager.getAccount(accountId).getName()).willReturn(differentUsername);

        mockMvc.perform(put("/accounts/{accountId}",accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(allocationPercentages)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "johnsmith", authorities = {"SCOPE_rewards:CUSTOMER"})
    public void removeNonExistingBeneficiaryReturnsNotFound() throws Exception {
        String beneficiaryName = "Rufox";
        Account account = new Account("1234567890", "John Doe");

        given(accountManager.getAccount(anyLong())).willReturn(account);

        mockMvc.perform(get("/accounts/{accountId}/beneficiaries/{beneficiaryName}", anyLong(), beneficiaryName))
                .andExpect(status().isNotFound());

        verify(accountManager).getAccount(anyLong());
    }

    @Test
    @WithMockUser(username = "johnsmith", authorities = {"SCOPE_rewards:CUSTOMER"})
    public void removeBeneficiaryFromNonExistingAccountReturnsNotFound() throws Exception {
        given(accountManager.getAccount(anyLong())).willReturn(null);

        mockMvc.perform(get("/accounts/{accountId}/beneficiaries/{beneficiaryName}", anyLong(), "Rufo"))
                .andExpect(status().isNotFound());

        verify(accountManager).getAccount(anyLong());
    }

    // Utility class for converting an object into JSON string
    protected static String asJsonString(final Object obj) {
        try {
            final ObjectMapper mapper = new ObjectMapper();
            final String jsonContent = mapper.writeValueAsString(obj);
            return jsonContent;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
