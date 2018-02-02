/*
 *
 * Copyright 2016 JFrog Ltd. All rights reserved.
 * JFROG PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package org.artifactory.work.queue;

import ch.qos.logback.classic.Level;
import org.artifactory.test.TestUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.TimeUnit.MINUTES;

/**
 * @author gidis
 */
@Test
public class NonBlockingOnWriteQueueTest extends WorkQueueTestBase {

    @BeforeClass
    public void setup() {
        TestUtils.setLoggingLevel("org.artifactory.work.queue", Level.WARN);
    }

    @Test
    public void addConcurrentAddRemoveTest() throws InterruptedException {
        Double maxWorkItemNumber = 100d;
        Integer numberOfProducerThreads = 100;
        Integer numberOfInputs = 10000;
        Integer numberOfConsumerThreads = 8;
        ExecutorService producerExecutor = Executors.newFixedThreadPool(numberOfProducerThreads);
        NonBlockingOnWriteQueue<IntegerWorkItem> queue = new NonBlockingOnWriteQueue<>(QUEUE_NAME);
        // Create producer threads that will fill the queue
        for (int i = 0; i < numberOfProducerThreads; i++) {
            producerExecutor.submit(() -> {
                for (int j = 0; j < numberOfInputs; j++) {
                    IntegerWorkItem workItem = new IntegerWorkItem((int) (Math.random() * maxWorkItemNumber));
                    queue.addToPending(workItem, null);
                }
            });
        }
        // Create Consumer threads that will execute the jobs
        AtomicInteger counter = new AtomicInteger();
        ExecutorService consumerExecutor = Executors.newFixedThreadPool(numberOfConsumerThreads);
        for (int i = 0; i < numberOfConsumerThreads; i++) {
            consumerExecutor.submit(() -> {
                while (!producerExecutor.isTerminated() || queue.getQueueSize() > 0 || queue.getRunningSize() > 0) {
                    WorkQueuePromotedItem<IntegerWorkItem> promotion = queue.promote();
                    if (promotion != null) {
                        queue.remove(promotion);
                        counter.getAndIncrement();
                    }
                }
            });
        }

        producerExecutor.shutdown();
        producerExecutor.awaitTermination(2, MINUTES);
        consumerExecutor.shutdown();
        consumerExecutor.awaitTermination(2, MINUTES);
        Assert.assertEquals(queue.getRunningSize(), 0);
        Assert.assertEquals(queue.getQueueSize(), 0);
        Assert.assertTrue(counter.get() >= 100);
        Assert.assertTrue(counter.get() <= 1000);
        System.out.println("Number of successful promotions (removals): " + counter.get());
    }
}