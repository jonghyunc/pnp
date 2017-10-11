package org.allenai.pnp

import scala.collection.mutable.ListBuffer
import scala.util.Random

import com.google.common.base.Preconditions
import com.jayantkrish.jklol.training.LogFunction
import com.jayantkrish.jklol.util.KbestQueue

import ExecutionScore.ExecutionScore
import edu.cmu.dynet._

/**
 * Beam search trainer implementing a LaSO-like algorithm.
 * This trainer implements a margin loss between the
 * lowest-scoring item on the beam and the highest-scoring
 * correct execution. Thus, a gradient update is performed
 * every time all correct executions fall off the beam.
 */
class BsoTrainer(val epochs: Int, val beamSize: Int, val maxIters: Int,
    val model: PnpModel, val trainer: Trainer, val log: LogFunction) {

  Preconditions.checkArgument(model.locallyNormalized == false,
      "BsoTrainer expects model to be not locally normalized".asInstanceOf[Any])

  import BsoTrainer.EXECUTION_INCORRECT_VAR_NAME

  def train[A](examples: Seq[PnpExample[A]]): Unit = {
    for (i <- 0 until epochs) {
      var loss = 0.0
      log.notifyIterationStart(i)
      for (example <- Random.shuffle(examples)) {
        loss +=  doExampleUpdate(example)
      }
      
      trainer.updateEpoch()

      log.logStatistic(i, "loss", loss)
      log.notifyIterationEnd(i)
    }
  }

  private def doExampleUpdate[A](example: PnpExample[A]): Double = {
    ComputationGraph.renew()
    var loss = 0.0

    val env = example.env
    Preconditions.checkArgument(example.conditionalExecutionScore != null,
        "BsoTrainer requires an execution score".asInstanceOf[AnyRef])
    val stateCost = example.conditionalExecutionScore
    val graph = model.getComputationGraph()

    val queue = new BsoPnpQueue[A](beamSize, stateCost)
    val finished = new BsoPnpQueue[A](beamSize, stateCost)
    val endContinuation = new PnpEndContinuation[A]()

    val startEnv = env
    val context = PnpInferenceContext.init(model)
        .setLog(log)

    queue.offer(example.unconditional, env, 0.0, context, null, null)

    log.startTimer("bso/beam_search")
    val beam = new Array[SearchState[A]](beamSize)
    var numIters = 0
    val losses = ListBuffer[Expression]()
    while (queue.queue.size > 0 &&
        (maxIters < 0 || numIters < maxIters)) {
      // println(numIters + " " + queue.queue.size)

      // TODO: check this
      val beamSize = queue.queue.size
      
      // Check margin constraint.
      // The highest-scoring correct execution's score must exceed
      // that of the lowest-scoring execution on beam by a margin of 1.

      // Note that beam(0) is the lowest-scoring element   
      // val (beamEx, beamCost) = queue.queue.getItems()(0)
      log.startTimer("bso/beam_search/get_executions")
      val worstIncorrectEx = if (queue.incorrectQueue.size > 0) {
        queue.incorrectQueue.getItems()(0) 
      } else {
        null
      }

      val bestCorrectEx = if (queue.correctQueue.size > 0) {
        queue.correctQueue.getItems.slice(
            0, queue.correctQueue.size).maxBy(x => x.logProb)
      } else {
        null
      }
      log.stopTimer("bso/beam_search/get_executions")

      var nextBeamSize = -1

      // Check for a margin violation. Note that the cost (margin)
      // of worstIncorrectEx is included in worstIncorrectEx.logProb.
      if (numIters != 0 && bestCorrectEx != null && worstIncorrectEx != null &&
          worstIncorrectEx.logProb > bestCorrectEx.logProb) {
        // Margin violation
        // println("m: " + numIters + " " + worstIncorrectEx.logProb + " " + bestCorrectEx.logProb)

        // Add to the loss.
        log.startTimer("bso/beam_search/margin_violation")
        
        val beamScoreExpr = worstIncorrectEx.env.getScore
        val margin = worstIncorrectEx.env.getVar[Double](EXECUTION_INCORRECT_VAR_NAME)
        val correctScoreExpr = bestCorrectEx.env.getScore
        losses += ((beamScoreExpr + margin.toFloat) - correctScoreExpr)

        // Continue the search with the best correct execution.
        beam(0) = bestCorrectEx
        nextBeamSize = 1
        
        log.stopTimer("bso/beam_search/margin_violation")
      } else {
        // No margin violation. Queue up all beam executions for 
        // the next search step.
        for (i <- 0 until beamSize) {
          beam(i) = queue.queue.getItems()(i)
        }

        nextBeamSize = beamSize
      }

      queue.queue.clear
      queue.correctQueue.clear
      queue.incorrectQueue.clear

      // Continue beam search.
      log.startTimer("bso/beam_search/search_step")
      for (i <- 0 until nextBeamSize) {
        val state = beam(i)
        state.value.searchStep(state.env, state.logProb, context, endContinuation, queue, finished)
      }
      log.stopTimer("bso/beam_search/search_step")

      numIters += 1
    }
    log.stopTimer("bso/beam_search")
              
    // Compute margin loss for final highest-scoring incorrect entry
    // vs. correct.
    val finalBestIncorrect = if (finished.incorrectQueue.size > 0) {
      finished.incorrectQueue.getItems.slice(
          0, finished.incorrectQueue.size).maxBy(_.logProb)
    } else {
      null
    }
        
    val finalBestCorrect = if (finished.correctQueue.size > 0) {
      finished.correctQueue.getItems.slice(
          0, finished.correctQueue.size).maxBy(_.logProb)
    } else {
      null
    }

    if (finalBestIncorrect != null && finalBestCorrect != null &&
        finalBestIncorrect.logProb > finalBestCorrect.logProb) {
      // Margin violation
      // println("m: end " + finalBestIncorrect.logProb + " " + finalBestCorrect.logProb)

      // Add to the loss.
      val beamScoreExpr = finalBestIncorrect.env.getScore
      val margin = finalBestIncorrect.env.getVar[Double](EXECUTION_INCORRECT_VAR_NAME)
      val correctScoreExpr = finalBestCorrect.env.getScore

      losses += ((beamScoreExpr + margin.toFloat) - correctScoreExpr)
    }

    if (losses.size > 0) {
      val lossExpr = Expression.sum(new ExpressionVector(losses))
      
      log.startTimer("bso/eval_loss")
      loss += ComputationGraph.incrementalForward(lossExpr).toFloat
      log.stopTimer("bso/eval_loss")
      
      log.startTimer("bso/backward")
      ComputationGraph.backward(lossExpr)
      trainer.update(1.0f)
      log.stopTimer("bso/backward")
    }

    loss
  }
}

