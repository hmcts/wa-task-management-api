package uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.verifiers;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.VerificationData;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.VerificationResult;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.Assignment;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Component
public class DateTimeVerifier implements Verifier<VerificationData> {

    public DateTimeVerifier() {
        //no-op constructor
    }

    @Override
    public VerificationResult verify(VerificationData verificationData) {

        canVerify(verificationData);

        return new VerificationResult(
            hasEndTimePermission(verificationData.getRoleAssignment())
            && hasBeginTimePermission(verificationData.getRoleAssignment()));
    }

    private boolean hasEndTimePermission(Assignment roleAssignment) {
        LocalDateTime endTime = roleAssignment.getEndTime();

        ZoneId zoneId = ZoneId.of("Europe/London");
        ZonedDateTime endTimeLondonTime = endTime.atZone(zoneId);
        ZonedDateTime currentDateTimeLondonTime = ZonedDateTime.now(zoneId);

        return currentDateTimeLondonTime.isBefore(endTimeLondonTime);
    }

    private boolean hasBeginTimePermission(Assignment roleAssignment) {
        LocalDateTime beginTime = roleAssignment.getBeginTime();

        ZoneId zoneId = ZoneId.of("Europe/London");
        ZonedDateTime beginTimeLondonTime = beginTime.atZone(zoneId);
        ZonedDateTime currentDateTimeLondonTime = ZonedDateTime.now(zoneId);

        return currentDateTimeLondonTime.isAfter(beginTimeLondonTime);
    }


}
