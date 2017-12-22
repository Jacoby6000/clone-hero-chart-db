package com.jacoby6000.cloneherodb.parsing.chart

import com.jacoby6000.cloneherodb.parsing.ini.parser.{DuplicateKey, DuplicateSectionName, INIFile, INIKey, INISectionName, INIValue, MalformedConfigLine}
import org.scalatest._
import parser._

import scalaz._
import Scalaz._
import scalaz.Maybe.Empty

import scala.collection.immutable.List

class INIParserTests extends FunSpec with Matchers {

  val k = INIKey.apply _
  val v = (INIValue.apply _) andThen (_.widen)
  val s = (INISectionName.apply _) andThen (_.just)

  describe("The chart parser") {
    describe("Passing cases") {
      it("Should properly parse an empty chart string.") {
        parse("") shouldEqual INIFile.empty.successNel
      }

      it("Should properly parse an chart string with only whitespaces.") {
        parse("   \n } \t \n  { \n  \t \r\n  ") shouldEqual INIFile.empty.successNel
      }

      it("Should properly parse an chart string with an empty section.") {
        parse("[foo]") shouldEqual INIFile(s("foo") -> IMap.empty).successNel
      }

      it("Should properly parse an chart string with a value and no section.") {
        parse("k=v") shouldEqual INIFile(Empty() -> IMap(k("k") -> v("v").widen)).successNel
      }

      it("Should properly parse an chart string with a section and a value.") {
        parse("[foo]\n{\n\tk=v\n}") shouldEqual INIFile(
          s("foo") -> IMap(k("k") -> v("v").widen)
        ).successNel
      }

      it("Should properly parse an chart string with a section and multiple values.") {
        parse("[foo]\n{\n\tk=v\n\tfoo=bar\n}") shouldEqual INIFile(
          s("foo") -> IMap(k("k") -> v("v").widen, k("foo") -> v("bar"))
        ).successNel
      }

      it("Should properly parse an chart string with a section and multiple values, and a trailing empty INI section.") {
        parse("[foo]\nk=v\nfoo=bar\n[baz]") shouldEqual INIFile(
          s("baz") -> IMap.empty,
          s("foo") -> IMap(k("k") -> v("v").widen, k("foo") -> v("bar"))
        ).successNel
      }

      it("Should properly parse an chart string with a section and multiple values, and values before the section.") {
        parse("x=y\n[foo]\nk=v\nfoo=bar\n[baz]") shouldEqual INIFile(
          Empty() -> IMap(k("x") -> v("y").widen),
          s("baz") -> IMap.empty,
          s("foo") -> IMap(k("k") -> v("v").widen, k("foo") -> v("bar"))
        ).successNel
      }

      it("Should recognize alphanumeric section names and conf keys") {
        val weirdStr = (('0' to '9') ++ ('a' to 'z') ++ ('A' to 'Z') ++ List('-', '_')).mkString
        parse(s"[$weirdStr]\n$weirdStr=x") shouldEqual INIFile(
          s(weirdStr) -> IMap(k(weirdStr) -> v("x").widen)
        ).successNel
      }
    }

    describe("Failing cases") {
      it("Should fail with a malformed k/v pair.") {
        parse("@x=yz") shouldEqual MalformedConfigLine(1, "@x=yz", Empty()).failureNel
      }

      it("Should report the correct section for a malformed line error.") {
        parse("[foo]\n@x=yz") shouldEqual MalformedConfigLine(2, "@x=yz", s("foo")).failureNel
      }

      it("Should accumulate multiple malformed line errors.") {
        parse("@x=yz;\n141\naslkdfjasd\nfoo") shouldEqual NonEmptyList(
          MalformedConfigLine(1, "@x=yz;", Empty()),
          MalformedConfigLine(2, "141", Empty()),
          MalformedConfigLine(3, "aslkdfjasd", Empty()),
          MalformedConfigLine(4, "foo", Empty())
        ).failure
      }

      it("Should fail with a duplicate section name."){
        parse("[foo]\n[foo]") shouldEqual DuplicateSectionName(s("foo")).failureNel
      }

      it("Should fail with a duplicate kv pair.") {
        parse("[foo]\nk=v\nk=y") shouldEqual DuplicateKey(s("foo"), k("k"), v("v"), v("y")).failureNel
      }
    }
  }
}

