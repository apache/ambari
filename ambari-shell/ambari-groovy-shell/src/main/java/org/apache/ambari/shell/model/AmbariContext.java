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
package org.apache.ambari.shell.model;

import org.springframework.stereotype.Component;

/**
 * Holds information about the connected Ambari Server.
 */
@Component
public class AmbariContext {

  private String cluster;
  private boolean blueprintsAvailable;
  private Focus focus;
  private Hints hint;

  public AmbariContext() {
    this.focus = getRootFocus();
  }

  /**
   * Sets the name of the cluster.
   *
   * @param cluster
   */
  public void setCluster(String cluster) {
    this.cluster = cluster;
  }

  /**
   * Sets the focus to the root.
   */
  public void resetFocus() {
    this.focus = getRootFocus();
  }

  /**
   * Sets the focus.
   *
   * @param id   target of the focus, can be anything (blueprint id, host id..)
   * @param type type of the focus
   */
  public void setFocus(String id, FocusType type) {
    this.focus = new Focus(id, type);
  }

  /**
   * Returns the target of the focus.
   *
   * @return target
   */
  public String getFocusValue() {
    return focus.getValue();
  }

  /**
   * Checks whether blueprints are available or not.
   */
  public boolean areBlueprintsAvailable() {
    return blueprintsAvailable;
  }

  /**
   * Sets what should be the next hint message.
   *
   * @param hint the new message
   */
  public void setHint(Hints hint) {
    this.hint = hint;
  }

  /**
   * Returns the context sensitive prompt.
   *
   * @return text of the prompt
   */
  public String getPrompt() {
    return focus.isType(FocusType.ROOT) ?
      isConnectedToCluster() ? formatPrompt(focus.getPrefix(), cluster) : "ambari-shell>" :
      formatPrompt(focus.getPrefix(), focus.getValue());
  }

  public boolean isConnectedToCluster() {
    return cluster != null;
  }

  /**
   * Checks whether the focus is on the host or not.
   *
   * @return true if the focus is on a host false otherwise
   */
  public boolean isFocusOnHost() {
    return isFocusOn(FocusType.HOST);
  }

  /**
   * Checks whether the focus is on the cluster build or not.
   *
   * @return true if the focus is on a cluster build false otherwise
   */
  public boolean isFocusOnClusterBuild() {
    return isFocusOn(FocusType.CLUSTER_BUILD);
  }

  /**
   * Returns some context sensitive hint.
   *
   * @return hint
   */
  public String getHint() {
    return "Hint: " + hint.message();
  }

  /**
   * Returns the name of the cluster.
   *
   * @return cluster's name
   */
  public String getCluster() {
    return cluster;
  }

  /**
   * Sets whether there are blueprints available or not.
   *
   * @param blueprintsAvailable
   */
  public void setBlueprintsAvailable(boolean blueprintsAvailable) {
    this.blueprintsAvailable = blueprintsAvailable;
  }

  private boolean isFocusOn(FocusType type) {
    return focus.isType(type);
  }

  private Focus getRootFocus() {
    return new Focus("root", FocusType.ROOT);
  }

  private String formatPrompt(String prefix, String postfix) {
    return String.format("%s:%s>", prefix, postfix);
  }
}
