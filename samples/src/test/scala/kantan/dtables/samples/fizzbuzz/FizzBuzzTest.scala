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

import annotation.tailrec
import kantan.dtables.properties._
import kantan.dtables.scalatest._
import org.scalacheck._
import org.scalatest._

/** Actual FizzBuzz test. */
class FizzBuzzTest extends FunSuite with DTables {
  // - Simple FizzBuzz implementation ----------------------------------------------------------------------------------
  // -------------------------------------------------------------------------------------------------------------------
  val sut: Int => String = i =>
    if(i % 3 == 0) {
      if(i % 5 == 0) "FizzBuzz"
      else "Fizz"
    }
    else if(i % 5 == 0) "Buzz"
    else i.toString

  // - Decision table decoding -----------------------------------------------------------------------------------------
  // -------------------------------------------------------------------------------------------------------------------

  // Divides `i` by `div` until the result is no longer divisible by `div`.
  // This is necessary to make sure that "seed" values aren't divisible by 3 or 5.
  @tailrec
  private def nonDivisible(i: Int, div: Int): Int =
    if(i % div == 0) nonDivisible(i / div, div)
    else i

  // Decodes a set of conditions into a Gen[Int] whose output map these conditions.
  implicit val genDecoder: ConditionsDecoder[Int] = ConditionsDecoder.decoder("n % 3", "n % 5") {
    (mod3: Boolean, mod5: Boolean) =>
      // Generates integers that are:
      // - divisible by 3 if mod3 is true
      // - divisible by 5 is mod5 is true
      Gen.choose(1, 100).map { i =>
        val seed = nonDivisible(nonDivisible(i, 3), 5)

        if(mod3) {
          if(mod5) seed * 3 * 5
          else seed * 3
        }
        else if(mod5) seed * 5
        else seed
      }
  }

  // Decodes outcomes into expected behaviours.
  implicit val outcomeDecoder: OutcomeDecoder[Int => String] = OutcomeDecoder.fromEntry[String]("Output").emap {
    case "N"                                => DecodeResult.success(_.toString)
    case s @ ("Fizz" | "Buzz" | "FizzBuzz") => DecodeResult.success(_ => s)
    case s                                  => DecodeResult.error(s"Not a valid fizzbuzz output: $s")
  }

  // - Actual test -----------------------------------------------------------------------------------------------------
  // -------------------------------------------------------------------------------------------------------------------

  // Loads the fizzbuzz specs.
  val spec = csv.load(getClass.getResource("/fizzbuzz.csv")).getOrElse(sys.error("Failed to load fizzbuz specs"))

  // For all test cases, makes sure output matches expectations.
  checkAll("FizzBuzz", spec) { (input: Int, expected: Int => String) =>
    sut(input) == expected(input)
  }
}
