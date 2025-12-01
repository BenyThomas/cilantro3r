package com.helper;

import com.service.XapiWebService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import philae.ach.TaResponse;

@Component
@RequiredArgsConstructor
public class CardValidator {
    public static boolean validateCardNumber(String cardNumber) {
        String sanitizedCardNumber = cardNumber.replaceAll("\\s+", ""); // Remove spaces
        int total = 0;

        // Process digits from right to left
        boolean alternate = false;
        for (int i = sanitizedCardNumber.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(sanitizedCardNumber.charAt(i));

            if (alternate) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9; // Subtract 9 if the result is a two-digit number
                }
            }

            total += digit;
            alternate = !alternate; // Flip the alternate flag
        }

        return total % 10 == 0;
    }

}
