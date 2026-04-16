package se.chasacademy.concurrency;

/**
 * ═══════════════════════════════════════════════════════════
 *  Uppgift 1 + 2 – Race Simulator & Race Condition Bug
 * ═══════════════════════════════════════════════════════════
 *
 *  Uppgift 1: Visualisera hur trådar körs icke-deterministiskt.
 *  Uppgift 2: Se ett klassiskt race condition bug — vinnaren kanske inte är korrekt.
 *
 *  Kör programmet flera gånger och observera:
 *   - Ordningen ändras varje gång  → icke-deterministisk
 *   - Vinnaren kan vara fel         → race condition
 */
public class RaceSimulator {

    // ── ANSI-färgkoder för snygg output ──────────────────────────────────────
    static final String RESET  = "\u001B[0m";
    static final String RED    = "\u001B[31m";
    static final String GREEN  = "\u001B[32m";
    static final String YELLOW = "\u001B[33m";
    static final String CYAN   = "\u001B[36m";
    static final String BOLD   = "\u001B[1m";

    static final String[] COLORS = {RED, GREEN, YELLOW, CYAN, "\u001B[35m"};

    // ── Uppgift 2: Delad variabel — ingen synkronisering! ────────────────────
    /**
     * RACE CONDITION: winner är en delad variabel utan synchronized.
     * Två trådar kan läsa winner == null samtidigt och båda skriva → fel vinnare!
     */
    static volatile String winner = null;

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Uppgift 1 – Simulerar en deltagare i ett lopp.
     *
     * Varje "steg" tar slumpmässig tid (0–500ms) → olika ordning varje körning.
     * join() i main() väntar tills ALLA trådar är klara innan programmet avslutas.
     */
    static void race(String name, String color) {
        try {
            for (int i = 1; i <= 5; i++) {
                Thread.sleep((int) (Math.random() * 500));
                System.out.println(color + name + " → steg " + i + "/5" + RESET);
            }

            // ── Uppgift 2: Race condition ──────────────────────────────────
            // Problem: Kontroll + tilldelning är INTE atomär.
            // Om tråd A läser winner == null och tråd B också läser winner == null
            // INNAN A hinner skriva → båda tror de är vinnare!
            if (winner == null) {
                // ↑ En annan tråd kan passera denna rad precis nu!
                winner = name;  // ← Inte thread-safe utan synkronisering
            }

            System.out.println(color + BOLD + "★ " + name + " FINISHED!" + RESET);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println(BOLD + "\n════════════════════════════════════");
        System.out.println("    🏁  RACE SIMULATOR  🏁");
        System.out.println("════════════════════════════════════" + RESET + "\n");

        // ── Uppgift 1: Skapa 5 trådar med olika namn ─────────────────────
        String[] runners = {"Usain", "Carl", "Yohan", "Asafa", "Justin"};
        Thread[] threads = new Thread[runners.length];

        for (int i = 0; i < runners.length; i++) {
            final int idx = i;
            threads[i] = new Thread(() -> race(runners[idx], COLORS[idx]));
        }

        // Starta alla trådar
        System.out.println("Starta racet! Alla deltagare startar nu...\n");
        for (Thread t : threads) t.start();

        // join() — väntar på att ALLA ska bli klara
        // Ta bort join() och se vad som händer: main() avslutas innan trådarna! → Extra-uppgift
        for (Thread t : threads) t.join();

        // ── Uppgift 2: Vem vann? ─────────────────────────────────────────
        System.out.println("\n" + BOLD + "═══════════════════════════════════════" + RESET);
        System.out.println(BOLD + "🏆  Winner: " + GREEN + winner + RESET + BOLD + "  🏆" + RESET);
        System.out.println(BOLD + "═══════════════════════════════════════" + RESET);

        System.out.println(YELLOW + """

        ┌─ REFLEKTION ─────────────────────────────────────────────────────┐
        │ Varför vinner olika varje gång?                                   │
        │   → Thread.sleep() är slumpmässig → obestämd exekveringsordning  │
        │   → OS schedulern bestämmer vilken tråd som får köra             │
        │                                                                   │
        │ Är vinnaren alltid korrekt?                                       │
        │   → NEJ! winner == null kan läsas av 2 trådar innan någon        │
        │      hinner skriva → race condition → fel vinnare möjlig!        │
        │                                                                   │
        │ Fix: synchronized (RaceSimulator.class) { if (winner == null)... }│
        └───────────────────────────────────────────────────────────────────┘
        """ + RESET);
    }
}
