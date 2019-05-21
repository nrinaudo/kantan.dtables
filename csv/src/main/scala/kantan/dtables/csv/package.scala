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

import kantan.codecs.resource._
import kantan.csv._
import kantan.csv.ops._

package object csv {

  // - CSV reading ------------------------------------------------------------------------------------------------------
  // --------------------------------------------------------------------------------------------------------------------
  // Drops the separator column - the first one of the second list of values.
  private def dropSeparator(ss: (Seq[String], Seq[String])) = (ss._1, ss._2.drop(1))

  private def parseHeader(h: Seq[String]): Map[Int, String] = h.zipWithIndex.map(_.swap).toMap

  private def toEntry[A](value: Seq[String], header: Map[Int, String])(f: String => A): Map[String, A] =
    value.zipWithIndex.map {
      case (value, index) => header(index) -> f(value)
    }.toMap

  // TODO: error handling, this is far too naive.
  implicit val ruleHeaderDecoder: HeaderDecoder[Rule] = new HeaderDecoder[Rule] {
    override def fromHeader(header: Seq[String]): DecodeResult[RowDecoder[Rule]] = {
      val (condEntry, outcomeEntry) = dropSeparator(header.span(_.nonEmpty))

      val condHeader    = parseHeader(condEntry)
      val outcomeHeader = parseHeader(outcomeEntry)

      DecodeResult.success(RowDecoder.from { row =>
        val (condition, outcome) = dropSeparator(row.splitAt(condEntry.size))

        DecodeResult.success(
          Rule(
            toEntry(condition, condHeader) {
              case "-"   => ConditionValue.Wildcard
              case value => ConditionValue.Point(value)
            },
            toEntry(outcome, outcomeHeader) {
              case "???" => OutcomeValue.Undefined
              case value => OutcomeValue.Point(value)
            }
          )
        )
      })
    }

    override def noHeader: RowDecoder[Rule] = sys.error("A header is required.")
  }

  def load[A: InputResource](input: A): ReadResult[Specification] =
    ReadResult
      .sequence(input.readCsv[Seq, Rule](rfc.withHeader))
      .map(rules => {
        rules.headOption match {
          case None => Specification(Seq.empty, Seq.empty, rules)
          case Some(Rule(cond, out)) =>
            val conditions = cond.keys.map(key => key -> Set.newBuilder[String]).toMap
            val outcome    = out.keys.map(key => key  -> Set.newBuilder[String]).toMap

            rules.foreach {
              case Rule(cond, out) =>
                cond.collect {
                  case (name, ConditionValue.Point(value)) => conditions(name) += value
                }

                out.collect {
                  case (name, OutcomeValue.Point(value)) => outcome(name) += value
                }
            }

            Specification(
              conditions.map { case (name, values) => Entry(name, values.result()) }.toSeq,
              outcome.map { case (name, values)    => Entry(name, values.result()) }.toSeq,
              rules
            )
        }
      })

  // - CSV writing ------------------------------------------------------------------------------------------------------
  // --------------------------------------------------------------------------------------------------------------------
  def write[A: OutputResource](output: A, specs: Specification): Unit = {
    implicit val inputEncoder: CellEncoder[ConditionValue] = CellEncoder.from {
      case ConditionValue.Wildcard     => "-"
      case ConditionValue.Point(value) => value
    }

    implicit val outputEncoder: CellEncoder[OutcomeValue] = CellEncoder.from {
      case OutcomeValue.Undefined    => "???"
      case OutcomeValue.Point(value) => value
    }

    implicit val encoder: HeaderEncoder[Rule] = new HeaderEncoder[Rule] {
      val inEntry  = specs.conditions.map(_.name)
      val outEntry = specs.outcome.map(_.name)

      override def header: Option[Seq[String]] = Some(inEntry ++ Seq("") ++ outEntry)
      override def rowEncoder: RowEncoder[Rule] = RowEncoder.from {
        case Rule(conditions, outcome) =>
          inEntry.map(name => conditions(name)).asCsvRow ++ Seq("") ++ outEntry
            .map(name => outcome(name))
            .asCsvRow

      }
    }

    output.writeCsv(specs.rules, rfc.withHeader)
  }
}
