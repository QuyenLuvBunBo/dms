package vn.edu.hust.dms.support;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.hust.dms.common.error.BusinessRuleViolationException;
import vn.edu.hust.dms.common.error.ConflictException;
import vn.edu.hust.dms.common.error.InvalidRequestException;
import vn.edu.hust.dms.common.error.InvalidStateTransitionException;
import vn.edu.hust.dms.common.error.NotFoundException;

import java.util.Map;

/**
 * Test-only endpoints that throw each kind of error, so the advice can be checked through real
 * HTTP. Lives in the test tree; it is picked up because tests share the scanned base package.
 */
@RestController
@RequestMapping("/api/test/errors")
public class ErrorProbeController {

    public record Payload(@NotBlank String name, @Min(1) int quantity) {
    }

    @GetMapping("/conflict")
    public void conflict() {
        throw new InvalidStateTransitionException("Cannot confirm a registration in status EXPIRED");
    }

    @GetMapping("/resource-conflict")
    public void resourceConflict() {
        throw new ConflictException("Room 301 has occupied beds");
    }

    @GetMapping("/rule")
    public void rule() {
        throw new BusinessRuleViolationException("BR-03", "Cannot hold a bed in a FEMALE room for a MALE student");
    }

    @GetMapping("/invalid-request")
    public void invalidRequest() {
        throw new InvalidRequestException("The room type belongs to another building");
    }

    @GetMapping("/not-found")
    public void notFound() {
        throw new NotFoundException("Registration 42 not found");
    }

    @GetMapping("/data-integrity")
    public void dataIntegrity() {
        throw new DataIntegrityViolationException("Duplicate entry 'B6' for key 'buildings.uk_buildings_code'");
    }

    @GetMapping("/unexpected")
    public void unexpected() {
        throw new IllegalStateException("boom: internal detail that must not leak");
    }

    @PostMapping("/validated")
    public Map<String, Object> validated(@Valid @RequestBody Payload body) {
        return Map.of("name", body.name(), "quantity", body.quantity());
    }
}
