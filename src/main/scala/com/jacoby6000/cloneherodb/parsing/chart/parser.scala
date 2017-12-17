package com.jacoby6000.cloneherodb.parsing.chart

import com.jacoby6000.cloneherodb.parsing.ini.parser.ParseResult
import com.jacoby6000.cloneherodb.parsing.ini.{parser => iniParser}


object parser {

  // Chart files look like INI files, but with curly braces and indentation.
  // Until I see evidence of it being anything different, this solution will suffice.
  def parse(input: String): ParseResult =
    iniParser.parse(input.lines.map(_.trim).filterNot(Set("{", "}")).mkString("\n"))
}
