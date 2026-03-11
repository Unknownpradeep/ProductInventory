package com.hepl.product.Service;

import java.util.List;
import com.hepl.product.model.Category;

public interface CategoryService {
    List<Category> listAll();
    Category get(Long id);
    Category save(Category category);
    Category update(Long id, Category category);
    void delete(Long id);
}
