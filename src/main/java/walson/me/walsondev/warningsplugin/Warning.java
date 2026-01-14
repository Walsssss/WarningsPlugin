package walson.me.walsondev.warningsplugin;

import java.util.UUID;

public class Warning {

    private final UUID targetUuid;
    private final String targetName;
    private final String reason;
    private final String warnerName;
    private final UUID warnerUuid;
    private final long timestamp;

    public Warning(UUID targetUuid, String targetName, String reason,
                   String warnerName, UUID warnerUuid, long timestamp) {
        this.targetUuid = targetUuid;
        this.targetName = targetName;
        this.reason = reason;
        this.warnerName = warnerName;
        this.warnerUuid = warnerUuid;
        this.timestamp = timestamp;
    }

    public UUID getTargetUuid() {
        return targetUuid;
    }

    public String getTargetName() {
        return targetName;
    }

    public String getReason() {
        return reason;
    }

    public String getWarnerName() {
        return warnerName;
    }

    public UUID getWarnerUuid() {
        return warnerUuid;
    }

    public long getTimestamp() {
        return timestamp;
    }
}