package org.allenai.pnp.examples
import com.jayantkrish.jklol.training.{DefaultLogFunction, NullLogFunction}

import scala.collection.JavaConverters._
import com.jayantkrish.jklol.util.IndexedList
import edu.cmu.dynet._
import org.allenai.pnp.ExecutionScore.ExecutionScore
import org.allenai.pnp._

/**
  * Created by jonghyunc on 4/7/17.
  */
class SeqTagger(val vocab: IndexedList[String]) {

  import SeqTagger._

  def applyHelper(input: List[(String, Int)]) : Pnp[List[String]] = {
    val pos = Array("noun", "verb")

    if(input.isEmpty) {
      Pnp.value(List())
    } else {
      val elem = input(0)
      val token = elem._1
      val tokenIndex = elem._2
      val rest = input.drop(1)

      for {
        paramLabelScores <- Pnp.param(LABEL_SCORES)
        w1 <- Pnp.param(W_1)
        w2 <- Pnp.param(W_2)
        b <- Pnp.param(B)
        ax <- Pnp.lookupParam(A_X, vocab.getIndex(token))

        score = w2 * Expression.rectify( w1 * ax + b ) + paramLabelScores

        posOutput <- Pnp.choose(pos, score, tokenIndex)
        restElem <- applyHelper(rest)
      } yield {
        posOutput :: restElem
      }
    }
  }

  def apply(input: List[String]) : Pnp[List[String]] = {
    applyHelper(input.zipWithIndex)
  }

  def getLabelCost(label: List[String]): ExecutionScore = {
    new SeqTaggerExecutionScore(label.toArray)
  }
}


class SeqTaggerExecutionScore(val targetTokensLabel: Array[String]) extends ExecutionScore {
  def apply(tag: Any, choice: Any, env: Env): Double = {
    if (tag != null && tag.isInstanceOf[Int]) {
      // The tag is the index of the choice in the target
      // sequence, and choice is the chosen token.
      // Cost is 0 if the choice agrees with the label
      // and -infinity otherwise.
      val tokenIndex = tag.asInstanceOf[Int]

      val chosen = choice.asInstanceOf[String]
      if (targetTokensLabel(tokenIndex) == chosen) {
        0.0
      } else {
        Double.NegativeInfinity
      }
    } else {
      0.0
    }
  }
}


object SeqTagger {
  def main(args: Array[String]): Unit = {
    // Initialize dynet
    Initialize.initialize()

    val vocab = List("the", "man", "likes", "woman", "who", "is", "attractive")

    val pnpModel = PnpModel.init(true)
    val seqTagger = SeqTagger.create(IndexedList.create(vocab.asJava), pnpModel)
    val bb = seqTagger.apply(List("man", "likes", "woman", "who", "is", "attractive"))
    val dist = bb.beamSearch(100, pnpModel)
//    dist.executions.zipWithIndex.foreach(e => println(e))

    val trainingData = Array(("man", "noun"), ("likes", "verb"))

    val testData = Array(("man likes", "noun verb"))

    // Tokenize input
    val trainingDataTokenized = trainingData.map(x => (x._1.split(" ").toList,
      x._2.split(" ").toList))
    val testDataTokenized = testData.map(x => (x._1.split(" ").toList,
      x._2.split(" ").toList))

    // 2. Generate training examples.
    val trainingExamples = for {
      d <- trainingDataTokenized
    } yield {
      // Generate a probabilistic neural program over all possible target
      // sequences given the input sequence. The parameters of the neural
      // network will be trained such that the unconditionalPnp's
      // distribution is close to the label, defined below.
      val unconditionalPnp = seqTagger.apply(d._1)

      // Labels can be represented either as a conditional distribution
      // over correct program executions, or a cost function that assigns
      // a cost to each program execution. In this case we're using a cost
      // function.
      val conditionalPnp = unconditionalPnp
      val oracle = seqTagger.getLabelCost(d._2)
      PnpExample(unconditionalPnp, conditionalPnp, Env.init, oracle)
    }

    // 3. Train the model. We can select both an optimization algorithm and
    // an objective function.
    val sgd = new SimpleSGDTrainer(pnpModel.model, 0.1f, 0.01f)

    // Train with maximum likelihood (i.e., the usual way
    // seq2seq models are trained).
    val trainer = new LoglikelihoodTrainer(50, 100, false, pnpModel, sgd, new DefaultLogFunction())
    trainer.train(trainingExamples)

    for((x, y) <- testDataTokenized) {
      println(x)
      val outputHat = seqTagger.apply(x).beamSearch(100, pnpModel)
      outputHat.executions.foreach(e => println(e.value + " " + e.prob))
    }
  }

  val LABEL_SCORES = "labelScores"
  val W_1 = "w_1"
  val B = "b"
  val W_2 = "w_2"
  val A_X = "a_x"


  /**
    * Creates a new sequence-tagging model given the
    * source and target vocabularies and a model within which
    * to initialize parameters.
    */
  def create(vocab: IndexedList[String], model: PnpModel): SeqTagger = {

    val labelDim = 2
    val inputDim = 10
    val hiddenDim = 5

    model.addParameter(LABEL_SCORES, Dim(labelDim))
    model.addParameter(W_1, Dim(hiddenDim, inputDim))
    model.addParameter(B, Dim(hiddenDim))
    model.addParameter(W_2, Dim(labelDim, hiddenDim))

    model.addLookupParameter(A_X, vocab.size(), Dim(inputDim))



    new SeqTagger(vocab)
  }
}
