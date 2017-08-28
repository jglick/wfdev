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

import hudson.Extension;
import hudson.MarkupText;
import hudson.console.ConsoleAnnotator;
import hudson.console.ConsoleLogFilter;
import hudson.console.ConsoleNote;
import hudson.console.LineTransformationOutputStream;
import hudson.model.Run;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jenkinsci.plugins.workflow.steps.BodyExecutionCallback;
import org.jenkinsci.plugins.workflow.steps.BodyInvoker;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;

public class CountGreetingsStep extends Step {

    private final String name;

    @DataBoundConstructor
    public CountGreetingsStep(String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new Execution(context, name);
    }

    private static class Execution extends StepExecution {

        private final String name;
        private int count;

        Execution(StepContext context, String name) {
            super(context);
            this.name = name;
        }

        @Override
        public boolean start() throws Exception {
            getContext().newBodyInvoker().
                withContext(BodyInvoker.mergeConsoleLogFilters(getContext().get(ConsoleLogFilter.class), new LogFilter(this))).
                withCallback(new Tallier(this)).
                start();
            return false;
        }

        @Override
        public void stop(Throwable cause) throws Exception {}

        private static final long serialVersionUID = 1;

    }

    private static class LogFilter extends ConsoleLogFilter implements Serializable {

        private final Execution exec;

        LogFilter(Execution exec) {
            this.exec = exec;
        }

        @SuppressWarnings("rawtypes")
        @Override
        public OutputStream decorateLogger(Run build, final OutputStream base) throws IOException, InterruptedException {
            return new LineTransformationOutputStream() {
                Pattern p = Pattern.compile(Pattern.quote(exec.name));
                @Override
                protected void eol(byte[] b, int len) throws IOException {
                    Matcher m = p.matcher(new String(b, 0, len, StandardCharsets.UTF_8));
                    StringBuffer buf = new StringBuffer();
                    while (m.find()) {
                        m.appendReplacement(buf, Boldface.encodeTo(exec.name));
                        exec.count++;
                        exec.getContext().saveState();
                    }
                    m.appendTail(buf);
                    base.write(buf.toString().getBytes(StandardCharsets.UTF_8));
                }
            };
        }

        private static final long serialVersionUID = 1;

    }

    private static class Boldface extends ConsoleNote<Object> {

        private final int length;

        private Boldface(int length) {
            this.length = length;
        }

        @Override
        public ConsoleAnnotator<?> annotate(Object context, MarkupText text, int charPos) {
            text.addMarkup(charPos, charPos + length, "<b>", "</b>");
            return null;
        }

        static String encodeTo(String text) throws IOException {
            return new Boldface(text.length()).encode() + text;
        }

        private static final long serialVersionUID = 1;

    }

    private static class Tallier extends BodyExecutionCallback {

        private final Execution exec;

        Tallier(Execution exec) {
            this.exec = exec;
        }

        @Override
        public void onSuccess(StepContext context, Object result) {
            context.onSuccess(exec.count);
        }

        @Override
        public void onFailure(StepContext context, Throwable t) {
            context.onFailure(t);
        }

        private static final long serialVersionUID = 1;

    }

    @Extension
    public static class DescriptorImpl extends StepDescriptor {

        @Override
        public String getFunctionName() {
            return "countGreetings";
        }

        @Override
        public String getDisplayName() {
            return "Count Greetings";
        }

        @Override
        public boolean takesImplicitBlockArgument() {
            return true;
        }

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return Collections.emptySet();
        }

    }

}
