package org.allenai.dqa.matching

import org.allenai.dqa.labeling.DiagramLabel
import org.allenai.dqa.labeling.Diagram
import com.google.common.base.Preconditions

import spray.json._
import spray.json.DefaultJsonProtocol._
import scala.io.Source

/**
 * Example for diagram part matching model. Each example
 * consists of a source diagram whose parts are to be
 * matched with those of the target diagram. The label
 * is the correct matching. sourceLabel and targetLabel are
 * included for evaluation purposes, but should not be 
 * used in a matching model. They will be null for real
 * test examples.  
 */
case class MatchingExample(source: Diagram, sourceLabel: DiagramLabel,
    target: Diagram, targetLabel: DiagramLabel, label: MatchingLabel) {
  
}

case class MatchingLabel(targetToSourcePartMap: Map[Int, Option[Int]]) {
  def getSourcePartInd(targetPartInd: Int): Option[Int] = {
    targetToSourcePartMap(targetPartInd)
  }
}

object MatchingExample {
  
  def fromJsonFile(filename: String, labeledDiagrams: Map[String, (Diagram, DiagramLabel)]
    ): Array[MatchingExample] = {
    val lines = Source.fromFile(filename).getLines
    lines.map(fromJsonLine(_, labeledDiagrams)).toArray
  }

  def fromJsonLine(line: String, labeledDiagrams: Map[String, (Diagram, DiagramLabel)]
    ): MatchingExample = {
    val js = line.parseJson.asJsObject
    val src = js.fields("src").convertTo[String]
    val target = js.fields("target").convertTo[String]

    val (srcDiagram, srcLabel) = labeledDiagrams(src)
    val (targetDiagram, targetLabel) = labeledDiagrams(target)
    fromDiagrams(srcDiagram, srcLabel, targetDiagram, targetLabel)
  }

  /**
   * Create a matching example from two diagrams by matching 
   * their equivalently-labeled parts. 
   */
  def fromDiagrams(source: Diagram, sourceLabel: DiagramLabel,
      target: Diagram, targetLabel: DiagramLabel): MatchingExample = {

    val partMap = target.parts.map { targetPart =>
      val targetPartLabel = targetLabel.partLabels(targetPart.ind)
      val sourceInd = sourceLabel.partLabels.indexOf(targetPartLabel)

      if (sourceInd >= 0) {
        (targetPart.ind, Some(sourceInd))
      } else {
        (targetPart.ind, None)
      }
    }

    val label = MatchingLabel(partMap.toMap)

    MatchingExample(source, sourceLabel, target, targetLabel, label)
  }
}