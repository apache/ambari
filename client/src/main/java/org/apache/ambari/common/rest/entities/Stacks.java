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
package org.apache.ambari.common.rest.entities;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Stacks", propOrder = {
    "stack"
})
@XmlRootElement (name = "Stacks")
public class Stacks {

        @XmlElement(name = "Stack", required = true)
    protected List<Stack> stack = new ArrayList<Stack>();
    private static Stacks StacksTypeRef=null;
        
    private Stacks() {}
    
    public static synchronized Stacks getInstance() {
        if(StacksTypeRef == null) {
                StacksTypeRef = new Stacks();
        }
        return StacksTypeRef;
    }

    public Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }

    /* 
     * Add StackType to Stack list 
    */
    public void addStack(Stack s) throws Exception {
        /*
         * TODO: Validate the cluster definition that could not be
         * done on client side.
         */
        synchronized (stack) {
                for (Stack cls : stack) {
                        if (cls.getName().equals(s.getName())) {
                                throw new Exception("Stack Already Exists");
                        }
                }
                stack.add(s);
                
                /*
                 * TODO: Persist the cluster definition to data store
                 * as a initial version r0. 
                 */
        }
        return;
    } 
    
    /* 
     * Update the existing ClusterType from cluster list 
    */
    public void updateStack(Stack s) throws Exception {
        /*
         * TODO: Validate the StackType element?
         */
        synchronized (stack) {
                int i;
                for (i=0; i<stack.size(); i++) {
                        if (stack.get(i).getName().equals(s.getName())) {
                                if (s.getDescription() != null) stack.get(i).setDescription(s.getDescription());
                                if (s.getLocationURL() != null) stack.get(i).setLocationURL(s.getLocationURL());
                        }
                }
                if (i==stack.size()) {
                        throw new Exception ("Stack:["+s.getName()+"] does not exists");
                }
        }
        return;
    }
    
    /* 
     * Delete stack 
    */
    public void deleteStack(String stackName) throws Exception {
        /* 
         * 
         */
        synchronized (stack) {
                for (Stack stk : stack) {
                        if (stk.getName().equals(stackName)) {
                                stack.remove(stk);
                        }
                }
        }       
        return;
    }

    /* 
     * Get StackType from stack list given its name 
    */
    public Stack getStack(String stackName) throws Exception {
        for (Stack stk : stack) {
                        if (stk.getName().equals(stackName)) {
                                return stk;
                        }
                }
        throw new Exception ("Stack:["+stackName+"] does not exists");
    }
    
    public synchronized List<Stack> getStackList() {
        return stack;
    }
}
