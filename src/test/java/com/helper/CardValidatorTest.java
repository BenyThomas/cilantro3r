package com.helper;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CardValidatorTest {

    @Test
    void validateVisaCardNumber() {
        String validVisaCard = "4021 4102 3000 0000";
        assertTrue(CardValidator.validateCardNumber(validVisaCard), "The VISA card number should be valid.");
    }

    @Test
    void validateInvalidCardNumber() {
        String invalidCard = "1234 5678 9012 3456";
        assertFalse(CardValidator.validateCardNumber(invalidCard), "The card number should be invalid.");
    }

    @Test
    void validateVisaCardWithoutSpaces() {
        String validVisaCard = "4021410230000000";
        assertTrue(CardValidator.validateCardNumber(validVisaCard), "The VISA card number without spaces should be valid.");
    }
}