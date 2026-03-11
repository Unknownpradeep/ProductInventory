package com.hepl.product.Service.ServiceImpl;

import java.util.List;
import org.springframework.stereotype.Service;
import com.hepl.product.Repository.CategoryRepository;
import com.hepl.product.Service.CategoryService;
import com.hepl.product.model.Category;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    
    private final CategoryRepository repository;

    @Override
    public List<Category> listAll() {
        return repository.findAll();
    }

    @Override
    public Category get(Long id) {
        return repository.findById(id).orElse(null);
    }

    @Override
    public Category save(Category category) {
        return repository.save(category);
    }

    @Override
    public Category update(Long id, Category category) {
        Category existing = repository.findById(id).orElseThrow(() -> new RuntimeException("Category Not Found"));
        existing.setName(category.getName());
        return repository.save(existing);
    }

    @Override
    public void delete(Long id) {
        repository.deleteById(id);
    }
}
