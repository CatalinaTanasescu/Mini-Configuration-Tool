package com.example.configtool.application.port;

import java.util.List;

import com.example.configtool.domain.validation.ValidationRule;

public interface ValidationRuleRepository {

    List<ValidationRule> findAll();
}
