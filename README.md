
This repository contains sbt plugins which are targeted for
eventual inclusion in sbt core. They are semi-experimental: we
think they are ready to use, and we intend to keep these ABIs
stable. However, they may change a little bit when they make their
way into sbt proper.

These new APIs were mostly created to enable
[sbt server](https://github.com/sbt/sbt-remote-control), but they
have null or fallback behavior in traditional non-server sbt, so
they are safe to use unconditionally.

# Using the plugins

These plugins are all in the same jar, to depend on it create a
file in `project` such as `project/core-next.sbt` containing:
`addSbtPlugin("org.scala-sbt" % "sbt-core-next" % "0.1.1")`

They are all
[auto plugins](http://www.scala-sbt.org/0.13/docs/Plugins.html) so
there's nothing to do other than that.

# Intro to the plugins in core-next

## BackgroundRunPlugin

This plugin introduces the concept of _background jobs_, which are
threads or processes which exist in the background (outside of any
task execution). Jobs can be managed similar to OS processes:
there are tasks `jobList`, `jobStop`, `jobWaitFor`. To create a
job from a task, use the `jobService` setting to obtain a
`BackgroundJobService` and then call the `runInBackgroundThread`
method.

The `BackgroundRunPlugin` also changes `run` and `runMain` to be
blocking wrappers around `backgroundRun` and
`backgroundRunMain`. `backgroundRun` starts a job to run an
application, then returns to the sbt command loop, while the
traditional `run` task blocks the sbt command loop.

The purpose of this is to allow you to run multiple processes from
the same sbt instance, or to run a process and then also do
something else in sbt.

Unlike tasks, background jobs do not yield a value; they have no
result.

## InteractionServicePlugin

This plugin abstracts asking for a string, or asking for
confirmation, through a setting `interactionService` which returns
an `InteractionService` instance. This keeps tasks from relying on
direct interaction with standard input, and allows tasks to
interact with the user even if the only client is a GUI client.

## SendEventServicePlugin

This plugin adds the `sendEventService` key returning a
`SendEventService` instance. You would use this to send structured
events to clients of sbt server; an event can be anything you can
serialize with
[sbt.serialization](https://github.com/sbt/serialization).  Events
might be for displaying progress, to show errors as they happen,
or for any other purpose. Unlike task results, events stream
immediately.

Events have no effect unless some client knows about the event
type and chooses to do something with it. When running outside of
sbt server, events are dropped and go nowhere.

## SerializersPlugin

This is a workaround for our inability to change the ABI of the
sbt 0.13.x series. We need a pickler and unpickler for task
results. In 0.13.x, we can't modify
[TaskKey](http://www.scala-sbt.org/0.13/api/sbt/TaskKey.html) and
[SettingKey](http://www.scala-sbt.org/0.13/api/index.html#sbt.SettingKey),
and the associated macros `taskKey` and `settingKey`, to require
an implicit pickler.

So the solution is to allow plugins to register picklers, and we
look them up by runtime class, since `TaskKey` and `SettingKey` do
already capture the type manifest of the result type.

If you're curious, you can see where the runtime lookup of task
result picklers happens in sbt server
[here](https://github.com/sbt/sbt-remote-control/blob/master/server/src/main/scala/sbt/server/TaskProgressShim.scala#L79).

For plugins who want their results to be available to sbt clients,
they would register a pickler with the `registeredSerializers`
key. There's also a `registeredProtocolConversions` key, which
transforms the result to another type prior to serialization.
