/**
 * Copyright (c) 2013, 2015, The Regents of the University of California, The Cytoscape Consortium
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */
package org.ndexbio.common.models.object.network;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;
import org.ndexbio.model.exceptions.NdexException;

public class RawCitationTest {

	@Test
	public void test() throws NdexException {
		
       RawCitation c1 = new RawCitation ("title1", "pubmed", "1001", null);
       RawCitation c2 = new RawCitation ("title1", "pubmed", "1001", null);
       RawCitation c3 = new RawCitation ("title2", "pubmed", "1001", null);
       RawCitation c4 = new RawCitation ("title2", "pubmed", "1002", null);
       

       RawCitation c5 = new RawCitation ("", null, "1001", null);

       
       assertEquals ( c1.compareTo(c2), 0);
       assertEquals ( c2.compareTo(c1), 0);
       
       assertEquals ( c2.compareTo(c3), 0);
       assertEquals ( c3.compareTo(c2), 0);
       assertEquals ( c3.compareTo(c4), -1);
       assertEquals ( c4.compareTo(c3), 1);
       assertEquals ( c1.compareTo(c5), 1);
       assertEquals ( c5.compareTo(c1), -1);
       
       assertEquals ( c1.equals(c2), true);
       assertEquals ( c2.equals(c1), true);
       assertEquals ( c1.equals(c3), true);
       assertEquals ( c3.equals(c1), true);
       
       assertEquals ( c2.equals(c4), false);
       assertEquals ( c4.equals(c1), false);
       
	}

}
