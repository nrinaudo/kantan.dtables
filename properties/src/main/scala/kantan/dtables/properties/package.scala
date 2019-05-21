/*
 * Copyright 2019 Nicolas Rinaudo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kantan.dtables

import kantan.codecs._
import org.scalacheck.Gen
import org.scalacheck.Prop
import org.scalacheck.Prop._

package object properties {
  type EntryDecoder[A]      = Decoder[String, A, DecodeError, codecs.type]
  type ConditionsDecoder[A] = Decoder[Conditions, Gen[A], DecodeError, codecs.type]
  type OutcomeDecoder[A]    = Decoder[Outcome, A, DecodeError, codecs.type]

  type DecodeResult[A] = Either[DecodeError, A]

  /** Turns the specified rule into a human readable label. */
  private def label(rule: Rule): String = {
    def printEntries[A](entries: Map[String, A])(f: A => String) =
      entries.map { case (name, value) => s"$name: ${f(value)}" }.mkString("{", ", ", "}")

    printEntries(rule.conditions) {
      case ConditionValue.Wildcard => "-"
      case ConditionValue.Point(p) => p
    } +
      " => " +
      printEntries(rule.outcome) {
        case OutcomeValue.Undefined => "???"
        case OutcomeValue.Point(p)  => p
      }
  }

  def create[C, O](
    specs: Specification
  )(
    f: (C, O) => Boolean
  )(implicit cd: ConditionsDecoder[C], od: OutcomeDecoder[O]): Seq[DecodeResult[(String, Prop)]] =
    specs.runnableRules.map { rule =>
      for {
        condition <- cd.decode(rule.conditions)
        outcome   <- od.decode(rule.outcome)
      } yield label(rule) -> forAll(condition) { testCase =>
        f(testCase, outcome)
      }
    }
}
