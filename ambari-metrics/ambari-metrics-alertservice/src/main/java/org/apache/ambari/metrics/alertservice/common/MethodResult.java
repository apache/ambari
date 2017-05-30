package org.apache.ambari.metrics.alertservice.common;

public abstract class MethodResult {
    protected String methodType;
    public abstract String prettyPrint();

    public String getMethodType() {
        return methodType;
    }
}
