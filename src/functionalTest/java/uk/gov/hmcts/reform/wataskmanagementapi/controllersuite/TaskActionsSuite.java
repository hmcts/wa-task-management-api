package uk.gov.hmcts.reform.wataskmanagementapi.controllersuite;

import org.junit.Test;
import org.junit.experimental.ParallelComputer;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.suites.AssignByIdSuite;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.suites.CancelByIdSuite;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.suites.ClaimByIdSuite;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.suites.CompleteByIdSuite;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.suites.DeleteTaskByIdSuite;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.suites.GetTaskByIdSuite;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.suites.UnclaimSuite;

import static org.junit.Assert.assertTrue;

public class TaskActionsSuite {

    boolean shouldRunTestInParallel = Boolean.parseBoolean(System.getenv("RUN_TESTS_IN_PARALLEL"));

    @Test
    public void runInParallel() {

        System.out.println("======================");
        System.out.println(shouldRunTestInParallel);
        System.out.println("======================");
        Class[] cls = {
            AssignByIdSuite.class,
            CancelByIdSuite.class,
            ClaimByIdSuite.class,
            CompleteByIdSuite.class,
            DeleteTaskByIdSuite.class,
            GetTaskByIdSuite.class,
            UnclaimSuite.class
        };

        Result result = shouldRunTestInParallel
            ? JUnitCore.runClasses(ParallelComputer.classes(), cls)
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
