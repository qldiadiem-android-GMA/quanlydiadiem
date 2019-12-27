package com.example.wherenow;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

//import com.android.volley.toolbox.HttpResponse;
import com.google.android.gms.location.places.Place;

import org.json.JSONException;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class CatarogyAdapter extends ArrayAdapter<PlaceModel> {

    Context context;
    ArrayList<PlaceModel> arrayList;
    ImageView img;

    public CatarogyAdapter(Context context, ArrayList<PlaceModel> arrayList) {
        super(context, 0, arrayList);
        this.context = context;
        this.arrayList = arrayList;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        PlaceModel p = getItem(position);

        LayoutInflater layoutInflater = LayoutInflater.from(getContext());
        convertView = layoutInflater.inflate(R.layout.item_list, null);

        TextView txtName = convertView.findViewById(R.id.tvName);
        TextView txtAddress = convertView.findViewById(R.id.tvAdd);
        img = convertView.findViewById(R.id.img);

        txtName.setText(p.Name);
        txtAddress.setText(p.Address);
        new DownloadImage().execute(p.PicturePath);
        return convertView;
    }

    //class AsyncTask tải hình
    private class DownloadImage extends AsyncTask<String, Void, Bitmap> {

        @Override
        protected Bitmap doInBackground(String... params) {
            String link = params[0];
            try {
                URL url = new URL(link);
                InputStream is = url.openConnection().getInputStream();
                Bitmap bitmap = BitmapFactory.decodeStream(is);

                return bitmap;

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        //hiển thị
        @Override
        protected void onPostExecute(Bitmap result) {
            img.setImageBitmap(result);
        }
    }
}
