package oathkeeper.runtime.event;

import java.util.Objects;

public class MarkerEvent extends SemanticEvent{

    public enum Marker{
        EndOfTest;
    }

    public int marker;

    public MarkerEvent(){
        this.marker = -1;
    }

    public MarkerEvent(int marker){
        this.marker = marker;
    }

    @Override
    public String toString() {
        return "MarkerEvent{" +
                "marker=" + marker +
                '}';
    }

    public String getMapKey()
    {
        return "";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MarkerEvent that = (MarkerEvent) o;
        return marker == that.marker;
    }

    @Override
    public int hashCode() {
        return Objects.hash(marker);
    }

    public SemanticEvent clone()
    {
        return new MarkerEvent(marker);
    }
}
