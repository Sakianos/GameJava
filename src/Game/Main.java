package Game;

import engine.commands.*;
import engine.core.DataLoader;
import engine.core.GameState;
import engine.core.Item;
import engine.core.Room;
import engine.core.CommandConfig;
import engine.core.EventExecutor;
import engine.core.SaveManager;
import engine.parser.CommandParser;
import engine.parser.GreekSemanticParser;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        outer:
        while (true) {
            // Φρέσκια αρχικοποίηση κάθε φορά που επιστρέφουμε στο menu
            GameState state = new GameState();
            DataLoader dataLoader = new DataLoader();
            dataLoader.loadGame(state, "src/resources/map.json");

            List<CommandConfig> commandConfigs = dataLoader.loadCommandConfigs("src/resources/commands.json");
            CommandRegistry registry = new CommandRegistry(commandConfigs);

            GreekSemanticParser greekSemanticParser = null;
            try {
                greekSemanticParser = new GreekSemanticParser(new HashMap<>(), "src/resources/greek_grammar.json");
            } catch (IOException e) {
                System.err.println("Προσοχή: Ο Semantic Parser δεν φορτώθηκε: " + e.getMessage());
            }

            EventExecutor eventExecutor = null;
            try {
                eventExecutor = new EventExecutor("src/resources/events.json");
            } catch (IOException e) {
                System.err.println("Προσοχή: Τα events δεν φορτώθηκαν: " + e.getMessage());
            }

            CommandParser parser = new CommandParser(registry, greekSemanticParser, eventExecutor);

            // --- ΚΥΡΙΟ MENU ---
            System.out.println("====================================================");
            System.out.println("             " + state.getStoryTitle());
            System.out.println("====================================================");
            System.out.println();

            List<SaveManager.SaveInfo> saves = SaveManager.listSaves();
            boolean hasAnySave = saves.stream().anyMatch(s -> s.exists);

            boolean loadSave = false;
            if (hasAnySave) {
                System.out.println("  1. Νέο Παιχνίδι");
                System.out.println("  2. Φόρτωση Παιχνιδιού");
                System.out.println();
                System.out.print("  Επιλογή: ");
                loadSave = scanner.nextLine().trim().equals("2");
            } else {
                System.out.println("  1. Νέο Παιχνίδι");
                System.out.println();
                System.out.print("  Πατήστε Enter για να ξεκινήσετε...");
                scanner.nextLine();
            }

            System.out.println();
            System.out.println("====================================================");

            if (loadSave) {
                int slot = pickSlot(scanner, saves, "  ΦΟΡΤΩΣΗ ΠΑΙΧΝΙΔΙΟΥ", true);
                state.setCurrentSlot(slot);
                SaveManager.load(state, slot);
                System.out.println("[ Φορτώθηκε Slot " + slot + " ]");
                System.out.println("----------------------------------------------------");
            } else {
                System.out.println("  Επιλογή Δυσκολίας:");
                System.out.println("  1. Εύκολο");
                System.out.println("  2. Κανονικό");
                System.out.println("  3. Δύσκολο");
                System.out.println();
                System.out.print("  Επιλογή: ");
                String diff = scanner.nextLine().trim();
                switch (diff) {
                    case "1":
                        state.setDifficulty("Εύκολο");
                        state.setCoins(15);
                        System.out.println("\n  [ Εύκολο: Inventory 5 θέσεων, 15 νομίσματα, αντικείμενα εμφανίζονται αυτόματα ]");
                        break;
                    case "3":
                        state.setDifficulty("Δύσκολο");
                        state.setCoins(0);
                        System.out.println("\n  [ Δύσκολο: Inventory 3 θέσεων, 0 νομίσματα ]");
                        break;
                    default:
                        state.setDifficulty("Κανονικό");
                        state.setCoins(5);
                        System.out.println("\n  [ Κανονικό: Inventory 4 θέσεων, 5 νομίσματα ]");
                        break;
                }

                System.out.println();
                int slot = pickSlot(scanner, SaveManager.listSaves(), "  ΑΠΟΘΗΚΕΥΣΗ ΣΕ", false);
                if (SaveManager.hasSave(slot)) SaveManager.deleteSave(slot);
                state.setCurrentSlot(slot);

                System.out.println();
                System.out.println("====================================================");
                printSmooth(state.getStoryIntro(), 25);
                System.out.println("----------------------------------------------------");
            }

            Room current = state.getCurrentRoom();
            if (current != null) {
                System.out.println("\n>>> " + current.getName().toUpperCase() + " <<<");
                printSmooth(current.getDescription(), 20);
            }

            // --- GAME LOOP ---
            while (true) {
                System.out.print("\n > ");
                String input = scanner.nextLine();

                if (input.equalsIgnoreCase("quit game")) {
                    System.out.println("\n[ Επιστροφή στο μενού... ]\n");
                    continue outer;
                }

                if (input.equalsIgnoreCase("show inventory") || input.equalsIgnoreCase("inventory")) {
                    showInventoryLoop(scanner, state);
                    continue;
                }

                String result = parser.parseAndExecute(input, state);
                System.out.println("\n----------------------------------------------------");
                printSmooth(result, 20);
                System.out.println("----------------------------------------------------");
            }
        }
    }

    public static void showInventoryLoop(Scanner scanner, GameState state) {
        while (true) {
            System.out.println("\n====================================================");
            Collection<Item> inventory = state.getInventory();
            System.out.println("  INVENTORY  (" + inventory.size() + "/" + state.getMaxInventorySize() + ")");
            System.out.println("----------------------------------------------------");

            if (inventory.isEmpty()) {
                System.out.println("  Το inventory σου είναι άδειο.");
            } else {
                for (Item item : inventory) {
                    System.out.println("  • " + item.getEnglishName() + ": " + item.getEnglishDescription());
                }
            }

            System.out.println("\n  Χρήματα: " + state.getCoins() + " νομίσματα");
            System.out.println("----------------------------------------------------");
            System.out.println("  drop [όνομα αντικειμένου]  →  άφησε το κάτω");
            System.out.println("  x                          →  έξοδος");
            System.out.println("====================================================");
            System.out.print("\n > ");

            String input = scanner.nextLine().trim();

            if (input.equalsIgnoreCase("x")) {
                break;
            }

            if (input.toLowerCase().startsWith("drop ")) {
                String itemName = input.substring(5).trim();
                Item dropped = state.removeFromInventoryByName(itemName);
                if (dropped == null) {
                    System.out.println("\n  Δεν βρέθηκε '" + itemName + "' στο inventory σου.");
                } else {
                    state.getCurrentRoom().addItem(dropped);
                    SaveManager.save(state);
                    System.out.println("\n  Άφησες '" + dropped.getName() + "' κάτω.");
                }
            } else {
                System.out.println("\n  Άγνωστη εντολή. Χρησιμοποίησε drop [όνομα] ή x.");
            }
        }
    }

    /**
     * Εμφανίζει τη λίστα save slots και ζητά από τον παίκτη να επιλέξει έναν.
     *
     * @param onlyExisting  αν true, μόνο slots που έχουν save είναι επιλέξιμα (για φόρτωση)
     */
    public static int pickSlot(Scanner scanner, List<SaveManager.SaveInfo> saves,
                                String title, boolean onlyExisting) {
        while (true) {
            System.out.println("====================================================");
            System.out.println(title);
            System.out.println("====================================================");
            for (SaveManager.SaveInfo info : saves) {
                System.out.println(info.toMenuLine());
            }
            System.out.println("----------------------------------------------------");
            System.out.print("  Επιλογή (1-" + SaveManager.MAX_SLOTS + "): ");

            String input = scanner.nextLine().trim();
            try {
                int choice = Integer.parseInt(input);
                if (choice < 1 || choice > SaveManager.MAX_SLOTS) {
                    System.out.println("  Μη έγκυρη επιλογή. Δοκίμασε ξανά.");
                    continue;
                }
                if (onlyExisting && !saves.get(choice - 1).exists) {
                    System.out.println("  Αυτό το slot είναι άδειο. Επίλεξε άλλο.");
                    continue;
                }
                return choice;
            } catch (NumberFormatException e) {
                System.out.println("  Γράψε έναν αριθμό.");
            }
        }
    }

    public static void printSmooth(String text, int delay) {
        if (text == null) return;
        for (char c : text.toCharArray()) {
            System.out.print(c);
            System.out.flush();
            try {
                if (c == '.' || c == '!' || c == '?') Thread.sleep(delay * 10);
                else if (c == ',') Thread.sleep(delay * 4);
                else Thread.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        System.out.println();
    }
}
