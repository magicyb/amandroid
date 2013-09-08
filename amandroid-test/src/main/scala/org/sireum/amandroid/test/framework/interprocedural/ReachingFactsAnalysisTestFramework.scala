package org.sireum.amandroid.test.framework.interprocedural

import org.sireum.pipeline._
import org.sireum.util._
import java.io.PrintWriter
import org.sireum.core.module.ChunkingPilarParserModule
import org.sireum.amandroid.module.PilarAndroidSymbolResolverModule
import org.sireum.amandroid.module.AndroidIntraProceduralModule
import org.sireum.amandroid.module.AndroidInterProceduralModule
import java.io.File
import org.sireum.amandroid.AmandroidCodeSource
import org.sireum.amandroid.pilar.parser.LightWeightPilarParser
import org.sireum.amandroid.Center
import org.sireum.amandroid.AmandroidResolver
import org.sireum.amandroid.interProcedural.callGraph.CallGraphBuilder
import org.sireum.amandroid.test.framework.TestFramework
import org.sireum.amandroid.interProcedural.reachingFactsAnalysis.ReachingFactsAnalysis

	/**
 * @author <a href="mailto:fgwei@k-state.edu">Fengguo Wei</a>
 */
trait ReachingFactsAnalysisTestFramework extends TestFramework {

  def Analyzing : this.type = this

  def title(s : String) : this.type = {
    _title = caseString + s
    this
  }

  def model(code : String) =
    InterProceduralConfiguration(title, Left(code))

  def file(fileUri : FileResourceUri) =
    InterProceduralConfiguration(title, Right(fileUri))

  case class InterProceduralConfiguration //
  (title : String,
   src : Either[String, FileResourceUri]) {

    test(title) {
    	println("####" + title + "#####")
    	Center.reset
    	AmandroidCodeSource.clearAppRecordsCodes
    	LightWeightPilarParser(src, AmandroidCodeSource.CodeType.APP)
    	AmandroidCodeSource.getAppRecordsCodes.keys foreach{
    	  k =>
    	    Center.resolveRecord(k, Center.ResolveLevel.BODIES)
    	}
    	
    	val entryPoints = Center.getEntryPoints
    	entryPoints.foreach{
    	  ep =>
    	    ReachingFactsAnalysis(ep)
    	}
    	
//    	val r = Center.resolveRecord("[|java:lang:StringBuilder|]", Center.ResolveLevel.BODIES)
//    	r.getProcedures.toSeq.sortBy(f => f.getSignature).foreach{
//    	  p =>
//    	    println("  case \"" + p.getSignature + "\" =>")
//    	}
    }
  }

  protected var _title : String = null
  protected var num = 0
  protected def title() = if (_title == null) {
    num += 1
    "Analysis #" + num
  } else _title
}