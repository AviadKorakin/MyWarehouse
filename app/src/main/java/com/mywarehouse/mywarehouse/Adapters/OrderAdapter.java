package com.mywarehouse.mywarehouse.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.imageview.ShapeableImageView;
import com.mywarehouse.mywarehouse.Interfaces.OrderCallback;
import com.mywarehouse.mywarehouse.Models.Order;
import com.mywarehouse.mywarehouse.R;

import java.util.List;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {

    private List<Order> orders;
    private OrderCallback orderCallback;

    public OrderAdapter(List<Order> orders, OrderCallback orderCallback) {
        this.orders = orders;
        this.orderCallback = orderCallback;
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Order order = orders.get(position);
        holder.orderTitle.setText(order.getTitle());
        holder.orderDescription.setText(order.getDescription());
        holder.orderDate.setText(order.getDate());

        holder.itemView.setOnClickListener(v -> orderCallback.onOrderItemClicked(order));
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    public static class OrderViewHolder extends RecyclerView.ViewHolder {

        ShapeableImageView orderIcon;
        TextView orderTitle, orderDescription, orderDate;
        ShapeableImageView orderCheckmark;

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            orderIcon = itemView.findViewById(R.id.order_icon);
            orderTitle = itemView.findViewById(R.id.order_title);
            orderDescription = itemView.findViewById(R.id.order_description);
            orderDate = itemView.findViewById(R.id.order_date);
            orderCheckmark = itemView.findViewById(R.id.order_checkmark);
        }
    }
}
