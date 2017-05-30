package org.apache.ambari.metrics.alertservice.common;


import java.util.ArrayList;
import java.util.List;

public class ResultSet {

    List<double[]> resultset = new ArrayList<>();

    public ResultSet(List<double[]> resultset) {
        this.resultset = resultset;
    }

    public void print() {
        System.out.println("Result : ");
        if (!resultset.isEmpty()) {
            for (int i = 0; i<resultset.get(0).length;i++) {
                for (double[] entity : resultset) {
                    System.out.print(entity[i] + " ");
                }
                System.out.println();
            }
        }
    }
}
