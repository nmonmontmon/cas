package org.apereo.cas.services;

import org.apereo.cas.authentication.DefaultAuthenticationServiceSelectionPlan;
import org.apereo.cas.authentication.DefaultAuthenticationServiceSelectionStrategy;
import org.apereo.cas.authentication.DefaultAuthenticationTransaction;
import org.apereo.cas.authentication.policy.AllAuthenticationHandlersSucceededAuthenticationPolicy;
import org.apereo.cas.authentication.policy.AtLeastOneCredentialValidatedAuthenticationPolicy;
import org.apereo.cas.authentication.policy.GroovyScriptAuthenticationPolicy;
import org.apereo.cas.authentication.policy.NotPreventedAuthenticationPolicy;
import org.apereo.cas.authentication.policy.RegisteredServiceAuthenticationPolicyResolver;
import org.apereo.cas.authentication.policy.RestfulAuthenticationPolicy;

import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.ArrayList;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * This is {@link RegisteredServiceAuthenticationPolicyResolverTests}.
 *
 * @author Misagh Moayyed
 * @since 6.2.0
 */
public class RegisteredServiceAuthenticationPolicyResolverTests {
    private ServicesManager servicesManager;

    @BeforeEach
    public void initialize() {
        val list = new ArrayList<RegisteredService>();

        val svc1 = RegisteredServiceTestUtils.getRegisteredService("serviceid1");
        val p1 = new DefaultRegisteredServiceAuthenticationPolicy();
        val cr1 = new DefaultRegisteredServiceAuthenticationPolicyCriteria();
        cr1.setType(RegisteredServiceAuthenticationPolicyCriteria.AuthenticationPolicyTypes.ANY_AUTHENTICATION_HANDLER);
        cr1.setTryAll(true);
        p1.setCriteria(cr1);
        svc1.setAuthenticationPolicy(p1);
        list.add(svc1);

        val svc2 = RegisteredServiceTestUtils.getRegisteredService("serviceid2");
        svc2.setAuthenticationPolicy(new DefaultRegisteredServiceAuthenticationPolicy());
        list.add(svc2);

        val svc3 = RegisteredServiceTestUtils.getRegisteredService("serviceid3");
        val p3 = new DefaultRegisteredServiceAuthenticationPolicy();
        val cr3 = new DefaultRegisteredServiceAuthenticationPolicyCriteria();
        cr3.setType(RegisteredServiceAuthenticationPolicyCriteria.AuthenticationPolicyTypes.ALL_AUTHENTICATION_HANDLERS);
        p3.setCriteria(cr3);
        svc3.setAuthenticationPolicy(p3);
        list.add(svc3);

        val svc4 = RegisteredServiceTestUtils.getRegisteredService("serviceid4");
        val p4 = new DefaultRegisteredServiceAuthenticationPolicy();
        val cr4 = new DefaultRegisteredServiceAuthenticationPolicyCriteria();
        cr4.setType(RegisteredServiceAuthenticationPolicyCriteria.AuthenticationPolicyTypes.NOT_PREVENTED);
        p4.setCriteria(cr4);
        svc4.setAuthenticationPolicy(p4);
        list.add(svc4);

        val svc5 = RegisteredServiceTestUtils.getRegisteredService("serviceid5");
        val p5 = new DefaultRegisteredServiceAuthenticationPolicy();
        val cr5 = new DefaultRegisteredServiceAuthenticationPolicyCriteria();
        cr5.setScript("groovy { return Optional.empty() }");
        cr5.setType(RegisteredServiceAuthenticationPolicyCriteria.AuthenticationPolicyTypes.GROOVY);
        p5.setCriteria(cr5);
        svc5.setAuthenticationPolicy(p5);
        list.add(svc5);

        val svc6 = RegisteredServiceTestUtils.getRegisteredService("serviceid6");
        val p6 = new DefaultRegisteredServiceAuthenticationPolicy();
        val cr6 = new DefaultRegisteredServiceAuthenticationPolicyCriteria();
        cr6.setUrl("https://example.org");
        cr6.setBasicAuthPassword("uid");
        cr6.setBasicAuthUsername("password");
        cr6.setType(RegisteredServiceAuthenticationPolicyCriteria.AuthenticationPolicyTypes.REST);
        p6.setCriteria(cr6);
        svc6.setAuthenticationPolicy(p6);
        list.add(svc6);

        val dao = new InMemoryServiceRegistry(mock(ApplicationEventPublisher.class), list, new ArrayList<>());

        this.servicesManager = new DefaultServicesManager(dao, mock(ApplicationEventPublisher.class), new HashSet<>());
        this.servicesManager.load();
    }

