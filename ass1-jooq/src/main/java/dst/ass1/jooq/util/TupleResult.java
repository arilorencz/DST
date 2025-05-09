package dst.ass1.jooq.util;

public class TupleResult<T, U> {

    private final T first;
    private final U second;

    public TupleResult(T first, U second) {
        this.first = first;
        this.second = second;
    }

    public T getFirst() {
        return first;
    }

    public U getSecond() {
        return second;
    }
}
