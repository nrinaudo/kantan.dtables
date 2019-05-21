# kantan.dtables

## Motivation

Specifying and testing software are two activities I'm not particularly fond of, so of course I had to start a project used to do both.

The goal of kantan.dtables is to define an AST for decision tables, and to generate test cases from this AST.

Take, for example, this decision table that specs out fizzbuzz:

| n % 3  | n % 5 |   Output |
| ------ | ----- | -------- |
|   T    |   T   | FizzBuzz |
|   T    |   F   | Fizz     |
|   F    |   T   | Buzz     |
|   F    |   F   | input    |

The idea is that this table should be able to generate the following properties:
- {x % 3 = 0 ^ x % 5 = 0} y := fizzBuzz(x) {y = FizzBuzz}
- {x % 3 = 0 ^ x % 5 ≠ 0} y := fizzBuzz(x) {y = Fizz}
- {x % 3 ≠ 0 ^ x % 5 = 0} y := fizzBuzz(x) {y = Buzz}
- {x % 3 ≠ 0 ^ x % 5 ≠ 0} y := fizzBuzz(x) {y = x}

## AST
The root of the AST is `Specifications`. It's pretty straightforward, with one currently unnecessary flourish: entries are typed (where a type is represented as a set of legal string values), which is not yet used.

## CSV format

The current implementation uses CSV as a serialization format, because, well, I wrote a CSV serder library.

All decision tables expect a header containing exactly one empty cell, used to separate conditions from outcome. Heres how the FizzBuzz table would look:

```
n % 3,n % 5,,Output
T,T,,FizzBuzz
T,F,,Fizz
F,T,,Buzz
F,F,,N
```

Two special values are allowed:
- in conditions, `-` is interpreted as `any legal value`.
- in the outcome, `???` is interpreted as `undefined`, and used to flag rules that still need to be specced out.

## Properties
In order to generate scalacheck properties from an AST, one must provide:
- a `ConditionDecoder`, which maps a condition to a `Gen[A]` where `A` is the type describing a test case.
- an `OutcomeDecoder`, which maps an outcome to an expected output.
- a function that, given a test case and an expected output, returns `true` if the SUT returned the right value.

See the `samples` project for a concrete example of the FizzBuzz algorithm

## Where to from here?

This is an early draft, undocumented and untested. At this point, I intend to write as many specifications as possible to make sure the AST is reasonable and the API flexible enough for most use cases.

A few critical features are also missing:
- find ambiguous rules (same conditions yield different outcomes)
- find irrelevant conditions (conditions whose values don't actually impact outcomes)
- find missing rules (set of conditions that don't map to an outcome)
- fold identical rules together - typically when a condition has no impact and can be turned into a `-`

I'd also like to play with pretty printing decision tables to different formats, such as (gasp) JIRA markdown, to have specifications and tests both be generated from the same data.

## How can you help?

Assuming you'd want to help, here are the things I really need:
- specs and their implementations, or why they couldn't be implemented, to stress the API out
- expertise and best practices when designing decision tables. I know decision tables, but am no expert
- crazy ideas - I wasted a few hours on the connection between decision tables and Hoare logic and while that didn't go anywhere, it could have!
