package org.apereo.cas.adaptors.generic.config;

import org.apache.commons.lang3.StringUtils;
import org.apereo.cas.adaptors.generic.RejectUsersAuthenticationHandler;
import org.apereo.cas.authentication.AuthenticationEventExecutionPlan;
import org.apereo.cas.authentication.AuthenticationHandler;
import org.apereo.cas.authentication.principal.DefaultPrincipalFactory;
import org.apereo.cas.authentication.principal.PrincipalFactory;
import org.apereo.cas.authentication.principal.PrincipalResolver;
import org.apereo.cas.authentication.support.password.PasswordPolicyConfiguration;
import org.apereo.cas.config.support.authentication.AuthenticationEventExecutionPlanConfigurer;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.configuration.model.support.generic.RejectAuthenticationProperties;
import org.apereo.cas.configuration.support.Beans;
import org.apereo.cas.services.ServicesManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

/**
 * This is {@link RejectUsersAuthenticationEventExecutionPlanConfiguration}.
 *
 * @author Misagh Moayyed
 * @since 5.1.0
 */
@Configuration("rejectUsersAuthenticationEventExecutionPlanConfiguration")
@EnableConfigurationProperties(CasConfigurationProperties.class)
public class RejectUsersAuthenticationEventExecutionPlanConfiguration implements AuthenticationEventExecutionPlanConfigurer {
    private static final Logger LOGGER = LoggerFactory.getLogger(RejectUsersAuthenticationEventExecutionPlanConfiguration.class);

    @Autowired(required = false)
    @Qualifier("rejectPasswordPolicyConfiguration")
    private PasswordPolicyConfiguration rejectPasswordPolicyConfiguration;

    
    @Autowired
    @Qualifier("servicesManager")
    private ServicesManager servicesManager;

    @Autowired
    private CasConfigurationProperties casProperties;
    
    @Autowired
    @Qualifier("personDirectoryPrincipalResolver")
    private PrincipalResolver personDirectoryPrincipalResolver;

    @ConditionalOnMissingBean(name = "rejectPrincipalFactory")
    @Bean
    public PrincipalFactory rejectUsersPrincipalFactory() {
        return new DefaultPrincipalFactory();
    }

    @RefreshScope
    @Bean
    public AuthenticationHandler rejectUsersAuthenticationHandler() {
        final RejectAuthenticationProperties rejectProperties = casProperties.getAuthn().getReject();
        final Set<String> users = org.springframework.util.StringUtils.commaDelimitedListToSet(rejectProperties.getUsers());
        final RejectUsersAuthenticationHandler h = new RejectUsersAuthenticationHandler(rejectProperties.getName(), servicesManager, users);
        h.setPrincipalFactory(rejectUsersPrincipalFactory());
        h.setPasswordEncoder(Beans.newPasswordEncoder(rejectProperties.getPasswordEncoder()));
        if (rejectPasswordPolicyConfiguration != null) {
            h.setPasswordPolicyConfiguration(rejectPasswordPolicyConfiguration);
        }
        h.setPrincipalNameTransformer(Beans.newPrincipalNameTransformer(rejectProperties.getPrincipalTransformation()));
        return h;
    }
    
    @Override
    public void configureAuthenticationExecutionPlan(final AuthenticationEventExecutionPlan plan) {
        if (StringUtils.isNotBlank(casProperties.getAuthn().getReject().getUsers())) {
            LOGGER.debug("Added rejecting authentication handler");
            plan.registerAuthenticationHandlerWithPrincipalResolver(rejectUsersAuthenticationHandler(), personDirectoryPrincipalResolver);
        }
    }
}
