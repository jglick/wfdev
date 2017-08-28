/*
 * The MIT License
 *
 * Copyright 2017 CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package demo;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.test.steps.SemaphoreStep;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.Rule;
import org.junit.runners.model.Statement;
import org.jvnet.hudson.test.BuildWatcher;
import org.jvnet.hudson.test.RestartableJenkinsRule;

public class CountGreetingsStepTest {

    @ClassRule public static BuildWatcher buildWatcher = new BuildWatcher();
    @Rule public RestartableJenkinsRule rr = new RestartableJenkinsRule();

    @Test public void smokes() throws Exception {
        rr.addStep(new Statement() {
            @Override
            public void evaluate() throws Throwable {
                WorkflowJob p = rr.j.createProject(WorkflowJob.class, "p");
                p.setDefinition(new CpsFlowDefinition("def count = countGreetings('Jesse') {def name = semaphore 'input'; echo(/Hello, $name!/); semaphore 'wait'; echo(/$name! How are you, $name?/)}; echo(/greeted $count times/)", true));
                WorkflowRun b1 = p.scheduleBuild2(0).waitForStart();
                SemaphoreStep.waitForStart("input/1", b1);
            }
        });
        rr.addStep(new Statement() {
            @Override
            public void evaluate() throws Throwable {
                WorkflowRun b1 = rr.j.jenkins.getItemByFullName("p", WorkflowJob.class).getBuildByNumber(1);
                SemaphoreStep.success("input/1", "Jesse");
                SemaphoreStep.waitForStart("wait/1", b1);
            }
        });
        rr.addStep(new Statement() {
            @Override
            public void evaluate() throws Throwable {
                WorkflowRun b1 = rr.j.jenkins.getItemByFullName("p", WorkflowJob.class).getBuildByNumber(1);
                SemaphoreStep.success("wait/1", null);
                rr.j.waitForCompletion(b1);
                rr.j.assertLogContains("greeted 3 times", b1);
            }
        });
    }

}
