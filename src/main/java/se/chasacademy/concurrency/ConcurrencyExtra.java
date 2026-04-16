package se.chasacademy.concurrency;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ═══════════════════════════════════════════════════════════
 *  Extra – Live Leaderboard & Stress Test (20+ trådar)
 * ═══════════════════════════════════════════════════════════
 *
 *  Extra 1: Live Leaderboard — se vem som leder racet just nu.
 *           En separat "display-tråd" pollar läget varje 200ms.
 *
 *  Extra 2: Stress test — 20 trådar startar samtidigt.
 *           Observera hur OS schedulern hanterar det.
 *
 *  Extra 3: Vad händer utan join()?
 *           Kommentera bort join() och kör → main() avslutas direkt!
 */
public class ConcurrencyExtra {

    static final String RESET  = "\u001B[0m";
    static final String GREEN  = "\u001B[32m";
    static final String YELLOW = "\u001B[33m";
    static final String CYAN   = "\u001B[36m";
    static final String RED    = "\u001B[31m";
    static final String BOLD   = "\u001B[1m";

    /**
     * Håller varje deltagares nuvarande steg — trådsäkert.
     * ConcurrentHashMap: trådsäker version av HashMap.
     * AtomicInteger: trådsäker heltalstäknare (ingen synchronized behövs).
     */
    static final Map<String, AtomicInteger> leaderboard = new ConcurrentHashMap<>();

    // ─────────────────────────────────────────────────────────────────────────
    // Extra 1 – Live Leaderboard
    // ─────────────────────────────────────────────────────────────────────────

    static void runWithLeaderboard(String name, int steps) {
        leaderboard.put(name, new AtomicInteger(0));
        try {
            for (int i = 1; i <= steps; i++) {
                Thread.sleep((int) (Math.random() * 400) + 50);
                leaderboard.get(name).set(i);  // AtomicInteger.set() är trådsäkert!
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Displaytråd: skriver ut leaderboard varje 200ms tills loppet är klart. */
    static void displayLeaderboard(int totalSteps) throws InterruptedException {
        while (true) {
            Thread.sleep(300);

            System.out.println(BOLD + "\033[2J\033[H" + // Rensa terminalen
                    "══════════════════════════════\n" +
                    "      🏆  LIVE LEADERBOARD  🏆\n" +
                    "══════════════════════════════" + RESET);

            leaderboard.entrySet().stream()
                    .sorted((a, b) -> Integer.compare(
                            b.getValue().get(), a.getValue().get()))
                    .forEach(entry -> {
                        String name = entry.getKey();
                        int step = entry.getValue().get();
                        int pct = (step * 100) / totalSteps;
                        String bar = "█".repeat(pct / 5) + "░".repeat(20 - pct / 5);
                        String color = step == totalSteps ? GREEN : CYAN;
                        System.out.printf("%s  %-10s [%s] %d/%d%s%n",
                                color, name, bar, step, totalSteps, RESET);
                    });

            // Klar om alla nått sista steget
            boolean allDone = leaderboard.values().stream()
                    .allMatch(v -> v.get() >= totalSteps);
            if (allDone) break;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Extra 2 – Stress test: 20+ trådar
    // ─────────────────────────────────────────────────────────────────────────

    static void stressTest(int numThreads) throws InterruptedException {
        System.out.println(BOLD + "\n══════════════════════════════════════════════════");
        System.out.println("  STRESS TEST: " + numThreads + " TRÅDAR STARTAR SAMTIDIGT");
        System.out.println("══════════════════════════════════════════════════" + RESET);

        AtomicInteger finishedCount = new AtomicInteger(0);
        AtomicInteger totalSteps   = new AtomicInteger(0);
        CountDownLatch startLatch  = new CountDownLatch(1); // alla startar exakt samtidigt
        CountDownLatch doneLatch   = new CountDownLatch(numThreads);

        Thread[] threads = new Thread[numThreads];
        for (int i = 0; i < numThreads; i++) {
            final String name = "T-" + String.format("%02d", i + 1);
            threads[i] = new Thread(() -> {
                try {
                    startLatch.await(); // vänta på startskottet
                    for (int s = 0; s < 5; s++) {
                        Thread.sleep((int) (Math.random() * 200));
                        totalSteps.incrementAndGet();
                    }
                    System.out.println(GREEN + "✔ " + name + " klar" + RESET);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    finishedCount.incrementAndGet();
                    doneLatch.countDown();
                }
            });
        }

        for (Thread t : threads) t.start();

        System.out.println(YELLOW + numThreads + " trådar redo — startar nu!" + RESET + "\n");
        startLatch.countDown(); // startskottet — alla trådar släpps

        // ── Extra: Vad händer utan join()? ───────────────────────────────
        // Kommentera bort doneLatch.await() → main() skriver ut resultatet
        // INNAN trådarna är klara → totalSteps och finishedCount stämmer inte!
        doneLatch.await();

        System.out.println(BOLD + "\n════════════════════════════════════");
        System.out.printf("  Klara trådar:  %d / %d%n", finishedCount.get(), numThreads);
        System.out.printf("  Totalt steg:   %d%n", totalSteps.get());
        System.out.println("════════════════════════════════════" + RESET);

        System.out.println(RED + """
        ┌─ OBSERVATION ──────────────────────────────────────────────────────┐
        │ Vad märker du med 20+ trådar?                                      │
        │   → Utskrifterna är oordnade — trådarna avbryts av varandra       │
        │   → OS schedulern växlar snabbt mellan trådar → "context switch"  │
        │   → Fler trådar ≠ alltid snabbare (overhead ökar)                 │
        │                                                                    │
        │ Ta bort doneLatch.await() och kör:                                 │
        │   → main() skriver ut INNAN alla trådar är klara!                 │
        │   → finishedCount = 0 eller delvis → icke-deterministiskt         │
        └────────────────────────────────────────────────────────────────────┘
        """ + RESET);
    }

    // ─────────────────────────────────────────────────────────────────────────

    public static void main(String[] args) throws InterruptedException {
        // ── Extra 1: Live Leaderboard ────────────────────────────────────
        System.out.println(BOLD + "\n════════════════════════════════════════");
        System.out.println("  EXTRA 1: LIVE LEADERBOARD");
        System.out.println("═════════════════════════════════════════" + RESET);

        String[] runners = {"Usain", "Carl", "Yohan", "Asafa", "Justin"};
        int STEPS = 10;

        Thread[] raceThreads = new Thread[runners.length];
        for (int i = 0; i < runners.length; i++) {
            final int idx = i;
            raceThreads[i] = new Thread(() -> runWithLeaderboard(runners[idx], STEPS));
        }

        for (Thread t : raceThreads) t.start();

        // Display-tråd läser leaderboard och skriver ut det
        Thread displayThread = new Thread(() -> {
            try { displayLeaderboard(STEPS); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        });
        displayThread.start();

        for (Thread t : raceThreads) t.join();
        displayThread.join();

        // ── Kommentera in för att testa utan join() ──────────────────────
        // System.out.println("(join() borttagen — detta skrivs ut direkt!)");

        // ── Extra 2: Stress Test med 25 trådar ───────────────────────────
        stressTest(25);

        System.out.println(CYAN + BOLD + "\n✅ Alla extra-uppgifter klara!" + RESET);
    }
}
