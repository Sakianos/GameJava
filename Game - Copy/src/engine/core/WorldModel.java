package engine.core;

import java.util.List;
import java.util.Map;

public class WorldModel {
    public List<RoomData> rooms;

    public static class RoomData {
        public String id;
        public String name;
        public String description;
        public Map<String, String> exits;
        public List<ItemData> items;
    }

    public static class ItemData {
        public String id;
        public String name;
        public String description;
    }
}