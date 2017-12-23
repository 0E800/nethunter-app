package com.offsec.nethunter.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.offsec.nethunter.WifiteNetwork;
import com.offsec.nethunter.databinding.WifiteItemBinding;

import java.util.List;

/**
 * Created by ilak on 12/11/17.
 */

public class WifiteRecyclerAdapter extends RecyclerView.Adapter<WifiteRecyclerAdapter.ViewHolder> {


    private List<WifiteNetwork> networks;

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        WifiteItemBinding itemBinding = WifiteItemBinding.inflate(inflater, parent, false);
        return new ViewHolder(itemBinding);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        WifiteNetwork network = networks.get(position);
    }

    @Override
    public int getItemCount() {
        return networks.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final WifiteItemBinding binding;

        public ViewHolder(WifiteItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(WifiteNetwork network) {
            binding.setNetwork(network);
            binding.executePendingBindings();
        }
    }


}
