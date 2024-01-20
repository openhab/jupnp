/**
 * Copyright (C) 2014 4th Line GmbH, Switzerland and others
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License Version 1 or later
 * ("CDDL") (collectively, the "License"). You may not use this file
 * except in compliance with the License. See LICENSE.txt for more
 * information.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package org.jupnp.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Christian Bauer
 */
public class Iterators {

    /**
     * A default implementation with no elements.
     */
    static public class Empty<E> implements Iterator<E> {

        public boolean hasNext() {
            return false;
        }

        public E next() {
            throw new NoSuchElementException();
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * A fixed single element.
     */
    static public class Singular<E> implements Iterator<E> {

        final protected E element;
        protected int current;

        public Singular(E element) {
            this.element = element;
        }

        public boolean hasNext() {
            return current == 0;
        }

        public E next() {
            current++;
            return element;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Wraps a collection and provides stable iteration with thread-safe removal.
     * <p>
     * Internally uses the iterator of a <code>CopyOnWriteArrayList</code>, when
     * <code>remove()</code> is called, delegates to {@link #synchronizedRemove(int)}.
     * </p>
     */
    static public abstract class Synchronized<E> implements Iterator<E> {

        final Iterator<E> wrapped;

        int nextIndex = 0;
        boolean removedCurrent = false;

        public Synchronized(Collection<E> collection) {
            this.wrapped = new CopyOnWriteArrayList<E>(collection).iterator();
        }

        public boolean hasNext() {
            return wrapped.hasNext();
        }

        public E next() {
            removedCurrent = false;
            nextIndex++;
            return wrapped.next();
        }

        public void remove() {
            if (nextIndex == 0)
                throw new IllegalStateException("Call next() first");
            if (removedCurrent)
                throw new IllegalStateException("Already removed current, call next()");
            synchronizedRemove(nextIndex - 1);
            removedCurrent = true;
        }

        /**
         * Must remove the element at the given index from the original collection in a
         * thread-safe fashion.
         */
        abstract protected void synchronizedRemove(int index);
    }
}
