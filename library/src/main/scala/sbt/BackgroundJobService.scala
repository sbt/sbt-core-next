package sbt

/**
 * Interface between tasks and jobs; tasks aren't allowed
 *  to directly mess with the BackgroundJob above. Methods
 *  on this interface should all be pure (conceptually this
 *  is immutable).
 */
sealed trait BackgroundJobHandle {
  def id: Long
  def humanReadableName: String
  def spawningTask: ScopedKey[_]
  // def tags: SomeType
}

sealed trait BackgroundJobService extends java.io.Closeable {

  /**
   * Launch a background job which is a function that runs inside another thread;
   *  killing the job will interrupt() the thread. If your thread blocks on a process,
   *  then you should get an InterruptedException while blocking on the process, and
   *  then you could process.destroy() for example.
   *
   *  TODO if we introduce a ServiceManager, we can pass that in to start instead of
   *  two hardcoded services.
   */
  def runInBackgroundThread(spawningTask: ScopedKey[_], start: (Logger, SendEventService) => Unit): BackgroundJobHandle

  def list(): Seq[BackgroundJobHandle]
  def stop(job: BackgroundJobHandle): Unit
  def waitFor(job: BackgroundJobHandle): Unit

  // TODO we aren't using this anymore; do we want it?
  def handleFormat: sbinary.Format[BackgroundJobHandle]
}

object BackgroundJobServiceKeys {
  // jobService is a setting not a task because semantically it's required to always be the same one
  // TODO create a separate kind of key to lookup services separately from tasks
  val jobService = settingKey[BackgroundJobService]("Job manager used to run background jobs.")
  val jobList = taskKey[Seq[BackgroundJobHandle]]("List running background jobs.")
  val jobStop = inputKey[Unit]("Stop a background job by providing its ID.")
  val jobWaitFor = inputKey[Unit]("Wait for a background job to finish by providing its ID.")
  val backgroundRun = inputKey[BackgroundJobHandle]("Start an application's default main class as a background job")
  val backgroundRunMain = inputKey[BackgroundJobHandle]("Start a provided main class as a background job")
}

private[sbt] trait SbtPrivateBackgroundJobService extends BackgroundJobService
private[sbt] trait SbtPrivateBackgroundJobHandle extends BackgroundJobHandle
