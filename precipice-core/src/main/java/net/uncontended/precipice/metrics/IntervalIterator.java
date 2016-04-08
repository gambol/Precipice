package net.uncontended.precipice.metrics;

import java.util.Iterator;
import java.util.concurrent.TimeUnit;

public interface IntervalIterator<T> extends Iterator<T> {
    @Override
    boolean hasNext();

    @Override
    T next();

    @Override
    void remove();

    long intervalStart();

    long intervalEnd();

    void limit(long duration, TimeUnit unit);

}
