package org.allenai.pnp

import com.jayantkrish.jklol.training.{ LogFunction, NullLogFunction }
import edu.cmu.dynet.Expression
import org.allenai.pnp.AuxiliaryLoss.AuxiliaryLoss
import org.allenai.pnp.ExecutionScore.ExecutionScore

class PnpInferenceContext(
  cg: CompGraph = null,
  val log: LogFunction = new NullLogFunction(),
  activeScores: Set[ExecutionScore] = Set.empty,
  auxiliaryLosses: Set[AuxiliaryLoss] = Set.empty) {

  def compGraph: CompGraph = {
    assert (cg != null)
    cg
  }

  def addExecutionScore(es: ExecutionScore) =
    new PnpInferenceContext(cg, log, activeScores + es, auxiliaryLosses)

  def computeScore(tag: Any, choice: Any, env: Env): Double =
    activeScores.map(_(tag, choice, env)).sum

  def addAuxiliaryLoss(al: AuxiliaryLoss) =
    new PnpInferenceContext(cg, log, activeScores, auxiliaryLosses + al)

  def computeAuxiliaryLoss(tag: Any, item: Any, env: Env): Expression = {
    val losses = auxiliaryLosses.map(_(tag, item, env))

    if (losses.nonEmpty) {
      Expression.sum(losses.toVector: _*)
    } else {
      Expression.input(0.0f)
    }
  }

  def setLog(newLog: LogFunction): PnpInferenceContext = {
    new PnpInferenceContext(cg, newLog, activeScores)
  }
}

object PnpInferenceContext {
  def init: PnpInferenceContext = new PnpInferenceContext()
  def init(cg: CompGraph): PnpInferenceContext = new PnpInferenceContext(cg)
  def init(model: PnpModel): PnpInferenceContext = init(model.getComputationGraph())
}