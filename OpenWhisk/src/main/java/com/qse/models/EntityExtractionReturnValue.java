package com.qse.models;

import java.util.Map;

import org.semanticweb.yars.nx.Node;

import com.qse.encoders.StringEncoder;

public class EntityExtractionReturnValue {
    public String CECShardName; // Map from classID to number of entities of that class

    public EntityExtractionReturnValue (String CECShardName) {
        this.CECShardName = CECShardName;
    }

    public EntityExtractionReturnValue() {
    }

    public String getCECShardName() {
        return CECShardName;
    }

    public void setCECShardName(String CECShardName) {
        this.CECShardName = CECShardName;
    }

}
