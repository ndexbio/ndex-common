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

import org.junit.Test;

public class RawNamespaceTest {

	@Test
	public void test() {
       RawNamespace n1 = new RawNamespace(null, "uri1");
       RawNamespace n2 = new RawNamespace(null, "uri2");
       RawNamespace n3 = new RawNamespace(null, "uri1");
       

       RawNamespace n4 = new RawNamespace("dc1", "uri1");
       RawNamespace n5 = new RawNamespace("dc2", "uri2");
       RawNamespace n6 = new RawNamespace( "dc3", "uri1");
       RawNamespace n62 = new RawNamespace( "dc3", "uri1");
       RawNamespace n63 = new RawNamespace( "dc1", "uri0");
	
       RawNamespace n7 = new RawNamespace("dc1", "uri1");
       RawNamespace n8 = new RawNamespace("dc2", null);
       RawNamespace n9 = new RawNamespace( "dc3",null);
       RawNamespace n10 = new RawNamespace( "dc3",null);
       
       assertEquals ( n10, n9);
       assertEquals ( n1, n3);
       assertEquals ( n6, n62);
       
       assertEquals (n1.compareTo(n2), -1);
       assertEquals (n1.compareTo(n4), -1);
       assertEquals (n1.compareTo(n5), -1);
       assertEquals (n4.compareTo(n1), 1);
       assertEquals (n5.compareTo(n1), 1);
       
       assertEquals (n4.compareTo(n63), 1);
       assertEquals (n63.compareTo(n4), -1);
       assertEquals (n6.compareTo(n4) > 0 , true);
       assertEquals (n4.compareTo(n6)< 0 , true);
       
       assertEquals (n7.compareTo(n8), 1);
       assertEquals (n8.compareTo(n7), -1);
	}

}
