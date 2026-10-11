package com.example.configtool.application.port;

import java.util.List;

import com.example.configtool.domain.validation.ValidationRule;

/** Supplies additional validation policies to the composition root. */
public interface ValidationRuleRepository {

    /** @return immutable or defensively copied validation rules */
    List<ValidationRule> findAll();
}
