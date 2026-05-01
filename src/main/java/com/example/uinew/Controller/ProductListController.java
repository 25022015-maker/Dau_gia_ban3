package com.example.uinew.Controller;

import javafx.application.Application;
import javafx.stage.Stage;
import com.example.uinew.Interface.IProductListView;
import java.util.*;

public class ProductListController extends MainController {

        private IProductListView view;      // Kết nối với UI thông qua Interface
        private ProductService service;    // Kết nối với Backend/API

        public ProductListController(IProductListView view) {
            this.view = view;
            this.service = new ProductService();
        }

        // Hàm chính để load dữ liệu
        public void loadProducts() {
            view.showLoading(true);
            service.getAllProducts(new Callback<List<Product>>() {
                @Override
                public void onSuccess(List<Product> products) {
                    view.showLoading(false);
                    view.displayProducts(products);
                }
            });
        }


        // Xử lý khi nhấn vào một sản phẩm để sang LiveBiddingView
        public void onProductClick(Product product) {
            MainController.getInstance().changeScene(ViewType.LIVE_BIDDING, product.getId());
        }
    }

