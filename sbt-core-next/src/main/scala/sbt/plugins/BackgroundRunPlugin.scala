package sbt
package plugins

import complete.Parser
import complete.DefaultParsers
import Keys._
import Def.Initialize
import sbinary.DefaultProtocol.StringFormat
import Cache.seqFormat
import Attributed.data
import BackgroundJobServiceKeys._

object BackgroundRunPlugin extends AutoPlugin {
  override def trigger = AllRequirements
  override def requires = plugins.JvmPlugin

  override val globalSettings: Seq[Setting[_]] = Seq(
    jobService := { new CommandLineBackgroundJobService() },
    onUnload := { s => try onUnload.value(s) finally jobService.value.close() },
    jobList := { jobService.value.list() },
    jobStop <<= jobStopTask(),
    jobWaitFor <<= jobWaitForTask())

  override val projectSettings = inConfig(Compile)(Seq(
    // note that we use the same runner and mainClass as plain run
    backgroundRunMain <<= backgroundRunMainTask(fullClasspath, runner in run),
    backgroundRun <<= backgroundRunTask(fullClasspath, mainClass in run, runner in run),
    runMain <<= runMainTask(),
    run <<= runTask()))

  def backgroundRunMainTask(classpath: Initialize[Task[Classpath]], scalaRun: Initialize[Task[ScalaRun]]): Initialize[InputTask[BackgroundJobHandle]] =
    {
      import DefaultParsers._
      val parser = Defaults.loadForParser(discoveredMainClasses)((s, names) => Defaults.runMainParser(s, names getOrElse Nil))
      Def.inputTask {
        val (mainClass, args) = parser.parsed
        jobService.value.runInBackgroundThread(resolvedScoped.value, { (logger, uiContext) =>
          toError(scalaRun.value.run(mainClass, data(classpath.value), args, logger))
        })
      }
    }

  def backgroundRunTask(classpath: Initialize[Task[Classpath]], mainClassTask: Initialize[Task[Option[String]]], scalaRun: Initialize[Task[ScalaRun]]): Initialize[InputTask[BackgroundJobHandle]] =
    {
      import Def.parserToInput
      import sys.error

      val parser = Def.spaceDelimited()
      Def.inputTask {
        val mainClass = mainClassTask.value getOrElse error("No main class detected.")
        jobService.value.runInBackgroundThread(Keys.resolvedScoped.value, { (logger, uiContext) =>
          // TODO - Copy the classpath into some tmp directory so we don't immediately die if a recompile happens.
          toError(scalaRun.value.run(mainClass, data(classpath.value), parser.parsed, logger))
        })
      }
    }

  private def runMainTask(): Initialize[InputTask[Unit]] =
    Def.inputTask {
      val handle = backgroundRunMain.evaluated
      // TODO it would be better to use the jobWaitFor task in case someone
      // customizes that task, but heck if I can figure out how to do it.
      val service = jobService.value
      service.waitFor(handle)
    }

  private def runTask(): Initialize[InputTask[Unit]] =
    Def.inputTask {
      val handle = backgroundRun.evaluated
      // TODO it would be better to use the jobWaitFor task in case someone
      // customizes that task, but heck if I can figure out how to do it.
      val service = jobService.value
      service.waitFor(handle)
    }

  private def jobIdParser: (State, Seq[BackgroundJobHandle]) => Parser[Seq[BackgroundJobHandle]] = {
    import DefaultParsers._
    (state, handles) => {
      val stringIdParser: Parser[Seq[String]] = Space ~> token(NotSpace examples handles.map(_.id.toString).toSet, description = "<job id>").+
      stringIdParser.map { strings =>
        strings.map(Integer.parseInt(_)).flatMap(id => handles.find(_.id == id))
      }
    }
  }

  private def foreachJobTask(f: (BackgroundJobService, BackgroundJobHandle) => Unit): Initialize[InputTask[Unit]] = {
    import DefaultParsers._
    val parser: State => Parser[Seq[BackgroundJobHandle]] = { state =>
      val extracted = Project.extract(state)
      val service = extracted.get(jobService)
      // you might be tempted to use the jobList task here, but the problem
      // is that its result gets cached during execution and therefore stale
      jobIdParser(state, service.list())
    }
    Def.inputTask {
      val handles = parser.parsed
      for (handle <- handles) {
        f(jobService.value, handle)
      }
    }
  }

  private def jobStopTask(): Initialize[InputTask[Unit]] =
    foreachJobTask { (manager, handle) => manager.stop(handle) }

  private def jobWaitForTask(): Initialize[InputTask[Unit]] =
    foreachJobTask { (manager, handle) => manager.waitFor(handle) }
}

private[sbt] class CommandLineBackgroundJobService extends AbstractBackgroundJobService {
  override def makeContext(id: Long, spawningTask: ScopedKey[_]) = {
    // TODO this is no good; what we need to do is replicate how sbt
    // gets loggers from Streams, but without the thing where they
    // are all closed when the Streams is closed. So we need "detached"
    // loggers. Potentially on command line we also want to avoid
    // showing the logs on the console as they arrive and only store
    // them in the file for retrieval with "last" - except "last"
    // takes a task name which we don't have.
    val logger = new Logger with java.io.Closeable {
      // TODO
      override def close(): Unit = ()
      // TODO
      override def log(level: sbt.Level.Value, message: => String): Unit = System.err.println(s"background log: $level: $message")
      // TODO
      override def success(message: => String): Unit = System.out.println(s"bg job success: $message")
      // TODO
      override def trace(t: => Throwable): Unit = t.printStackTrace(System.err)
    }
    (logger, CommandLineUIServices)
  }
}
