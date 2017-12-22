package com.jacoby6000.cloneherodb.parsing.ini

import scalaz._
import Scalaz._
import fastparse.all._

import scala.Predef.{Set, $conforms}
import scala.collection.immutable.List

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

  val alphaNumDash = CharsWhileIn(('a' to 'z') ++ ('A' to 'Z') ++ ('0' to '9') ++ List('-', '_', '.'))

  val ignoreChars = Set('"')

  val ignore = P(CharsWhileIn('_' :: ignoreChars.toList, 0))

  val section: P[INISectionName] = P("[" ~ alphaNumDash.! ~ "]" ~ End).map(INISectionName(_))
  val key: P[INIKey] = P(alphaNumDash.!)map(INIKey(_))
  val value: P[INIValue] = P(CharsWhile(x => !ignoreChars(x)).!).map(INIValue(_))

  val kvPair: P[INIKVPair] = P(key ~ ignore ~ "=" ~ ignore ~ value)

  val lineParser: P[INIKVPair \/ INISectionName] =
    P(Start ~ ignore ~ (kvPair.map(_.left) | section.map(_.right)) ~ ignore ~ End)

  type ParseResult = ValidationNel[ParseError, INIFile]

  def parse(s: String): ValidationNel[ParseError, INIFile] = {

    s.lines.zipWithIndex.foldLeft((Maybe.empty[INISectionName], INIFile.empty.successNel[ParseError])) {
      case (acc, (line, _)) if line.trim.isEmpty || line.startsWith(";") => acc
      case ((currentSection, parsed), (line, lineNumber)) =>

        def handleParseError: ValidationNel[ParseError, INIFile] =
          MalformedConfigLine(lineNumber + 1, line, currentSection).widen.failureNel[INIFile]

        lineParser.parse(line.trim).fold(
          (_, _, err) => (currentSection, parsed |+| handleParseError),
          (result, _) => result.fold(
            newPair =>
              if (ISet.fromList("synctrack" :: "expertsingle" :: scala.Nil).contains(currentSection.map(_.value).getOrElse("").toLowerCase))
                (currentSection, parsed)
              else
              (currentSection, parsed.traverse(_.insertValue(currentSection, newPair)).fold(_.failure, identity)),
            newSection => (newSection.just, parsed.traverse(_.insertNewSection(newSection.just)).fold(_.failure, identity))
          )
        )
    }._2

  }


}
