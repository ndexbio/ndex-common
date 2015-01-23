package org.ndexbio.task;

import java.io.FileReader;
import java.util.Hashtable;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;

import org.ndexbio.model.exceptions.NdexException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Configuration
{
    public static final String UPLOADED_NETWORKS_PATH_PROPERTY = "Uploaded-Networks-Path";
    
    private static Configuration INSTANCE = null;
    private static final Logger _logger = LoggerFactory.getLogger(Configuration.class);
    private Properties _configurationProperties;
    
	private String dbURL;
	private static final String dbUserPropName 	   = "OrientDB-Username";
	private static final String dbPasswordPropName = "OrientDB-Password";
	    
	private String hostURI ;
	private String ndexSystemUser ;
	private String ndexSystemUserPassword;
	private String ndexRoot;
   
    
    /**************************************************************************
    * Default constructor. Made private to prevent instantiation. 
     * @throws NdexException 
    **************************************************************************/
    private Configuration() throws NdexException
    {
        try
        {
        	String configFilePath = System.getenv("ndexConfigurationPath");
        	
        	if ( configFilePath == null) {
        		InitialContext ic = new InitialContext();
        	    configFilePath = (String) ic.lookup("java:comp/env/ndexConfigurationPath"); 
        	}    
        	
        	if ( configFilePath == null) {
        		_logger.error("ndexConfigurationPath is not defined in environement.");
        		throw new NdexException("ndexConfigurationPath is not defined in environement.");
        	} 
        	
        	_logger.info("Loadinging ndex configuration from " + configFilePath);
        	
        	_configurationProperties = new Properties();
        	FileReader reader = new FileReader(configFilePath);
        	_configurationProperties.load(reader);
        	reader.close();
            
            dbURL 	= getRequiredProperty("OrientDB-URL");
            hostURI = getRequiredProperty("HostURI");

            this.ndexSystemUser = getRequiredProperty("NdexSystemUser");
            this.ndexSystemUserPassword = getRequiredProperty("NdexSystemUserPassword");

            this.ndexRoot = getRequiredProperty("NdexRoot");
            
            
        }
        catch (Exception e)
        {
            _logger.error("Failed to load the configuration file.", e);
            throw new NdexException ("Failed to load the configuration file. " + e.getMessage());
        }
    }
    
    
    private String getRequiredProperty (String propertyName ) throws NdexException {
    	String result = _configurationProperties.getProperty(propertyName);
        if ( result == null) {
        	throw new NdexException ("property " + propertyName + " not found in configuration.");
        }
        return result;
    }
    
    
    /**************************************************************************
    * Gets the singleton instance. 
     * @throws NdexException 
    **************************************************************************/
    public static Configuration getInstance() throws NdexException
    {
    	if ( INSTANCE == null)  { 
    		INSTANCE = new Configuration();
    	}
        return INSTANCE;
    }

    
    
    /**************************************************************************
    * Gets the singleton instance. 
    * 
    * @param propertyName
    *            The property name.
    **************************************************************************/
    public String getProperty(String propertyName)
    {
        return _configurationProperties.getProperty(propertyName);
    }
    
    /**************************************************************************
    * Gets the singleton instance. 
    * 
    * @param propertyName
    *            The property name.
    * @param propertyValue
    *            The property value.
    **************************************************************************/
    public void setProperty(String propertyName, String propertyValue)
    {
        _configurationProperties.setProperty(propertyName, propertyValue);
    }
    
    public String getDBURL () { return dbURL; }
    public String getDBUser() { return _configurationProperties.getProperty(dbUserPropName); }
    public String getDBPasswd () { return _configurationProperties.getProperty(dbPasswordPropName); }
    public String getHostURI () { return hostURI; }
    public String getSystmUserName() {return this.ndexSystemUser;}
    public String getSystemUserPassword () {return this.ndexSystemUserPassword;}
    public String getNdexRoot()  {return this.ndexRoot;}
    
}
