
package pson;

import java.nio.*;
import java.util.*;

/**
 * Buffer manipulation utilities.
 */
public class Buffers {
	/**
	 * Remove data from the head of a buffer array. No copying of
	 * data is done. This is useful if data if being resent. This operation
	 * might leave some buffers in the array with remaining()==0.
	 */
	public static ByteBuffer[] slice(ByteBuffer[] buffer, int size) {
		ArrayList res=new ArrayList();
		for(int i=0;size>0;i++) {
			int slice=buffer[i].remaining();
			if (slice>size)
				slice=size;
			if (slice==0)
				continue;
			ByteBuffer chunk=buffer[i].asReadOnlyBuffer();
			chunk.limit(chunk.position()+slice);
			buffer[i].position(buffer[i].position()+slice);
			res.add(chunk);
			size-=slice;
		}
		return (ByteBuffer[])res.toArray(new ByteBuffer[res.size()]);
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
		if (buffer[0].remaining()>=size) {
			ByteBuffer chunk=buffer[0].asReadOnlyBuffer();
			chunk.limit(chunk.position()+size);
			buffer[0].position(buffer[0].position()+size);
			return chunk;
		}

		// Need to compact by copying
		ByteBuffer chunk=ByteBuffer.allocate(size);
		for(int i=0;size>0;i++) {
			int slice=buffer[i].remaining();
			if (slice>size)
				slice=size;
			size-=slice;
			while(slice-->0)
				chunk.put(buffer[i].get());
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
		int size=0;
		for(int i=0;i<buffer.length;i++)
			size+=buffer[i].remaining();
		ByteBuffer res=ByteBuffer.allocate(size);
		for(int i=0;i<buffer.length;i++)
			while(buffer[i].hasRemaining())
				res.put(buffer[i].get());
		res.rewind();
		return res;
	}

	/**
	 * Clone a buffer array. Upon returning, the supplied buffers
	 * remain intact and a read only copy is returned.
	 */
	public static ByteBuffer[] clone(ByteBuffer[] buffer) {
		ByteBuffer[] res=new ByteBuffer[buffer.length];
		for(int i=0;i<buffer.length;i++)
			res[i]=buffer[i].asReadOnlyBuffer();
		return res;
	}

};

// arch-tag: 8ef66d5b-cc0a-47e6-b8db-8fcfe04095ac
