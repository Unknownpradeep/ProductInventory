package com.hepl.product.Controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.hepl.product.Payload.Response.ApiResponse;
import com.hepl.product.Repository.CustomerRepository;
import com.hepl.product.Repository.PriceListRepository;
import com.hepl.product.Repository.ProductRepository;
import com.hepl.product.model.PriceList;
import com.hepl.product.model.PriceListEntry;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/price-lists")
@RequiredArgsConstructor
public class PriceListController {

    private final PriceListRepository priceListRepo;
    private final CustomerRepository customerRepo;
    private final ProductRepository productRepo;

    @GetMapping
    public ResponseEntity<ApiResponse> getAll() {
        return ResponseEntity.ok(new ApiResponse(HttpStatus.OK.value(), "Success",
                priceListRepo.findAllByOrderByCreatedAtDesc()));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<ApiResponse> getByCustomer(@PathVariable Long customerId) {
        return ResponseEntity.ok(new ApiResponse(HttpStatus.OK.value(), "Success",
                priceListRepo.findByCustomerId(customerId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse> create(@RequestBody Map<String, Object> body) {
        try {
            PriceList pl = buildFromBody(new PriceList(), body);
            return ResponseEntity.ok(new ApiResponse(HttpStatus.CREATED.value(), "Created",
                    priceListRepo.save(pl)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(400, e.getMessage(), null));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return priceListRepo.findById(id).map(pl -> {
            try {
                pl.getEntries().clear();
                buildFromBody(pl, body);
                return ResponseEntity.ok(new ApiResponse(HttpStatus.OK.value(), "Updated",
                        priceListRepo.save(pl)));
            } catch (Exception e) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(400, e.getMessage(), null));
            }
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> delete(@PathVariable Long id) {
        priceListRepo.deleteById(id);
        return ResponseEntity.ok(new ApiResponse(HttpStatus.OK.value(), "Deleted", null));
    }

    @SuppressWarnings("unchecked")
    private PriceList buildFromBody(PriceList pl, Map<String, Object> body) {
        pl.setName(body.get("name").toString());
        pl.setDiscountType(body.getOrDefault("discountType", "Fixed Amount").toString());

        Object custId = body.get("customerId");
        if (custId != null && !custId.toString().isBlank()) {
            customerRepo.findById(Long.valueOf(custId.toString())).ifPresent(pl::setCustomer);
        } else {
            pl.setCustomer(null);
        }

        List<Map<String, Object>> entries = (List<Map<String, Object>>) body.get("entries");
        if (entries != null) {
            for (Map<String, Object> e : entries) {
                Long productId = Long.valueOf(e.get("productId").toString());
                double customPrice = Double.parseDouble(e.get("customPrice").toString());
                productRepo.findById(productId).ifPresent(product -> {
                    PriceListEntry entry = new PriceListEntry();
                    entry.setPriceList(pl);
                    entry.setProduct(product);
                    entry.setCustomPrice(customPrice);
                    pl.getEntries().add(entry);
                });
            }
        }
        return pl;
    }
}
