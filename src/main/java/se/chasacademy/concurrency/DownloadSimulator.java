package se.chasacademy.concurrency;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ═══════════════════════════════════════════════════════════
 *  Uppgift 3 + 4 – Download Simulator & Thread Pool
 * ═══════════════════════════════════════════════════════════
 *
 *  Uppgift 3: Simulera flera filer som laddas ner parallellt.
 *             Observera att varje fil går i sin egen takt.
 *
 *  Uppgift 4: Begränsa antalet samtidiga nedladdningar med en
 *             ThreadPool (Executors.newFixedThreadPool(2)).
 *             Resten köar och väntar på en ledig plats.
 *
 *  Kör programmet och jämför de två delarna:
 *   Del 1 (obegränsad): Alla 4 filer körs SAMTIDIGT
 *   Del 2 (pool=2):     Bara 2 filer körs åt gången, resten köar
 */
public class DownloadSimulator {

    static final String RESET  = "\u001B[0m";
    static final String GREEN  = "\u001B[32m";
    static final String YELLOW = "\u001B[33m";
    static final String CYAN   = "\u001B[36m";
    static final String BLUE   = "\u001B[34m";
    static final String BOLD   = "\u001B[1m";
    static final String DIM    = "\u001B[2m";

    // Håller reda på hur många trådar som körs just nu (Uppgift 4)
    static final AtomicInteger activeDownloads = new AtomicInteger(0);

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Uppgift 3 – Simulerar nedladdning av en fil.
     *
     * Varje "procentsteg" tar 100–800ms slumpmässigt.
     * Filer med kort sleep-tid verkar ladda snabbt —
     * i verkligheten = snabbt nätverkssvar / liten fil.
     */
    static void download(String file, String color) {
        int active = activeDownloads.incrementAndGet();
        System.out.println(color + "▶ " + file + " startar   [aktiva: " + active + "]" + RESET);

        try {
            int steps = 5;
            for (int i = 1; i <= steps; i++) {
                Thread.sleep((int) (Math.random() * 700) + 100);
                int percent = i * 20;
                String bar = progressBar(percent);
                System.out.println(color + "  " + file + " " + bar + " " + percent + "%" + RESET);
            }
            System.out.println(color + BOLD + "✔ " + file + " DONE" + RESET);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            activeDownloads.decrementAndGet();
        }
    }

    /** Skapar en enkel ASCII progress bar. */
    private static String progressBar(int percent) {
        int filled = percent / 10;
        return "["
                + "█".repeat(filled)
                + DIM + "░".repeat(10 - filled) + RESET
                + "]";
    }

    // ─────────────────────────────────────────────────────────────────────────

    public static void main(String[] args) throws InterruptedException {
        String[] files = {"video.mp4", "photo.jpg", "music.mp3", "docs.zip"};
        String[] colors = {"\u001B[31m", "\u001B[32m", "\u001B[33m", "\u001B[36m"};

        // ════════════════════════════════════════════════════════════════════
        // DEL 1 – Uppgift 3: Obegränsade trådar (alla kör parallellt)
        // ════════════════════════════════════════════════════════════════════
        System.out.println(BOLD + "\n══════════════════════════════════════════════════");
        System.out.println("  DEL 1: Obegränsad parallellism (4 trådar direkt)");
        System.out.println("══════════════════════════════════════════════════" + RESET);
        System.out.println(CYAN + "Alla 4 filer startar SAMTIDIGT → ingen kö" + RESET + "\n");

        activeDownloads.set(0);
        Thread[] directThreads = new Thread[files.length];
        for (int i = 0; i < files.length; i++) {
            final int idx = i;
            directThreads[i] = new Thread(() -> download(files[idx], colors[idx]));
        }
        for (Thread t : directThreads) t.start();
        for (Thread t : directThreads) t.join(); // vänta på alla

        // ════════════════════════════════════════════════════════════════════
        // DEL 2 – Uppgift 4: Begränsad pool (max 2 simultana)
        // ════════════════════════════════════════════════════════════════════
        System.out.println(BOLD + "\n══════════════════════════════════════════════════");
        System.out.println("  DEL 2: Begränsad ThreadPool (max 2 trådar)");
        System.out.println("══════════════════════════════════════════════════" + RESET);
        System.out.println(YELLOW + "Bara 2 körs åt gången — resten väntar i kön!" + RESET + "\n");

        /*
         * newFixedThreadPool(2):
         *   - Exakt 2 trådar i poolen
         *   - Om alla 2 är upptagna → uppgiften köar i en BlockingQueue
         *   - Precis som ett café med 2 kassor: kund 3+4 väntar i kö
         *
         * I verkliga system: begränsar DB-connections, HTTP-calls, etc.
         */
        ExecutorService pool = Executors.newFixedThreadPool(2);
        activeDownloads.set(0);

        for (int i = 0; i < files.length; i++) {
            final int idx = i;
            pool.submit(() -> download(files[idx], colors[idx]));
        }

        pool.shutdown();
        pool.awaitTermination(30, TimeUnit.SECONDS);

        // ════════════════════════════════════════════════════════════════════
        // Reflektion
        // ════════════════════════════════════════════════════════════════════
        System.out.println(BLUE + """

        ┌─ REFLEKTION ──────────────────────────────────────────────────────┐
        │ Varför går vissa filer snabbare?                                   │
        │   → Thread.sleep() är slumpmässig → simulerar varierande          │
        │      nätverkshastighet / filstorlek                                │
        │                                                                    │
        │ DEL 1 vs DEL 2 — skillnaden:                                       │
        │   DEL 1: 4 trådar körs parallellt → snabbare totalt               │
        │   DEL 2: Max 2 åt gången → sämre total tid, men kontrollerat      │
        │                                                                    │
        │ Hur ser det ut i ett riktigt program?                              │
        │   → HTTP-klienter: begränsar simultana anrop till externa API:er  │
        │   → DB-connections: pool.size = max simultana DB-frågor           │
        │   → Spring Boot: embedded Tomcat har en thread pool (default 200) │
        └────────────────────────────────────────────────────────────────────┘
        """ + RESET);
    }
}
