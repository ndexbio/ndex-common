/**
 * Copyright (c) 2013, 2016, The Regents of the University of California, The Cytoscape Consortium
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
package org.ndexbio.task.parsingengines;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.TreeSet;
import java.util.UUID;
import java.util.logging.Logger;

import org.junit.Test;
import org.ndexbio.common.exporter.BioPAXNetworkExporter;
import org.ndexbio.common.exporter.XGMMLNetworkExporter;
import org.ndexbio.common.exporter.XbelNetworkExporter;
import org.ndexbio.common.models.dao.orientdb.CXNetworkExporter;
import org.ndexbio.common.models.dao.orientdb.NetworkDAO;
import org.ndexbio.common.models.dao.orientdb.NetworkDocDAO;
import org.ndexbio.common.persistence.orientdb.CXNetworkLoader;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.exceptions.ObjectNotFoundException;
import org.ndexbio.model.object.network.BaseTerm;
import org.ndexbio.model.object.network.Edge;
import org.ndexbio.model.object.network.Network;
import org.ndexbio.model.object.network.NetworkSourceFormat;
import org.ndexbio.model.object.network.Node;
import org.xml.sax.SAXException;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerException;

import org.apache.commons.io.FilenameUtils;



public class ImportExportTest {

//	private static final boolean BaseTerm = false;
	static Logger logger = Logger.getLogger(ImportExportTest.class.getName());

	@Test
	public void test() throws Exception {

		for ( TestMeasurement m : AllTests.testList) {
		  
			
		  logger.info("Testting " +m.fileName+ "\nFirst round import start.");
		  IParsingEngine parser = importFile(AllTests.testFileDirectory + m.fileName, m);
			  	
		 // get the UUID of the new test network
		 UUID networkID = parser.getUUIDOfUploadedNetwork();
			
		 logger.info("Verifying loaded content.");
		 assertEquivalence(networkID, m);
		
		 logger.info("First round import passed. Start exporting ...");

		 System.out.println("Working Directory = " + System.getProperty("user.dir"));
		 ODatabaseDocumentTx conn = AllTests.db.getAConnection();
		 exportNetwork(m, conn, networkID);

		// String oldNetworkID = networkID.toString();
		 logger.info("First export done.");

		 File file1 = null;
		 File file2 = null;
		 UUID nativeReloadedNetworkID = null;
		 if ( m.srcFormat != NetworkSourceFormat.SIF) { 
		  
			 logger.info("Started importing exported network.");
			 parser = importFile ( System.getProperty("user.dir") + "/"+ networkID.toString(), m);

			 logger.info("Verifying network loaded from exported file.");
			 nativeReloadedNetworkID = parser.getUUIDOfUploadedNetwork();
			 assertEquivalence(networkID, m);
          
 		  
			 logger.info("Exporting the re-imported network.");
			 exportNetwork(m, conn, nativeReloadedNetworkID);
 

			 logger.info("checking if the 2 exported files have similar sizes");
			 file1 = new File(networkID.toString());
			 file2 = new File(nativeReloadedNetworkID.toString());
			 assertTrue( file2.exists());
			 double sizeDiff = Math.abs(file1.length()-file2.length());
			 assertTrue ( sizeDiff/file1.length() < 0.005 || sizeDiff <100);
			 //assertEquals(file1.length(), file2.length()); 
		 } 

		  NetworkDAO dao = new NetworkDAO (conn);
		 
 		  // test the CX IO functions for this network 
		  if ( m.testCX)  {
			  logger.info("Started exporting network in CX format.");

			  String fileName = exportNetworkInCX( networkID);
			  logger.info("network exported into CX file " + fileName);
		 
			  logger.info("Started importing exported CX file");
			  UUID newCXUUID = importCXFile(fileName);
			  assertEquivalence(newCXUUID, m);
 		
			  UUID reimporedNetworkID = null;
			  if ( m.srcFormat != NetworkSourceFormat.SIF) { 
				  logger.info("Started exporting CX network in its original source format.");
				  exportNetwork(m, conn, newCXUUID);

				  logger.info("Started importing exported network.");
				  parser = importFile ( System.getProperty("user.dir") + "/"+ newCXUUID.toString(), m);
		  
				  logger.info("Verifying network loaded from exported file.");
				  reimporedNetworkID = parser.getUUIDOfUploadedNetwork();
				  assertEquivalence(reimporedNetworkID, m);
			 
			  }

			  logger.info("Deleting CX related test networks related to " + m.fileName + " from db.");
			  
 			  deleteTestNetwork( newCXUUID, dao); 
 			  deleteTestNetwork( reimporedNetworkID, dao); 

		  }
		 
  		 logger.info("Deleting all other test networks related to " + m.fileName + " from db.");
 	
 		 deleteTestNetwork( networkID, dao); 
 		 deleteTestNetwork( nativeReloadedNetworkID, dao); 
		  
 		  conn.close();
		  
		  logger.info("Deleting network document exported in first round.");
		  if ( file1 !=null )
			  file1.delete();
 		  
 		 logger.info("Deleting network document exported in second round " + networkID.toString());
 		  if (file2 != null ) 
 			  file2.delete();
 		  
 		 logger.info("All done for "+ m.fileName);
		}
		
		logger.info("All tests passed.");

	}


	private static void deleteTestNetwork(UUID newCXUUID, NetworkDAO dao) throws ObjectNotFoundException, NdexException {
		if ( newCXUUID !=null) {
			logger.info( "Deleting network " + newCXUUID.toString()); 
			dao.logicalDeleteNetwork(newCXUUID.toString());
			dao.deleteNetwork(newCXUUID.toString());
			dao.commit();
		}
	}
	

	private static void exportNetwork(TestMeasurement m, ODatabaseDocumentTx conn,
			UUID networkID) throws ParserConfigurationException, ClassCastException, NdexException, TransformerException, SAXException, IOException, XMLStreamException {
		  if ( m.srcFormat == NetworkSourceFormat.XGMML) {
			  XGMMLNetworkExporter exporter = new XGMMLNetworkExporter(conn);
			  FileOutputStream out = new FileOutputStream (networkID.toString());
			  exporter.exportNetwork(networkID,out);
			  out.close();
              
		  }	else if ( m.srcFormat == NetworkSourceFormat.BIOPAX) {
			  BioPAXNetworkExporter exporter = new BioPAXNetworkExporter(conn);
			  try (FileOutputStream out = new FileOutputStream (networkID.toString())) {
				  exporter.exportNetwork(networkID,out);
			  }	  
		  }
		  else if ( m.srcFormat == NetworkSourceFormat.BEL) {
				NetworkDocDAO  dao = new NetworkDocDAO(conn);

				// initiate the network state
				XbelNetworkExporter exporter = 
						new XbelNetworkExporter(AllTests.testUser, networkID.toString(),dao,networkID.toString());
			//
				exporter.exportNetwork();
			  
		  } 

	}

	// return the file name
	private static String exportNetworkInCX( UUID networkID) throws Exception {
		try (CXNetworkExporter exporter = new CXNetworkExporter(networkID.toString()) ) {
			String outputFileName = networkID.toString() + ".cx";
			try (FileOutputStream out = new FileOutputStream (outputFileName)) {
			  exporter.writeNetworkInCX(out, true);
			  return outputFileName;
			}	  
		}
	}
	
	
	private static IParsingEngine importFile (String fileName, TestMeasurement m) throws Exception {
		  IParsingEngine parser;	
		  String testFile = fileName;
		  if ( m.srcFormat == NetworkSourceFormat.XGMML) {
			  parser = new XgmmlParser(testFile, AllTests.testUser, 
			  			AllTests.db,m.fileName, "");
		  } else if ( m.srcFormat == NetworkSourceFormat.BEL) {
			  parser = new XbelParser(testFile,AllTests.testUser, AllTests.db, "");
		  } else if (m.srcFormat == NetworkSourceFormat.SIF) {
			  parser = new SifParser(testFile,AllTests.testUser, AllTests.db, FilenameUtils.getBaseName( m.fileName), "" );
		  } else if ( m.srcFormat == NetworkSourceFormat.BIOPAX) {
			  parser = new BioPAXParser ( testFile, AllTests.testUser, AllTests.db, FilenameUtils.getBaseName( m.fileName), "");
		  } else 
			  throw new Exception ("unsupported source format " + m.srcFormat);
		  
		  parser.parseFile();
		  
		  return parser;

	}	
	
	private static UUID importCXFile (String fileName) throws Exception {
		  FileInputStream input = new FileInputStream ( fileName);
		  try (CXNetworkLoader loader = new CXNetworkLoader ( input, AllTests.testUser) ) {
			 return loader.persistCXNetwork();
		  }
	}	

	
    private static void assertEquivalence(UUID networkID, TestMeasurement m) throws NdexException {

    	// verify a uploaded network
		 try (ODatabaseDocumentTx conn = AllTests.db.getAConnection()) {
			 NetworkDAO dao = new NetworkDAO(conn);
			 Network n = dao.getNetworkById(networkID);
			 assertEquals(n.getName(), m.networkName);
			 assertNotNull(n.getDescription());
			 assertEquals(n.getNodeCount(), n.getNodes().size());
			 assertEquals(n.getNodeCount(), m.nodeCnt);
			 assertEquals(n.getEdgeCount(), m.edgeCnt);
			 assertEquals(n.getEdges().size(), m.edgeCnt);
			 if (m.basetermCnt >=0 ) {
/*			 TreeSet<String> s = new TreeSet<>();  // uncomment this section to debug

				 for ( BaseTerm ss : n.getBaseTerms().values()) {
					 s.add(ss.getName());
					 
				 }
				 int i =0;
				 for(String si : s) { 
				   System.out.println(i + "\t" + si);
				   i++;
				 }       // uncomment this section to debug
				  */
				 assertEquals(n.getBaseTerms().size(), m.basetermCnt);
			 }  
			 if ( m.citationCnt >= 0 )
				 assertEquals(n.getCitations().size(), m.citationCnt);
	//		 if ( m.elmtPresPropCnt >= 0 )
	//			 assertEquals(n.getBaseTerms().size(), m.basetermCnt);
	//		 if ( m.elmtPropCnt >=0)
	//			 assertEquals(n.getBaseTerms().size(), m.basetermCnt);
			 if ( m.funcTermCnt >=0 )
				 assertEquals(n.getFunctionTerms().size(), m.funcTermCnt);
			 if ( m.nameSpaceCnt >=0 )
				 assertEquals(n.getNamespaces().size(), m.nameSpaceCnt);
		//	 if ( m.netPresPropCnt >=0 )
		//		 assertEquals(n.getPresentationProperties().size(), m.netPresPropCnt);
			 if ( m.netPropCnt >=0 )
				 assertEquals(n.getProperties().size(), m.netPropCnt+1);
			 if ( m.reifiedEdgeCnt >=0 )
				 assertEquals(n.getReifiedEdgeTerms().size(), m.reifiedEdgeCnt);
			 if ( m.support >=0 )
				 assertEquals(n.getSupports().size(), m.support);
			 if ( m.nodePropCnt >=0 ) {
				 int i = 0 ;
				 for ( Node node : n.getNodes().values() ) {
					 i += node.getProperties().size();
				 }
				 assertEquals(i, m.nodePropCnt);
			 }
			 if ( m.edgePropCnt >=0 ) {
				 int i = 0 ;
				 for ( Edge edge : n.getEdges().values() ) {
					 i += edge.getProperties().size();
				 }
				 assertEquals(i, m.edgePropCnt);
			 }
			 n = null;	 
		 }
   
    }
 

}
