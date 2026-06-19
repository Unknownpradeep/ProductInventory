package com.hepl.product.Controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.hepl.product.Payload.Response.ApiResponse;
import com.hepl.product.Repository.ItemGroupProductRepository;
import com.hepl.product.Repository.ItemGroupRepository;
import com.hepl.product.Repository.ProductRepository;
import com.hepl.product.model.ItemGroup;
import com.hepl.product.model.ItemGroupProduct;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/item-groups")
@RequiredArgsConstructor
public class ItemGroupController {

    private final ItemGroupRepository itemGroupRepo;
    private final ItemGroupProductRepository itemGroupProductRepo;
    private final ProductRepository productRepo;

    @GetMapping
    public ResponseEntity<ApiResponse> getAll() {
        return ResponseEntity.ok(new ApiResponse(HttpStatus.OK.value(), "Success",
                itemGroupRepo.findAllByOrderByCreatedAtDesc()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse> create(@RequestBody Map<String, String> body) {
        String name = body.get("name");
        if (name == null || name.isBlank())
            return ResponseEntity.badRequest().body(new ApiResponse(400, "Name is required", null));
        if (itemGroupRepo.existsByName(name.trim()))
            return ResponseEntity.badRequest().body(new ApiResponse(400, "Group name already exists", null));
        ItemGroup group = new ItemGroup();
        group.setName(name.trim());
        group.setDescription(body.getOrDefault("description", ""));
        return ResponseEntity.ok(new ApiResponse(HttpStatus.CREATED.value(), "Created", itemGroupRepo.save(group)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse> update(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return itemGroupRepo.findById(id).map(group -> {
            if (body.containsKey("name") && !body.get("name").isBlank())
                group.setName(body.get("name").trim());
            if (body.containsKey("description"))
                group.setDescription(body.get("description"));
            return ResponseEntity.ok(new ApiResponse(HttpStatus.OK.value(), "Updated", itemGroupRepo.save(group)));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> delete(@PathVariable Long id) {
        itemGroupRepo.deleteById(id);
        return ResponseEntity.ok(new ApiResponse(HttpStatus.OK.value(), "Deleted", null));
    }

    // Save all items with quantities for a group (replaces existing)
    // Body: [ { productId: 1, quantity: 5 }, ... ]
    @PostMapping("/{id}/products")
    public ResponseEntity<ApiResponse> saveProducts(@PathVariable Long id,
            @RequestBody List<Map<String, Object>> items) {
        return itemGroupRepo.findById(id).map(group -> {
            // Remove all existing items
            group.getItems().clear();
            itemGroupRepo.save(group);

            // Add new items with quantities
            for (Map<String, Object> item : items) {
                Long productId = Long.valueOf(item.get("productId").toString());
                int quantity = Integer.parseInt(item.getOrDefault("quantity", 1).toString());

                productRepo.findById(productId).ifPresent(product -> {
                    ItemGroupProduct igp = new ItemGroupProduct();
                    igp.setItemGroup(group);
                    igp.setProduct(product);
                    igp.setQuantity(quantity);
                    group.getItems().add(igp);
                });
            }

            return ResponseEntity.ok(new ApiResponse(HttpStatus.OK.value(), "Items saved",
                    itemGroupRepo.save(group)));
        }).orElse(ResponseEntity.notFound().build());
    }

    // Remove single product from group
    @DeleteMapping("/{id}/products/{productId}")
    public ResponseEntity<ApiResponse> removeProduct(@PathVariable Long id, @PathVariable Long productId) {
        itemGroupProductRepo.deleteByItemGroupIdAndProductId(id, productId);
        return ResponseEntity.ok(new ApiResponse(HttpStatus.OK.value(), "Removed", null));
    }
}
