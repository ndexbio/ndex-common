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
