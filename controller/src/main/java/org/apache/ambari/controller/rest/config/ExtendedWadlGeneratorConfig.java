package org.apache.ambari.controller.rest.config;

import java.util.List;

import com.sun.jersey.api.wadl.config.WadlGeneratorConfig;
import com.sun.jersey.api.wadl.config.WadlGeneratorDescription;
import com.sun.jersey.server.wadl.generators.WadlGeneratorGrammarsSupport;
import com.sun.jersey.server.wadl.generators.resourcedoc.WadlGeneratorResourceDocSupport;

public class ExtendedWadlGeneratorConfig extends WadlGeneratorConfig {

  @Override
  public List<WadlGeneratorDescription> configure() {
    return generator(WadlGeneratorGrammarsSupport.class)
        .prop("grammarsStream", "application-grammars.xml")
        //.generator(WadlGeneratorResourceDocSupport.class)
        //.prop("resourceDocStream", "resourcedoc.xml")
        .descriptions();
    }
}
