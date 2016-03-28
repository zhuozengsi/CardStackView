package com.sige.cardstackview;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        CardStackView stackView = (CardStackView)findViewById(R.id.cardStackView);
        CardView cardView = (CardView) LayoutInflater.from(this).inflate(R.layout.card_view_in_framelayout, null);
        stackView.addCardView(cardView);
        CardView cardView1 = (CardView) LayoutInflater.from(this).inflate(R.layout.card_view_in_framelayout,null);
        stackView.addCardView(cardView1);
        CardView cardView2 = (CardView) LayoutInflater.from(this).inflate(R.layout.card_view_in_framelayout,null);
        stackView.addCardView(cardView2);

    }
}
