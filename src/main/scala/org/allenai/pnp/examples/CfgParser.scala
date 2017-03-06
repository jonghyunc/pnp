package org.allenai.pnp.examples

import scala.collection.mutable.{ Map => MutableMap }
import org.allenai.pnp.Pnp

/**
 * Context-free grammar parser.
 */
class CfgParser(val nonterminalRules: Array[NonterminalRule],
    val terminalRules: Array[TerminalRule]) {
  
  val indexedNonterminals: Map[(Int, Int), Array[NonterminalRule]] = null
  val indexedTerminals: Map[Int, Array[TerminalRule]] = null
  
  def parseCky(terminals: Array[Int]): Pnp[Int] = {
    val chart = MutableMap[(Int, Int), Pnp[Int]]()
    
    for (i <- 0 until terminals.length) {
      val applicableTerminals = indexedTerminals(i)
      val terminalDist = for {
        chosenTerminalRule <- Pnp.choose(applicableTerminals)
      } yield {
        chosenTerminalRule.parent
      }
      chart.put((i, i + 1), terminalDist)
    }
    
    for (spanSize <- 1 to terminals.length) {
      for (spanStart <- 0 to (terminals.length - spanSize)) {
        val spanEnd = spanStart + spanSize
        for {
          k <- Pnp.choose((1 until spanSize).toSeq)
          left = chart((spanStart, spanStart + k))
          right = chart((spanStart + k, spanEnd))
          joint <- Pnp.independent(left, right)
          applicableRules = indexedNonterminals.apply(joint)
          rule <- Pnp.choose(applicableRules)
        } yield {
          rule.parent
        }
      }
    }

    chart((0, terminals.length))
  }
  
  def forward(root: Int): Pnp[Array[Int]] = {
    null
  }
}

case class NonterminalRule(parent: Int, left: Int, right: Int)

case class TerminalRule(parent: Int, terminal: Int)