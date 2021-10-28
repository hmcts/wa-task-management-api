package uk.gov.hmcts.reform.wataskmanagementapi.controllers.suites;

import org.junit.Test;
import org.junit.experimental.ParallelComputer;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.PostTaskForSearchCompletionControllerTest;

import static org.junit.Assert.assertTrue;

public class SearchForCompletableSuite {
    @Test
    public void runInParallel() {
        Class[] cls = {PostTaskForSearchCompletionControllerTest.class};

        // Parallel among classes
        Result result = JUnitCore.runClasses(ParallelComputer.classes(), cls);

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
