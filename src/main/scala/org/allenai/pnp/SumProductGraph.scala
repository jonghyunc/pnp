package org.allenai.pnp

import scala.collection.mutable.ListBuffer
import scala.collection.mutable.{Map => MutableMap}
import com.jayantkrish.jklol.util.IndexedList

class SumProductGraph[A] {
  
  val vertexes = IndexedList.create[SpgVertex[A]]
  val edges = ListBuffer[(Int, Int, Double)]()
  
  val inboundEdges = MutableMap[Int, ListBuffer[(Int, Double)]]()
  val vertexScores = MutableMap[Int, Double]()
  
  def addVertex(vertex: Pnp[A], endState: Boolean): Int = {
    vertexes.add(SpgVertex(vertex, endState))
  }
  
  def getVertex(vid: Int): SpgVertex[A] = {
    vertexes.get(vid)
  }
  
  def numVertexes(): Int = {
    vertexes.size
  }
  
  def addEdge(v1Id: Int, v2Id: Int, score: Double): Unit = {
    edges += ((v1Id, v2Id, score))
    
    if (!inboundEdges.contains(v2Id)) {
      inboundEdges(v2Id) = ListBuffer()
    }
    
    inboundEdges(v2Id) += ((v1Id, score))
  }
  
  def getScore(vid: Int): Double = {
    if (vertexScores.contains(vid)) {
      val score = vertexScores(vid)
      println("Hit cache: " + vid + " " + score + " " + vertexes.get(vid))
      score
    } else {
      val inboundScores = for {
        (inboundId, score) <- inboundEdges.getOrElse(vid, List())
        inboundScore = getScore(inboundId)
      } yield {
        inboundScore + score
      }
      
      // sum the scores in logspace
      val myScore = if (inboundScores.length > 0) {
        val max = inboundScores.max
        max + Math.log(inboundScores.map(x => Math.exp(x - max)).sum)
      } else {
        0.0
      }
      
      vertexScores(vid) = myScore
      myScore
    }
  }
  
  override def toString(): String = {
    vertexes.toString() + edges.toString()
  }
}

case class SpgVertex[A](pnp: Pnp[A], endState: Boolean)