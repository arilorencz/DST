package dst.ass2.aop.grading;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class LocalGradingClassRule implements TestRule {

    public void afterAll() {
        GradingData.getInstance().outputSummary();
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                try {
                    for (Description child : description.getChildren()) {
                        beforeTestExecution(child);
                    }

                    base.evaluate();
                } finally {
                    afterAll();
                }
            }
        };
    }

    public void beforeTestExecution(Description context) throws Exception {

        if (context.getAnnotation(GitHubClassroomGrading.class) != null) {
            String methodId = getMethodId(context);
            String className = context.getClassName();
            String methodName = context.getMethodName();
            int score = context.getAnnotation(GitHubClassroomGrading.class).maxScore();

            GradingData.getInstance().addTestMethod(methodId, className, methodName, score);
        }
    }


    private String getMethodId(Description descr) {
        return descr.getClassName() + "." + descr.getMethodName();
    }
}
