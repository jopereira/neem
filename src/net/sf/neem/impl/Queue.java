/*
 * NeEM - Network-friendly Epidemic Multicast
 * Copyright (c) 2005-2007, University of Minho
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

package net.sf.neem.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Implementation of a FIFO queue with random purging.
 */
public class Queue {
    /**
     * Creates a new queue.
     * 
     * @param max The maximum number of elements this queue can hold.
     * @param random 
     */
    public Queue(int max, Random random) {
        this.queue = new ArrayList<Queued>();
        this.max = max;
        this.rand=random;
    }

    /**
     * Inserts (enqueues) an Object to this queue.
     * 
     * @param o The object to be enqueued.
     */
    public void push(Queued o) {
        if (queue.size()<max)
            this.queue.add(o);
        else {
            int pos = rand.nextInt(queue.size() - 1);

            queue.remove(pos);
            this.queue.add(o);
        }
    }

    /**
     * Retrieves (dequeues) an Object from this queue.
     * 
     * @return The object to be dequeued.
     */
    public Queued pop() {
        return queue.remove(0);
    }

    public boolean isEmpty() {
    	return queue.isEmpty();
    }
    
    public String toString() {
    	return queue.toString();
    }
    
    /* Object storage vector */
    private List<Queued> queue;
    
    /* Level of occupancy above which this queue drops all messages. */
    public int max;
    
    private Random rand;
}

