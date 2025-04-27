package com.test.sap.sap_rfc_demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.sap.conn.jco.JCoDestination;
import com.sap.conn.jco.JCoDestinationManager;
import com.sap.conn.jco.JCoException;
import com.sap.conn.jco.ext.DestinationDataProvider;
import com.sap.conn.jco.ext.Environment;
import java.util.Properties;

@Configuration
public class SapConfig {

    private static final String DESTINATION_NAME = "RFC_DESTINATION";

    @Value("${sap.client.mshost}")
    private String mshost;
    
    @Value("${sap.client.client}")
    private String client;
    
    @Value("${sap.client.user}")
    private String user;
    
    @Value("${sap.client.passwd}")
    private String password;
    
    @Value("${sap.client.lang}")
    private String lang;
    
    @Value("${sap.client.group}")
    private String group;
    
    @Value("${sap.client.r3name}")
    private String r3name;
    
    @Value("${sap.destination.peak_limit}")
    private String peakLimit;
    
    @Value("${sap.destination.pool_capacity}")
    private String poolCapacity;

    @Bean
    public JCoDestination sapDestination() throws JCoException {
        Properties connectProperties = new Properties();
        connectProperties.setProperty(DestinationDataProvider.JCO_MSHOST, mshost);
        connectProperties.setProperty(DestinationDataProvider.JCO_CLIENT, client);
        connectProperties.setProperty(DestinationDataProvider.JCO_USER, user);
        connectProperties.setProperty(DestinationDataProvider.JCO_PASSWD, password);
        connectProperties.setProperty(DestinationDataProvider.JCO_LANG, lang);
        connectProperties.setProperty(DestinationDataProvider.JCO_GROUP, group);
        connectProperties.setProperty(DestinationDataProvider.JCO_R3NAME, r3name);
        connectProperties.setProperty(DestinationDataProvider.JCO_PEAK_LIMIT, peakLimit);
        connectProperties.setProperty(DestinationDataProvider.JCO_POOL_CAPACITY, poolCapacity);

        CustomDestinationDataProvider.register();
        CustomDestinationDataProvider.getInstance().addDestination(DESTINATION_NAME, connectProperties);
        
        return JCoDestinationManager.getDestination(DESTINATION_NAME);
    }
} 