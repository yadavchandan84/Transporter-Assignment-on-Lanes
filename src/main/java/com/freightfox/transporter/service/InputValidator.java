package com.freightfox.transporter.service;

import com.freightfox.transporter.dto.InputRequest;
import com.freightfox.transporter.dto.LaneDto;
import com.freightfox.transporter.dto.LaneQuoteDto;
import com.freightfox.transporter.dto.TransporterDto;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Cross-field business rules that Bean Validation cannot express. Collects every error.
 * Null elements are skipped; Bean Validation reports those.
 */
@Component
public class InputValidator {

    public List<String> validate(InputRequest request) {
        List<String> errors = new ArrayList<>();
        if (request == null) {
            return errors;
        }

        Set<Integer> laneIds = new HashSet<>();
        Set<Integer> duplicateLanes = new LinkedHashSet<>();
        if (request.lanes() != null) {
            for (LaneDto lane : request.lanes()) {
                if (lane != null && lane.id() != null && !laneIds.add(lane.id())) {
                    duplicateLanes.add(lane.id());
                }
            }
        }
        duplicateLanes.forEach(id -> errors.add("Duplicate lane id: %d".formatted(id)));

        if (request.transporters() == null) {
            return errors;
        }
        Set<Integer> transporterIds = new HashSet<>();
        Set<Integer> duplicateTransporters = new LinkedHashSet<>();
        for (TransporterDto t : request.transporters()) {
            if (t != null && t.id() != null && !transporterIds.add(t.id())) {
                duplicateTransporters.add(t.id());
            }
        }
        duplicateTransporters.forEach(id -> errors.add("Duplicate transporter id: %d".formatted(id)));

        for (TransporterDto t : request.transporters()) {
            if (t == null || t.laneQuotes() == null) {
                continue;
            }
            Set<Integer> quoted = new HashSet<>();
            Set<Integer> duplicateQuotes = new LinkedHashSet<>();
            Set<Integer> unknown = new LinkedHashSet<>();
            for (LaneQuoteDto q : t.laneQuotes()) {
                if (q == null || q.laneId() == null) {
                    continue;
                }
                if (!laneIds.contains(q.laneId())) {
                    unknown.add(q.laneId());
                }
                if (!quoted.add(q.laneId())) {
                    duplicateQuotes.add(q.laneId());
                }
            }
            unknown.forEach(l -> errors.add("Transporter %d references unknown lane id %d".formatted(t.id(), l)));
            duplicateQuotes.forEach(l ->
                    errors.add("Transporter %d has duplicate quotes for lane id %d".formatted(t.id(), l)));
        }
        return errors;
    }
}
