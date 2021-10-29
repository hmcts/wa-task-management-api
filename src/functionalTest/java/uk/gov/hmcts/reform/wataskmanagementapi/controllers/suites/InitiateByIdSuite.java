package uk.gov.hmcts.reform.wataskmanagementapi.controllers.suites;

import org.junit.Test;
import org.junit.experimental.ParallelComputer;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.PostTaskInitiateByIdControllerTest;

import static org.junit.Assert.assertTrue;

public class InitiateByIdSuite {
    boolean shouldRunTestInParallel = Boolean.parseBoolean(System.getenv("RUN_TESTS_IN_PARALLEL"));


    @Test
    public void runInParallel() {
        Class[] cls = {PostTaskInitiateByIdControllerTest.class};

        Result result = shouldRunTestInParallel
            ? JUnitCore.runClasses(ParallelComputer.methods(), cls)
            : JUnitCore.runClasses(cls);

        String failures = "";

        if (!result.wasSuccessful()) {
            System.out.print("Tests failed with " + result.getFailureCount() + " error(s).\n");
            for (Failure failure : result.getFailures()) {
                System.out.println(failure.toString() + "\n");
                failures += failure + "\n";
            }
        }

        assertTrue(failures, result.wasSuccessful());

    }
}
