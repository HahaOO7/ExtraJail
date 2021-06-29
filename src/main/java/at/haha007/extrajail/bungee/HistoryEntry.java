package at.haha007.extrajail.bungee;

import at.haha007.extrajail.common.MySqlDatabase;
import at.haha007.extrajail.common.PluginVariables;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@AllArgsConstructor
public class HistoryEntry {
    @Getter
    private final UUID jailerId;
    @Getter
    private final String jailerName;
    @Getter
    private final UUID targetId;
    @Getter
    private final String targetName;
    @Getter
    private final String reason;
    @Getter
    private final long timeCreated;
    @Getter
    private final int amount;
    @Getter
    private final String action;

    private static final ExtraJailBungeePlugin plugin = ExtraJailBungeePlugin.getInstance();
    private static final MySqlDatabase database = plugin.getDatabase();

    public static void createTable() throws SQLException {
        PreparedStatement ps = database.prepareStatement("CREATE TABLE IF NOT EXISTS " + PluginVariables.historyTable +
                "(UUID varchar(36), JAILER varchar(36), REASON varchar(256), AMOUNT int(32), ACTION varchar(10), TIME bigint(64))");
        ps.executeUpdate();
    }

    public static List<HistoryEntry> getHistory(UUID target) {
        List<HistoryEntry> history = new ArrayList<>();
        String name = plugin.getName(target);
        try {
            PreparedStatement ps = database.prepareStatement("SELECT * FROM " + PluginVariables.historyTable + " WHERE UUID = ?");
            ps.setString(1, target.toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                history.add(getHistoryEntry(rs, target, name));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return history;
    }

    private static HistoryEntry getHistoryEntry(ResultSet rs, UUID uuid, String name) throws SQLException {
        UUID jailer = UUID.fromString(rs.getString(2));
        return new HistoryEntry(jailer,
                plugin.getName(jailer),
                uuid,
                name,
                rs.getString(3),
                rs.getLong(6),
                rs.getInt(4),
                rs.getString(5));
    }

    public void insert(){
        try {
            PreparedStatement ps = database.prepareStatement("INSERT INTO " + PluginVariables.historyTable +
                    " (UUID, JAILER, REASON, AMOUNT, ACTION, TIME) VAlUES (?,?,?,?,?,?)");
            ps.setString(1, targetId.toString());
            ps.setString(2, jailerId.toString());
            ps.setString(3, reason);
            ps.setInt(4, amount);
            ps.setString(5, action);
            ps.setLong(6, timeCreated);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        return String.format("%s [%s][%d][%s] %s",
                jailerName,
                action,
                amount,
                new SimpleDateFormat("yyyy-MM-dd").format(new Date(timeCreated)),
                reason);
    }
}
