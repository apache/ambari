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

package org.apache.hms.common.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.math.NumberRange;


public class HostUtil {
  private static int MAX = 100000;
  private static String regex = "\\[(\\d+)-(\\d+)\\]";
  private static Pattern p = Pattern.compile(regex);
  
  private static List<NumberRange> ranges;
  private List<Integer> rangesPaddingSize = null;
  private String[] expressions = null;
  private static Integer[] indices;
  private static List<String> hosts;
  
  public HostUtil(String[] expressions) {
//    this.automaton = new RegExp(regex).toAutomaton();
    this.ranges = new ArrayList<NumberRange>();
    this.rangesPaddingSize = new ArrayList<Integer>();
    this.expressions = expressions;
    this.hosts = new ArrayList<String>();
  }
  
  /*
   * Compile a list of nodes by range expression like [1-10] or [01-10]
   */
  public List<String> generate() {
    for(String exp : expressions) {
      if(exp.indexOf('[')>=0) {
        ranges.clear();
        rangesPaddingSize.clear();
        findRange(exp);
        String template = generateTemplate(exp);
        indices = new Integer[ranges.size()];
        compute(0, ranges.get(0), template);
      } else {
        hosts.add(exp);
      }
    }
    return hosts;
  }

  private void findRange(String exp) {
    Matcher m = p.matcher(exp);
    while(m.find()) {
      int low = Integer.parseInt(m.group(1));
      int high = Integer.parseInt(m.group(2));
      int padding = checkPadding(m.group(1));
      if(low > high) {
        throw new RuntimeException("Invalid range - low: "+low+" high: "+high);
      }
      NumberRange range = new NumberRange(low, high);
      ranges.add(range);
      rangesPaddingSize.add(padding);
    }
  }

  private String generateTemplate(String exp) {
    String template = exp;
    for(int i = 0; i < rangesPaddingSize.size(); i++) {
      if(rangesPaddingSize.get(i)>0) {
        StringBuilder s = new StringBuilder();
        s.append("%0");
        s.append(rangesPaddingSize.get(i));
        s.append("d");
        template = template.replaceFirst(regex, s.toString());
      } else {
        template = template.replaceFirst(regex, "\\%d");        
      }
    }
    return template;
  }
  
//  private List<String> populateNames(List<NumberRange> ranges) {
//    List<String> list = new ArrayList<String>();
//    int[] index = new int[ranges.size()];
//    int tracker = 0;
//    for(NumberRange range : ranges) {
//      for(int current = range.getMinimumInteger();current < range.getMaximumInteger(); current++)
//      index[tracker] = current;
//      locateName(tracker, index, range);
//      tracker++;
//    }
//    return list;    
//  }

  private static void compute(int depth, NumberRange range, String template) {
    int depth2 = depth;
    for (int i=range.getMinimumInteger(); i<=range.getMaximumInteger(); i++) {
      indices[depth] = new Integer(i);
      if(depth < ranges.size() - 1 ) {
        depth++;
        compute(depth, ranges.get(depth), template);
        depth = depth2;
      } else {
        String buffer = String.format(template, indices);
        hosts.add(buffer);
        //System.out.println(buffer);
      }
    }
   }
  
  private int checkPadding(String start) {
    if(start.indexOf("0")==0) {
      return start.length();
    }
    return 0;
  }
  
  public List<String> generateFromCluster(String cluster) {
    // TODO Auto-generated method stub
    return null;
  }
  
//  private List<String> range(String exp) {
//    List<String> range = new ArrayList<String>();
//    return range;
//  }
//  
//  private final Automaton automaton;
  
//  public String[] generate() {
//    String[] hosts = null;
//    int size = 1;
//    ArrayList<byte[]> array = new ArrayList<byte[]>();
//    generate(array, automaton.getInitialState());
//    for(byte[] choices : array) {
//      size=size*choices.length;
//      for(int loop = 0; loop<choices.length; loop++) {
//        System.out.print((char) choices[loop]);
//      }
//      System.out.println("");
//    }
//    hosts = new String[size];
//
//    return hosts;
//  }

//  private void generate(ArrayList<byte[]> array, State state) {
//    List<Transition> transitions = state.getSortedTransitions(true);
//    if (transitions.size() == 0) {
//        assert state.isAccept();
//        return;
//    }
//    int nroptions = state.isAccept() ? transitions.size() : transitions.size() - 1;
//    int option = transitions.size() -1;
//    if (state.isAccept() && option == 0) {          // 0 is considered stop
//        return;
//    }
//    // Moving on to next transition
//    Transition transition = transitions.get(option - (state.isAccept() ? 1 : 0));
//    findChoice(array, transition);
//    generate(array, transition.getDest());
//  }
//
//  private void findChoice(ArrayList<byte[]> array, Transition transition) {
//    byte[] list= new byte[transition.getMax()-transition.getMin()+1];
//    int j = 0;
//    for(int i = transition.getMin(); i<= transition.getMax(); i++) {
//      list[j] =(byte) i;
//      j++;
//    }
//    array.add(list);
//  }
  
//  private String walk(ArrayList<byte[]> tree, StringBuilder buffer) {
//    if(tree != null) {
//      for(int i=0; i<tree.size(); i++) {
//        if(tree.get(i)!=null) {
//          walk(tree.get(i), buffer);
//        } else {
//          return buffer.toString();
//        }
//      }
//    }
//    return buffer.toString();
//  }
  
  public static void main(String[] args) {
    HostUtil util = new HostUtil(args);
    List<String> hosts = util.generate();
    for(String host : hosts) {
      System.out.println(host);
    }
  }

}
