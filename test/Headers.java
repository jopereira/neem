
package test;

import pson.*;

import java.nio.*;
import java.util.*;

public class Headers {
	public static ByteBuffer[] alloc() {
		ByteBuffer[] res=new ByteBuffer[20];
		for(int i=0;i<20;i++) {
			res[i]=ByteBuffer.allocate(i*4);
			for(int j=0;j<i;j++)
				res[i].putInt(i*10+j);
			res[i].flip();
		}
		return res;
	}

	public static void main(String[] args) {
		int max=20; 

		ByteBuffer[] b=alloc();
		for(int i=0;i<b.length;i++)
			System.out.print(b[i].remaining()+" ");
		System.out.println();
		for(int i=0;i<max;i++) {
			ByteBuffer c=Buffers.sliceCompact(b, 4);
			System.out.print(c.getInt()+" ");
		}
		System.out.println();

		b=alloc();
		ArrayList a=new ArrayList();
		for(int i=0;i<max;i++)
			a.add(Buffers.sliceCompact(b, 1+i));
		b=(ByteBuffer[])a.toArray(new ByteBuffer[a.size()]);
		for(int i=0;i<b.length;i++)
			System.out.print(b[i].remaining()+" ");
		System.out.println();
		for(int i=0;i<max;i++) {
			ByteBuffer c=Buffers.sliceCompact(b, 4);
			System.out.print(c.getInt()+" ");
		}
		System.out.println();

		b=alloc();
		a=new ArrayList();
		for(int i=0;i<max;i++) {
			ByteBuffer[] d=Buffers.slice(b, 1+i);
			for(int j=0;j<d.length;j++)
				a.add(d[j]);
		}
		b=(ByteBuffer[])a.toArray(new ByteBuffer[a.size()]);
		for(int i=0;i<b.length;i++)
			System.out.print(b[i].remaining()+" ");
		System.out.println();
		for(int i=0;i<max;i++) {
			ByteBuffer c=Buffers.sliceCompact(b, 4);
			System.out.print(c.getInt()+" ");
		}
		System.out.println();

		b=alloc();
		for(int i=0;i<b.length;i++)
			System.out.print(b[i].remaining()+" ");
		System.out.println();
		b=Buffers.clone(b);
		for(int i=0;i<b.length;i++)
			System.out.print(b[i].remaining()+" ");
		System.out.println();
		b=new ByteBuffer[]{Buffers.compact(b)};
		for(int i=0;i<b.length;i++)
			System.out.print(b[i].remaining()+" ");
		System.out.println();
		for(int i=0;i<max;i++) {
			ByteBuffer c=Buffers.sliceCompact(b, 4);
			System.out.print(c.getInt()+" ");
		}
		System.out.println();
	}
};

// arch-tag: 008f8e0e-f250-4107-8f66-6bae262272c8
