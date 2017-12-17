package com.jacoby6000.cloneherodb.parsing.ini

import scalaz._
import Scalaz._
import fastparse.all._

import scalaz.Maybe.Empty

object parser {
  case class INIKey(value: String) extends AnyVal
  object INIKey {
    implicit val order: Order[INIKey] =
      Order[String].contramap[INIKey](_.value)
  }

  case class INIValue(value: String) { def widen: INIValue = this }
  object INIValue {
    implicit val iniValueSemigroup: Semigroup[INIValue] =
      Semigroup.instance[INIValue]((_, v) => v)
  }

  case class INISectionName(value: String) extends AnyVal
  object INISectionName {
    implicit val order: Order[INISectionName] =
      Order[String].contramap[INISectionName](_.value)
  }

  type INIKVPair = (INIKey, INIValue)

  type INIFile = IMap[Maybe[INISectionName], IMap[INIKey, INIValue]]
  object INIFile {
    val empty: INIFile = IMap(Empty[INISectionName]() -> IMap.empty[INIKey, INIValue])
    def apply(pairs: (Maybe[INISectionName], IMap[INIKey, INIValue])*) = empty |+| IMap(pairs: _*)
  }

  implicit class INIFileOps(val iniFile: INIFile) extends AnyVal {
    def insertValue(section: Maybe[INISectionName], kvPair: INIKVPair): ValidationNel[ParseError, INIFile] =
      iniFile.lookup(section).flatMap(_.lookup(kvPair._1)).toMaybe.cata(
        found => DuplicateKey(section, kvPair._1, found, kvPair._2).widen.failureNel,
        iniFile.update(section, _.insert(kvPair._1, kvPair._2).some).successNel
      )

    def insertNewSection(section: Maybe[INISectionName]): ValidationNel[ParseError, INIFile] =
      iniFile.lookup(section).cata(
        _ => DuplicateSectionName(section).widen.failureNel,
        iniFile.insert(section, IMap.empty).successNel
      )
  }

  sealed trait ParseError { def widen: ParseError = this }
  case class DuplicateSectionName(section: Maybe[INISectionName]) extends ParseError
  case class DuplicateKey(section: Maybe[INISectionName], key: INIKey, existingValue: INIValue, newValue: INIValue) extends ParseError
  case class MalformedConfigLine(lineNumber: Int, contents: String, iniSectionName: Maybe[INISectionName]) extends ParseError

  val alphaNumDash = CharIn(('a' to 'z') ++ ('A' to 'Z') ++ ('0' to '9') ++ List('-', '_', '.'))

  val section: P[INISectionName] = P("[" ~ alphaNumDash.rep.! ~ "]" ~ End).map(INISectionName(_))
  val key: P[INIKey] = P(Start ~ alphaNumDash.rep.!)map(INIKey(_))
  val value: P[INIValue] = P(AnyChar.rep.! ~ End).map(INIValue(_).widen)

  val kvPair: P[INIKVPair] = P(key ~ "=" ~ value)

  val lineParser: P[INIKVPair \/ INISectionName] = P(kvPair.map(_.left) | section.map(_.right))

  type ParseResult = ValidationNel[ParseError, INIFile]

  def parse(s: String): ValidationNel[ParseError, INIFile] = {

    s.lines.zipWithIndex.foldLeft((Maybe.empty[INISectionName], INIFile.empty.successNel[ParseError])) {
      case (acc, (line, _)) if line.trim.isEmpty => acc
      case ((currentSection, parsed), (line, lineNumber)) =>

        def handleParseError: ValidationNel[ParseError, INIFile] =
          MalformedConfigLine(lineNumber + 1, line, currentSection).widen.failureNel[INIFile]

        lineParser.parse(line.trim).fold(
          (_, _, _) => (currentSection, parsed |+| handleParseError),
          (result, _) => result.fold(
            newPair => (currentSection, parsed.traverse(_.insertValue(currentSection, newPair)).fold(_.failure, identity)),
            newSection => (newSection.just, parsed.traverse(_.insertNewSection(newSection.just)).fold(_.failure, identity))
          )
        )
    }._2
  }


}
