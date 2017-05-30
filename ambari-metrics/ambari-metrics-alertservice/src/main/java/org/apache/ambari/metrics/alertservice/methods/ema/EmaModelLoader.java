package org.apache.ambari.metrics.alertservice.methods.ema;

import com.google.gson.Gson;
import com.sun.org.apache.commons.logging.Log;
import com.sun.org.apache.commons.logging.LogFactory;
import org.apache.spark.SparkContext;
import org.apache.spark.mllib.util.Loader;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class EmaModelLoader implements Loader<EmaModel> {
    private static final Log LOG = LogFactory.getLog(EmaModelLoader.class);

    @Override
    public EmaModel load(SparkContext sc, String path) {
        Gson gson = new Gson();
        try {
            String fileString = new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
            return gson.fromJson(fileString, EmaModel.class);
        } catch (IOException e) {
            LOG.error(e);
        }
        return null;
    }
}
