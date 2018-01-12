package br.com.makeshiftonjava.productapiservice.application;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.security.Principal;

@RestController
public class ProductApiController {

    private static final Logger LOG = LoggerFactory.getLogger(ProductApiController.class);

    private final RestTemplate restTemplate;
    private final LoadBalancerClient loadBalancer;

    @Autowired
    public ProductApiController(RestTemplate restTemplate, LoadBalancerClient loadBalancer) {
        this.restTemplate = restTemplate;
        this.loadBalancer = loadBalancer;
    }

    @PreAuthorize("hasAuthority('FOO_READ')")
    @RequestMapping(value = "/{productId}", method = RequestMethod.GET)
    @HystrixCommand(fallbackMethod = "defaultProductComposite")
    public ResponseEntity<String> getProductComposite(
            @PathVariable Long productId,
            @RequestHeader(value = "Authorization") String authorizationHeader,
            Principal currentUser) {

        LOG.info("ProductApi: User={}, Auth={}, called with productId={}", currentUser.getName(), authorizationHeader, productId);
        URI uri = loadBalancer.choose("composite-product-service").getUri();
        String url = uri.toString() + "/product-composite/" + productId;
        LOG.debug("GetProductComposite from URL: {}", url);

        ResponseEntity<String> result = restTemplate.getForEntity(url, String.class);
        LOG.info("GetProductComposite http-status: {}", result.getStatusCode());
        LOG.debug("GetProductComposite body: {}", result.getBody());

        return result;
    }

    /**
     * Fallback method for getProductComposite()
     *
     * @param productId
     * @return
     */
    public ResponseEntity<String> defaultProductComposite(
            @PathVariable Long productId,
            @RequestHeader(value="Authorization") String authorizationHeader,
            Principal currentUser) {

        LOG.warn("Using fallback method for product-composite-service. User={}, Auth={}, called with productId={}", currentUser.getName(), authorizationHeader, productId);
        return new ResponseEntity<String>("", HttpStatus.BAD_GATEWAY);
    }
}
