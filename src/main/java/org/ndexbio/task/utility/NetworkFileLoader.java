package org.ndexbio.task.utility;

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.ndexbio.common.access.NdexAOrientDBConnectionPool;
import org.ndexbio.common.access.NdexDatabase;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.task.Configuration;
import org.ndexbio.task.parsingengines.SifParser;
import org.ndexbio.task.parsingengines.XbelParser;
import org.ndexbio.task.parsingengines.XgmmlParser;

public class NetworkFileLoader {

	public static void main(String[] args) throws NdexException {

		if ( args.length != 3) {
			System.out.println("Usage: networkFileLoader <accountName> <fileType> <dir>");
			System.out.println("       Supported file types are: sif, xbel, and xgmml.");
		}
	
    	// read configuration
    	Configuration configuration = Configuration.getInstance();
    	
    	//and initialize the db connections
    	NdexAOrientDBConnectionPool.createOrientDBConnectionPool(
    			configuration.getDBURL(),
    			configuration.getDBUser(),
    			configuration.getDBPasswd(), 1);
    	
    	
		NdexDatabase db = new NdexDatabase(configuration.getHostURI());
		String type = args[1].toLowerCase();
		
		try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(
				Paths.get(args[2]))) 
		{
            for (Path path : directoryStream) {
              System.out.println("Processing file " +path.toString());
              
      			if ( type.equals("xbel") ) {
      				XbelParser parser = new XbelParser(path.toString(), args[0], db);
      				parser.parseFile();
      			} else if ( type.equals("sif")) {
      				SifParser parser2 = new SifParser(path.toString(),args[0], db, 
      						com.google.common.io.Files.getNameWithoutExtension(path.toString()));
      				parser2.parseFile();
      			} else if ( type.equals("xgmml")) {
      				XgmmlParser parser = new XgmmlParser(path.toString(), args[0], db, path.toString());
      				parser.parseFile();
      			} else {
      				System.out.println ("Error: " + type + " is not a supported file type of this loader.");
      				System.exit(-1);
      			}
      		
      			System.out.println("file upload for  " + path.toString() +" finished.");
            }
        } catch (Exception e) {
        	System.out.println( "Error:  " + e.getMessage());
        	System.exit(-1);
        }
		
		db.close();
		NdexAOrientDBConnectionPool.close();
	}

}
