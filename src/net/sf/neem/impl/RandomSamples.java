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

import java.util.Random;

/**
 * Efficient computation of random samples from a small universe.
 * This is used for gossiping.
 */
public class RandomSamples {
	/**
	 * Efficiently calculate a random sample. This works by
	 * shuffling the array such that the first n elements, i.e.
	 * from universe[0] to universe[n-1] are an uniform 
	 * random sample. This is true even if the same universe
	 * is used repeatedly without being reinitialized.
	 * Usually, the result is used as indexes in some
	 * ordered collection, which is the true universe. This
	 * is efficient, as it is O(n), for small samples and
	 * O(universe.length-n) for large samples.
	 * 
	 * @param n size of sample.
	 * @param universe universe to draw from.
	 * @param rand random generator.
	 */
	public static int uniformSample(int n, int[] universe, Random rand) {
		if (n>universe.length)
			return universe.length;
		if (n>universe.length/2) {
			for(int i=0;i<universe.length-n;i++) {
				int idx=rand.nextInt(universe.length-i);
				if (idx==0)
					continue;
				int one=universe.length-(idx+i+1);
				int other=universe.length-(i+1);
				
				int tmp=universe[one];
				universe[one]=universe[other];
				universe[other]=tmp;
			}			
		} else {
			for(int i=0;i<n;i++) {
				int idx=rand.nextInt(universe.length-i);
				if (idx==0)
					continue;
				int one=idx+i;
				int other=i;
				
				int tmp=universe[one];
				universe[one]=universe[other];
				universe[other]=tmp;
			}
		}
		return n;
	}
	
	/**
	 * Initializes the universe for computing random samples.
	 * This generates an integers array that can be used to
	 * compute random samples.
	 * @param n size of the universe
	 */
	public static int[] mkUniverse(int n) {
		int[] result=new int[n];
		for(int i=0;i<n;i++)
			result[i]=i;
		return result;
	}
}

