package ca.uwaterloo.flix

import java.nio.file.Paths

import ca.uwaterloo.flix.lang.Parser
import ca.uwaterloo.flix.util.{Shell, Options}

object Main {

  def main(args: Array[String]): Unit = {

    implicit val options = Options()

    val asts = Parser.parse(args.map(arg => Paths.get(arg)))

    println(asts)

    val shell = new Shell()
    shell.startAndAwait()

  }

}