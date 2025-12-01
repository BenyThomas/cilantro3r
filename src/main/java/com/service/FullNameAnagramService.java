package com.service;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.Map;

@Service
public class FullNameAnagramService {

    public boolean areAnagrams(String fullName1, String fullName2) {
        String cleaned1 = clean(fullName1);
        String cleaned2 = clean(fullName2);

        return buildFrequencyMap(cleaned1).equals(buildFrequencyMap(cleaned2));
    }

    public double getSimilarityPercentage(String fullName1, String fullName2) {
        String cleaned1 = clean(fullName1);
        String cleaned2 = clean(fullName2);

        Map<Character, Integer> freq1 = buildFrequencyMap(cleaned1);
        Map<Character, Integer> freq2 = buildFrequencyMap(cleaned2);

        int match = 0;

        for (Map.Entry<Character, Integer> entry : freq1.entrySet()) {
            char ch = entry.getKey();
            int freqInOther = freq2.getOrDefault(ch, 0);
            match += Math.min(entry.getValue(), freqInOther);
        }

        int maxLength = Math.max(cleaned1.length(), cleaned2.length());
        return maxLength > 0 ? ((double) match / maxLength) * 100 : 0.0;
    }

    private String clean(String input) {
        if (input == null) return "";
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        return normalized.replaceAll("[^a-zA-Z]", "").toLowerCase(); // remove spaces, digits, punctuation
    }

    private Map<Character, Integer> buildFrequencyMap(String input) {
        Map<Character, Integer> map = new HashMap<>();
        for (char ch : input.toCharArray()) {
            map.put(ch, map.getOrDefault(ch, 0) + 1);
        }
        return map;
    }
}


