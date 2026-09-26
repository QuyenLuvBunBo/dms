package vn.edu.hust.dms.support;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.hust.dms.common.error.BusinessRuleViolationException;
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
        throw new InvalidStateTransitionException("Cannot approve an application in status DRAFT");
    }

    @GetMapping("/rule")
    public void rule() {
        throw new BusinessRuleViolationException("BR-03", "Cannot assign a MALE student to a FEMALE room");
    }

    @GetMapping("/not-found")
    public void notFound() {
        throw new NotFoundException("Application 42 not found");
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
