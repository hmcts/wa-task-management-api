package uk.gov.hmcts.reform.wataskmanagementapi.controllersuite;

import org.junit.Test;
import org.junit.experimental.ParallelComputer;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.suites.InitiateByIdSuite;

import static org.junit.Assert.assertTrue;

public class TaskExclusiveAccessSuite {
    @Value("${run.parallel}")
    boolean shouldRunTestInParallel;

    @Test
    public void runInParallel() {
        Class[] cls = {
            InitiateByIdSuite.class,
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
