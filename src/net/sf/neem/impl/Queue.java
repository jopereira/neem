/*
 * NeEM - Network-friendly Epidemic Multicast
 * Copyright (c) 2005-2006, University of Minho
 * All rights reserved.
 *
 * Contributors:
 *  - Pedro Santos <psantos@gmail.com>
 *  - Jose Orlando Pereira <jop@di.uminho.pt>
 * 
 * Partially funded by FCT, project P-SON (POSC/EIA/60941/2004).
 * See http://pson.lsd.di.uminho.pt/ for more information.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 *  - Redistributions of source code must retain the above copyright
 *  notice, this list of conditions and the following disclaimer.
 * 
 *  - Redistributions in binary form must reproduce the above copyright
 *  notice, this list of conditions and the following disclaimer in the
 *  documentation and/or other materials provided with the distribution.
 * 
 *  - Neither the name of the University of Minho nor the names of its
 *  contributors may be used to endorse or promote products derived from
 *  this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/*
 * Queue.java
 *
 * Created on April 27, 2005, 12:34 PM
 */

package net.sf.neem.impl;

import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Vector;

/**
 * Implementation of a FIFO queue with a simplified aproach to the RED queue
 * management strategy. New elements are added to the top (head insertion).
 * Getting an element from the queue means removing the first one (head
 * extraction). This class also provides methods to inspect the first element,
 * without removing it, as well as to check if its empty. The state of the queue
 * means how filled up it is. Whether the number of objects in the queue is less
 * than min, between min and max, or max threshold values.
 * 
 * This class is backed by a java.util.Vector.
 * 
 * @author Pedro Santos <psantos@gsd.di.uminho.pt>
 */
public class Queue {

    /**
     *  Creates a new queue with RED (Random Early Detection) purging that
     * can hold size number of objects
     * 
     * @param default_Q_size The maximum number of elements this queue can hold.
     */
    public Queue(int default_Q_size) {
        this.q = new Vector<Object>();
        this.min_threshold = default_Q_size;
        this.max_threshold = default_Q_size;
        this.discard_probability = 1.0;
        this.size = 0;
        this.dropped = 0;
        this.lastdrop = 0;
    }

    /**
     * Creates a new queue with RED scaling technique.
     * 
     * @param min
     *            The threshold to start dropping messages probabilisticly
     * @param max
     *            The threshold to drop all messages
     */
    public Queue(int min, int max, double p) {
        this.q = new Vector<Object>();
        this.min_threshold = min;
        this.max_threshold = max;
        this.discard_probability = p;
        this.size = 0;
        this.dropped = 0;
        this.lastdrop = 0;
    }

    /**
     * Inserts (enqueues) an Object to this queue.
     * 
     * @param o
     *            The object to be enqueued.
     */
    public void push(Object o) {
        // this.q.add(o);/*
        if (size < min_threshold) {
            this.q.add(o);
            this.size++;
        } else if (size >= min_threshold && size < max_threshold) {
            double d = rand.nextDouble();
            if (d < this.discard_probability) {
                this.q.add(o); // appends to the end of the array
                this.size++; // the queue now has a new element
            } else {
                int pos = rand.nextInt(this.size - 1);

                q.remove(pos);
                this.lastdrop = System.nanoTime();
                this.dropped++;
                this.q.add(o);
            }
        } // */
        // otherwise, sure drop a random one
        else {
            int pos = rand.nextInt(this.size - 1);

            q.remove(pos);
            this.q.add(o);
            this.dropped++;
            this.lastdrop = System.nanoTime();
        }
    }

    /**
     * Retrieves (dequeues) an Object from this queue.
     * 
     * @return The object to be dequeued.
     */
    public Object pop() throws NoSuchElementException,
            ArrayIndexOutOfBoundsException {
        Object ret = null;

        ret = this.q.remove(0); // shifts the array left
        this.size--;
        return ret;
    }

    /**
     * Returns the first element in this queue.
     * 
     * @return The object at the head of the queue.
     */
    public Object top() {
        Object ret = null;

        try {
            ret = this.q.get(0);
        } catch (NoSuchElementException nse) {
        } catch (ArrayIndexOutOfBoundsException aio) {
        }
        return ret;
    }

    /**
     * Tests if this queue is empty.
     * 
     * @return A boolean indicating the state of the queue.
     */
    public boolean isEmpty() {
        return this.q.isEmpty();
    }

    /*
     * private static double round(double a, int b) { return new BigDecimal("" +
     * a).setScale(b, BigDecimal.ROUND_HALF_UP).doubleValue(); }
     */

    /** Get the number of dropped messages in this queue */
    public int droppedMessages() {
        return this.dropped;
    }

    /** Get the time of the last drop */
    public long lastDrop() {
        return this.lastdrop;
    }

    /** Object storage vector */
    private Vector<Object> q;

    /** Level of occupancy above which this queue drops some messages. */
    public int min_threshold;

    /** Level of occupancy above which this queue drops all messages. */
    public int max_threshold;

    /** Probability of dropping a message when fill level is between thresholds. */
    public double discard_probability;

    /** The current number of objects held. */
    private int size;

    /** The number of discarded messages since in the current state of the queue */
    private int dropped;

    /** Last point in time where a message got purged from the queue */
    private long lastdrop;

    /** Random number generator for random purging */
    private Random rand = new Random();

    /**
     * Gets the maximum threshold value for this queue (see
     * {@link net.sf.neem.impl.Queue#max_threshold}).
     * 
     * @return The current maximum threshold value.
     */
    public int getMax_threshold() {
        return max_threshold;
    }

    /**
     * Sets the maximum threshold value for this queue (see
     * {@link net.sf.neem.impl.Queue#max_threshold}).
     * 
     * @param max_threshold
     *            New maximum threshold value.
     */
    public void setMax_threshold(int max_threshold) {
        this.max_threshold = max_threshold;
    }

    /**
     * Gets the minimum threshold value for this queue (see
     * {@link net.sf.neem.impl.Queue#min_threshold}).
     * 
     * @return The current minimum threshold value.
     */
    public int getMin_threshold() {
        return min_threshold;
    }

    /**
     * Sets the minimum threshold value for this queue (see
     * {@link net.sf.neem.impl.Queue#max_threshold}).
     * 
     * @param min_threshold
     *            New minimum threshold value.
     */
    public void setMin_threshold(int min_threshold) {
        this.min_threshold = min_threshold;
    }

    /**
     * Gets the message discard probability value (0<p<1) for this queue (see
     * {@link net.sf.neem.impl.Queue#discard_probability}).
     * 
     * @return The current discard probability value.
     */
    public double getDiscard_probability() {
        return discard_probability;
    }

    /**
     * Sets the message discard probability value (0<p<1) for this queue (see
     * {@link net.sf.neem.impl.Queue#max_threshold}).
     * 
     * @param p
     *            New minimum threshold value.
     */
    public void setDiscard_probability(double p) {
        this.discard_probability = p;
    }

    /**
     * Gets the current number of objects in this queue.
     * 
     * @return This queue's current occupancy level
     */
    public int getSize() {
        return size;
    }

    /**
     * Gets the number of discarded messages since queue entered it's current
     * state.
     * 
     * @return The current number of dropped messages
     */
    public int getDropped() {
        return dropped;
    }

    /**
     * Gets the system's time of the last message drop.
     * 
     * @return The time, in milis, when there was a message drop.
     */
    public long getLastdrop() {
        return lastdrop;
    }
};

// arch-tag: 9f8ed933-eadd-4217-bde0-990e8fd56f8e
