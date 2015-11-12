package org.apache.ambari.server.state.stack.upgrade;

  import javax.xml.bind.annotation.XmlAccessType;
  import javax.xml.bind.annotation.XmlAccessorType;
  import javax.xml.bind.annotation.XmlRootElement;
  import javax.xml.bind.annotation.XmlTransient;
  import javax.xml.bind.annotation.XmlType;

/**
 * Used to represent Configuring of a component.
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name="configure_function")
public class ConfigureFunction extends Task {

  @XmlTransient
  private Task.Type type = Type.CONFIGURE_FUNCTION;

  public static final String actionVerb = "Configuring";

  @Override
  public Task.Type getType() {
    return type;
  }

  @Override
  public StageWrapper.Type getStageWrapperType() {
    return StageWrapper.Type.CONFIGURE;
  }

  @Override
  public String getActionVerb() {
    return actionVerb;
  }
}
