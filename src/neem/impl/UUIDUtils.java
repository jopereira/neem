package neem.impl;

import java.nio.ByteBuffer;
import java.util.UUID;

public class UUIDUtils {
	/** Write an UUID to a ByteBuffer.
     * @param uuid The uuid to be written.
     * @return The Buffer with the uuid written into.
     */
    public static ByteBuffer writeUUIDToBuffer(UUID uuid) {
        ByteBuffer uuid_bytes = ByteBuffer.allocate(16);
        uuid_bytes.putLong(uuid.getMostSignificantBits());
        uuid_bytes.putLong(uuid.getLeastSignificantBits());
        uuid_bytes.flip();
        return uuid_bytes;
    }
    
    /** Read an UUID from an array of ByteBuffers into an UUID.
     * @param msg The buffer from which to read the UUID from.
     * @return The address read.
     */
    public static UUID readUUIDFromBuffer(ByteBuffer[] msg) {
    	ByteBuffer tmp = Buffers.sliceCompact(msg, 16); 
        long msb = tmp.getLong();
        long lsb = tmp.getLong();
        return new UUID(msb, lsb);
    }
}

// arch-tag: fb3615b7-6e20-4f6a-9b1c-9f60d92dde29
