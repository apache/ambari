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

import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.ambari.common.rest.entities.Node;
import com.google.inject.Singleton;

@Singleton
public class Nodes {
        
    public static final String AGENT_DEPLOYMENT_STATE_TOBE_INSTALLED = "AGENT_TOBE_INSTALLED";
    public static final String AGENT_DEPLOYMENT_STATE_INSTALLED = "AGENT_INSTALLED";
    
    
    public static final short NODE_HEARTBEAT_INTERVAL_IN_MINUTES = 5;
    public static final short NODE_MAX_MISSING_HEARBEAT_INTERVALS = 3;
    public static final long  NODE_NOT_RESPONDING_DURATION = NODE_HEARTBEAT_INTERVAL_IN_MINUTES * 
                                                             NODE_MAX_MISSING_HEARBEAT_INTERVALS * 60 * 1000;
    
    // node name to Node object hashmap
    protected ConcurrentHashMap<String, Node> nodes = new ConcurrentHashMap<String, Node>();

    public ConcurrentHashMap<String, Node> getNodes () {
        return nodes;
    }
    
    /*
     * Get Nodes 
     * TODO: simplify logic? 
     */
    public List<Node> getNodesByState (String allocatedx, String alivex) throws Exception {
        /*
         * Convert string to boolean states
         */
        Boolean allocated = true; Boolean alive = true;
        if (allocatedx.equalsIgnoreCase("false")) { allocated = false; }
        if (alivex.equalsIgnoreCase("false")) { alive = false; }
        
        //System.out.println("allocated:<"+allocated+">");
        //System.out.println("alive:<"+alive+">");
        //System.out.println("allocatedx:<"+allocatedx+">");
        //System.out.println("alivex:<"+alivex+">");
        
        List<Node> list = new ArrayList<Node>();
        GregorianCalendar cal = new GregorianCalendar(); 
        cal.setTime(new Date());
        XMLGregorianCalendar curTime = DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
        
        for (Node n : this.nodes.values()) {
            if (allocatedx.equals("") && alivex.equals("")) {
                list.add(n); 
            }
            if (allocatedx.equals("") && alive) {
                if (getTimeDiffInMillis(curTime, n.getNodeState().getLastHeartbeatTime()) < NODE_NOT_RESPONDING_DURATION) {
                    list.add(n);
                }
            }
            if (allocatedx.equals("") && !alive) {        
                if (getTimeDiffInMillis(curTime, n.getNodeState().getLastHeartbeatTime()) >= NODE_NOT_RESPONDING_DURATION) {
                    list.add(n);
                }
            }
            if (alivex.equals("") && allocated ) {
                if (n.getNodeState().getAllocatedToCluster()) {
                    list.add(n);
                }
            }
            if (alivex.equals("") && !allocated) {
                if (!n.getNodeState().getAllocatedToCluster()) {
                    list.add(n);
                }
            }
            if (allocated && alive) {
                if (n.getNodeState().getAllocatedToCluster() && 
                    getTimeDiffInMillis(curTime, n.getNodeState().getLastHeartbeatTime()) < NODE_NOT_RESPONDING_DURATION) {
                    list.add(n);
                }
            }
            if (allocated && !alive) {
                if (n.getNodeState().getAllocatedToCluster() && 
                    getTimeDiffInMillis(curTime, n.getNodeState().getLastHeartbeatTime()) >= NODE_NOT_RESPONDING_DURATION) {
                    list.add(n);
                }
            }
            if (!allocated && alive) {
                if (!n.getNodeState().getAllocatedToCluster() && 
                    getTimeDiffInMillis(curTime, n.getNodeState().getLastHeartbeatTime()) < NODE_NOT_RESPONDING_DURATION) {
                    list.add(n);
                }
            }
            if (!allocated && !alive) {
                if (!n.getNodeState().getAllocatedToCluster() && 
                    getTimeDiffInMillis(curTime, n.getNodeState().getLastHeartbeatTime()) >= NODE_NOT_RESPONDING_DURATION) {
                    list.add(n);
                }
            }
        }
        
        return list;
    }
    
    /*
     * Get the node
     */
    public Node getNode (String name) throws Exception {
        if (!this.nodes.containsKey(name)) {
            String msg = "Node ["+name+"] does not exist";
            throw new WebApplicationException((new ExceptionResponse(msg, Response.Status.NOT_FOUND)).get());
        }
        return this.nodes.get(name);
    }
    
    /*
     * Register new node
     */
    public synchronized void checkAndUpdateNode (String name, Date hearbeatTime) throws Exception {
        Node node = this.nodes.get(name);
        
        if (node == null) {
            node = new Node(name);
            getNodes().put(name, node);
        }
        node.getNodeState().setLastHeartbeatTime(hearbeatTime);      
    }
    
    /*
     * Get time difference
     */
    public static long getTimeDiffInMillis (XMLGregorianCalendar t2, 
                                            XMLGregorianCalendar t1
                                            ) throws Exception {
        return t2.toGregorianCalendar().getTimeInMillis() -  
            t1.toGregorianCalendar().getTimeInMillis();
    }
    
    public static void main (String args[]) {
       XMLGregorianCalendar t1;
       XMLGregorianCalendar t2;
       
       try {
           GregorianCalendar t1g = new GregorianCalendar();
           t1g.setTime(new Date());
           t1 = DatatypeFactory.newInstance().newXMLGregorianCalendar(t1g);
           
           Thread.sleep(500);
           
           GregorianCalendar t2g = new GregorianCalendar();
           t2g.setTime(new Date());
           t2 = DatatypeFactory.newInstance().newXMLGregorianCalendar(t2g);
           
           System.out.println("TIME ["+Nodes.getTimeDiffInMillis(t2, t1)+"]");
           
           System.out.println("TIME1 ["+t1.toString()+"]");
           System.out.println("TIME2 ["+t2.toString()+"]");
           
       } catch (Exception e) {
           e.printStackTrace();
       }
    }
}
