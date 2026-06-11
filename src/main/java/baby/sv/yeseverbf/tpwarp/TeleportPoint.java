package baby.sv.yeseverbf.tpwarp;

/**
 * 一个传送点。使用公共字段，方便 Gson 直接序列化/反序列化。
 */
public class TeleportPoint {
    public String id;
    public String title;
    public String description;
    public String dimension;
    public double x;
    public double y;
    public double z;
    public float yaw;
    public float pitch;

    public TeleportPoint() {
    }

    public TeleportPoint(String id, String title, String description, String dimension,
                         double x, double y, double z, float yaw, float pitch) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.dimension = dimension;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public String displayTitle() {
        return (title == null || title.isEmpty()) ? id : title;
    }
}
