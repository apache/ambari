/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.ambari.logsearch.util;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Set;

import org.apache.ambari.logsearch.common.MessageEnums;
import org.apache.ambari.logsearch.view.VHost;
import org.apache.ambari.logsearch.view.VSummary;
import org.apache.log4j.Logger;

public class FileUtil {
  private static final Logger logger = Logger.getLogger(FileUtil.class);

  private FileUtil() {
    throw new UnsupportedOperationException();
  }
  
  public static Response saveToFile(String text, String fileName, VSummary vsummary) {
    String mainExportedFile = "";
    FileOutputStream fis = null;
    try {
      mainExportedFile = mainExportedFile + "**********************Summary**********************\n";
      mainExportedFile = mainExportedFile + "Number of Logs : " + vsummary.getNumberLogs() + "\n";
      mainExportedFile = mainExportedFile + "From           : " + vsummary.getFrom() + "\n";
      mainExportedFile = mainExportedFile + "To             : " + vsummary.getTo() + "\n";

      List<VHost> hosts = vsummary.getHosts();
      String blankCharacterForHost = String.format("%-8s", "");
      int numberHost = 0;
      for (VHost host : hosts) {
        numberHost += 1;
        String h = host.getName();
        String c = "";
        Set<String> comp = host.getComponents();
        boolean zonetar = true;
        if (comp != null) {
          for (String component : comp) {
            if (zonetar) {
              c = component;
              zonetar = false;
            } else {
              c = c + ", " + component;
            }
          }
        }
        if (numberHost > 9){
          blankCharacterForHost = String.format("%-7s", blankCharacterForHost);
        }else if (numberHost > 99){
          blankCharacterForHost = String.format("%-6s", blankCharacterForHost);
        }else if (numberHost > 999){
          blankCharacterForHost = String.format("%-5s", blankCharacterForHost);
        }else if (numberHost > 9999){
          blankCharacterForHost = String.format("%-4s", blankCharacterForHost);
        }else if (numberHost > 99999){
          blankCharacterForHost = String.format("%-3s", blankCharacterForHost);
        }
        if (numberHost == 1) {
          mainExportedFile = mainExportedFile + "Host" + blankCharacterForHost + "   : " + h + " [" + c + "] " + "\n";
        } else if (numberHost > 1) {
          mainExportedFile = mainExportedFile + "Host_" + numberHost + blankCharacterForHost + " : " + h + " [" + c + "] " + "\n";
        }

      }
      mainExportedFile = mainExportedFile + "Levels"+String.format("%-9s", blankCharacterForHost)+": " + vsummary.getLevels() + "\n";
      mainExportedFile = mainExportedFile + "Format"+String.format("%-9s", blankCharacterForHost)+": " + vsummary.getFormat() + "\n";
      mainExportedFile = mainExportedFile + "\n";

      mainExportedFile = mainExportedFile + "Included String: [" + vsummary.getIncludeString() + "]\n\n";
      mainExportedFile = mainExportedFile + "Excluded String: [" + vsummary.getExcludeString() + "]\n\n";
      mainExportedFile = mainExportedFile + "************************Logs***********************" + "\n";
      mainExportedFile = mainExportedFile + text + "\n";
      File file = File.createTempFile(fileName, vsummary.getFormat());
      fis = new FileOutputStream(file);
      fis.write(mainExportedFile.getBytes());
      return Response
        .ok(file, MediaType.APPLICATION_OCTET_STREAM)
        .header("Content-Disposition", "attachment;filename=" + fileName + vsummary.getFormat())
        .build();
    } catch (Exception e) {
      logger.error(e.getMessage());
      throw RESTErrorUtil.createRESTException(e.getMessage(), MessageEnums.ERROR_SYSTEM);
    } finally {
      if (fis != null) {
        try {
          fis.close();
        } catch (IOException e) {
        }
      }
    }
  }

  public static File getFileFromClasspath(String filename) {
    URL fileCompleteUrl = Thread.currentThread().getContextClassLoader().getResource(filename);
    logger.debug("File Complete URI :" + fileCompleteUrl);
    File file = null;
    try {
      file = new File(fileCompleteUrl.toURI());
    } catch (Exception exception) {
      logger.debug(exception.getMessage(), exception.getCause());
    }
    return file;
  }

}
