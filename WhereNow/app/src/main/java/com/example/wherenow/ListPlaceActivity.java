package com.example.wherenow;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;

public class ListPlaceActivity extends AppCompatActivity {

    ListView listView;
    ArrayList<PlaceModel> arrayList;
    CatarogyAdapter catarogyAdapter;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        listView = findViewById(R.id.listView);
        Intent intent = getIntent();
        //ArrayList<PlaceModel> arrTemp = intent.getStringExtra("category"));
        //ArrayList<PlaceModel> arrTemp = (ArrayList<PlaceModel>)intent.getSerializableExtra("category");

        System.out.println("Đã chuyển dc dữ liệu !!");
        String listSerializedToJson = getIntent().getExtras().getString("category");
        ArrayList<PlaceModel> arrTemp = new Gson().fromJson(listSerializedToJson, new TypeToken<ArrayList<PlaceModel>>() {}.getType());

        arrayList = arrTemp;
        System.out.println("Đã lấy dc dữ liệu !!");

        //arrayList
        catarogyAdapter = new CatarogyAdapter(getBaseContext(), arrayList);
        listView.setAdapter(catarogyAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(ListPlaceActivity.this, MapsActivity.class);
                String listSerializedToJson = new Gson().toJson(arrayList.get(i));
                intent.putExtra("oneplace", listSerializedToJson);
                System.out.println("Truyền lại !!");
                startActivity(intent);
            }
        });
    }
}

