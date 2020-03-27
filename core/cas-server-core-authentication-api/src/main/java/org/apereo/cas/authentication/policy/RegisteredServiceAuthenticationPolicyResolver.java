package org.apereo.cas.authentication.policy;

import org.apereo.cas.authentication.AuthenticationPolicy;
import org.apereo.cas.authentication.AuthenticationPolicyResolver;
import org.apereo.cas.authentication.AuthenticationServiceSelectionPlan;
import org.apereo.cas.authentication.AuthenticationTransaction;
import org.apereo.cas.services.RegisteredServiceAuthenticationPolicyCriteria;
import org.apereo.cas.services.ServicesManager;
import org.apereo.cas.services.UnauthorizedSsoServiceException;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * This is {@link RegisteredServiceAuthenticationPolicyResolver}
 * that acts on the criteria presented by a registered service to
 * detect which handler(s) should be resolved for authentication.
 *
 * @author Misagh Moayyed
 * @since 5.0.0
 */
@Slf4j
@RequiredArgsConstructor
@Getter
@Setter
public class RegisteredServiceAuthenticationPolicyResolver implements AuthenticationPolicyResolver {

    /**
     * The Services manager.
     */
    protected final ServicesManager servicesManager;

    /**
     * The service selection plan.
     */
    protected final AuthenticationServiceSelectionPlan authenticationServiceSelectionPlan;

    private int order;

    @Override
    public Set<AuthenticationPolicy> resolve(final AuthenticationTransaction transaction) {
        val service = authenticationServiceSelectionPlan.resolveService(transaction.getService());
        val registeredService = this.servicesManager.findServiceBy(service);
        val criteria = registeredService.getAuthenticationPolicy().getCriteria();
        val policies = new LinkedHashSet<AuthenticationPolicy>();
        if (criteria != null) {
            switch (criteria.getType()) {
                case ANY_AUTHENTICATION_HANDLER:
                    policies.add(new AtLeastOneCredentialValidatedAuthenticationPolicy(criteria.isTryAll()));
                    break;
                case REST:
                    policies.add(new RestfulAuthenticationPolicy(criteria.getUrl(),
                        criteria.getBasicAuthUsername(), criteria.getBasicAuthPassword()));
                    break;
                case ALL_AUTHENTICATION_HANDLERS:
                    policies.add(new AllAuthenticationHandlersSucceededAuthenticationPolicy());
                    break;
                case NOT_PREVENTED:
                    policies.add(new NotPreventedAuthenticationPolicy());
                    break;
                case GROOVY:
                    policies.add(new GroovyScriptAuthenticationPolicy(criteria.getScript()));
                    break;
                case DEFAULT:
                default:
                    break;
            }
        }
        LOGGER.debug("Authentication policies for this transaction are [{}]", policies);
        return policies;
    }

    @Override
    public boolean supports(final AuthenticationTransaction transaction) {
        val service = authenticationServiceSelectionPlan.resolveService(transaction.getService());
        if (service != null) {
            val registeredService = this.servicesManager.findServiceBy(service);
            LOGGER.trace("Located registered service definition [{}] for this authentication transaction", registeredService);
            if (registeredService == null || !registeredService.getAccessStrategy().isServiceAccessAllowed()) {
                LOGGER.warn("Service [{}] is not allowed to use SSO.", service);
                throw new UnauthorizedSsoServiceException();
            }
            val authenticationPolicy = registeredService.getAuthenticationPolicy();
            if (authenticationPolicy != null) {
                val criteria = authenticationPolicy.getCriteria();
                return criteria != null
                    && criteria.getType() != RegisteredServiceAuthenticationPolicyCriteria.AuthenticationPolicyTypes.DEFAULT;
            }
        }
        return false;
    }
}
