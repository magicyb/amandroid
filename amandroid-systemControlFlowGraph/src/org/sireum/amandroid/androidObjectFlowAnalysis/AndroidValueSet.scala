package org.sireum.amandroid.androidObjectFlowAnalysis

import org.sireum.amandroid.objectFlowAnalysis._

class AndroidValueSet extends NormalValueSet{
  override def copy : AndroidValueSet = {
    val clone = new AndroidValueSet
    clone.addInstances(this.insts)
    clone
  }
  def getDiff(vsSucc : AndroidValueSet) : AndroidValueSet = {
    val d : AndroidValueSet = new AndroidValueSet
    d.addInstances(this.insts.diff(vsSucc.instances))
    d
  }
}