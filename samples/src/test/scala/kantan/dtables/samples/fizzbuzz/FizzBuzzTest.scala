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
package samples
package fizzbuzz

import kantan.dtables.properties._
import kantan.dtables.scalatest._
import org.scalacheck._
import org.scalatest._

/** Enumerates all possible output behaviours. */
sealed abstract class FizzBuzzOutput extends Product with Serializable
object FizzBuzzOutput {
  case object Input    extends FizzBuzzOutput
  case object Fizz     extends FizzBuzzOutput
  case object Buzz     extends FizzBuzzOutput
  case object FizzBuzz extends FizzBuzzOutput

  implicit val outputDecoder: EntryDecoder[FizzBuzzOutput] = EntryDecoder.fromUnsafe {
    case "N"        => FizzBuzzOutput.Input
    case "Fizz"     => FizzBuzzOutput.Fizz
    case "Buzz"     => FizzBuzzOutput.Buzz
    case "FizzBuzz" => FizzBuzzOutput.FizzBuzz
  }
}

/** System under test: FizzBuzz implementation. */
object FizzBuzz extends Function1[Int, String] {
  override def apply(i: Int): String =
    if(i % 3 == 0) {
      if(i % 5 == 0) "FizzBuzz"
      else "Fizz"
    }
    else if(i % 5 == 0) "Buzz"
    else i.toString
}

/** Actual FizzBuzz test. */
class FizzBuzzTest extends FunSuite with DTables {
  // Decodes a set of conditions into a Gen[Int] whose output map these conditions.
  implicit val genDecoder: ConditionsDecoder[Int] = ConditionsDecoder.decoder("n % 3", "n % 5") {
    (mod3: Boolean, mod5: Boolean) =>
      // Divides `i` by `div` until the result is no longer divisible by `div`.
      // This is necessary to make sure that "seed" values aren't divisible by 3 or 5.
      def simplify(i: Int, div: Int): Int =
        if(i % div == 0) simplify(i / div, div)
        else i

      // Generates integers that are:
      // - divisible by 3 if mod3 is true
      // - divisible by 5 is mod5 is true
      Gen.choose(1, 100).map { i =>
        val seed = simplify(simplify(i, 3), 5)

        if(mod3) {
          if(mod5) seed * 3 * 5
          else seed * 3
        }
        else if(mod5) seed * 5
        else seed
      }
  }

  // Decodes outcomes into expected behaviours.
  implicit val outcomeDecoder: OutcomeDecoder[FizzBuzzOutput] = OutcomeDecoder.fromEntry("Output")

  // Loads the fizzbuzz specs.
  val spec = csv.load(getClass.getResource("/fizzbuzz.csv")).getOrElse(sys.error("Failed to load fizzbuz specs"))

  // For all test cases, makes sure output matches expectations.
  checkAll("FizzBuzz", spec) { (input: Int, expected: FizzBuzzOutput) =>
    val result = FizzBuzz(input)

    expected match {
      case FizzBuzzOutput.Fizz     => result == "Fizz"
      case FizzBuzzOutput.FizzBuzz => result == "FizzBuzz"
      case FizzBuzzOutput.Buzz     => result == "Buzz"
      case FizzBuzzOutput.Input    => result == input.toString
    }
  }
}
