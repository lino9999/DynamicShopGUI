# 📈 DynamicShopGUI - Advanced Supply & Demand Economy (1.21+)

> **Revolutionize your server's economy with a realistic market system.**
> Prices fluctuate automatically based on player trading activity. Includes **AutoSell Chests**, **Harvester Hoes**, and **Discord Alerts**.

![Java](https://img.shields.io/badge/Java-21-orange) ![Spigot](https://img.shields.io/badge/API-1.21-yellow) ![License](https://img.shields.io/badge/License-MIT-blue)

---

## 💎 Why DynamicShopGUI?
Static prices are boring. **DynamicShopGUI** creates a living economy where prices rise when items are scarce and drop when the market is flooded. It manages stock, taxes, and decay automatically using a local SQLite database for maximum performance.

### ✨ Key Features

* **📊 Dynamic Pricing Algorithm**
    * **Supply & Demand:** Prices adjust in real-time based on stock levels and transaction volume.
    * **Stock Decay:** Oversupplied items slowly reduce in stock to stabilize prices over time.
    * **Item Worth:** Displays the real-time sell value of items directly in the player's inventory lore.

* **⚡ Special Economy Items**
    * **AutoSell Chests:** Automatically sells contents every 5 minutes (configurable).
    * **Harvester Hoes:** Automatically harvests, replants, and sells crops instantly! Perfect for Skyblock/Factions.

* **🔔 Integrations & Alerts**
    * **Discord Webhooks:** Sends alerts to your Discord channel when high-demand items go out of stock.
    * **PlaceholderAPI:** Full support for displaying dynamic prices on scoreboards/holograms.

---

## ⚙️ Configuration
The plugin is highly configurable. You can set tax rates, price limits, and restock timers in `config.yml`.

```yaml
# Example: Supply & Demand Sensitivity
price-factors:
  increase: 0.05  # Price goes up 5% when bought
  decrease: 0.03  # Price goes down 3% when sold

# Taxes to control inflation
tax:
  enabled: true
  rate: 15        # 15% tax on sales
