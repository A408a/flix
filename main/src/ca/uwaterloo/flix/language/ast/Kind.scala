/*
 * Copyright 2016 Magnus Madsen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ca.uwaterloo.flix.language.ast

/**
  * A kind represents the "type" of a type expression.
  */
trait Kind {

  override def toString: String = this match {
    case Kind.Star => "*"
    case Kind.Arrow(List(Kind.Star), Kind.Star) => "* -> *"
    case Kind.Arrow(List(Kind.Star), kr) => s"* -> ($kr)"
    case Kind.Arrow(kparams, Kind.Star) => s"(${kparams.mkString(", ")}) -> *"
    case Kind.Arrow(kparams, kr) => s"(${kparams.mkString(", ")}) -> ($kr)"
  }

}

object Kind {

  /**
    * The kind of all nullary type expressions.
    */
  object Star extends Kind

  /**
    * The kind of type expressions that take a sequence of kinds `kparams` to a kind `kr`.
    */
  case class Arrow(kparams: List[Kind], kr: Kind) extends Kind {
    assert(kparams.nonEmpty)
  }

}
