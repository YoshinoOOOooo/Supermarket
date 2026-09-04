package com.supermarket.pricing;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PricingCalculatorTest {

    @Test
    void calculatesApplesAndStrawberries() {
        assertMoney("55.00", calculate(items(
                item("APPLE", 2, "8.00"),
                item("STRAWBERRY", 3, "13.00"))));
    }

    @Test
    void includesMango() {
        assertMoney("75.00", calculate(items(
                item("APPLE", 2, "8.00"),
                item("STRAWBERRY", 3, "13.00"),
                item("MANGO", 1, "20.00"))));
    }

    @Test
    void discountsStrawberriesToEightyPercent() {
        assertMoney("67.20", calculateWithStrawberryDiscount(twoThreeOneItems()));
    }

    @Test
    void appliesThresholdOnceAfterProductDiscount() {
        assertMoney("102.00", calculateWithAllPromotions(items(
                item("APPLE", 5, "8.00"),
                item("STRAWBERRY", 5, "13.00"),
                item("MANGO", 1, "20.00"))));
    }

    @Test
    void appliesReductionAtExactlyThreshold() {
        assertMoney("90.00", calculateWithThreshold(items(item("MANGO", 5, "20.00"))));
    }

    @Test
    void doesNotApplyReductionBelowThreshold() {
        assertMoney("99.99", calculateWithThreshold(items(item("OTHER", 1, "99.99"))));
    }

    @Test
    void returnsZeroForZeroQuantityItems() {
        PricingResult result = new PricingCalculator().calculate(
                items(item("APPLE", 0, "8.00")),
                allPromotions());

        assertMoney("0.00", result.getOriginalAmount());
        assertMoney("0.00", result.getDiscountAmount());
        assertMoney("0.00", result.getPayableAmount());
    }

    @Test
    void rejectsNegativeQuantity() {
        assertThrows(IllegalArgumentException.class,
                () -> item("APPLE", -1, "8.00"));
    }

    @Test
    void appliesEveryRuleExactlyOnceInOrder() {
        final List<Integer> applied = new java.util.ArrayList<Integer>();
        PricingRule later = recordingRule(200, applied);
        PricingRule earlier = recordingRule(100, applied);

        new PricingCalculator().calculate(Collections.<PricingItem>emptyList(),
                Arrays.asList(later, earlier));

        assertEquals(Arrays.asList(100, 200), applied);
    }

    @Test
    void rejectsPayableAmountBelowZero() {
        assertThrows(IllegalStateException.class, () -> new PricingCalculator().calculate(
                items(item("OTHER", 1, "5.00")),
                Collections.<PricingRule>singletonList(
                        new OrderThresholdReductionRule(BigDecimal.ZERO, new BigDecimal("10.00")))));
    }

    @Test
    void exposesNormalizedTotalsAndImmutableLineResults() {
        PricingResult result = new PricingCalculator().calculate(
                items(item("OTHER", 1, "1.005")), Collections.<PricingRule>emptyList());

        assertMoney("1.01", result.getOriginalAmount());
        assertMoney("0.00", result.getDiscountAmount());
        assertMoney("1.01", result.getPayableAmount());
        assertThrows(UnsupportedOperationException.class, () -> result.getLineResults().clear());
    }

    private BigDecimal calculate(List<PricingItem> items) {
        return new PricingCalculator().calculate(items, Collections.<PricingRule>emptyList()).getPayableAmount();
    }

    private BigDecimal calculateWithStrawberryDiscount(List<PricingItem> items) {
        return new PricingCalculator().calculate(items,
                Collections.<PricingRule>singletonList(new ProductDiscountRule(strawberryRates())))
                .getPayableAmount();
    }

    private BigDecimal calculateWithAllPromotions(List<PricingItem> items) {
        return new PricingCalculator().calculate(items, allPromotions()).getPayableAmount();
    }

    private BigDecimal calculateWithThreshold(List<PricingItem> items) {
        return new PricingCalculator().calculate(items,
                Collections.<PricingRule>singletonList(
                        new OrderThresholdReductionRule(new BigDecimal("100.00"), new BigDecimal("10.00"))))
                .getPayableAmount();
    }

    private static List<PricingItem> twoThreeOneItems() {
        return items(item("APPLE", 2, "8.00"), item("STRAWBERRY", 3, "13.00"),
                item("MANGO", 1, "20.00"));
    }

    private static List<PricingRule> allPromotions() {
        return Arrays.<PricingRule>asList(
                new OrderThresholdReductionRule(new BigDecimal("100.00"), new BigDecimal("10.00")),
                new ProductDiscountRule(strawberryRates()));
    }

    private static Map<String, BigDecimal> strawberryRates() {
        Map<String, BigDecimal> rates = new HashMap<String, BigDecimal>();
        rates.put("STRAWBERRY", new BigDecimal("0.80"));
        return rates;
    }

    private static PricingRule recordingRule(final int order, final List<Integer> applied) {
        return new PricingRule() {
            @Override
            public int getOrder() {
                return order;
            }

            @Override
            public void apply(PricingContext context) {
                applied.add(order);
            }
        };
    }

    private static PricingItem item(String code, int quantity, String unitPrice) {
        return new PricingItem(code, quantity, new BigDecimal(unitPrice));
    }

    private static List<PricingItem> items(PricingItem... items) {
        return Arrays.asList(items);
    }

    private static void assertMoney(String expected, BigDecimal actual) {
        assertEquals(new BigDecimal(expected), actual);
    }
}
