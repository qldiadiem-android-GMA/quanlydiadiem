package com.example.wherenow;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;

public class SearchActivity extends AppCompatActivity {
    SearchView searchView;
    ArrayList<PlaceModel> arrayList;
    ListView listView;
    CatarogyAdapter catarogyAdapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        arrayList = new ArrayList<>();
        listView = findViewById(R.id.listView);

        System.out.println("Đã chuyển dc dữ liệu !!");
        String listSerializedToJson = getIntent().getExtras().getString("category");
        ArrayList<PlaceModel> arrTemp = new Gson().fromJson(listSerializedToJson, new TypeToken<ArrayList<PlaceModel>>() {}.getType());

        arrayList = arrTemp;
        System.out.println("Đã lấy dc dữ liệu !!");

        catarogyAdapter = new CatarogyAdapter(SearchActivity.this, arrayList);
        listView.setAdapter(catarogyAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(SearchActivity.this, MapsActivity.class);
                String listSerializedToJson = new Gson().toJson(arrayList.get(i));
                intent.putExtra("oneplace", listSerializedToJson);
                System.out.println("Truyền lại !!");
                startActivity(intent);
            }
        });

        searchView = findViewById(R.id.searchView1);
        searchView.setIconifiedByDefault(true);
        searchView.setFocusable(true);
        searchView.setIconified(false);
        searchView.requestFocusFromTouch();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                searchPlace(newText);
                return false;
            }
        });
    }
    private void searchPlace(String newText) {
        ArrayList<PlaceModel> tmp = new ArrayList<>();
        for(PlaceModel p : arrayList){
            if(p.Name.toLowerCase().contains(newText.toLowerCase())){
                tmp.add(p);
            }
        }
        Toast.makeText(this, tmp.size()+"", Toast.LENGTH_SHORT).show();
        if(tmp.size() > 0){
            catarogyAdapter.clear();
            catarogyAdapter.addAll(tmp);
            catarogyAdapter.notifyDataSetChanged();
            listView.setVisibility(View.VISIBLE);
        }
        if(newText.isEmpty()){
            listView.setVisibility(View.GONE);
        }
    }
    public void hideSoftKeyboard(View view){
        InputMethodManager imm =(InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }


}
