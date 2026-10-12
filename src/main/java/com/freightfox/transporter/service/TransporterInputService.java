package com.freightfox.transporter.service;

import com.freightfox.transporter.dto.InputRequest;
import com.freightfox.transporter.dto.LaneDto;
import com.freightfox.transporter.dto.LaneQuoteDto;
import com.freightfox.transporter.dto.StoredInputResponse;
import com.freightfox.transporter.dto.TransporterDto;
import com.freightfox.transporter.entity.LaneEntity;
import com.freightfox.transporter.entity.TransporterEntity;
import com.freightfox.transporter.exception.InvalidInputException;
import com.freightfox.transporter.exception.NoInputDataException;
import com.freightfox.transporter.repository.LaneQuoteRepository;
import com.freightfox.transporter.repository.LaneRepository;
import com.freightfox.transporter.repository.TransporterRepository;
import jakarta.persistence.EntityManager;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Stores and reads back the (single) current dataset. */
@Service
public class TransporterInputService {

    private final LaneRepository laneRepository;
    private final TransporterRepository transporterRepository;
    private final LaneQuoteRepository laneQuoteRepository;
    private final InputValidator validator;
    private final EntityManager entityManager;

    public TransporterInputService(LaneRepository laneRepository, TransporterRepository transporterRepository,
                                   LaneQuoteRepository laneQuoteRepository, InputValidator validator,
                                   EntityManager entityManager) {
        this.laneRepository = laneRepository;
        this.transporterRepository = transporterRepository;
        this.laneQuoteRepository = laneQuoteRepository;
        this.validator = validator;
        this.entityManager = entityManager;
    }

    /** Validates and atomically replaces the stored dataset. */
    @Transactional
    public void replaceInput(InputRequest request) {
        List<String> errors = validator.validate(request);
        if (!errors.isEmpty()) {
            throw new InvalidInputException(errors);
        }
        laneQuoteRepository.deleteAllInBatch();
        transporterRepository.deleteAllInBatch();
        laneRepository.deleteAllInBatch();
        entityManager.flush();
        entityManager.clear();

        // Ids are client-assigned, so save() merges; keep the managed instances it returns.
        Map<Integer, LaneEntity> lanes = new HashMap<>();
        laneRepository.saveAll(request.lanes().stream()
                        .map(dto -> new LaneEntity(dto.id(), dto.origin().strip(), dto.destination().strip()))
                        .toList())
                .forEach(l -> lanes.put(l.getId(), l));

        List<TransporterEntity> transporters = request.transporters().stream().map(dto -> {
            TransporterEntity t = new TransporterEntity(dto.id(), dto.name().strip());
            for (LaneQuoteDto q : dto.laneQuotes()) {
                t.addQuote(lanes.get(q.laneId()), q.quote());
            }
            return t;
        }).toList();
        transporterRepository.saveAll(transporters);
    }

    /** The stored dataset, sorted by ids. */
    @Transactional(readOnly = true)
    public StoredInputResponse getInput() {
        List<LaneEntity> lanes = laneRepository.findAll(Sort.by("id"));
        if (lanes.isEmpty()) {
            throw new NoInputDataException();
        }
        List<LaneDto> laneDtos = lanes.stream()
                .map(l -> new LaneDto(l.getId(), l.getOrigin(), l.getDestination()))
                .toList();
        List<TransporterDto> transporterDtos = transporterRepository.findAllWithQuotes().stream()
                .sorted(Comparator.comparing(TransporterEntity::getId))
                .map(t -> new TransporterDto(t.getId(), t.getName(), t.getLaneQuotes().stream()
                        .map(q -> new LaneQuoteDto(q.getLane().getId(), q.getQuote()))
                        .sorted(Comparator.comparing(LaneQuoteDto::laneId))
                        .toList()))
                .toList();
        return new StoredInputResponse("success", laneDtos, transporterDtos);
    }
}
