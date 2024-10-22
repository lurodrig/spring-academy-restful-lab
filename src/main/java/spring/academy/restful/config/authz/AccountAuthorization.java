package spring.academy.restful.config.authz;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import spring.academy.restful.accounts.AccountManager;

@Component("authz")
public class AccountAuthorization {

    @Autowired
    private AccountManager accountManager;

    public AccountAuthorization(AccountManager accountManager) {
        this.accountManager = accountManager;
    }

    public boolean isOwnerOfTheAccount(Long accountId) {
        String userName = SecurityContextHolder.getContext().getAuthentication().getName();
        String accountOwner = accountManager.getAccount(accountId).getName();
        assert accountOwner != null;
        return userName.equals(accountOwner);
    }

}
