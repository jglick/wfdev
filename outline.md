# Plugin Development for Pipeline

## abstract

The Pipeline system, showcased in Jenkins 2, represents a significant new concept of what a “project” and “build” could be. Jenkins plugin developers need to understand how to best integrate into the new ecosystem: from the minimum effort to a fresh design. Learn the details and recommendations.

## description

Have you written a Jenkins plugin, or helped to maintain one, or are planning to write one? If so, you need to understand where the Pipeline feature might fit into your plugin’s design.

The bare minimum of being “Pipeline-compatible” is that the plugin’s features can be used in a way analogous to their use in traditional Jenkins projects. Learn about the critical APIs that make this possible, and the accompanying restrictions needed due to both the “durability” and greater flexibility of Pipeline builds.

More sophisticated plugins can use Pipeline-specific APIs, mainly to define new “steps”. See the options available and the reasons why you would—or would not—need to add this dependency. Learn the advantages and disadvantages of special DSL additions and libraries.

You will also get an overview of plugin features which do not _need_ to be ported to Pipeline because there is already a way to accomplish the goal without them. This can help you judge whether a new development effort will pay off or whether the time would be better spent documenting a different usage mode.

Whatever implementation choice you make, see how the Jenkins test harnesses can be used to prove smooth operation of the result.

# How Pipeline differs

## Your workflow as code, not UI

* traditional Jenkins “freestyle” projects are configured via UI
* but a Pipeline `Jenkinsfile` is a script, written in a Groovy DSL
* so plugin features that perform logic are unnecessary
    * send mail only if the build failed → `post {failure {…}}`
    * expand build environment variables in this URL → `"http://server/${var}/"`
    * retry up to three times → `retry(3) {…}`

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
    * might await user input, external events, etc. indefinitely
