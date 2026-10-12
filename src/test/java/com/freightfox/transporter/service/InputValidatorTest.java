package com.freightfox.transporter.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.freightfox.transporter.dto.InputRequest;
import com.freightfox.transporter.dto.LaneDto;
import com.freightfox.transporter.dto.LaneQuoteDto;
import com.freightfox.transporter.dto.TransporterDto;
import java.util.List;
import org.junit.jupiter.api.Test;

class InputValidatorTest {

    private final InputValidator validator = new InputValidator();

    private static LaneDto lane(int id) {
        return new LaneDto(id, "A" + id, "B" + id);
    }

    private static LaneQuoteDto quote(int laneId, long q) {
        return new LaneQuoteDto(laneId, q);
    }

    private static TransporterDto transporter(int id, LaneQuoteDto... quotes) {
        return new TransporterDto(id, "T" + id, List.of(quotes));
    }

    @Test
    void validInput_noErrors() {
        InputRequest r = new InputRequest(List.of(lane(1), lane(2)),
                List.of(transporter(1, quote(1, 10), quote(2, 20)), transporter(2)));
        assertThat(validator.validate(r)).isEmpty();
    }

    @Test
    void duplicateLaneId() {
        InputRequest r = new InputRequest(List.of(lane(1), lane(1)), List.of(transporter(1, quote(1, 10))));
        assertThat(validator.validate(r)).containsExactly("Duplicate lane id: 1");
    }

    @Test
    void duplicateTransporterId() {
        InputRequest r = new InputRequest(List.of(lane(1)),
                List.of(transporter(1, quote(1, 10)), transporter(1, quote(1, 12))));
        assertThat(validator.validate(r)).containsExactly("Duplicate transporter id: 1");
    }

    @Test
    void unknownLaneReference() {
        InputRequest r = new InputRequest(List.of(lane(1)), List.of(transporter(2, quote(9, 10))));
        assertThat(validator.validate(r)).containsExactly("Transporter 2 references unknown lane id 9");
    }

    @Test
    void duplicateLaneQuoteWithinTransporter() {
        InputRequest r = new InputRequest(List.of(lane(1)), List.of(transporter(3, quote(1, 10), quote(1, 11))));
        assertThat(validator.validate(r)).containsExactly("Transporter 3 has duplicate quotes for lane id 1");
    }

    @Test
    void multipleErrorsCollectedTogether() {
        InputRequest r = new InputRequest(List.of(lane(1), lane(1), lane(2)),
                List.of(transporter(1, quote(5, 10), quote(2, 1), quote(2, 3)), transporter(1)));
        assertThat(validator.validate(r)).containsExactly(
                "Duplicate lane id: 1",
                "Duplicate transporter id: 1",
                "Transporter 1 references unknown lane id 5",
                "Transporter 1 has duplicate quotes for lane id 2");
    }
}
