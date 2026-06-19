package com.hepl.product.Controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.hepl.product.Payload.Response.ApiResponse;
import com.hepl.product.Repository.CompositeItemRepository;
import com.hepl.product.Repository.ProductRepository;
import com.hepl.product.model.CompositeItem;
import com.hepl.product.model.CompositeItemComponent;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/composite-items")
@RequiredArgsConstructor
public class CompositeItemController {

    private final CompositeItemRepository compositeItemRepo;
    private final ProductRepository productRepo;

    // ── GET all ──────────────────────────────────────────────────────────────
    @GetMapping
    public ResponseEntity<ApiResponse> getAll() {
        return ResponseEntity.ok(new ApiResponse(HttpStatus.OK.value(), "Success",
                compositeItemRepo.findAllByOrderByCreatedAtDesc()));
    }

    // ── GET single ───────────────────────────────────────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse> getById(@PathVariable Long id) {
        return compositeItemRepo.findById(id)
                .map(item -> ResponseEntity.ok(new ApiResponse(HttpStatus.OK.value(), "Success", item)))
                .orElse(ResponseEntity.notFound().build());
    }

    // ── CREATE ───────────────────────────────────────────────────────────────
    @PostMapping
    public ResponseEntity<ApiResponse> create(@RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        if (name == null || name.isBlank())
            return ResponseEntity.badRequest().body(new ApiResponse(400, "Name is required", null));
        if (compositeItemRepo.existsByName(name.trim()))
            return ResponseEntity.badRequest().body(new ApiResponse(400, "Composite item name already exists", null));

        CompositeItem item = new CompositeItem();
        item.setName(name.trim());
        item.setDescription(body.getOrDefault("description", "").toString());

        // Attach components if provided in same request
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> comps = (List<Map<String, Object>>) body.get("components");
        if (comps != null) {
            for (Map<String, Object> c : comps) {
                Long productId = Long.valueOf(c.get("productId").toString());
                int qty = Integer.parseInt(c.getOrDefault("quantity", 1).toString());
                productRepo.findById(productId).ifPresent(product -> {
                    CompositeItemComponent comp = new CompositeItemComponent();
                    comp.setCompositeItem(item);
                    comp.setProduct(product);
                    comp.setQuantity(qty);
                    item.getComponents().add(comp);
                });
            }
        }

        CompositeItem saved = compositeItemRepo.save(item);
        return ResponseEntity.ok(new ApiResponse(HttpStatus.CREATED.value(), "Created", saved));
    }

    // ── UPDATE ───────────────────────────────────────────────────────────────
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse> update(@PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        return compositeItemRepo.findById(id).map(item -> {
            String name = (String) body.get("name");
            if (name != null && !name.isBlank()) {
                if (compositeItemRepo.existsByNameAndIdNot(name.trim(), id))
                    return ResponseEntity.badRequest()
                            .body(new ApiResponse(400, "Composite item name already exists", null));
                item.setName(name.trim());
            }
            if (body.containsKey("description"))
                item.setDescription(body.get("description").toString());

            // Replace components if provided
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> comps = (List<Map<String, Object>>) body.get("components");
            if (comps != null) {
                item.getComponents().clear();
                compositeItemRepo.save(item); // flush removals first

                for (Map<String, Object> c : comps) {
                    Long productId = Long.valueOf(c.get("productId").toString());
                    int qty = Integer.parseInt(c.getOrDefault("quantity", 1).toString());
                    productRepo.findById(productId).ifPresent(product -> {
                        CompositeItemComponent comp = new CompositeItemComponent();
                        comp.setCompositeItem(item);
                        comp.setProduct(product);
                        comp.setQuantity(qty);
                        item.getComponents().add(comp);
                    });
                }
            }

            return ResponseEntity.ok(new ApiResponse(HttpStatus.OK.value(), "Updated",
                    compositeItemRepo.save(item)));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ── DELETE ───────────────────────────────────────────────────────────────
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> delete(@PathVariable Long id) {
        if (!compositeItemRepo.existsById(id))
            return ResponseEntity.notFound().build();
        compositeItemRepo.deleteById(id);
        return ResponseEntity.ok(new ApiResponse(HttpStatus.OK.value(), "Deleted", null));
    }

    // ── SAVE COMPONENTS (replace all) ────────────────────────────────────────
    @PostMapping("/{id}/components")
    public ResponseEntity<ApiResponse> saveComponents(@PathVariable Long id,
            @RequestBody List<Map<String, Object>> components) {
        return compositeItemRepo.findById(id).map(item -> {
            item.getComponents().clear();
            compositeItemRepo.save(item);

            for (Map<String, Object> c : components) {
                Long productId = Long.valueOf(c.get("productId").toString());
                int qty = Integer.parseInt(c.getOrDefault("quantity", 1).toString());
                productRepo.findById(productId).ifPresent(product -> {
                    CompositeItemComponent comp = new CompositeItemComponent();
                    comp.setCompositeItem(item);
                    comp.setProduct(product);
                    comp.setQuantity(qty);
                    item.getComponents().add(comp);
                });
            }

            return ResponseEntity.ok(new ApiResponse(HttpStatus.OK.value(), "Components saved",
                    compositeItemRepo.save(item)));
        }).orElse(ResponseEntity.notFound().build());
    }
}
