/*
 * Copyright 2015-2016 Magnus Madsen, Ming-Ho Yee
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

package ca.uwaterloo.flix.api

import ca.uwaterloo.flix.language.ast.Type

final class WrappedType(val tpe: Type) extends IType {

  def isUnit: Boolean =
    tpe == Type.Unit

  def isBool: Boolean =
    tpe == Type.Bool

  def isChar: Boolean =
    tpe == Type.Char

  def isFloat32: Boolean =
    tpe == Type.Float32

  def isFloat64: Boolean =
    tpe == Type.Float64

  def isInt8: Boolean =
    tpe == Type.Int8

  def isInt16: Boolean =
    tpe == Type.Int16

  def isInt32: Boolean =
    tpe == Type.Int32

  def isInt64: Boolean =
    tpe == Type.Int64

  def isBigInt: Boolean =
    tpe == Type.BigInt

  def isStr: Boolean =
    tpe == Type.Str

  def isEnum: Boolean = tpe match {
    case Type.Enum(name, kind) => true
    case _ => false
  }

  def isFunction: Boolean = tpe match {
    case Type.Apply(Type.Arrow(l), _) => true
    case _ => false
  }

  def isTuple: Boolean = tpe match {
    case Type.Apply(Type.FTuple(l), _) => true
    case _ => false
  }

  def isSet: Boolean = tpe match {
    case Type.Apply(Type.FSet, _) => true
    case _ => false
  }

  def isMap: Boolean = tpe match {
    case Type.Apply(Type.Apply(Type.FMap, _), _) => true
    case _ => false
  }

  def isNative: Boolean = tpe match {
    case Type.Native => true
    case _ => false
  }

  def getTupleParams: Array[IType] = tpe match {
    case Type.Apply(Type.FTuple(l), elms) => elms.map(t => new WrappedType(t)).toArray
    case _ => throw new UnsupportedOperationException(s"Unexpected type: '$tpe'.")
  }

  def getSetParam: IType = tpe match {
    case Type.Apply(Type.FSet, List(elm)) => new WrappedType(elm)
    case _ => throw new UnsupportedOperationException(s"Unexpected type: '$tpe'.")
  }

  def getMapKeyParam: IType = tpe match {
    case Type.Apply(Type.FMap, List(k, v)) => new WrappedType(k)
    case _ => throw new UnsupportedOperationException(s"Unexpected type: '$tpe'.")
  }

  def getMapValueParam: IType = tpe match {
    case Type.Apply(Type.FMap, List(k, v)) => new WrappedType(v)
    case _ => throw new UnsupportedOperationException(s"Unexpected type: '$tpe'.")
  }

  override def equals(other: Any): Boolean = other match {
    case that: WrappedType => tpe == that.tpe
    case _ => false
  }

  override def hashCode(): Int = tpe.hashCode()
}