package framework;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class LiveScoreObject extends FixtureObject {

    @Getter(AccessLevel.PUBLIC)
    final List<LiveEvent> liveEvents = new ArrayList<>();

    void addLiveEvent(LiveEvent event) {
        liveEvents.add(event);
    }

    void printEvents() {
        liveEvents.forEach(liveEvent -> {
            System.out.println("Event: " + liveEvent.toString());
        });
    }
}
