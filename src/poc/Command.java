package poc;

public interface Command<T> {
    void execute(T processableObj);
}
