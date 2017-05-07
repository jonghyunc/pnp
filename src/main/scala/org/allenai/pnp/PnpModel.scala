package org.allenai.pnp

import edu.cmu.dynet._

import scala.collection.mutable.ListBuffer

/** A neural probabilistic program model consisting
  * of a collection of named Tensor parameters. These
  * parameters are used to initialize the computation
  * graph of a program during inference.
  */
class PnpModel(var names: Map[String, Parameter], var lookupNames: Map[String, LookupParameter], 
    var lstmBuilders: Map[String, LstmBuilder], val model: Model, var locallyNormalized: Boolean) {

  def addParameter(name: String, dim: Dim): Parameter = {
    val param = model.addParameters(dim)
    names += (name -> param)
    param
  }
  
  def addParameter(name: String, dim: Dim, init: ParameterInit): Parameter = {
    val param = model.addParameters(dim, init)
    names += (name -> param)
    param
  }
  
  def addLookupParameter(name: String, lookupNum: Long, dim: Dim): LookupParameter = {
    val param = model.addLookupParameters(lookupNum, dim)
    lookupNames += (name -> param)
    param
  }

  def addLookupParameter(name: String, lookupNum: Long, dim: Dim,
      init: ParameterInit): LookupParameter = {
    val param = model.addLookupParameters(lookupNum, dim, init)
    lookupNames += (name -> param)
    param
  }

  def addLstmBuilder(name: String, builder: LstmBuilder): Unit = {
    lstmBuilders += (name -> builder)
  }

  def getParameter(name: String): Parameter = {
    names(name)
  }

  def getLookupParameter(name: String): LookupParameter = {
    lookupNames(name)
  }

  def getLstmBuilder(name: String): LstmBuilder = {
    lstmBuilders(name)
  }

  def getComputationGraph(): CompGraph = {
    new CompGraph(this)
  }

  def save(saver: ModelSaver): Unit = {
    saver.addModel(model)
    saver.addBoolean(locallyNormalized)
    
    saver.addInt(names.size)
    for ((k, v) <- names) {
      saver.addObject(k)
      saver.addParameter(v)
    }

    saver.addInt(lookupNames.size)
    for ((k, v) <- lookupNames) {
      saver.addObject(k)
      saver.addLookupParameter(v)
    }

    saver.addInt(lstmBuilders.size)
    for ((k, v) <- lstmBuilders) {
      saver.addObject(k)
      saver.addLstmBuilder(v)
    }
  }
}

object PnpModel {
  def init(locallyNormalized: Boolean): PnpModel = {
    new PnpModel(Map(), Map(), Map(), new Model, locallyNormalized)
  }
  
  def load(loader: ModelLoader): PnpModel = {
    val model = loader.loadModel()
    val locallyNormalized = loader.loadBoolean()
    
    val numParams = loader.loadInt()
    val params = ListBuffer[(String, Parameter)]()
    for (i <- 0 until numParams) {
      val name = loader.loadObject(classOf[String])
      val param = loader.loadParameter()
      params += ((name, param))
    }

    val numLookups = loader.loadInt()
    val lookups = ListBuffer[(String, LookupParameter)]()
    for (i <- 0 until numLookups) {
      val name = loader.loadObject(classOf[String])
      val param = loader.loadLookupParameter()
      lookups += ((name, param))
    }

    val numLstms = loader.loadInt()
    val lstmBuilders = ListBuffer[(String, LstmBuilder)]()
    for (i <- 0 until numLstms) {
      val name = loader.loadObject(classOf[String])
      val lstmBuilder = loader.loadLstmBuilder()
      lstmBuilders += ((name, lstmBuilder))
    }

    new PnpModel(params.toMap, lookups.toMap, lstmBuilders.toMap, model, locallyNormalized)
  }
}