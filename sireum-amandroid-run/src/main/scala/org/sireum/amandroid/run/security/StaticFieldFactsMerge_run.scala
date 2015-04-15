/*
Copyright (c) 2013-2014 Fengguo Wei & Sankardas Roy, Kansas State University.        
All rights reserved. This program and the accompanying materials      
are made available under the terms of the Eclipse Public License v1.0 
which accompanies this distribution, and is available at              
http://www.eclipse.org/legal/epl-v10.html                             
*/
package org.sireum.amandroid.run.security

import org.sireum.amandroid.security.AmandroidSocket
import org.sireum.util.FileUtil
import org.sireum.amandroid.security.interComponentCommunication.IccCollector
import org.sireum.amandroid.util.AndroidLibraryAPISummary
import org.sireum.amandroid.security.AmandroidSocketListener
import org.sireum.jawa.MessageCenter._
import org.sireum.amandroid.alir.dataRecorder.DataCollector
import org.sireum.amandroid.alir.dataRecorder.MetricRepo
import org.sireum.amandroid.AndroidGlobalConfig
import java.io.PrintWriter
import java.io.File
import org.sireum.amandroid.AndroidConstants
import org.sireum.jawa.JawaCodeSource
import org.sireum.jawa.util.SubStringCounter
import org.sireum.util.FileResourceUri
import org.sireum.jawa.util.IgnoreException
import org.sireum.jawa.GlobalConfig
import org.sireum.amandroid.alir.pta.reachingFactsAnalysis.AndroidReachingFactsAnalysisConfig
import org.sireum.jawa.util.MyTimer


/**
 * @author <a href="mailto:fgwei@k-state.edu">Fengguo Wei</a>
 * @author <a href="mailto:sroy@k-state.edu">Sankardas Roy</a>
 */ 
object StaticFieldFactsMerge_run {
  private final val TITLE = "StaticFieldFactsMerge_run"
  object StaticFieldCounter {
    var total = 0
    var haveresult = 0
    var haveIccCall = 0
    var iccTotal = 0
    
    override def toString : String = "total: " + total + ", haveResult: " + haveresult + ", haveIccCall: " + haveIccCall + ", iccTotal: " + iccTotal
  }
  
  private class StaticFieldListener(source_apk : FileResourceUri, app_info : IccCollector) extends AmandroidSocketListener {
    def onPreAnalysis: Unit = {
      StaticFieldCounter.total += 1
      val iccSigs = AndroidConstants.getIccMethods()
      val codes = JawaCodeSource.getAppRecordsCodes
		  if(codes.exists{
    	  case (rName, code) =>
    	    iccSigs.exists(code.contains(_))
    	}) StaticFieldCounter.haveIccCall += 1
		  
		  codes.foreach{
    	  case (rName, code) =>
  	      StaticFieldCounter.iccTotal += iccSigs.map(sig => SubStringCounter.countSubstring(code, sig + " @classDescriptor")).reduce((i, j) => i + j)
    	}
    }

    def entryPointFilter(eps: Set[org.sireum.jawa.JawaProcedure]): Set[org.sireum.jawa.JawaProcedure] = {
//      val res = eps.filter(e=>app_info.getIccContainers.contains(e.getDeclaringRecord))
//      if(!res.isEmpty){
//    	  IccCounter.foundIccContainer += 1
//    	}
//      res
      eps
    }

    def onTimeout : Unit = {}

    def onAnalysisSuccess : Unit = {
      val appData = DataCollector.collect
    	MetricRepo.collect(appData)
    	val outputDir = AndroidGlobalConfig.amandroid_home + "/output"
    	val apkName = source_apk.substring(source_apk.lastIndexOf("/"), source_apk.lastIndexOf("."))
    	val appDataDirFile = new File(outputDir + "/" + apkName)
    	if(!appDataDirFile.exists()) appDataDirFile.mkdirs()
    	val out = new PrintWriter(appDataDirFile + "/AppData.txt")
	    out.print(appData.toString)
	    out.close()
	    val mr = new PrintWriter(outputDir + "/MetricInfo.txt")
		  mr.print(MetricRepo.toString)
		  mr.close()
		  StaticFieldCounter.haveresult += 1
    }

    def onPostAnalysis: Unit = {
      msg_critical(TITLE, StaticFieldCounter.toString)
    }
    
    def onException(e : Exception) : Unit = {
      e match{
        case ie : IgnoreException => System.err.println("Ignored!")
        case a => 
          e.printStackTrace()
      }
    }
  }
  
  def main(args: Array[String]): Unit = {
    if(args.size != 2){
      System.err.print("Usage: source_path output_path")
      return
    }

    GlobalConfig.ICFG_CONTEXT_K = 1
    AndroidReachingFactsAnalysisConfig.resolve_icc = false
    AndroidReachingFactsAnalysisConfig.parallel = false
    AndroidReachingFactsAnalysisConfig.resolve_static_init = false


    val socket = new AmandroidSocket
    socket.preProcess
    
    val sourcePath = args(0)
    val outputPath = args(1)
    
    val files = FileUtil.listFiles(FileUtil.toUri(sourcePath), ".apk", true).toSet
    
    files.foreach{
      file =>
        try{
          msg_critical(TITLE, "####" + file + "#####")
          val timer = new MyTimer(10)
          timer.start
          val outUri = socket.loadApk(file, outputPath, AndroidLibraryAPISummary)
          val app_info = new IccCollector(file, outUri, Some(timer))
          app_info.collectInfo
          socket.plugListener(new StaticFieldListener(file, app_info))
          socket.runIrfaWithCompMerge(false, false, Some(timer))
        } catch {
          case e : Throwable =>
            e.printStackTrace()
        } finally {
          socket.cleanEnv
        }
    }
  }
}