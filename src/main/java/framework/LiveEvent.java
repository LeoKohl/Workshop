package framework;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;

@Setter(AccessLevel.PUBLIC)
@Getter(AccessLevel.PUBLIC)
@FieldDefaults(level = AccessLevel.PRIVATE)
@Accessors(chain = true)
public class LiveEvent {
    EventType eventType = null;
    String param1 = null;
    String param2 = null;
    String param3 = null;
    String param4 = null;
    int gameTimeMinute = -1;

    public String toString() {
        var temp = new StringBuilder();
        temp.append("Type: ").append(eventType);
        temp.append(" | Param1: ").append(param1);
        if (param2 != null) {
            temp.append(" | Param2: ").append(param2);
        }
        if (param3 != null) {
            temp.append(" | Param3: ").append(param3);
        }
        if (param4 != null) {
            temp.append(" | Param4: ").append(param4);
        }
        if (gameTimeMinute != -1) {
            temp.append(" | Game Time: ").append(gameTimeMinute);
        }

        return temp.toString();
    }
}
