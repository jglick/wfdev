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
import org.jenkinsci.plugins.workflow.steps.CoreStep;
import org.jenkinsci.plugins.workflow.steps.StepConfigTester;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.Rule;
import org.jvnet.hudson.test.BuildWatcher;
import org.jvnet.hudson.test.JenkinsRule;

public class HelloWorldBuilderTest {

    @ClassRule
    public static BuildWatcher buildWatcher = new BuildWatcher();

    @Rule
    public JenkinsRule r = new JenkinsRule();

    @Test
    public void smokes() throws Exception {
        WorkflowJob p = r.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition("node {greet 'Jesse'}", true));
        r.assertLogContains("Hello, Jesse!", r.buildAndAssertSuccess(p));
        p.setDefinition(new CpsFlowDefinition("node {greet name: 'Jesse', useFrench: true}", true));
        r.assertLogContains("Bonjour, Jesse!", r.buildAndAssertSuccess(p));
    }

    @Test
    public void ui() throws Exception {
        StepConfigTester t = new StepConfigTester(r);
        HelloWorldBuilder b = new HelloWorldBuilder("Name");
        r.assertEqualDataBoundBeans(new CoreStep(b), t.configRoundTrip(new CoreStep(b)));
        b.setUseFrench(true);
        r.assertEqualDataBoundBeans(new CoreStep(b), t.configRoundTrip(new CoreStep(b)));
    }

}
