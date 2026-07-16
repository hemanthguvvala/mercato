package com.interview.inventoryservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.interview.inventoryservice.entity.InventoryItem;
import com.interview.inventoryservice.repository.InventoryRepository;
import com.interview.inventoryservice.service.InventoryService;

/**
 * Concurrency "stampede" tests for the Phase-4 threading lesson.
 *
 * Each test fires CONCURRENT_ORDERS reservations of 1 unit, all released at the same instant,
 * against a product holding only INITIAL_STOCK units. The invariant: stock must NEVER go
 * negative (no overselling).
 *
 *   - optimistic (@Version): safe, but losers FAIL with ObjectOptimisticLockingFailureException
 *     and would need a retry in production.
 *   - pessimistic (SELECT ... FOR UPDATE): safe AND zero version failures — losers simply WAIT,
 *     then find the stock gone (clean InSufficientStockException).
 *
 * Isolated in-memory H2 (won't touch ./data/inventorydb); Eureka off; generous lock timeout so
 * pessimistic waiters don't spuriously time out under the burst.
 */
@Tag("integration") // full-context @SpringBootTest — excluded from the fast CI, runs in the integration phase
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:invtest;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
        "eureka.client.enabled=false"
})
class InventoryConcurrencyTest {

    private static final Long PRODUCT_ID = 1L;
    private static final int INITIAL_STOCK = 5;
    private static final int CONCURRENT_ORDERS = 20;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryRepository inventoryRepository;

    @FunctionalInterface
    interface ReserveFn {
        void apply(Long productId, int quantity);
    }

    @Test
    void optimisticLock_neverOversells_butLosersFailAndMustRetry() throws InterruptedException {
        Result r = runStampede("OPTIMISTIC (@Version)", inventoryService::reserve);

        assertTrue(r.finalStock() >= 0, "OVERSOLD! stock went negative: " + r.finalStock());
        assertEquals(INITIAL_STOCK - r.finalStock(), r.succeeded(), "each success removes exactly one unit");
    }

    @Test
    void pessimisticLock_neverOversells_andHasNoVersionFailures() throws InterruptedException {
        Result r = runStampede("PESSIMISTIC (FOR UPDATE)", inventoryService::reservePessimistic);

        assertTrue(r.finalStock() >= 0, "OVERSOLD! stock went negative: " + r.finalStock());
        assertEquals(INITIAL_STOCK - r.finalStock(), r.succeeded(), "each success removes exactly one unit");
        // the whole point of pessimistic locking: serialized access => no optimistic-lock clashes
        assertTrue(r.failureTypes().stream().noneMatch(t -> t.contains("OptimisticLocking")),
                "pessimistic locking should produce NO optimistic-lock failures, got: " + r.failureTypes());
    }

    /** Resets stock, fires CONCURRENT_ORDERS reservations simultaneously, returns the tally. */
    private Result runStampede(String label, ReserveFn reserveFn) throws InterruptedException {
        InventoryItem item = inventoryRepository.findByProductId(PRODUCT_ID).orElseThrow();
        item.setAvailableQuantity(INITIAL_STOCK);
        inventoryRepository.saveAndFlush(item);

        AtomicInteger succeeded = new AtomicInteger();
        AtomicInteger failed = new AtomicInteger();
        ConcurrentLinkedQueue<String> failureTypes = new ConcurrentLinkedQueue<>();

        ExecutorService pool = Executors.newFixedThreadPool(CONCURRENT_ORDERS);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneGate = new CountDownLatch(CONCURRENT_ORDERS);

        for (int i = 0; i < CONCURRENT_ORDERS; i++) {
            pool.submit(() -> {
                try {
                    startGate.await();
                    reserveFn.apply(PRODUCT_ID, 1);
                    succeeded.incrementAndGet();
                } catch (Exception e) {
                    failed.incrementAndGet();
                    failureTypes.add(e.getClass().getSimpleName());
                } finally {
                    doneGate.countDown();
                }
            });
        }
        startGate.countDown();   // FIRE — all threads charge the last units together
        doneGate.await();
        pool.shutdown();

        int finalStock = inventoryRepository.findByProductId(PRODUCT_ID).orElseThrow().getAvailableQuantity();
        System.out.println("===== " + label + " =====");
        System.out.println("initial stock     : " + INITIAL_STOCK);
        System.out.println("concurrent orders : " + CONCURRENT_ORDERS);
        System.out.println("succeeded         : " + succeeded.get());
        System.out.println("failed            : " + failed.get());
        System.out.println("final stock       : " + finalStock);
        System.out.println("failure types     : " + failureTypes);
        System.out.println("=================================");

        return new Result(succeeded.get(), failed.get(), finalStock, failureTypes);
    }

    private record Result(int succeeded, int failed, int finalStock, ConcurrentLinkedQueue<String> failureTypes) {
    }
}
