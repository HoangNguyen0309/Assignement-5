package market;

import java.util.List;

import characters.Hero;
import config.GameBalance;
import io.InputHandler;
import io.Renderer;
import items.Armor;
import items.Item;
import items.Potion;
import items.Weapon;

public class MarketController {

    private Renderer renderer;
    private InputHandler input;

    public MarketController(Renderer renderer, InputHandler input) {
        this.renderer = renderer;
        this.input = input;
    }

    public void openMarket(Market market, List<Hero> party) {
        boolean done = false;
        while (!done) {
            renderer.renderMessage("=== MARKET ===");
            renderer.renderMessage("1) Buy");
            renderer.renderMessage("2) Sell");
            renderer.renderMessage("3) Leave");

            int choice = input.readInt();
            switch (choice) {
                case 1:
                    handleBuy(market, party);
                    break;
                case 2:
                    handleSell(party);
                    break;
                case 3:
                    done = true;
                    break;
                default:
                    renderer.renderMessage("Invalid choice.");
                    break;
            }
        }
    }

    private void handleBuy(Market market, List<Hero> party) {
        if (party.isEmpty()) {
            renderer.renderMessage("No heroes in party.");
            return;
        }

        renderer.renderMessage("Choose a hero to buy for:");
        for (int i = 0; i < party.size(); i++) {
            Hero h = party.get(i);
            renderer.renderMessage("  " + (i+1) + ") " + h.getName() +
                    " (Lv " + h.getLevel() + ", Gold " + h.getGold() + ")");
        }
        renderer.renderMessage("  0) Back");

        int idx = input.readInt();
        if (idx == 0) return;
        idx--;
        if (idx < 0 || idx >= party.size()) {
            renderer.renderMessage("Invalid hero.");
            return;
        }

        Hero buyer = party.get(idx);
        List<Item> stock = market.getStock();
        if (stock.isEmpty()) {
            renderer.renderMessage("The market is empty.");
            return;
        }

        renderer.renderMessage("Items for sale:");
        for (int i = 0; i < stock.size(); i++) {
            Item item = stock.get(i);
            String type = "Item";
            if (item instanceof Weapon) type = "Weapon";
            else if (item instanceof Armor) type = "Armor";
            else if (item instanceof Potion) type = "Potion";

            renderer.renderMessage("  " + (i+1) + ") [" + type + "] " +
                    item.getName() +
                    " | Price: " + item.getPrice() +
                    " | Req Lv: " + item.getRequiredLevel());
        }
        renderer.renderMessage("  0) Back");

        int choice = input.readInt();
        if (choice == 0) return;
        choice--;
        if (choice < 0 || choice >= stock.size()) {
            renderer.renderMessage("Invalid item choice.");
            return;
        }

        Item selected = stock.get(choice);
        if (buyer.getLevel() < selected.getRequiredLevel()) {
            renderer.renderMessage("Hero level too low.");
            return;
        }

        int price = selected.getPrice();
        if (!buyer.spendGold(price)) {
            renderer.renderMessage("Not enough gold.");
            return;
        }

        buyer.getInventory().add(selected);
        renderer.renderMessage(buyer.getName() + " bought " +
                selected.getName() + " for " + price + " gold.");
    }

    private void handleSell(List<Hero> party) {
        if (party.isEmpty()) {
            renderer.renderMessage("No heroes in party.");
            return;
        }

        renderer.renderMessage("Choose a hero to sell from:");
        for (int i = 0; i < party.size(); i++) {
            Hero h = party.get(i);
            renderer.renderMessage("  " + (i+1) + ") " + h.getName() +
                    " (Gold " + h.getGold() + ")");
        }
        renderer.renderMessage("  0) Back");

        int idx = input.readInt();
        if (idx == 0) return;
        idx--;
        if (idx < 0 || idx >= party.size()) {
            renderer.renderMessage("Invalid hero.");
            return;
        }

        Hero seller = party.get(idx);
        List<Item> items = seller.getInventory().getItems();
        if (items.isEmpty()) {
            renderer.renderMessage("This hero has no items.");
            return;
        }

        renderer.renderMessage("Choose an item to sell (for " +
                GameBalance.SELL_PRICE_PERCENT + "% of price):");
        for (int i = 0; i < items.size(); i++) {
            Item it = items.get(i);
            String type = "Item";
            if (it instanceof Weapon) type = "Weapon";
            else if (it instanceof Armor) type = "Armor";
            else if (it instanceof Potion) type = "Potion";

            renderer.renderMessage("  " + (i+1) + ") [" + type + "] " +
                    it.getName() +
                    " | Price: " + it.getPrice());
        }
        renderer.renderMessage("  0) Back");

        int choice = input.readInt();
        if (choice == 0) return;
        choice--;
        if (choice < 0 || choice >= items.size()) {
            renderer.renderMessage("Invalid choice.");
            return;
        }

        Item toSell = items.get(choice);
        int original = toSell.getPrice();
        int sellPrice = (original * GameBalance.SELL_PRICE_PERCENT) / 100;

        seller.addGold(sellPrice);
        seller.getInventory().remove(toSell);

        renderer.renderMessage(seller.getName() + " sold " +
                toSell.getName() + " for " + sellPrice + " gold.");
    }
}
