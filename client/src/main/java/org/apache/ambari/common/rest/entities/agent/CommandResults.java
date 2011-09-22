package org.apache.ambari.common.rest.entities.agent;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * 
 * Data model for describing commands to be executed on Ambari Agent.
 *
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {})
public class CommandResults {

  public CommandResults() {
  }
  
  @XmlElement
  public List<CommandResult> commandResults;
  
  public List<CommandResult>getCommandResults() {
    return commandResults;
  }
  
  public void setCommandResults(List<CommandResult> commandResults) {
    this.commandResults = commandResults;
  }
  
  public void add(CommandResult commandResult) {
    if(this.commandResults == null) {
      this.commandResults = new ArrayList<CommandResult>();
    }
    commandResults.add(commandResult);
  }
  
  public void add(String cmdId, int exitCode, String stdout, String stderr) {
    if(this.commandResults == null) {
      this.commandResults = new ArrayList<CommandResult>();
    }
    commandResults.add(new CommandResult(cmdId, exitCode, stdout, stderr));
  }

}
