package org.ndexbio.common.models.object.network;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;
import org.ndexbio.common.exceptions.NdexException;

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
