package edu.bluejack24_1.shot_review.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import edu.bluejack24_1.shot_review.databinding.CardCoffeeShopBinding
import edu.bluejack24_1.shot_review.models.CoffeeShops

class CoffeeShopAdapter(private var coffeeShopList: List<CoffeeShops>) : RecyclerView.Adapter<CoffeeShopAdapter.CoffeeShopViewHolder>() {

    // Interface untuk callback item click
    interface IOnItemClickCallback {
        fun onItemClick(coffeeShops: CoffeeShops)
    }

    private lateinit var onItemClickCallback: IOnItemClickCallback

    // Setter untuk callback item click
    fun setOnItemClickCallback(onItemClickCallback: IOnItemClickCallback) {
        this.onItemClickCallback = onItemClickCallback
    }

    // Method untuk memperbarui data coffee shop list
    fun updateData(newCoffeeShopList: List<CoffeeShops>) {
        coffeeShopList = newCoffeeShopList
        notifyDataSetChanged()
    }

    private lateinit var binding: CardCoffeeShopBinding

    class CoffeeShopViewHolder(private val binding: CardCoffeeShopBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(coffeeShops: CoffeeShops) {
            // Bind data ke layout
            binding.coffeeShops = coffeeShops
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CoffeeShopViewHolder {
        // Inflate layout untuk view holder
        binding = CardCoffeeShopBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CoffeeShopViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return coffeeShopList.size
    }

    override fun onBindViewHolder(holder: CoffeeShopViewHolder, position: Int) {
        val coffeeShops = coffeeShopList[position]
        holder.bind(coffeeShops)
        holder.itemView.setOnClickListener {
            // Handle item click
            onItemClickCallback.onItemClick(coffeeShopList[position])
        }
    }
}
