package net.fred.taskgame.hero.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import net.fred.taskgame.hero.R;
import net.fred.taskgame.hero.models.Card;
import net.fred.taskgame.hero.views.GameCardView;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class BuyCardsAdapter extends RecyclerView.Adapter<BuyCardsAdapter.CardViewHolder> {

    private final List<Card> mCards;
    private final RecyclerViewItemListener mItemListener;

    public static class CardViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.card)
        GameCardView mCard;

        @BindView(R.id.buy)
        ImageButton mBuyButton;

        @BindView(R.id.price)
        TextView mPrice;

        public CardViewHolder(View v) {
            super(v);
            ButterKnife.bind(this, v);
        }
    }

    public BuyCardsAdapter(List<Card> cards, RecyclerViewItemListener listener) {
        mCards = cards;
        mItemListener = listener;

        setHasStableIds(true);
    }

    public List<Card> getCards() {
        return mCards;
    }

    @Override
    public long getItemId(int position) {
        return mCards.get(position).id;
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    @Override
    public CardViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        final View v = inflater.inflate(R.layout.item_buy_card, parent, false);
        return new CardViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final CardViewHolder holder, int position) {
        Card card = mCards.get(position);

        holder.mCard.setCard(card);
        holder.mPrice.setText(String.valueOf(card.price));

        holder.mBuyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mItemListener.onItemClicked(holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return mCards.size();
    }
}