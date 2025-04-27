package com.test.sap.sap_rfc_demo.config;

import com.sap.conn.jco.ext.DestinationDataEventListener;
import com.sap.conn.jco.ext.DestinationDataProvider;
import com.sap.conn.jco.ext.Environment;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class CustomDestinationDataProvider implements DestinationDataProvider {
    private static final CustomDestinationDataProvider INSTANCE = new CustomDestinationDataProvider();
    private final ConcurrentHashMap<String, Properties> destinations = new ConcurrentHashMap<>();

    private CustomDestinationDataProvider() {
        // private constructor
    }

    public static CustomDestinationDataProvider getInstance() {
        return INSTANCE;
    }

    public void addDestination(String destinationName, Properties properties) {
        destinations.put(destinationName, properties);
    }

    @Override
    public Properties getDestinationProperties(String destinationName) {
        return destinations.get(destinationName);
    }

    @Override
    public void setDestinationDataEventListener(DestinationDataEventListener eventListener) {
        // Not needed for this implementation
    }

    @Override
    public boolean supportsEvents() {
        return false;
    }

    public static void register() {
        if (!Environment.isDestinationDataProviderRegistered()) {
            Environment.registerDestinationDataProvider(getInstance());
        }
    }
} 