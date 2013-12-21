package org.ndexbio.service.helpers;

import java.util.Properties;

public class Configuration

{
    private static final Configuration INSTANCE = new Configuration();
    private final Properties ndexProperties = new Properties();
    public static final String UPLOADED_NETWORKS_PATH_PROPERTY = "Uploaded-Networks-Path";
    
    
    
    
    /**************************************************************************
    * Default constructor. Made private to prevent instantiation. 
    **************************************************************************/
    private Configuration()
    {
        try
        {
            ndexProperties.load(this.getClass().getClassLoader().getResourceAsStream("ndex.properties"));
        }
        catch (Exception e)
        {
            System.out.println(e.getMessage());
        }
    }
    
    
    
    /**************************************************************************
    * Gets the singleton instance. 
    **************************************************************************/
    public static Configuration getInstance()
    {
        return INSTANCE;
    }

    
    
    /**************************************************************************
    * Gets the singleton instance. 
    **************************************************************************/
    public String getProperty(String propertyName)
    {
        return ndexProperties.getProperty(propertyName);
    }
    
    /**************************************************************************
    * Gets the singleton instance. 
    **************************************************************************/
    public void setProperty(String propertyName, String propertyValue)
    {
        ndexProperties.setProperty(propertyName, propertyValue);
    }
}
