package spring.academy.restful.rewards.internal.restaurant;

import spring.academy.restful.rewards.Dining;
import spring.academy.restful.rewards.internal.account.Account;

/**
 * Determines if benefit is available for an account for dining.
 * 
 * A value object. A strategy. Scoped by the Resturant aggregate.
 */
public interface BenefitAvailabilityPolicy {

	/**
	 * Calculates if an account is eligible to receive benefits for a dining.
	 * @param account the account of the member who dined
	 * @param dining the dining event
	 * @return benefit availability status
	 */
	public boolean isBenefitAvailableFor(Account account, Dining dining);
}
