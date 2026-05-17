package com.auction.project.UI.Interface;
import com.auction.project.Entities.Item;

import java.util.List;


public interface IItemListView {

        void displayItems(List<Item> Items);
        void showLoading(boolean isLoading);           // Hiện/ẩn icon loading
        void updateItemCard(String ItemId);      // Chỉ vẽ lại 1 card cụ thể khi giá thay đổi
        void showError(String message);                // Thông báo lỗi khi không lấy được data
    }

