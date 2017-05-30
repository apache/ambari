package org.apache.ambari.metrics.alertservice.methods.ema;

import org.apache.ambari.metrics.alertservice.common.MethodResult;

public class EmaResult extends MethodResult{

    double diff;

    public EmaResult(double diff) {
        this.methodType = "EMA";
        this.diff = diff;
    }


    @Override
    public String prettyPrint() {
        return methodType + "(` = " + diff + ")";
    }
}
