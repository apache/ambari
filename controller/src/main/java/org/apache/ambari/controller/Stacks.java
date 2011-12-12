/*
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
package org.apache.ambari.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.apache.ambari.common.rest.entities.Stack;
import org.apache.ambari.common.rest.entities.StackInformation;
import org.apache.ambari.common.rest.entities.Component;
import org.apache.ambari.common.rest.entities.Property;
import org.apache.ambari.datastore.DataStoreFactory;
import org.apache.ambari.datastore.DataStore;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class Stacks {

  private final DataStore dataStore;

    @Inject
    Stacks(DataStoreFactory dataStore) throws IOException {
      this.dataStore = dataStore.getInstance();
      recoverStacksAfterRestart();
    }
    
    /*
     * Stack name -> latest revision is always cached for each stack.
     * 
     */
    protected ConcurrentHashMap<String, Integer> stacks = new ConcurrentHashMap<String, Integer>();
    
    
    /*
     * Check if stack exists. Names and latest version number is always 
     * cached in the memory
     */
    public boolean stackExists(String stackName) throws IOException {
        if (!this.stacks.containsKey(stackName) &&
             !this.dataStore.stackExists(stackName)) {  
            return false;
        } 
        return true;
    }
    
    public int getStackLatestRevision(String stackName) {
        return this.stacks.get(stackName).intValue();
    }
    
    /*
     * Get stack. If revision = -1 then return latest revision
     */
    public Stack getStack(String stackName, int revision
                          ) throws WebApplicationException, IOException {
        
        if (!stackExists(stackName)) {
            String msg = "Stack ["+stackName+"] is not defined";
            throw new WebApplicationException ((new ExceptionResponse(msg, Response.Status.NOT_FOUND)).get());
        }
        
        /*
         * If revision is -1, then return the latest revision
         */  
        Stack bp = null;
        if (revision < 0) {
            bp = dataStore.retrieveStack(stackName, getStackLatestRevision(stackName));
        } else {
            if ( revision > getStackLatestRevision(stackName)) {  
                String msg = "Stack ["+stackName+"], revision ["+revision+"] does not exist";
                throw new WebApplicationException ((new ExceptionResponse(msg, Response.Status.NOT_FOUND)).get());
            }
            bp = dataStore.retrieveStack(stackName, revision);
        }
        return bp;  
    }
     
    /*
     * Add or update the stack
     */
    public Stack addStack(String stackName, Stack bp) throws Exception {
        /*
         * Validate and set the defaults add the stack as new revision
         */
        validateAndSetStackDefaults(stackName, bp);
        int latestStackRevision = dataStore.storeStack(stackName, bp);
        this.stacks.put(stackName, new Integer(latestStackRevision));
        return bp;
    }
    
    /*
     * Import the default stack from the URL location
     */
    public Stack importDefaultStack (String stackName, String locationURL) throws IOException {
        Stack stack;
        URL stackUrl;
        try {
            stackUrl = new URL(locationURL);
            InputStream is = stackUrl.openStream();
            
            /* JSON FORMAT READER
            ObjectMapper m = new ObjectMapper();
            stack = m.readValue(is, Stack.class);
            */
            JAXBContext jc = JAXBContext.newInstance(org.apache.ambari.common.rest.entities.Stack.class);
            Unmarshaller u = jc.createUnmarshaller();
            stack = (Stack)u.unmarshal(is);
            return addStack(stackName, stack);
        } catch (WebApplicationException we) {
            throw we;
        } catch (Exception e) {
            throw new WebApplicationException ((new ExceptionResponse(e)).get());
        }
    }
   
    /*
     * Validate the stack before importing into controller
     */
    public void validateAndSetStackDefaults(String stackName, Stack stack) throws Exception {
        
        if (stack.getName() == null || stack.getName().equals("")) {
            stack.setName(stackName);
        } else if (!stack.getName().equals(stackName)) { 
            String msg = "Name of stack in resource URL and stack definition does not match!";
            throw new WebApplicationException ((new ExceptionResponse(msg, Response.Status.BAD_REQUEST)).get());
        }
        
        if (stack.getRevision() == null || stack.getRevision().equals("") ||
            stack.getRevision().equalsIgnoreCase("null")) {
            stack.setRevision("-1");
        }
        if (stack.getParentName() != null && 
            (stack.getParentName().equals("") || stack.getParentName().equalsIgnoreCase("null"))) {
            stack.setParentName(null);
        }
        /*
         * Set the creation time 
         */
        stack.setCreationTime(Util.getXMLGregorianCalendar(new Date()));
    }
    
    /*
     *  Get the list of stack revisions
     */
    public List<StackInformation> getStackRevisions(String stackName) throws Exception {
        List<StackInformation> list = new ArrayList<StackInformation>();
        if (!this.stacks.containsKey(stackName)) {
            String msg = "Stack ["+stackName+"] does not exist";
            throw new WebApplicationException ((new ExceptionResponse(msg, Response.Status.NOT_FOUND)).get());
        }
        
        for (int rev=0; rev<=this.stacks.get(stackName); rev++) {
            // Get the stack
            Stack bp = dataStore.retrieveStack(stackName, rev);
            StackInformation bpInfo = new StackInformation();
            bpInfo.setCreationTime(bp.getCreationTime());
            bpInfo.setName(bp.getName());
            bpInfo.setRevision(bp.getRevision());
            bpInfo.setParentName(bp.getParentName());
            bpInfo.setParentRevision(bp.getParentRevision());
            List<String> componentNameVersions = new ArrayList<String>();
            for (Component com : bp.getComponents()) {
                String comNameVersion = ""+com.getName()+"-"+com.getVersion();
                componentNameVersions.add(comNameVersion);
            }
            bpInfo.setComponent(componentNameVersions);
            list.add(bpInfo);
        }
        return list;
    }
    
    /*
     * Return list of stack names
     */
    public List<StackInformation> getStackList() throws Exception {
        List<StackInformation> list = new ArrayList<StackInformation>();
        for (String bpName : this.stacks.keySet()) {
            // Get the latest stack
            Stack bp = dataStore.retrieveStack(bpName, -1);
            StackInformation bpInfo = new StackInformation();
            // TODO: get the creation and update times from stack
            bpInfo.setCreationTime(bp.getCreationTime());
            bpInfo.setName(bp.getName());
            bpInfo.setRevision(bp.getRevision());
            bpInfo.setParentName(bp.getParentName());
            bpInfo.setParentRevision(bp.getParentRevision());
            List<String> componentNameVersions = new ArrayList<String>();
            for (Component com : bp.getComponents()) {
                String comNameVersion = ""+com.getName()+"-"+com.getVersion();
                componentNameVersions.add(comNameVersion);
            }
            bpInfo.setComponent(componentNameVersions);
            list.add(bpInfo);
        }
        return list;
    }
    
    /*
     * Delete the stack including all its versions.
     * The caller must ensure that no cluster uses this stack.
     */
    public void deleteStack(String stackName) throws Exception {
        dataStore.deleteStack(stackName);
        this.stacks.remove(stackName);
    }
    
    /*
     * UTIL methods
     */
    public Property getProperty(String key, String value) {
        Property p = new Property();
        p.setName(key);
        p.setValue(value);
        return p;
    }
    
    private static String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }

    public static JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
        InputStream is = new URL(url).openStream();
        try {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
            String jsonText = readAll(rd);
            JSONObject json = new JSONObject(jsonText);
            return json;
        } finally {
            is.close();
        }
    }

    private void recoverStacksAfterRestart() throws IOException {
        List<String> stackList = dataStore.retrieveStackList();
        for (String stackName : stackList) {
            this.stacks.put(stackName, dataStore.retrieveLatestStackRevisionNumber(stackName));
        }
    }
}
