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

final case class Specification(conditions: Seq[Entry], outcome: Seq[Entry], rules: Seq[Rule]) {
  def runnableRules: Seq[Rule] = rules.filter(_.outcome.values.exists(_ != OutcomeValue.Undefined))
}

final case class Entry(name: String, typedAs: Set[String])

final case class Rule(conditions: Conditions, outcome: Outcome)

sealed abstract class ConditionValue extends Product with Serializable
object ConditionValue {
  final case object Wildcard            extends ConditionValue
  final case class Point(value: String) extends ConditionValue
}

sealed abstract class OutcomeValue extends Product with Serializable
object OutcomeValue {
  final case object Undefined           extends OutcomeValue
  final case class Point(value: String) extends OutcomeValue
}
