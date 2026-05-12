package com.example.uinew.Controller;

import com.example.uinew.model.Product;
import com.example.uinew.Interface.IProductListView;
import com.example.uinew.service.ProductService;

public class ProductListController extends HomeController {

        private IProductListView view;      // Kết nối với UI thông qua Interface
        private ProductService service;    // Kết nối với Backend/API

        public ProductListController(IProductListView view) {
            this.view = view;
            this.service = new ProductService();
        }

        public void onProductClick(Product product) {
        MainController.setSelectedProduct(product);
        // Chuyển sang trang đấu giá của sản phẩm đó
        setView("/com/example/uinew/View/ThisBidding.fxml");
    }
    // Xử lý khi nhấn vào một sản phẩm để sang LiveBiddingView

    }

