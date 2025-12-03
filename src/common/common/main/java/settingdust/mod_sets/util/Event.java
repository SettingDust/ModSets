package settingdust.mod_sets.util;

import it.unimi.dsi.fastutil.objects.ReferenceArrayList;

import java.util.List;

public class Event<T> {
    private final InvokerFactory<T> invokerFactory;
    private final ReferenceArrayList<T> listeners = new ReferenceArrayList<>();
    private T invoker;

    public Event(InvokerFactory<T> invokerFactory) {
        this.invokerFactory = invokerFactory;
        updateInvoker();
    }

    private void updateInvoker() {
        this.invoker = invokerFactory.create(listeners);
    }

    public void register(T listener) {
        listeners.add(listener);
        updateInvoker();
    }

    public T getInvoker() {
        return invoker;
    }

    @FunctionalInterface
    public interface InvokerFactory<T> {
        T create(List<T> listeners);
    }
}
