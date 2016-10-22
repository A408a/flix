/*
 *  Copyright 2016 Magnus Madsen
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package ca.uwaterloo.flix.language.phase

import ca.uwaterloo.flix.language.ast.{SourceLocation, Type}
import ca.uwaterloo.flix.language.errors.TypeError
import ca.uwaterloo.flix.util.Result
import ca.uwaterloo.flix.util.Result._
import ca.uwaterloo.flix.util.InternalCompilerException

object Unification {

  /**
    * Companion object for the [[Substitution]] class.
    */
  object Substitution {
    /**
      * Returns the empty substitution.
      */
    val empty: Substitution = Substitution(Map.empty)

    /**
      * Returns the singleton substitution mapping `x` to `tpe`.
      */
    def singleton(x: Type.Var, tpe: Type): Substitution = Substitution(Map(x -> tpe))
  }

  /**
    * A substitution is a map from type variables to types.
    */
  case class Substitution(m: Map[Type.Var, Type]) {

    /**
      * Applies `this` substitution to the given type `tpe`.
      */
    def apply(tpe: Type): Type = tpe match {
      case x: Type.Var =>
        m.get(x) match {
          case None => x
          case Some(y) if x.kind == tpe.kind => y
          case Some(y) if x.kind != tpe.kind => throw InternalCompilerException(s"Expected kind `${x.kind}' but got `${tpe.kind}'.")
        }
      case Type.Unit => Type.Unit
      case Type.Bool => Type.Bool
      case Type.Char => Type.Char
      case Type.Float32 => Type.Float32
      case Type.Float64 => Type.Float64
      case Type.Int8 => Type.Int8
      case Type.Int16 => Type.Int16
      case Type.Int32 => Type.Int32
      case Type.Int64 => Type.Int64
      case Type.BigInt => Type.BigInt
      case Type.Str => Type.Str
      case Type.Native => Type.Native
      case Type.Arrow(l) => Type.Arrow(l)
      case Type.FTuple(l) => Type.FTuple(l)
      case Type.FList => Type.FList
      case Type.FVec => Type.FVec
      case Type.FSet => Type.FSet
      case Type.FMap => Type.FMap
      case Type.Enum(name, cases, kind) => Type.Enum(name, cases.foldLeft(Map.empty[String, Type]) {
        case (macc, (tag, t)) => macc + (tag -> apply(t))
      }, kind)
      case Type.Apply(t1, t2) => Type.Apply(apply(t1), apply(t2))
    }

    /**
      * Applies `this` substitution to the given types `ts`.
      */
    def apply(ts: List[Type]): List[Type] = ts map apply

    /**
      * Returns the left-biased composition of `this` substitution with `that` substitution.
      */
    def ++(that: Substitution): Substitution = {
      Substitution(this.m ++ that.m.filter(kv => !this.m.contains(kv._1)))
    }

    /**
      * Returns the composition of `this` substitution with `that` substitution.
      */
    def @@(that: Substitution): Substitution = {
      val m = that.m.map {
        case (x, t) => x -> this.apply(t)
      }
      Substitution(m) ++ this
    }

  }

  /**
    * Returns the most general unifier of the two given types `tpe1` and `tpe2`.
    */
  def unify(tpe1: Type, tpe2: Type, loc: SourceLocation): Result[Substitution, TypeError] = {

    // NB: Uses a closure to capture the source location `loc`.

    /**
      * Unifies the given variable `x` with the given type `tpe`.
      *
      * Performs the so-called occurs-check to ensure that the substitution is kind-preserving.
      */
    def unifyVar(x: Type.Var, tpe: Type): Result[Substitution, TypeError] = {
      if (x == tpe) {
        return Result.Ok(Substitution.empty)
      }
      if (tpe.typeVars contains x) {
        return Result.Err(TypeError.OccursCheck())
      }
      // TODO: Kinds disabled for now. Requires changed to the
      // previous phase to associated type variables with their kinds.
      //if (x.kind != tpe.kind) {
      //  return Result.Err(TypeError.KindError())
      //}
      Result.Ok(Substitution.singleton(x, tpe))
    }

    /**
      * Unifies the two given types `tpe1` and `tpe2`.
      */
    def unifyTypes(tpe1: Type, tpe2: Type): Result[Substitution, TypeError] = (tpe1, tpe2) match {
      case (x: Type.Var, _) => unifyVar(x, tpe2)
      case (_, x: Type.Var) => unifyVar(x, tpe1)
      case (Type.Unit, Type.Unit) => Result.Ok(Substitution.empty)
      case (Type.Bool, Type.Bool) => Result.Ok(Substitution.empty)
      case (Type.Char, Type.Char) => Result.Ok(Substitution.empty)
      case (Type.Float32, Type.Float32) => Result.Ok(Substitution.empty)
      case (Type.Float64, Type.Float64) => Result.Ok(Substitution.empty)
      case (Type.Int8, Type.Int8) => Result.Ok(Substitution.empty)
      case (Type.Int16, Type.Int16) => Result.Ok(Substitution.empty)
      case (Type.Int32, Type.Int32) => Result.Ok(Substitution.empty)
      case (Type.Int64, Type.Int64) => Result.Ok(Substitution.empty)
      case (Type.BigInt, Type.BigInt) => Result.Ok(Substitution.empty)
      case (Type.Str, Type.Str) => Result.Ok(Substitution.empty)
      case (Type.Native, Type.Native) => Result.Ok(Substitution.empty)
      case (Type.Arrow(l1), Type.Arrow(l2)) if l1 == l2 => Result.Ok(Substitution.empty)
      case (Type.FTuple(l1), Type.FTuple(l2)) if l1 == l2 => Result.Ok(Substitution.empty)
      case (Type.FList, Type.FList) => Result.Ok(Substitution.empty)
      case (Type.FVec, Type.FVec) => Result.Ok(Substitution.empty)
      case (Type.FSet, Type.FSet) => Result.Ok(Substitution.empty)
      case (Type.FMap, Type.FMap) => Result.Ok(Substitution.empty)
      case (Type.Enum(name1, cases1, kind1), Type.Enum(name2, cases2, kind2)) if name1 == name2 =>
        val ts1 = cases1.values.toList
        val ts2 = cases2.values.toList
        unifyAll(ts1, ts2)
      case (Type.Apply(t1, ts1), Type.Apply(t2, ts2)) =>
        unifyTypes(t1, t2) match {
          case Result.Ok(subst1) => unifyAll(subst1(ts1), subst1(ts2)) match {
            case Result.Ok(subst2) => Result.Ok(subst2 @@ subst1)
            case Result.Err(e) => Result.Err(e)
          }
          case Result.Err(e) => Result.Err(e)
        }
      case _ => Result.Err(TypeError.UnificationError(tpe1, tpe2, loc))
    }

    /**
      * Unifies the two given lists of types `ts1` and `ts2`.
      */
    def unifyAll(ts1: List[Type], ts2: List[Type]): Result[Substitution, TypeError] = (ts1, ts2) match {
      case (Nil, Nil) => Result.Ok(Substitution.empty)
      case (t1 :: rs1, t2 :: rs2) => unifyTypes(t1, t2) match {
        case Result.Ok(subst1) => unifyAll(subst1(rs1), subst1(rs2)) match {
          case Result.Ok(subst2) => Result.Ok(subst2 @@ subst1)
          case Result.Err(e) => Result.Err(e)
        }
        case Result.Err(e) => Result.Err(e)
      }
      case _ => throw InternalCompilerException(s"Mismatched type lists: `$ts1' and `$ts2'.")
    }

    unifyTypes(tpe1, tpe2)
  }

  /**
    * A type inference state monad that maintains the current substitution.
    */
  case class InferMonad[A](run: Substitution => Result[(Substitution, A), TypeError]) {
    /**
      * Applies the given function `f` to the value in the monad.
      */
    def map[B](f: A => B): InferMonad[B] = {
      def runNext(s0: Substitution): Result[(Substitution, B), TypeError] = {
        // Run the original function and map over its result (since it may have error'd).
        run(s0) map {
          case (s, a) => (s, f(a))
        }
      }
      InferMonad(runNext)
    }

    /**
      * Applies the given function `f` to the value in the monad.
      */
    def flatMap[B](f: A => InferMonad[B]): InferMonad[B] = {
      def runNext(s0: Substitution): Result[(Substitution, B), TypeError] = {
        // Run the original function and flatMap over its result (since it may have error'd).
        run(s0) flatMap {
          case (s, a) => f(a) match {
            // Unwrap the returned monad and apply the inner function g.
            case InferMonad(g) => g(s)
          }
        }
      }
      InferMonad(runNext)
    }
  }

  /**
    * Lifts the given value `a` into the type inference monad
    */
  def liftM[A](a: A): InferMonad[A] = InferMonad(s => Ok((s, a)))

  /**
    * Lifts the given value `a` and substitution `s` into the type inference monad..
    */
  def liftM[A](a: A, s: Substitution): InferMonad[A] = InferMonad(_ => Ok(s, a))

  /**
    * Lifts the given error `e` into the type inference monad.
    */
  def failM[A](e: TypeError): InferMonad[A] = InferMonad(_ => Err(e))

  /**
    * Unifies the two given types `tpe1` and `tpe2` lifting their unified types and
    * associated substitution into the type inference monad.
    */
  def unifyM(tpe1: Type, tpe2: Type, loc: SourceLocation): InferMonad[Type] = {
    InferMonad((s: Substitution) =>
      unify(s(tpe1), s(tpe2), loc) match {
        case Result.Ok(s1) =>
          val subst = s1 @@ s
          Ok(subst, subst(tpe1))
        case Result.Err(e) => Err(e)
      }
    )
  }

  /**
    * Unifies the three given types `tpe1`, `tpe2`, and `tpe3`.
    */
  def unifyM(tpe1: Type, tpe2: Type, tpe3: Type, loc: SourceLocation): InferMonad[Type] = unifyM(List(tpe1, tpe2, tpe3), loc)

  /**
    * Unifies the four given types `tpe1`, `tpe2`, `tpe3` and `tpe4`.
    */
  def unifyM(tpe1: Type, tpe2: Type, tpe3: Type, tpe4: Type, loc: SourceLocation): InferMonad[Type] = unifyM(List(tpe1, tpe2, tpe3, tpe4), loc)

  /**
    * Unifies all the types in the given non-empty list `ts`.
    */
  def unifyM(ts: List[Type], loc: SourceLocation): InferMonad[Type] = {
    assert(ts.nonEmpty)
    def visit(x0: InferMonad[Type], xs: List[Type]): InferMonad[Type] = xs match {
      case Nil => x0
      case y :: ys => x0 flatMap {
        case tpe => visit(unifyM(tpe, y, loc), ys)
      }
    }
    visit(liftM(ts.head), ts.tail)
  }

  /**
    * Pairwise unifies the two given lists of types `xs` and `ys`.
    */
  def unifyM(xs: List[Type], ys: List[Type], loc: SourceLocation): InferMonad[List[Type]] = seqM((xs zip ys).map {
    case (x, y) => unifyM(x, y, loc)
  })

  /**
    * Collects the result of each type inference monad in `ts` going left to right.
    */
  def seqM[A](xs: List[InferMonad[A]]): InferMonad[List[A]] = xs match {
    case Nil => liftM(Nil)
    case y :: ys => y flatMap {
      case r => seqM(ys) map {
        case rs => r :: rs
      }
    }
  }

}
