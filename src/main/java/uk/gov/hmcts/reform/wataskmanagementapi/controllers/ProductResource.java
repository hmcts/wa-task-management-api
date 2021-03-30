package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zalando.problem.Status;
import org.zalando.problem.violations.ConstraintViolationProblem;
import org.zalando.problem.violations.Violation;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.Product;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.ProductBody;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.InsufficientPermissionsException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.TaskNotFoundProblem;

import java.net.URI;
import java.util.List;
import javax.validation.ConstraintViolationException;
import javax.validation.Valid;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

@RestController
@RequestMapping("/products")
class ProductResource {

    @RequestMapping(method = GET, value = "/{productId}", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<Product> getProduct(@PathVariable String productId) {

        switch (productId) {
            case "p1":

                //Internal exception that has to be mapped to a Problem in ControllerAdvice
                throw new InsufficientPermissionsException("You can't do this!!");

            case "p2":

                //Could point to github location for future documentation
                final String typeVa = "https://task-manager/problem/constraint-violation";
                final URI typeUri = URI.create(typeVa);

                List<Violation> violations =
                    ImmutableList.of(new Violation("something", "you need to provide something"));
                throw new ConstraintViolationProblem(
                    typeUri,
                    Status.BAD_REQUEST,
                    violations
                );

            case "p3":
                //Custom problem
                throw new TaskNotFoundProblem(
                    String.format("Could not find Task with id '%s'", productId)
                );

            case "p4":
                throw new RuntimeException("This is a generic exception");
            case "p5":
                throw new ConstraintViolationException(
                    "overridden exception",
                    ImmutableSet.of()
                );
            default:
                System.out.println("Default . . . returning success.");
        }

        return ResponseEntity.ok(new Product());
    }

    @RequestMapping(method = PUT, value = "/{productId}", consumes = APPLICATION_JSON_VALUE)
    public Product updateProduct(String productId, Product product) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    @RequestMapping(method = POST, value = "/do-something-with-body", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> getProducts(@Valid @RequestBody ProductBody productBody) {

        System.out.println("Received: " + productBody);

        return ResponseEntity.ok()
            .build();

    }

}
