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
package properties

import kantan.codecs.DecoderCompanion
import kantan.codecs.strings.StringDecoder

object EntryDecoder extends GeneratedOutcomeDecoders with DecoderCompanion[String, DecodeError, codecs.type]

trait EntryDecoderInstances {
  implicit val boolDecoder: EntryDecoder[Boolean] = EntryDecoder.fromPartial {
    case "T" => DecodeResult.success(true)
    case "F" => DecodeResult.success(false)
  }

  implicit def fromStringDecoder[A: StringDecoder]: EntryDecoder[A] =
    StringDecoder[A].tag[codecs.type].leftMap(e => DecodeError(e.getMessage, e.getCause))

}