class BsoPnpQueue[A](size: Int, val stateCost: ExecutionScore) extends PnpSearchQueue[A] {

  val queue = new KbestQueue(size, Array.empty[SearchState[A]])
  val correctQueue = new KbestQueue(size, Array.empty[SearchState[A]])
  val incorrectQueue = new KbestQueue(size, Array.empty[SearchState[A]])

  import BsoTrainer.EXECUTION_INCORRECT_VAR_NAME
  
  override def offer(value: Pnp[A], env: Env, logProb: Double, context: PnpInferenceContext, tag: Any, choice: Any): Unit = {
    if (logProb > Double.NegativeInfinity) {
      context.log.startTimer("bso/beam_search/search_step/eval_cost")
      val cost = stateCost(tag, choice, env)
      context.log.stopTimer("bso/beam_search/search_step/eval_cost")
      val nextEnv = if (cost != 0.0) {
        val prevCost = env.getVar(EXECUTION_INCORRECT_VAR_NAME, 0.0)
        // TODO: we may want to allow different combination functions.
        // We'd also have to adjust the scoring computation for queuing
        // below.
        env.setVar(EXECUTION_INCORRECT_VAR_NAME, (prevCost + cost))
      } else {
        env
      }
      
      val executionScore = logProb + cost
      val state = SearchState(value, nextEnv, executionScore, tag, choice)
      queue.offer(state, executionScore)

      if (nextEnv.isVarBound(EXECUTION_INCORRECT_VAR_NAME)) {
        incorrectQueue.offer(state, executionScore)
      } else {
        // Note that cost is 0 in this case.
        correctQueue.offer(state, executionScore)
      }
    }
  }
}

object BsoTrainer {
  val EXECUTION_INCORRECT_VAR_NAME = "**bso_execution_incorrect**"
}
