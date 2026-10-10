package com.example.configtool.domain.validation;

import java.util.List;

@FunctionalInterface
public interface ValidationRule {

    List<ValidationIssue> validate(ValidationContext context);
}
