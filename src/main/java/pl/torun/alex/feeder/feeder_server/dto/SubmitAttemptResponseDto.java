package pl.torun.alex.feeder.feeder_server.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmitAttemptResponseDto {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long sessionId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long nextAttemptId;

    private Integer attemptNumber;
    private Integer remainingAttempts;
    /** True when all 5 attempts are done and the session is awaiting user confirmation. */
    private boolean readyForConfirmation;
}

