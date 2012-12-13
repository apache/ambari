package org.apache.ambari.server.controller.predicate;

import org.apache.ambari.server.controller.spi.Resource;

import java.util.Collections;
import java.util.Set;

/**
 * A predicate that always evaluates to true.
 */
public class AlwaysPredicate implements BasePredicate {
  public static final AlwaysPredicate INSTANCE = new AlwaysPredicate();

  @Override
  public boolean evaluate(Resource resource) {
    return true;
  }

  @Override
  public Set<String> getPropertyIds() {
    return Collections.emptySet();
  }

  @Override
  public void accept(PredicateVisitor visitor) {
    visitor.acceptAlwaysPredicate(this);
  }
}
