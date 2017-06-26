package org.allenai.pnp

import edu.cmu.dynet.Expression

object AuxiliaryLoss {

  type AuxiliaryLoss = (Any, Any, Env) => Expression

  val Zero = new AuxiliaryLoss() {
    def apply(tag: Any, item: Any, env: Env): Expression = {
      Expression.input(0.0f)
    }
  }
}
