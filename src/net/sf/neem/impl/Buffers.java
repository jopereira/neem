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

import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * Buffer manipulation utilities.
 */
public abstract class Buffers {
	private Buffers() {}
	
    /**
     * Remove data from the head of a buffer array. No copying of
     * data is done. This is useful if data if being resent. This operation
     * might leave some buffers in the array with remaining()==0.
     */
    public static ByteBuffer[] slice(ByteBuffer[] buffer, int size) {
        ArrayList<ByteBuffer> res = new ArrayList<ByteBuffer>();

        for (int i = 0; size > 0; i++) {
            int slice = buffer[i].remaining();

            if (slice > size) {
                slice = size;
            }
            if (slice == 0) {
                continue;
            }
            ByteBuffer chunk = buffer[i].asReadOnlyBuffer();

            chunk.limit(chunk.position() + slice);
            buffer[i].position(buffer[i].position() + slice);
            res.add(chunk);
            size -= slice;
        }
        return (ByteBuffer[]) res.toArray(new ByteBuffer[res.size()]);
    }

    /**
     * Remove a contiguous header from the head of a buffer array.
     * Copying of data is done only if required to return a contiguous
     * buffer. This might be less efficient that popData() but the result
     * can be used safely to read data. This operation
     * might leave some buffers in the array with remaining()==0.
     */
    public static ByteBuffer sliceCompact(ByteBuffer[] buffer, int size) {
        // No need to compact, just slice
        if (buffer[0].remaining() >= size) {
            ByteBuffer chunk = buffer[0].asReadOnlyBuffer();

            chunk.limit(chunk.position() + size);
            buffer[0].position(buffer[0].position() + size);
            return chunk;
        }

        // Need to compact by copying
        ByteBuffer chunk = ByteBuffer.allocate(size);

        for (int i = 0; size > 0; i++) {
            int slice = buffer[i].remaining();

            if (slice > size) {
                slice = size;
            }
            size -= slice;
            while (slice-- > 0) {
                chunk.put(buffer[i].get());
            }
        }
        chunk.rewind();
        return chunk;
    }

    /**
     * Compact a buffer array to a single buffer. Upon returning,
     * all supplied buffers will have remaining()==0. Contents
     * are returned in a single freshly allocated buffer.
     */
    public static ByteBuffer compact(ByteBuffer[] buffer) {
        int size = count(buffer);
        ByteBuffer res = ByteBuffer.allocate(size);
		copy(res, buffer);
        res.rewind();
        return res;
    }

    /**
	 * Count remaining bytes in a buffer array.
     */
    public static int count(ByteBuffer[] buffer) {
        int size = 0;

        for (int i = 0; i < buffer.length; i++) {
            size += buffer[i].remaining();
        }
		return size;
	}

    /**
     * Copy a buffer array to a single buffer.
     */
    public static int copy(ByteBuffer res, ByteBuffer[] buffer) {
		int cnt=0;
        for (int i = 0; i < buffer.length && res.hasRemaining(); i++) {
            while (buffer[i].hasRemaining() && res.hasRemaining()) {
                res.put(buffer[i].get());
				cnt++;
            }
        }
        return cnt;
    }

    /**
     * Clone a buffer array. Upon returning, the supplied buffers
     * remain intact and a read only copy is returned.
     */
    public static ByteBuffer[] clone(ByteBuffer[] buffer) {
        ByteBuffer[] res = new ByteBuffer[buffer.length];

        for (int i = 0; i < buffer.length; i++) {
            res[i] = buffer[i].asReadOnlyBuffer();
        }
        return res;
    }
}

