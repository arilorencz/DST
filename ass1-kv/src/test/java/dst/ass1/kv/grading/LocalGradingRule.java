package dst.ass1.kv.grading;

import org.junit.AssumptionViolatedException;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

public class LocalGradingRule extends TestWatcher {


    @Override
    protected void succeeded(Description description) {
        try {
            afterTestExecution(description, true);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    protected void failed(Throwable e, Description description) {
        try {
            afterTestExecution(description, false);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    protected void skipped(AssumptionViolatedException e, Description description) {
        try {
            afterTestExecution(description, false);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        throw e;
    }


    private GitHubClassroomGrading getGradedTestAnnotation(Description context) {
        return context.getAnnotation(GitHubClassroomGrading.class);
    }

    private String getMethodId(Description descr) {
        return descr.getClassName() + "." + descr.getMethodName();
    }


    public void afterTestExecution(Description context, boolean testPassed) throws Exception {
        GitHubClassroomGrading gradedTest = getGradedTestAnnotation(context);
        if (gradedTest != null) {
            String methodId = getMethodId(context);

            if (testPassed) {
                GradingData.getInstance().markTestMethodPassed(methodId);
            }
        }
    }
}
