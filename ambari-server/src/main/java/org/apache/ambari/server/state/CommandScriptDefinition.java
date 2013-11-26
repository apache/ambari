package org.apache.ambari.server.state;


import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;


/**
 * Represents info about command script
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class CommandScriptDefinition {


  private String script = null;


  private Type scriptType = Type.PYTHON;

  /**
   * Timeout is given in seconds
   */
  private int timeout = 600;


  public String getScript() {
    return script;
  }

  public Type getScriptType() {
    return scriptType;
  }

  public int getTimeout() {
    return timeout;
  }

  public static enum Type {
    PYTHON,

    PUPPET // TODO: Not supported yet. Do we really need it?
  }

}
