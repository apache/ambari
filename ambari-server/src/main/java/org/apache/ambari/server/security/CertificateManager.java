/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.security;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.Map;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Ambari security.
 * Manages server and agent certificates
 */
@Singleton
public class CertificateManager {
	
  @Inject Configuration configs;
	
  private static final Log LOG = LogFactory.getLog(CertificateManager.class);
	
	
  private static final String GEN_SRVR_KEY = "openssl genrsa -des3 -passout pass:{0} -out {1}/{2} 4096 ";
  private static final String GEN_SRVR_REQ = "openssl req -passin pass:{0} -new -key {1}/{2} -out {1}/{3} -batch";
  private static final String SIGN_SRVR_CRT = "openssl x509 -passin pass:{0} -req -days 365 -in {1}/{3} -signkey {1}/{2} -out {1}/{3} \n";
  private static final String EXPRT_KSTR = "openssl pkcs12 -export -in {1}/{3} -inkey {1}/{2} -certfile {1}/{3} -out {1}/{4} -password pass:{0} -passin pass:{0} \n";
	
  /**
   * Verify that root certificate exists, generate it otherwise.
   */
  public void initRootCert() {
    LOG.info("Initialization of root certificate");
		
    boolean certExists = isCertExists();
		
    LOG.info("Certificate exists:" + certExists);
		
	if (!certExists) {
      generateServerCertificate();
	}
  }
	
  /**
   * Checks root certificate state.
   * @return "true" if certificate exists
   */
  private boolean isCertExists() {

    Map<String, String> configsMap = configs.getConfigsMap();
    String srvrKstrDir = configsMap.get(Configuration.SRVR_KSTR_DIR_KEY);
    String srvrCrtName = configsMap.get(Configuration.SRVR_CRT_NAME_KEY);
    File certFile = new File(srvrKstrDir + File.separator + srvrCrtName);
    
	return certFile.exists();
	}
  
  
  /**
   * Runs os command
   */
  private void runCommand(String command) {
	  
	  String line = null;
      Process process = null;
      try {
        process = Runtime.getRuntime().exec(command);
        BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
        
        while ((line = br.readLine()) != null) {
          LOG.info(line);
        }
        
        try {
          process.waitFor();
        }
        catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
      catch (IOException e){
        e.printStackTrace();
      }
	  
  }
  
  private void generateServerCertificate() {
    LOG.info("Generation of server certificate");
    
    Map<String, String> configsMap = configs.getConfigsMap();
    String srvrKstrDir = configsMap.get(Configuration.SRVR_KSTR_DIR_KEY);
    String srvrCrtName = configsMap.get(Configuration.SRVR_CRT_NAME_KEY);
    String srvrKeyName = configsMap.get(Configuration.SRVR_KEY_NAME_KEY);
    String kstrName = configsMap.get(Configuration.KSTR_NAME_KEY);
    String srvrCrtPass = configsMap.get(Configuration.SRVR_CRT_PASS_KEY);
    
    Object[] scriptArgs = {srvrCrtPass, srvrKstrDir, srvrKeyName,
			               srvrCrtName, kstrName};

    String command = MessageFormat.format(GEN_SRVR_KEY,scriptArgs);

    LOG.info("Executing command:" + command);
    
	runCommand(command);
	
    command = MessageFormat.format(GEN_SRVR_REQ,scriptArgs);
    
    LOG.info("Executing command:" + command);
    
	runCommand(command);
	
    command = MessageFormat.format(SIGN_SRVR_CRT,scriptArgs);
    
    LOG.info("Executing command:" + command);
    
	runCommand(command);
	
    command = MessageFormat.format(EXPRT_KSTR,scriptArgs);
    
    LOG.info("Executing command:" + command);
    
	runCommand(command);
	
	}

  /**
   * Returns server certificate content
   * @return string with server certificate content
   */
  public String getServerCert() {
    Map<String, String> configsMap = configs.getConfigsMap();
    File certFile = new File(configsMap.get(Configuration.SRVR_KSTR_DIR_KEY) + File.separator + configsMap.get(Configuration.SRVR_CRT_NAME_KEY));  
    String srvrCrtContent = null;
    try {
      srvrCrtContent = FileUtils.readFileToString(certFile);
	} catch (IOException e) {
        LOG.error(e.getMessage());
	}   
    return srvrCrtContent;
	}
}
