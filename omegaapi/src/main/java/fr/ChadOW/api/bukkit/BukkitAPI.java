package fr.ChadOW.api.bukkit;


import fr.ChadOW.api.accounts.UserAccount;
import fr.ChadOW.api.managers.JedisManager;
import fr.ChadOW.api.managers.OmegaAPIUtils;
import fr.ChadOW.api.managers.SQLManager;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;


public class BukkitAPI extends JavaPlugin {
    private static BukkitAPI INSTANCE;

    public void onEnable() {
        INSTANCE = this;

        saveDefaultConfig();

        initMySQL();
        initRedis();

        initGlobal();
    }

    private void initGlobal() {
        getServer().getPluginManager().registerEvents(new GlobalListener(), this);
        getServer().getScheduler().runTaskTimer(this, OmegaAPIUtils::getData, 6000, 6000);
    }

    public void onDisable() {
        JedisManager.getInstance().poolJedis.close();
        SQLManager.getInstance().closePool();
    }

    public static void resetDisplay(Player player) {
        UserAccount account = UserAccount.getAccount(player.getUniqueId());

        player.setPlayerListName(account.getRank().getTab() + player.getDisplayName());
    }


    public static BukkitAPI getInstance() {
        return INSTANCE;
    }

    private void initRedis() {
        new JedisManager(getConfig().getString("redis.hostname"), getConfig().getInt("redis.port"), getConfig().getString("redis.password"));
    }

    private void initMySQL() {
        new SQLManager(getConfig().getString("mysql.hostname"), getConfig().getInt("mysql.port"), getConfig().getString("mysql.database"), getConfig().getString("mysql.user"), getConfig().getString("mysql.password"));
    }
}