/**
  * Licensed to the Apache Software Foundation (ASF) under one
  * or more contributor license agreements.  See the NOTICE file
  * distributed with this work for additional information
  * regarding copyright ownership.  The ASF licenses this file
  * to you under the Apache License, Version 2.0 (the
  * "License"); you may not use this file except in compliance
  * with the License.  You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */

package org.apache.ambari.metrics.adservice.app

import com.fasterxml.jackson.module.scala._
import com.fasterxml.jackson.module.scala.deser.{ScalaNumberDeserializersModule, UntypedObjectDeserializerModule}
import com.fasterxml.jackson.module.scala.introspect.{ScalaAnnotationIntrospector, ScalaAnnotationIntrospectorModule}

/**
  * Extended Jackson Module that fixes the Scala-Jackson BytecodeReadingParanamer issue.
  */
class ADServiceScalaModule extends JacksonModule
  with IteratorModule
  with EnumerationModule
  with OptionModule
  with SeqModule
  with IterableModule
  with TupleModule
  with MapModule
  with SetModule
  with FixedScalaAnnotationIntrospectorModule
  with UntypedObjectDeserializerModule
  with EitherModule {

  override def getModuleName = "ADServiceScalaModule"

  object ADServiceScalaModule extends ADServiceScalaModule

}


trait FixedScalaAnnotationIntrospectorModule extends JacksonModule {
  this += { _.appendAnnotationIntrospector(ScalaAnnotationIntrospector) }
}
