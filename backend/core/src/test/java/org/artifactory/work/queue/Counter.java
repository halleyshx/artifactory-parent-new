package org.artifactory.work.queue;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Shay Bagants
 */
public class Counter {
    private List<AtomicInteger> counts;

    public Counter(List<AtomicInteger> counts) {
        this.counts = counts;
    }

    public Integer count(IntegerWorkItem integerWorkItem) {
        return counts.get(integerWorkItem.getValue()).incrementAndGet();
    }
}
