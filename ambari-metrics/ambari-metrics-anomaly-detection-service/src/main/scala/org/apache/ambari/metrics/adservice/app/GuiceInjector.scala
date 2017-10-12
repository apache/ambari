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

import java.lang.annotation.Annotation

import com.google.inject.{Guice, Injector, Module, TypeLiteral}

import scala.collection.JavaConversions._
import scala.language.implicitConversions
import scala.reflect._

object GuiceInjector {

  def withInjector(modules: Module*)(fn: (Injector) => Unit) = {
    val injector = Guice.createInjector(modules.toList: _*)
    fn(injector)
  }

  implicit def wrap(injector: Injector): InjectorWrapper = new InjectorWrapper(injector)
}

class InjectorWrapper(injector: Injector) {
  def instancesWithAnnotation[T <: Annotation](annotationClass: Class[T]): List[AnyRef] = {
    injector.getAllBindings.filter { case (k, v) =>
      !k.getTypeLiteral.getRawType.getAnnotationsByType[T](annotationClass).isEmpty
    }.map { case (k, v) => injector.getInstance(k).asInstanceOf[AnyRef] }.toList
  }

  def instancesOfType[T: ClassTag](typeClass: Class[T]): List[T] = {
    injector.findBindingsByType(TypeLiteral.get(classTag[T].runtimeClass)).map { b =>
      injector.getInstance(b.getKey).asInstanceOf[T]
    }.toList
  }

  def dumpBindings(): Unit = {
    injector.getBindings.keySet() foreach { k =>
      println(s"bind key = ${k.toString}")
    }
  }
}