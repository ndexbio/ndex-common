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
package org.ndexbio.task;

import java.io.File;
import java.io.FileReader;
import java.util.Properties;

import javax.naming.InitialContext;

import org.ndexbio.common.NdexServerProperties;
import org.ndexbio.model.exceptions.NdexException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;

public class Configuration
{
    public static final String UPLOADED_NETWORKS_PATH_PROPERTY = "Uploaded-Networks-Path";
    
    private static final String PROP_USE_AD_AUTHENTICATION = "USE_AD_AUTHENTICATION";
    private static final String SOLR_URL = "SolrURL";
    
    private static Configuration INSTANCE = null;
    private static final Logger _logger = LoggerFactory.getLogger(Configuration.class);
    private Properties _configurationProperties;
    
	private String dbURL;
	private static final String dbUserPropName 	   = "OrientDB-Username";
	private static final String dbPasswordPropName = "OrientDB-Password";
	
	public static final String networkPostEdgeLimit = "NETWORK_POST_ELEMENT_LIMIT";
	private static final String defaultSolrURL = "http://localhost:8983/solr";
	
	private String solrURL;
	private String hostURI ;
	private String ndexSystemUser ;
	private String ndexSystemUserPassword;
	private String ndexRoot;
	
	private String ndexNetworkCachePath;
   
	private boolean useADAuthentication ;
	
	// Possible values for Log-Level are:
    // trace, debug, info, warn, error, all, off
    // If no Log-Level config parameter specified in /opt/ndex/conf/ndex.properties, or the value is 
	// invalid/unrecognized, we set loglevel to 'info'.
	// Please see http://logback.qos.ch/manual/architecture.html for description of log levels	
	private Level logLevel;
    
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
        	
        	_logger.info("Loading ndex configuration from " + configFilePath);
        	
        	_configurationProperties = new Properties();
        
        	try (FileReader reader = new FileReader(configFilePath)) {
        		_configurationProperties.load(reader);
        	}
            
            dbURL 	= getRequiredProperty("OrientDB-URL");
            hostURI = getRequiredProperty("HostURI");
            solrURL = getProperty(SOLR_URL);
            if ( solrURL == null)
            	solrURL = defaultSolrURL;
            
            this.ndexSystemUser = getRequiredProperty("NdexSystemUser");
            this.ndexSystemUserPassword = getRequiredProperty("NdexSystemUserPassword");

            this.ndexRoot = getRequiredProperty("NdexRoot");
            
            ndexNetworkCachePath = ndexRoot + "/" + NdexServerProperties.workspaceDir + "/cache/";
            
			File file = new File (ndexNetworkCachePath );
			if (!file.exists()) {
				if ( ! file.mkdirs()) {
					throw new NdexException ("Server failed to create cache directory " + ndexNetworkCachePath);
				}
			}
            
            setLogLevel();
                        
            // get AD authentication flag
            String useAd = getProperty(PROP_USE_AD_AUTHENTICATION);
            if (useAd != null && Boolean.parseBoolean(useAd)) {
            	setUseADAuthentication(true);
            } else {
            	setUseADAuthentication(false);
            }
        }
        catch (Exception e)
        {
            _logger.error("Failed to load the configuration file.", e);
            throw new NdexException ("Failed to load the configuration file. " + e.getMessage());
        }
    }
    
    /*
     * This method reads Log-Level configuration parameter from /opt/ndex/conf/ndex.properties 
     * and sets the log level.
     * In case Log-Level is not found in ndex.properties or value of Log-Level is invalid/unrecognized,
     * we set log level to 'info'.
     */
    private void setLogLevel() {
        ch.qos.logback.classic.Logger rootLog = 
        		(ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        
    	String result = _configurationProperties.getProperty("Log-Level");

        if (result == null) {
        	// if no Log-Level is specified in /opt/ndex/conf/ndex.properties, we set loglevel to info 
        	this.logLevel = Level.INFO;
        	rootLog.setLevel(Level.INFO);
        	_logger.error("No 'Log-Level' parameter found in config ndex.properties file. Log level set to 'info'.");
        } else {
        	switch (result.toLowerCase()) {
        	case "trace": 
            	_logger.trace("Log-Level configuration property is " + result + ". Log level set to 'trace'.");
            	this.logLevel = Level.TRACE;
        		rootLog.setLevel(Level.TRACE);	
        		break;
        		
        	case "debug":
        		_logger.trace("Log-Level configuration property is " + result + ". Log level set to 'debug'.");
        		this.logLevel = Level.DEBUG;
        		rootLog.setLevel(Level.DEBUG);
        		break;
        		
        	case "info":
        		_logger.trace("Log-Level configuration property is " + result + ". Log level set to 'info'.");
        		this.logLevel = Level.INFO;
        		rootLog.setLevel(Level.INFO);
        		break;
        		
        	case "warn":
        		_logger.trace("Log-Level configuration property is " + result + ". Log level set to 'warn'.");
        		this.logLevel = Level.WARN;
        		rootLog.setLevel(Level.WARN);
        		break;
        		
        	case "error":
        		_logger.trace("Log-Level configuration property is " + result + ". Log level set to 'error'.");
        		this.logLevel = Level.ERROR;
        		rootLog.setLevel(Level.ERROR);
        		break;
        		
        	case "all":
        		_logger.trace("Log-Level configuration property is " + result + ". Log level set to 'all'.");
        		this.logLevel = Level.ALL;
        		rootLog.setLevel(Level.ALL);
        		break;
        		
        	case "off":	
        		_logger.trace("Log-Level configuration property is " + result + ". Log level set to 'off'.");
        		this.logLevel = Level.OFF;
        		rootLog.setLevel(Level.OFF);
        		break;
        		
        	default:
            	_logger.info("Unrecoginzed value for Log-Level configuration property found: " + "'" + 
        	        result + "'" + ". Log level set to 'info'.");
            	this.logLevel = Level.INFO;
        		rootLog.setLevel(Level.INFO);
        	    break;
        	}	
        }
	}


	public String getRequiredProperty (String propertyName ) throws NdexException {
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
    * Gets the value of a property from configuration.
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
    public String getSolrURL() {return this.solrURL; }
    public Level  getLogLevel()  {return this.logLevel;}

	public boolean getUseADAuthentication() {
		return useADAuthentication;
	}


	public void setUseADAuthentication(boolean useADAuthentication) {
		this.useADAuthentication = useADAuthentication;
	}
	
	public String getNdexNetworkCachePath() {
		return ndexNetworkCachePath;
	}
    
}
