package spring.academy.restful.rewards.internal.restaurant;

import spring.academy.restful.rewards.Dining;
import spring.academy.restful.rewards.internal.account.Account;

/**
 * A benefit availabilty policy that returns true at all times.
 */
public class AlwaysAvailable implements BenefitAvailabilityPolicy {
	static final BenefitAvailabilityPolicy INSTANCE = new AlwaysAvailable();

	public boolean isBenefitAvailableFor(Account account, Dining dining) {
		return true;
	}

	public String toString() {
		return "alwaysAvailable";
	}
}
