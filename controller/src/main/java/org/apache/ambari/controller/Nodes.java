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
import org.apache.ambari.common.rest.entities.RoleToNodesMapEntry;


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
    
    public List<Node> getClusterNodes (String clusterName, String roleName, boolean alive) throws Exception {
        List<Node> list = new ArrayList<Node>();
        ClusterDefinition c = Clusters.getInstance().getClusterDefinition(clusterName);
        if (c.getNodeRangeExpressions() == null) {
            String msg = "No nodes are reserved for the cluster. Typically cluster in ATTIC state does not have any nodes reserved";
            throw new WebApplicationException((new ExceptionResponse(msg, Response.Status.NO_CONTENT)).get());
        }
        List<String> hosts = Clusters.getInstance().getHostnamesFromRangeExpressions(c.getNodeRangeExpressions());
        for (String host : hosts) {
            if (!this.nodes.containsKey(host)) {
                String msg = "Node ["+host+"] is expected to be registered w/ controller but not locatable";
                throw new WebApplicationException((new ExceptionResponse(msg, Response.Status.INTERNAL_SERVER_ERROR)).get());
            }
            Node n = this.nodes.get(host);
            if (roleName != null && !roleName.equals("")) {
                if (!n.getNodeState().getNodeRoleNames().contains(roleName)) { continue; }
            }
            
            // TODO: If heartbeat is null
            
            GregorianCalendar cal = new GregorianCalendar(); 
            cal.setTime(new Date());
            XMLGregorianCalendar curTime = DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
            if ((alive && getTimeDiffInMillis(curTime, n.getNodeState().getLastHeartbeatTime()) < NODE_NOT_RESPONDING_DURATION)
                || (!alive && getTimeDiffInMillis(curTime, n.getNodeState().getLastHeartbeatTime()) >= NODE_NOT_RESPONDING_DURATION)) {
                list.add(this.nodes.get(host));
            }
        }
        return list;
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
