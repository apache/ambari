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
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.ambari.common.rest.entities.ClusterDefinition;
import org.apache.ambari.common.rest.entities.Node;
import org.apache.ambari.common.rest.entities.RoleToNodes;


public class Nodes {
        
    public static final String AGENT_DEPLOYMENT_STATE_TOBE_INSTALLED = "AGENT_TOBE_INSTALLED";
    public static final String AGENT_DEPLOYMENT_STATE_INSTALLED = "AGENT_INSTALLED";
    
    
    public static final short NODE_HEARTBEAT_INTERVAL_IN_MINUTES = 5;
    public static final short NODE_MAX_MISSING_HEARBEAT_INTERVALS = 3;
    public static final long  NODE_NOT_RESPONDING_DURATION = NODE_HEARTBEAT_INTERVAL_IN_MINUTES * 
                                                             NODE_MAX_MISSING_HEARBEAT_INTERVALS * 60 * 1000;
    
    // node name to Node object hashmap
    protected ConcurrentHashMap<String, Node> nodes = new ConcurrentHashMap<String, Node>();

    private static Nodes NodesTypeRef=null;
    
    private Nodes() {}
        
    public static synchronized Nodes getInstance() {
        if(NodesTypeRef == null) {
            NodesTypeRef = new Nodes();
        }
        return NodesTypeRef;
    }

    public Object clone() throws CloneNotSupportedException {
            throw new CloneNotSupportedException();
    }
       
    public ConcurrentHashMap<String, Node> getNodes () {
        return nodes;
    }
    
    /*
     * Return the list of nodes associated with cluster given the role name and alive state
     * If rolename or alive state is not specified (i.e. "") then all the nodes associated
     * with cluster are returned.
     */
    public List<Node> getClusterNodes (String clusterName, String roleName, String alive) throws Exception {
        
        List<Node> list = new ArrayList<Node>();
        ClusterDefinition c = Clusters.getInstance().operational_clusters.get(clusterName).getLatestClusterDefinition();
        if (c.getNodes() == null || c.getNodes().equals("") || Clusters.getInstance().getClusterByName(clusterName).getClusterState().getState().equalsIgnoreCase("ATTIC")) {
            String msg = "No nodes are reserved for the cluster. Typically cluster in ATTIC state does not have any nodes reserved";
            throw new WebApplicationException((new ExceptionResponse(msg, Response.Status.NO_CONTENT)).get());
        }
        List<String> hosts = Clusters.getInstance().getHostnamesFromRangeExpressions(c.getNodes());
        for (String host : hosts) {
            if (!this.nodes.containsKey(host)) {
                String msg = "Node ["+host+"] is expected to be registered w/ controller but not locatable";
                throw new WebApplicationException((new ExceptionResponse(msg, Response.Status.INTERNAL_SERVER_ERROR)).get());
            }
            Node n = this.nodes.get(host);
            if (roleName != null && !roleName.equals("")) {
                if (!n.getNodeState().getNodeRoleNames().contains(roleName)) { continue; }
            }
            
            // Heart beat is set to epoch during node initialization.
            GregorianCalendar cal = new GregorianCalendar(); 
            cal.setTime(new Date());
            XMLGregorianCalendar curTime = DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
            if (alive.equals("") || (alive.equalsIgnoreCase("true") && getTimeDiffInMillis(curTime, n.getNodeState().getLastHeartbeatTime()) < NODE_NOT_RESPONDING_DURATION)
                || (alive.equals("false") && getTimeDiffInMillis(curTime, n.getNodeState().getLastHeartbeatTime()) >= NODE_NOT_RESPONDING_DURATION)) {
                list.add(this.nodes.get(host));
            }
        }
        if (list.isEmpty()) {
            String msg = "No nodes found!";
            throw new WebApplicationException((new ExceptionResponse(msg, Response.Status.NO_CONTENT)).get());
        }
        return list;
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
        
        if (list.isEmpty()) {
            throw new WebApplicationException(Response.Status.NO_CONTENT);
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
            Nodes.getInstance().getNodes().put(name, node);
        }
        node.getNodeState().setLastHeartbeatTime(hearbeatTime);      
    }
    
    /*
     * Get time difference
     */
    public long getTimeDiffInMillis (XMLGregorianCalendar t2, XMLGregorianCalendar t1) throws Exception {
        return t2.toGregorianCalendar().getTimeInMillis() -  t1.toGregorianCalendar().getTimeInMillis();
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
           
           System.out.println("TIME ["+Nodes.getInstance().getTimeDiffInMillis(t2, t1)+"]");
           
           System.out.println("TIME1 ["+t1.toString()+"]");
           System.out.println("TIME2 ["+t2.toString()+"]");
           
       } catch (Exception e) {
           e.printStackTrace();
       }
    }
}
