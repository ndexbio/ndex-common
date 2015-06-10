/**
 *   Copyright (c) 2013, 2015
 *  	The Regents of the University of California
 *  	The Cytoscape Consortium
 *
 *   Permission to use, copy, modify, and distribute this software for any
 *   purpose with or without fee is hereby granted, provided that the above
 *   copyright notice and this permission notice appear in all copies.
 *
 *   THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 *   WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 *   MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 *   ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 *   WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 *   ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 *   OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
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
