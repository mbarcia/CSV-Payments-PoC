package poc;

import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;

public class Processable<T> implements Iterable<Command<T>> {
    List<Command<T>> commandList = null;

    public void setCommandList(List<Command<T>> list) {
        this.commandList = list;
    }

    @Override
    public Iterator<Command<T>> iterator() {
        return commandList.iterator();
    }

    @Override
    public void forEach(Consumer<? super Command<T>> action) {
        Iterable.super.forEach(action);
    }

    @Override
    public Spliterator<Command<T>> spliterator() {
        return Iterable.super.spliterator();
    }
}
