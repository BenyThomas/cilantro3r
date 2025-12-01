package com.queue;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;

@Slf4j
public class QueueConsumerTest {

    private final QueueConsumer queueConsumer = new QueueConsumer();

    @Test
    void testExactMatch() {
        double score = queueConsumer.namesMatch("BanyeNzachi mPanda Thomas", "Banyenzachi Mpanda Thomas");
        log.info("Exact Match {}%", String.format("%.2f", score * 100));
        assertEquals(1.0, score, 0.001);
    }

    @Test
    void testSimilarNames() {
        double score = queueConsumer.namesMatch("BanyeNzachi mPanda Thomas", "Banyenzachi Tell Thomas");
        log.info("Similar Names {}%", String.format("%.2f", score * 100));
        assertTrue(score > 0.85 && score < 1.0);
    }

    @Test
    void testDifferentNames() {
        double score = queueConsumer.namesMatch("BanyeNzachi mPanda Thomas", "Baraka Tell Thomas");
        log.info("Different Names {}%", String.format("%.2f", score * 100));
        assertTrue(score < 0.78);
    }
    @Test
    void testReversedNames() {
        double score = queueConsumer.namesMatch("BanyeNzachi mPanda Thomas", "Thomas Mpanda Banyenzachi");
        log.info("Reversed Names {}%", String.format("%.2f", score * 100));
        assertTrue(score < 0.78);
    }

    @Test
    void testNullName1() {
        double score = queueConsumer.namesMatch(null, "Bob");
        log.info("Null Name 1 {}%", String.format("%.2f", score * 100));
        assertEquals(0.0, score);
    }

    @Test
    void testNullName2() {
        double score = queueConsumer.namesMatch("Alice", null);
        log.info("Null Name 2 {}%", String.format("%.2f", score * 100));
        assertEquals(0.0, score);
    }

    @Test
    void testBothNull() {
        double score = queueConsumer.namesMatch(null, null);
        log.info("Both Names Null {}%", String.format("%.2f", score * 100));
        assertEquals(0.0, score);
    }

    @Test
    void testWhitespaceAndCaseInsensitive() {
        double score = queueConsumer.namesMatch("  BanyeNzachi mPanda Thomas", "BanYenzachi Mpanda Thomas");
        log.info("White Space and Case Insensitive {}%", String.format("%.2f", score * 100));
        assertEquals(1.0, score, 0.001);
    }
}