    @Test
    public void checkAnyPolicy() {
        val resolver = new RegisteredServiceAuthenticationPolicyResolver(this.servicesManager,
            new DefaultAuthenticationServiceSelectionPlan(new DefaultAuthenticationServiceSelectionStrategy()));

        val transaction = DefaultAuthenticationTransaction.of(RegisteredServiceTestUtils.getService("serviceid1"),
            RegisteredServiceTestUtils.getCredentialsWithSameUsernameAndPassword("casuser"));

        val policies = resolver.resolve(transaction);
        assertEquals(1, policies.size());
        assertTrue(policies.iterator().next() instanceof AtLeastOneCredentialValidatedAuthenticationPolicy);
    }

    @Test
    public void checkAllPolicy() {
        val resolver = new RegisteredServiceAuthenticationPolicyResolver(this.servicesManager,
            new DefaultAuthenticationServiceSelectionPlan(new DefaultAuthenticationServiceSelectionStrategy()));

        val transaction = DefaultAuthenticationTransaction.of(RegisteredServiceTestUtils.getService("serviceid3"),
            RegisteredServiceTestUtils.getCredentialsWithSameUsernameAndPassword("casuser"));

        val policies = resolver.resolve(transaction);
        assertEquals(1, policies.size());
        assertTrue(policies.iterator().next() instanceof AllAuthenticationHandlersSucceededAuthenticationPolicy);
    }

    @Test
    public void checkDefaultPolicy() {
        val resolver = new RegisteredServiceAuthenticationPolicyResolver(this.servicesManager,
            new DefaultAuthenticationServiceSelectionPlan(new DefaultAuthenticationServiceSelectionStrategy()));

        val transaction = DefaultAuthenticationTransaction.of(RegisteredServiceTestUtils.getService("serviceid2"),
            RegisteredServiceTestUtils.getCredentialsWithSameUsernameAndPassword("casuser"));

        assertFalse(resolver.supports(transaction));
        val policies = resolver.resolve(transaction);
        assertTrue(policies.isEmpty());
    }

    @Test
    public void checkNotPreventedPolicy() {
        val resolver = new RegisteredServiceAuthenticationPolicyResolver(this.servicesManager,
            new DefaultAuthenticationServiceSelectionPlan(new DefaultAuthenticationServiceSelectionStrategy()));

        val transaction = DefaultAuthenticationTransaction.of(RegisteredServiceTestUtils.getService("serviceid4"),
            RegisteredServiceTestUtils.getCredentialsWithSameUsernameAndPassword("casuser"));

        val policies = resolver.resolve(transaction);
        assertEquals(1, policies.size());
        assertTrue(policies.iterator().next() instanceof NotPreventedAuthenticationPolicy);
    }

    @Test
    public void checkGroovyPolicy() {
        val resolver = new RegisteredServiceAuthenticationPolicyResolver(this.servicesManager,
            new DefaultAuthenticationServiceSelectionPlan(new DefaultAuthenticationServiceSelectionStrategy()));

        val transaction = DefaultAuthenticationTransaction.of(RegisteredServiceTestUtils.getService("serviceid5"),
            RegisteredServiceTestUtils.getCredentialsWithSameUsernameAndPassword("casuser"));

        val policies = resolver.resolve(transaction);
        assertEquals(1, policies.size());
        assertTrue(policies.iterator().next() instanceof GroovyScriptAuthenticationPolicy);
    }

    @Test
    public void checkRestPolicy() {
        val resolver = new RegisteredServiceAuthenticationPolicyResolver(this.servicesManager,
            new DefaultAuthenticationServiceSelectionPlan(new DefaultAuthenticationServiceSelectionStrategy()));
        val transaction = DefaultAuthenticationTransaction.of(RegisteredServiceTestUtils.getService("serviceid6"),
            RegisteredServiceTestUtils.getCredentialsWithSameUsernameAndPassword("casuser"));
        val policies = resolver.resolve(transaction);
        assertEquals(1, policies.size());
        assertTrue(policies.iterator().next() instanceof RestfulAuthenticationPolicy);
    }

}
