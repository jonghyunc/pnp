package org.allenai.pnp

import edu.cmu.dynet._

/** Computation graph of a neural network.
  */
class CompGraph(val pnpModel: PnpModel) {

  def getParameter(name: String): Parameter = {
    pnpModel.getParameter(name)
  }

  def getParameterExpr(name: String): Expression = {
    Expression.parameter(getParameter(name))
  }

  def getLookupParameter(name: String): LookupParameter = {
    pnpModel.getLookupParameter(name)
  }

  def getLstmBuilder(name: String): LstmBuilder = {
    pnpModel.getLstmBuilder(name)
  }
}