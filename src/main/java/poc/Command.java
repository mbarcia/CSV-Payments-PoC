package poc;

public interface Command<T, S> {
    S execute(T processableObj);
}
