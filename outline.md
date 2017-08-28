# Plugin Development for Pipeline

## abstract

The Pipeline system, showcased in Jenkins 2, represents a significant new concept of what a “project” and “build” could be. Jenkins plugin developers need to understand how to best integrate into the new ecosystem: from the minimum effort to a fresh design. Learn the details and recommendations.

## description

Have you written a Jenkins plugin, or helped to maintain one, or are planning to write one? If so, you need to understand where the Pipeline feature might fit into your plugin’s design.

The bare minimum of being “Pipeline-compatible” is that the plugin’s features can be used in a way analogous to their use in traditional Jenkins projects. Learn about the critical APIs that make this possible, and the accompanying restrictions needed due to both the “durability” and greater flexibility of Pipeline builds.

More sophisticated plugins can use Pipeline-specific APIs, mainly to define new “steps”. See the options available and the reasons why you would—or would not—need to add this dependency. Learn the advantages and disadvantages of special DSL additions and libraries.

You will also get an overview of plugin features which do not _need_ to be ported to Pipeline because there is already a way to accomplish the goal without them. This can help you judge whether a new development effort will pay off or whether the time would be better spent documenting a different usage mode.

Whatever implementation choice you make, see how the Jenkins test harnesses can be used to prove smooth operation of the result.

## Why Support Pipeline?

* the centerpiece of Jenkins 2.0, needed for ”CI/CD as code”
* plugin integrations can be leveraged for much richer workflow options
* but a conceptual shift, needs a little extra effort

## Agenda

* overview of special requirements for Pipeline compatibility in plugins
* how to convert a plugin working in freestyle to work the same in Pipeline
* developing special support for Pipeline
* higher-level Groovy syntax

# How Pipeline Differs

## Your workflow as code, not UI

* traditional Jenkins “freestyle” projects are _static_: configured via UI
* but a Pipeline `Jenkinsfile` is a _dynamic_ script, written in a Groovy DSL
* so plugin features that perform logic are unnecessary
    * send mail only if the build failed → `post {failure {…}}`
    * expand build environment variables in this URL → `"http://server/${var}/"`
    * retry up to three times → `retry(3) {…}`
    * anything complicated → helper functions, libraries
* metadata about what steps a _job_ runs is not reliably available
    * only what steps a _build_ did run in the past (incl. actual args)

## Free execution order

* freestyle projects are not so free; roughly:
    * allocate executor & workspace from (1) agent
    * set up build wrappers
    * check out SCM (or not)
    * run build steps, halting on failure
    * run recorders (0-1 each)
    * tear down wrappers
    * run notifiers (0-1 each)
* Pipeline projects can do any of the above
    * in any order
    * never, once, or many times
    * in parallel
    * whether or not previous steps failed

## Durability & asynchronous activity

* freestyle builds tie up on executor and do not survive Jenkins restart
* Pipeline builds
    * use as many executors as there are `node {…}` blocks active
    * can run across restarts
        * any state must be safely serializable to disk
    * might await user input, external events, etc. indefinitely

# Minimum Compliance

[Plugin Developer Guide](https://github.com/jenkinsci/pipeline-plugin/blob/master/DEVGUIDE.md)

## Jenkins core APIs friendly to Pipeline

* `SimpleBuildStep` (builders, publishers)
* `SimpleBuildWrapper` (wrappers)
* some signatures of `SCM`, `Trigger`, `JobProperty`, etc.
* various core “baselines” needed, typically 1.580.x+
    * good time to use the 2.x Maven parent POM

## Removing assumptions

* `AbstractProject` → `Job` (& some specialized interfaces)
* `AbstractBuild` → `Run` (ditto)
* could run on multiple nodes in one build
* could be multiple SCMs checked out in one build
* do not know list of build steps ahead of time
* build as a whole might succeed even if this step failed
* different variables might be in scope at different points in a build
* `SimpleBuildWrapper`: state must be `Serializable`

## DSL binding & **Pipeline Syntax** integration

* add a `@Symbol`
* use `@DataBoundSetter` in addition to `@DataBoundConstructor` for defaultable fields
* package nested config into `Describable`s with their own `@Symbol`s
* use Credentials API to manage secrets
* otherwise the usual Jelly UI, all interoperable with freestyle

## Anticipating “CD-as-code”

* treat parameters as constants (scripts can interpolate variables as needed)
* avoid mandatory global configuration
    * each team can operate autonomously using just `Jenkinsfile`
    * shared config can come from folder properties, libraries, `load`ed data…

# Testing

## Interactive tests

* `mvn hpi:run`
* try copying **Pipeline Syntax**, pasting into `Jenkinsfile`, & running

## Automated tests

* use `JenkinsRule` to set up temporary environment
* test deps on `workflow-job`, `workflow-cps`, `workflow-basic-steps`, `workflow-durable-task-step`
* create a `WorkflowJob` w/ a `CpsFlowDefinition`, try running builds
* `StepConfigTester` to check basics of databinding
  * `SnippetizerTester` for advanced checks
* `SemaphoreStep` to simulate `input`, Jenkins restarts, concurrency, &c.

# Demo: `SimpleBuildStep` conversion

[Full patch](https://github.com/jglick/wfdev/compare/pipeline)

# Custom Steps

[Writing Pipeline steps](https://github.com/jenkinsci/workflow-step-api-plugin/blob/master/README.md)

## Why a custom step?

* use Pipeline-specific APIs (e.g., decorate “flow graph”)
* asynchronous (e.g., `input`)
* wrappers running body >1 times (e.g., `retry`)
* limitations in core interfaces (e.g., environment variable handling)
* freestyle configuration very inappropriate for Pipeline

## Pieces you need

* `Step`: the _definition_ of what to run
    * mostly interchangeable with one Groovy function call
* `StepDescriptor`: the _kind_ of step and its metadata (singleton)
* `StepExecution`: what is happening at runtime
    * consider `transient`, `onResume`, `serialVersionUID`, `readResolve`
    * convenience forms for “quick” steps
* `config.jelly` UI, `help-something.html`, `FormValidation doFillSomethingItems`, etc.
    * allows **Pipeline Syntax** to offer “live” examples

## Dealing with asynchrony

* `start` method happens in “CPS VM” thread
    * _must be quick_: this is coöperative multitasking
* use background threads for anything else
    * notify the engine when step completes or fails
    * engine notifies you when step is interrupted

## Fun with block-scoped steps

* run an body `{…}` 0, 1, or more times
    * asynchronous notification when body ends, may return same result
* set environment variables for nested steps
* adjust console output
    * though colors or hyperlinks not supported in Blue Ocean
* define alternate workspaces or pass down any other “context”

## Demo: block-scoped step

# DSLs & Libraries

## Defining global variables

* `GlobalVariable` extension point: predefine symbol in every build
* can have methods & JavaBeans-style properties
    * not like steps: no environment, no asynchronous mode
* may be stateful
* currently requires `workflow-cps` dep; use sparingly

## DSL extensions

* some `GlobalVariable`s load special DSLs written in Groovy
* generally incompatible with Declarative Pipeline
* avoid

## Pipeline libraries

* no need to write a plugin at all! share on GitHub
* if “trusted”, can access Jenkins internal APIs, or `@Grab` Java libraries
* can be opinionated & complement plugin-provided steps
