package com.example.uinew.Interface;
import java.util.List;
import com.example.uinew.model.Product;

public interface IProductListView {

        void displayProducts(List<Product> products);
        void showLoading(boolean isLoading);           // Hiện/ẩn icon loading
        void updateProductCard(String productId);      // Chỉ vẽ lại 1 card cụ thể khi giá thay đổi
        void showError(String message);                // Thông báo lỗi khi không lấy được data
    }

