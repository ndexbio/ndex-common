package org.ndexbio.service.helpers;

import java.util.Properties;

public class Configuration

{
    private static final Configuration INSTANCE = new Configuration();
    private final Properties ndexProperties = new Properties();
    public static final String UPLOADED_NETWORKS_PATH_PROPERTY = "Uploaded-Networks-Path";
    /*
     * OrientDB-URL=remote:localhost/ndex
OrientDB-Username=admin
OrientDB-Password=admin
OrientDB-Admin-Username=ndex
OrientDB-Admin-Password=ndex
SMTP-Auth=true
SMTP-Host=mail.ndexbio.org
SMTP-Port=587
SMTP-Username=support@ndexbio.org
SMTP-Password=ZrdF!nP8
Feedback-Email=support@ndexbio.org
Forgot-Password-Email=support@ndexbio.org
Forgot-Password-File=/home/john/Projects/ndex-rest/forgot-password.txt
Profile-Background-Width=670
Profile-Background-Height=200
Profile-Background-Path=/home/john/Projects/NDEx-Site/img/background/
Profile-Image-Width=100
Profile-Image-Height=125
Profile-Image-Path=/home/john/Projects/NDEx-Site/img/foreground/
Uploaded-Networks-Path=/opt/ndex/uploaded-networks/
     */
    
    
    
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
