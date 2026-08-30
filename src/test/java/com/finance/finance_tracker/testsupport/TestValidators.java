package com.finance.finance_tracker.testsupport;

import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

public final class TestValidators {

    private TestValidators() {}

    public static LocalValidatorFactoryBean permissive() {
        LocalValidatorFactoryBean factoryBean = new LocalValidatorFactoryBean();
        factoryBean.setConstraintValidatorFactory(new PermissiveConstraintValidatorFactory());
        factoryBean.afterPropertiesSet();
        return factoryBean;
    }
}
