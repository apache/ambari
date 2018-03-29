package org.apache.ambari.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The {@link SwaggerPreferredParent} is used to add information
 * to help {@link org.apache.ambari.swagger.AmbariSwaggerReader} decide how
 * to handle nested API resources which have multiple top level API parents.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SwaggerPreferredParent {

    /**
     * Class name of preferred parent object
     * @return
     */
    Class preferredParent();
}
