package org.allenai.pnp

import org.scalatest.Matchers
import org.scalatest.FlatSpec
import org.allenai.pnp.Pnp._
import com.jayantkrish.jklol.training.NullLogFunction

class PnpInferenceSpec extends FlatSpec with Matchers {

  "Pnp" should "continuation experiment" in {
        
    val foo = for {
      x <- choose(Seq(0, 1), Seq(0.5, 0.5))
      y <- choose(Seq(0, 1), Seq(0.5, 0.5))
    } yield {
      x + y
    }

    val bar = for {
      v <- foo
      z <- choose(Seq(0, 1), Seq(0.5, 0.5))
    } yield {
      v + z
    }
    
    val baz = for {
      x <- choose(Seq(0, 1), Seq(0.5, 0.5))
      y <- choose(Seq(0, 1), Seq(0.5, 0.5))
      z <- choose(Seq(0, 1), Seq(0.5, 0.5))
    } yield {
      x + y + z
    }

    val logFn = new NullLogFunction()
    bar.sumProduct(Env.init, ExecutionScore.zero, null, logFn)
    
    println("  ")
    baz.sumProduct(Env.init, ExecutionScore.zero, null, logFn)
  }
}