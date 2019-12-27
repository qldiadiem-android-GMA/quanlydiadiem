package com.example.wherenow;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.location.Address;
import android.location.Criteria;
import android.location.Location;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
//import android.support.v4.app.FragmentActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

//import android.location.LocationListener;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.GeoDataClient;
//import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

//Cho thanh tìm kiếm
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import androidx.appcompat.app.ActionBar;
import com.google.android.gms.common.UserRecoverableException;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.miguelcatalan.materialsearchview.MaterialSearchView;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        GoogleMap.OnMarkerDragListener,
        GoogleMap.OnMapLongClickListener,
        GoogleMap.OnMarkerClickListener,
        View.OnClickListener,
        DirectionFinderListener{

    private static final String TAG = "MapsActivity";
    private GoogleMap mMap;
    public double longitude;
    public double latitude;
    private GoogleApiClient googleApiClient;
    private ProgressDialog myProgress;
    protected GeoDataClient mGeoDataClient;

    //Cho thanh tìm kiếm
    PlaceAutocompleteFragment placeAutoComplete;

    private List<Marker> originMarkers = new ArrayList<>();
    private List<Marker> destinationMarkers = new ArrayList<>();
    private List<Polyline> polylinePaths = new ArrayList<>();
    private ProgressDialog progressDialog;
    private DirectionFinder directionFinder;
    private DirectionFinderListener directionFinderListener = new DirectionFinderListener() {
        @Override
        public void onDirectionFinderStart() {
            progressDialog = ProgressDialog.show(MapsActivity.this, "Please wait.",
                    "Finding direction..!", true);

            if (originMarkers != null) {
                for (Marker marker : originMarkers) {
                    marker.remove();
                }
            }

            if (destinationMarkers != null) {
                for (Marker marker : destinationMarkers) {
                    marker.remove();
                }
            }

            if (polylinePaths != null) {
                for (Polyline polyline:polylinePaths ) {
                    polyline.remove();
                }
            }
        }

        @Override
        public void onDirectionFinderSuccess(List<Route> routes) {
            progressDialog.dismiss();
            polylinePaths = new ArrayList<>();
            originMarkers = new ArrayList<>();
            destinationMarkers = new ArrayList<>();

            for (Route route : routes) {
               mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(route.startLocation, 13));

                originMarkers.add(mMap.addMarker(new MarkerOptions()
                    .position(route.startLocation)));
                Marker des = mMap.addMarker(new MarkerOptions()
                        .position(route.endLocation).title(route.endName).snippet(route.endAddress));
                destinationMarkers.add(des);
                des.showInfoWindow();

                PolylineOptions polylineOptions = new PolylineOptions().
                        geodesic(true).
                        color(Color.rgb(99,184,255)).
                        width(25);

                for (int i = 0; i < route.points.size(); i++) {
                    polylineOptions.add(route.points.get(i));
                }

                //polylinePaths.add(mMap.addPolyline(polylineOptions));
                mMap.addPolyline(polylineOptions);
            }
        }
    };

    EditText searchView;

    //Sử dụng cho tìm vị trí hiện tại
    private static final String MYTAG = "MYTAG";
    // Mã yêu cầu hỏi người dùng cho phép xem vị trí hiện tại của họ (***).
    // Giá trị mã 8bit (value < 256).
    public static final int REQUEST_ID_ACCESS_COURSE_FINE_LOCATION = 100;
    int PLACE_AUTOCOMPLETE_REQUEST_CODE = 1;

    //Tạo database
    SQLite db = new SQLite(this, "WhereNow.sqlite", null, 1);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Tạo Progress Bar
        myProgress = new ProgressDialog(this);
        myProgress.setTitle("Map Loading ...");
        myProgress.setMessage("Please wait...");
        myProgress.setCancelable(true);

        // Hiển thị Progress Bar
        myProgress.show();
        // Nhận SupportMapFragment và nhận thông báo khi bản đồ sẵn sàng được sử dụng.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //Initializing googleApiClient
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        googleApiClient.connect();

        //Tạo database
        taoDBBar();
        taoDBBenhVien();
        taoDBCafe();
        taoDBChuaDen();
        taoDBCongVienVuiChoi();
        taoDBCuaHang();
        taoDBKaraoke();
        taoDBKhachSan();
        taoDBKhuChoiGame();
        taoDBNganHang();
        taoDBNhaHang();
        taoDBNhaThuoc();
        taoDBQuanAnVat();
        taoDBQuanChay();
        taoDBRapChieuPhim();
        taoDBSieuThi();
        taoDBTramXang();
        taoDBTrungTamAnhNgu();
        taoDBTrungTamThuongMai();
        taoDBTruongHoc();
        taoDBVienThong();
    }

    @Override
    public void onDirectionFinderStart() {}

    @Override
    public void onDirectionFinderSuccess(List<Route> routes) {
//        progressDialog.dismiss();
//        polylinePaths = new ArrayList<>();
//        originMarkers = new ArrayList<>();
//        destinationMarkers = new ArrayList<>();
//
//        for (Route route : routes) {
//            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(route.startLocation, 16));
//            ((TextView) findViewById(R.id.tvDuration)).setText(route.duration.text);
//            ((TextView) findViewById(R.id.tvDistance)).setText(route.distance.text);

//            originMarkers.add(mMap.addMarker(new MarkerOptions()
//                    .position(route.startLocation)));
//            destinationMarkers.add(mMap.addMarker(new MarkerOptions()
//                    .position(route.endLocation)));

//            PolylineOptions polylineOptions = new PolylineOptions().
//                    geodesic(true).
//                    color(Color.BLUE).
//                    width(10);
//
//            for (int i = 0; i < route.points.size(); i++)
//                polylineOptions.add(route.points.get(i));
//
//            polylinePaths.add(mMap.addPolyline(polylineOptions));
//        }
    }


    //Hàm chỉnh vị trí nút vị trí hiện tại
    private void resetMyPositionButton() {
        Fragment fragment = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map));
        ViewGroup v1 = (ViewGroup) fragment.getView();
        ViewGroup v2 = (ViewGroup) v1.getChildAt(0);
        ViewGroup v3 = (ViewGroup) v2.getChildAt(2);
        View position = (View) v3.getChildAt(0);
        int positionWidth = position.getLayoutParams().width;
        int positionHeight = position.getLayoutParams().height;

        //lay out position button
        RelativeLayout.LayoutParams positionParams = new RelativeLayout.LayoutParams(positionWidth, positionHeight);
        int margin = positionWidth / 5;
        positionParams.setMargins(935, 0, 0, 0);
        positionParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        positionParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
        position.setLayoutParams(positionParams);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        mMap.setOnMarkerDragListener(this);
        mMap.setOnMapLongClickListener(this);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(false);

        //Chỉnh vị trí nút vị trí hiện tại
        resetMyPositionButton();

        //mMap.setMyLocationEnabled(true);
        this.mMap.setMyLocationEnabled(true);

        //Lấy vị trí hiện tại
        getCurrentLocation();

        mMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {

            @Override
            public void onMapLoaded() {
                //sự kiện search
                searchView = findViewById(R.id.searchView1);
                searchView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        goSearch(layTatCaDiaDiem());
                        //System.out.println("ABC: "+layTatCaDiaDiem().size());
                    }
                });

                // Đã tải thành công thì tắt Dialog Progress đi
                myProgress.dismiss();
                // Hiển thị vị trí người dùng.
                askPermissionsAndShowMyLocation();

                if(getIntent().getStringExtra("oneplace") != null) {
                    String SerializedToJson = getIntent().getExtras().getString("oneplace");
                    PlaceModel place = new Gson().fromJson(SerializedToJson, new TypeToken<PlaceModel>() {
                    }.getType());
                    // Thêm Marker cho Map:
                    addMarker(place);
                    taoDBLichSu(place);
                    Button btnDict = findViewById(R.id.btnDict);
                    btnDict.setVisibility(View.VISIBLE);
                }
            }
        });

    }


    //Hàm lấy vị trí hiện tại
    private void getCurrentLocation() {
        mMap.clear();
        Location myLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
        if (myLocation != null) {

            LatLng latLng = new LatLng(myLocation.getLatitude(), myLocation.getLongitude());
            this.latitude = myLocation.getLatitude();
            this.longitude = myLocation.getLongitude();
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 13));

            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(latLng)             // Sets the center of the map to location user
                    .zoom(15)                   // Sets the zoom
                    .bearing(90)                // Sets the orientation of the camera to east
                    .tilt(40)                   // Sets the tilt of the camera to 30 degrees
                    .build();                   // Creates a CameraPosition from the builder
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

            // Thêm Marker cho Map:
            MarkerOptions option = new MarkerOptions();
            option.title("My location");
            option.snippet("");
            option.position(latLng);
            Marker currentMarker = mMap.addMarker(option);
            currentMarker.showInfoWindow();
        } else {
              //Toast.makeText(this, "Location not found!", Toast.LENGTH_LONG).show();
              //Log.i(MYTAG, "Location not found");
        }
    }


    private void askPermissionsAndShowMyLocation() {
        // Với API >= 23, bạn phải hỏi người dùng cho phép xem vị trí của họ.
        if (Build.VERSION.SDK_INT >= 23) {
            int accessCoarsePermission
                    = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
            int accessFinePermission
                    = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);


            if (accessCoarsePermission != PackageManager.PERMISSION_GRANTED
                    || accessFinePermission != PackageManager.PERMISSION_GRANTED) {

                // Các quyền cần người dùng cho phép.
                String[] permissions = new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION};

                // Hiển thị một Dialog hỏi người dùng cho phép các quyền trên.
                ActivityCompat.requestPermissions(this, permissions,
                        REQUEST_ID_ACCESS_COURSE_FINE_LOCATION);

                return;
            }
        }
        // Hiển thị vị trí hiện thời trên bản đồ.
        getCurrentLocation();
    }


    // Khi người dùng trả lời yêu cầu cấp quyền (cho phép hoặc từ chối).
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        //
        switch (requestCode) {
            case REQUEST_ID_ACCESS_COURSE_FINE_LOCATION: {


                // Chú ý: Nếu yêu cầu bị bỏ qua, mảng kết quả là rỗng.
                if (grantResults.length > 1
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED) {

                    Toast.makeText(this, "Permission granted!", Toast.LENGTH_LONG).show();

                    // Hiển thị vị trí hiện thời trên bản đồ
                    getCurrentLocation();
                }
                // Hủy bỏ hoặc từ chối.
                else {
                    Toast.makeText(this, "Permission denied!", Toast.LENGTH_LONG).show();
                }
                break;
            }
        }
    }

    @Override
    public void onClick(View view) {
        Log.v(TAG, "view click event");
    }

    //Hàm tạo marker
    public void addMarker(PlaceModel p) {
        LatLng latLng = new LatLng(p.Latitude, p.Longitude);
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.title(p.Name);
        markerOptions.snippet(p.Address);
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
        Marker currentMarker = mMap.addMarker(markerOptions);
        currentMarker.showInfoWindow();
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        System.out.println("Xin chào !! "+latLng);
        mMap.animateCamera(CameraUpdateFactory.zoomTo(15));
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

        //Thanh tìm kiếm
        //Có người để trên onCreate nhưng để trên đó cũng ko chạy

        //Để trực tiếp trên onCreate https://stackoverflow.com/questions/45107806/autocomplete-search-bar-in-google-maps

        //Còn có người làm intent để chạy activity autocomplete

        //Ở trang https://viblo.asia/p/google-place-api-p1-place-autocomplete-QpmleQXrlrd

        //Và trang https://developers.google.com/places/android-sdk/autocomplete

        //Tình trạng mới nhất: có thể nhập được ở thanh tìm kiếm không bị tắt bàn phím ảo khi bản đồ đang được load
        //Khi bản đồ load xong thanh tìm kiếm lại bị tắt bàn phím ảo liên tục
        //Do để tìm kiếm không đúng vòng đời ? ==> Chưa biết...
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        // mMap.clear();
        mMap.addMarker(new MarkerOptions().position(latLng).draggable(true));
    }

    @Override
    public void onMarkerDragStart(Marker marker) {
        Toast.makeText(MapsActivity.this, "onMarkerDragStart", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onMarkerDrag(Marker marker) {
        Toast.makeText(MapsActivity.this, "onMarkerDrag", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onMarkerDragEnd(Marker marker) {
        // getting the Co-ordinates
        latitude = marker.getPosition().latitude;
        longitude = marker.getPosition().longitude;

        //move to current position
        //moveMap();
    }

    @Override
    protected void onStart() {
        googleApiClient.connect();
        super.onStart();
    }

    @Override
    protected void onStop() {
        googleApiClient.disconnect();
        super.onStop();
    }


    @Override
    public boolean onMarkerClick(Marker marker) {
        //Toast.makeText(MapsActivity.this, "onMarkerClick", Toast.LENGTH_SHORT).show();
        return true;
    }

    //Thấy trên mạng có người làm trên onactivityResult nên làm theo
    //Ở trang https://viblo.asia/p/google-place-api-p1-place-autocomplete-QpmleQXrlrd
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PLACE_AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Place place = PlaceAutocomplete.getPlace(this, data);
                Log.i(TAG, "Place: " + place.getName());
            } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
                Status status = PlaceAutocomplete.getStatus(this, data);
                // TODO: Handle the error.
                Log.i(TAG, status.getStatusMessage());

            } else if (resultCode == RESULT_CANCELED) {
                // The user canceled the operation.
            }
        }
    }


    public void taoDBLichSu(PlaceModel place) {
        try{
        db.QueryData("CREATE TABLE IF NOT EXISTS LichSu(ID INTEGER PRIMARY KEY AUTOINCREMENT, NAME NVARCHAR(150), ADDRESS NVARCHAR(150), LONGITUDE DOUBLE, LATITUDE DOUBLE, IMAGE VARCHAR(400))");


        db.QueryData("INSERT INTO LichSu VALUES(null, '" + place.Name + "', '" + place.Address + "', '" + place.Latitude + "', '" + place.Longitude + "', '" + place.PicturePath + "')");
        }

        catch (Exception ex) {
                    ex.printStackTrace();
                }

    }

    public void xemLichSu(View view){
        goListCategory(layLichSu("LichSu"));
    }

    public ArrayList<PlaceModel> layLichSu(String table) {
        Double latitude = 0.0;
        Double longitude = 0.0;

        ArrayList<PlaceModel> pList = new ArrayList<>();

        Cursor kq = db.GetData("SELECT * FROM "+table+" ORDER BY ID DESC");
        while (kq.moveToNext())
        {
            String ten = kq.getString(1);
            String diachi = kq.getString(2);
            latitude = kq.getDouble(3);
            longitude = kq.getDouble(4);
            String hinh = kq.getString(5);

            PlaceModel p = new PlaceModel(ten, diachi, latitude, longitude, hinh);
            pList.add(p);

        }
        return pList;
    }

    public void taoDBTrungTamAnhNgu() {
        // đọc file
        StringBuffer sbuffer = new StringBuffer();
        InputStream is = this.getResources().openRawResource(R.raw.trungtamanhngu);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        if (is != null) {
            // Tạo bảng
            db.QueryData("CREATE TABLE IF NOT EXISTS TrungTamAnhNgu(ID INTEGER PRIMARY KEY AUTOINCREMENT, NAME NVARCHAR(150), ADDRESS NVARCHAR(150), LONGITUDE DOUBLE, LATITUDE DOUBLE, IMAGE VARCHAR(400))");
            Cursor kq = db.GetData("SELECT * FROM TrungTamAnhNgu");
            if(!kq.moveToFirst()) {
                try {
                    int dem = 0;// biến thể hiện vị trí dòng
                    int vt = 0;// biến thể hiện địa điểm thứ mấy
                    String data = "";
                    String TenDiaDiem = "";
                    String DiaChi = "";
                    Double KinhDo = null;
                    Double ViDo = null;
                    String Hinh = "";

                    while ((data = reader.readLine()) != null) {
                        if (dem == vt * 6)
                            TenDiaDiem = data;
                        if (dem == vt * 6 + 1)
                            DiaChi = data;
                        if (dem == vt * 6 + 2)
                            KinhDo = Double.parseDouble(data);
                        if (dem == vt * 6 + 3)
                            ViDo = Double.parseDouble(data);
                        if (dem == vt * 6 + 4)
                            Hinh = data;
                        if (dem == vt * 6 + 5) {
                            // thêm dữ liệu
                            db.QueryData("INSERT INTO TrungTamAnhNgu VALUES(null, '" + TenDiaDiem + "', '" + DiaChi + "', '" + KinhDo + "', '" + ViDo + "', '" + Hinh + "')");
                            vt++;
                        }
                        dem++;
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }

    }

    public void taoDBTrungTamThuongMai() {
        // đọc file
        StringBuffer sbuffer = new StringBuffer();
        InputStream is = this.getResources().openRawResource(R.raw.trungtamthuongmai);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        if (is != null) {
            // Tạo bảng
            db.QueryData("CREATE TABLE IF NOT EXISTS TrungTamThuongMai(ID INTEGER PRIMARY KEY AUTOINCREMENT, NAME NVARCHAR(150), ADDRESS NVARCHAR(150), LONGITUDE DOUBLE, LATITUDE DOUBLE, IMAGE VARCHAR(400))");
            Cursor kq = db.GetData("SELECT * FROM TrungTamThuongMai");
            if (!kq.moveToFirst()) {
                try {
                    int dem = 0;// biến thể hiện vị trí dòng
                    int vt = 0;// biến thể hiện địa điểm thứ mấy
                    String data = "";
                    String TenDiaDiem = "";
                    String DiaChi = "";
                    Double KinhDo = null;
                    Double ViDo = null;
                    String Hinh = "";

                    while ((data = reader.readLine()) != null) {
                        if (dem == vt * 6)
                            TenDiaDiem = data;
                        if (dem == vt * 6 + 1)
                            DiaChi = data;
                        if (dem == vt * 6 + 2)
                            KinhDo = Double.parseDouble(data);
                        if (dem == vt * 6 + 3)
                            ViDo = Double.parseDouble(data);
                        if (dem == vt * 6 + 4)
                            Hinh = data;
                        if (dem == vt * 6 + 5) {
                            // thêm dữ liệu
                            db.QueryData("INSERT INTO TrungTamThuongMai VALUES(null, '" + TenDiaDiem + "', '" + DiaChi + "', '" + KinhDo + "', '" + ViDo + "', '" + Hinh + "')");
                            vt++;
                        }
                        dem++;
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }

    }

    public void taoDBTruongHoc() {
        // đọc file
        StringBuffer sbuffer = new StringBuffer();
        InputStream is = this.getResources().openRawResource(R.raw.truonghoc);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        if (is != null) {
            // Tạo bảng
            db.QueryData("CREATE TABLE IF NOT EXISTS TruongHoc(ID INTEGER PRIMARY KEY AUTOINCREMENT, NAME NVARCHAR(150), ADDRESS NVARCHAR(150), LONGITUDE DOUBLE, LATITUDE DOUBLE, IMAGE VARCHAR(400))");
            Cursor kq = db.GetData("SELECT * FROM TruongHoc");
            if (!kq.moveToFirst()) {
                try {
                    int dem = 0;// biến thể hiện vị trí dòng
                    int vt = 0;// biến thể hiện địa điểm thứ mấy
                    String data = "";
                    String TenDiaDiem = "";
                    String DiaChi = "";
                    Double KinhDo = null;
                    Double ViDo = null;
                    String Hinh = "";

                    while ((data = reader.readLine()) != null) {
                        if (dem == vt * 6)
                            TenDiaDiem = data;
                        if (dem == vt * 6 + 1)
                            DiaChi = data;
                        if (dem == vt * 6 + 2)
                            KinhDo = Double.parseDouble(data);
                        if (dem == vt * 6 + 3)
                            ViDo = Double.parseDouble(data);
                        if (dem == vt * 6 + 4)
                            Hinh = data;
                        if (dem == vt * 6 + 5) {
                            // thêm dữ liệu
                            db.QueryData("INSERT INTO TruongHoc VALUES(null, '" + TenDiaDiem + "', '" + DiaChi + "', '" + KinhDo + "', '" + ViDo + "', '" + Hinh + "')");
                            vt++;
                        }
                        dem++;
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

        }
    }

    public void taoDBVienThong() {
        // đọc file
        StringBuffer sbuffer = new StringBuffer();
        InputStream is = this.getResources().openRawResource(R.raw.trungtamthuongmai);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        if (is != null) {
            // Tạo bảng
            db.QueryData("CREATE TABLE IF NOT EXISTS VienThong(ID INTEGER PRIMARY KEY AUTOINCREMENT, NAME NVARCHAR(150), ADDRESS NVARCHAR(150), LONGITUDE DOUBLE, LATITUDE DOUBLE, IMAGE VARCHAR(400))");
            Cursor kq = db.GetData("SELECT * FROM VienThong");
            if (!kq.moveToFirst()) {
                try {
                    int dem = 0;// biến thể hiện vị trí dòng
                    int vt = 0;// biến thể hiện địa điểm thứ mấy
                    String data = "";
                    String TenDiaDiem = "";
                    String DiaChi = "";
                    Double KinhDo = null;
                    Double ViDo = null;
                    String Hinh = "";

                    while ((data = reader.readLine()) != null) {
                        if (dem == vt * 6)
                            TenDiaDiem = data;
                        if (dem == vt * 6 + 1)
                            DiaChi = data;
                        if (dem == vt * 6 + 2)
                            KinhDo = Double.parseDouble(data);
                        if (dem == vt * 6 + 3)
                            ViDo = Double.parseDouble(data);
                        if (dem == vt * 6 + 4)
                            Hinh = data;
                        if (dem == vt * 6 + 5) {
                            // thêm dữ liệu
                            db.QueryData("INSERT INTO VienThong VALUES(null, '" + TenDiaDiem + "', '" + DiaChi + "', '" + KinhDo + "', '" + ViDo + "', '" + Hinh + "')");
                            vt++;
                        }
                        dem++;
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    public void taoDBBar() {
        // đọc file
        StringBuffer sbuffer = new StringBuffer();
        InputStream is = this.getResources().openRawResource(R.raw.bar);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        if (is != null) {
            // Tạo bảng
            db.QueryData("CREATE TABLE IF NOT EXISTS Bar(ID INTEGER PRIMARY KEY AUTOINCREMENT, NAME NVARCHAR(150), ADDRESS NVARCHAR(150), LONGITUDE DOUBLE, LATITUDE DOUBLE, IMAGE VARCHAR(400))");
            Cursor kq = db.GetData("SELECT * FROM Bar");
            if (!kq.moveToFirst()) {
                try {
                    int dem = 0;// biến thể hiện vị trí dòng
                    int vt = 0;// biến thể hiện địa điểm thứ mấy
                    String data = "";
                    String TenDiaDiem = "";
                    String DiaChi = "";
                    Double KinhDo = null;
                    Double ViDo = null;
                    String Hinh = "";

                    while ((data = reader.readLine()) != null) {
                        if (dem == vt * 6)
                            TenDiaDiem = data;
                        if (dem == vt * 6 + 1)
                            DiaChi = data;
                        if (dem == vt * 6 + 2)
                            KinhDo = Double.parseDouble(data);
                        if (dem == vt * 6 + 3)
                            ViDo = Double.parseDouble(data);
                        if (dem == vt * 6 + 4)
                            Hinh = data;
                        if (dem == vt * 6 + 5) {
                            // thêm dữ liệu
                            db.QueryData("INSERT INTO Bar VALUES(null, '" + TenDiaDiem + "', '" + DiaChi + "', '" + KinhDo + "', '" + ViDo + "', '" + Hinh + "')");
                            vt++;
                        }
                        dem++;
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }

    }

    public void taoDBBenhVien() {
        // đọc file
        StringBuffer sbuffer = new StringBuffer();
        InputStream is = this.getResources().openRawResource(R.raw.benhvien);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        if (is != null) {
            // Tạo bảng
            db.QueryData("CREATE TABLE IF NOT EXISTS BenhVien(ID INTEGER PRIMARY KEY AUTOINCREMENT, NAME NVARCHAR(150), ADDRESS NVARCHAR(150), LONGITUDE DOUBLE, LATITUDE DOUBLE, IMAGE VARCHAR(400))");
            Cursor kq = db.GetData("SELECT * FROM BenhVien");
            if (!kq.moveToFirst()) {
                try {
                    int dem = 0;// biến thể hiện vị trí dòng
                    int vt = 0;// biến thể hiện địa điểm thứ mấy
                    String data = "";
                    String TenDiaDiem = "";
                    String DiaChi = "";
                    Double KinhDo = null;
                    Double ViDo = null;
                    String Hinh = "";

                    while ((data = reader.readLine()) != null) {
                        if (dem == vt * 6)
                            TenDiaDiem = data;
                        if (dem == vt * 6 + 1)
                            DiaChi = data;
                        if (dem == vt * 6 + 2)
                            KinhDo = Double.parseDouble(data);
                        if (dem == vt * 6 + 3)
                            ViDo = Double.parseDouble(data);
                        if (dem == vt * 6 + 4)
                            Hinh = data;
                        if (dem == vt * 6 + 5) {
                            // thêm dữ liệu
                            db.QueryData("INSERT INTO BenhVien VALUES(null, '" + TenDiaDiem + "', '" + DiaChi + "', '" + KinhDo + "', '" + ViDo + "', '" + Hinh + "')");
                            vt++;
                        }
                        dem++;
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }

    }

    public void taoDBCafe() {
        // đọc file
        StringBuffer sbuffer = new StringBuffer();
        InputStream is = this.getResources().openRawResource(R.raw.cafe);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        if (is != null) {
            // Tạo bảng
            db.QueryData("CREATE TABLE IF NOT EXISTS Cafe(ID INTEGER PRIMARY KEY AUTOINCREMENT, NAME NVARCHAR(150), ADDRESS NVARCHAR(150), LONGITUDE DOUBLE, LATITUDE DOUBLE, IMAGE VARCHAR(400))");
            Cursor kq = db.GetData("SELECT * FROM Cafe");
            if (!kq.moveToFirst()) {
                try {
                    int dem = 0;// biến thể hiện vị trí dòng
                    int vt = 0;// biến thể hiện địa điểm thứ mấy
                    String data = "";
                    String TenDiaDiem = "";
                    String DiaChi = "";
                    Double KinhDo = null;
                    Double ViDo = null;
                    String Hinh = "";

                    while ((data = reader.readLine()) != null) {
                        if (dem == vt * 6)
                            TenDiaDiem = data;
                        if (dem == vt * 6 + 1)
                            DiaChi = data;
                        if (dem == vt * 6 + 2)
                            KinhDo = Double.parseDouble(data);
                        if (dem == vt * 6 + 3)
                            ViDo = Double.parseDouble(data);
                        if (dem == vt * 6 + 4)
                            Hinh = data;
                        if (dem == vt * 6 + 5) {
                            // thêm dữ liệu
                            db.QueryData("INSERT INTO Cafe VALUES(null, '" + TenDiaDiem + "', '" + DiaChi + "', '" + KinhDo + "', '" + ViDo + "', '" + Hinh + "')");
                            vt++;
                        }
                        dem++;
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }

    }

    public void taoDBChuaDen() {
        // đọc file
        StringBuffer sbuffer = new StringBuffer();
        InputStream is = this.getResources().openRawResource(R.raw.chuaden);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        if (is != null) {
            // Tạo bảng
            db.QueryData("CREATE TABLE IF NOT EXISTS ChuaDen(ID INTEGER PRIMARY KEY AUTOINCREMENT, NAME NVARCHAR(150), ADDRESS NVARCHAR(150), LONGITUDE DOUBLE, LATITUDE DOUBLE, IMAGE VARCHAR(400))");
            Cursor kq = db.GetData("SELECT * FROM ChuaDen");
            if (!kq.moveToFirst()) {
                try {
                    int dem = 0;// biến thể hiện vị trí dòng
                    int vt = 0;// biến thể hiện địa điểm thứ mấy
                    String data = "";
                    String TenDiaDiem = "";
                    String DiaChi = "";
                    Double KinhDo = null;
                    Double ViDo = null;
                    String Hinh = "";

                    while ((data = reader.readLine()) != null) {
                        if (dem == vt * 6)
                            TenDiaDiem = data;
                        if (dem == vt * 6 + 1)
                            DiaChi = data;
                        if (dem == vt * 6 + 2)
                            KinhDo = Double.parseDouble(data);
                        if (dem == vt * 6 + 3)
                            ViDo = Double.parseDouble(data);
                        if (dem == vt * 6 + 4)
                            Hinh = data;
                        if (dem == vt * 6 + 5) {
                            // thêm dữ liệu
                            db.QueryData("INSERT INTO ChuaDen VALUES(null, '" + TenDiaDiem + "', '" + DiaChi + "', '" + KinhDo + "', '" + ViDo + "', '" + Hinh + "')");
                            vt++;
                        }
                        dem++;
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }

    }

    public void taoDBCongVienVuiChoi() {
        // đọc file
        StringBuffer sbuffer = new StringBuffer();
        InputStream is = this.getResources().openRawResource(R.raw.congvienvuichoi);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        if (is != null) {
            // Tạo bảng
            db.QueryData("CREATE TABLE IF NOT EXISTS CongVienVuiChoi(ID INTEGER PRIMARY KEY AUTOINCREMENT, NAME NVARCHAR(150), ADDRESS NVARCHAR(150), LONGITUDE DOUBLE, LATITUDE DOUBLE, IMAGE VARCHAR(400))");
            Cursor kq = db.GetData("SELECT * FROM CongVienVuiChoi");
            if (!kq.moveToFirst()) {
                try {
                    int dem = 0;// biến thể hiện vị trí dòng
                    int vt = 0;// biến thể hiện địa điểm thứ mấy
                    String data = "";
                    String TenDiaDiem = "";
                    String DiaChi = "";
                    Double KinhDo = null;
                    Double ViDo = null;
                    String Hinh = "";

                    while ((data = reader.readLine()) != null) {
                        if (dem == vt * 6)
                            TenDiaDiem = data;
                        if (dem == vt * 6 + 1)
                            DiaChi = data;
                        if (dem == vt * 6 + 2)
                            KinhDo = Double.parseDouble(data);
                        if (dem == vt * 6 + 3)
                            ViDo = Double.parseDouble(data);
                        if (dem == vt * 6 + 4)
                            Hinh = data;
                        if (dem == vt * 6 + 5) {
                            // thêm dữ liệu
                            db.QueryData("INSERT INTO CongVienVuiChoi VALUES(null, '" + TenDiaDiem + "', '" + DiaChi + "', '" + KinhDo + "', '" + ViDo + "', '" + Hinh + "')");
                            vt++;
                        }
                        dem++;
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

        }
    }

    public void taoDBCuaHang() {
        // đọc file
        StringBuffer sbuffer = new StringBuffer();
        InputStream is = this.getResources().openRawResource(R.raw.cuahang);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        if (is != null) {
            // Tạo bảng
            db.QueryData("CREATE TABLE IF NOT EXISTS CuaHang(ID INTEGER PRIMARY KEY AUTOINCREMENT, NAME NVARCHAR(150), ADDRESS NVARCHAR(150), LONGITUDE DOUBLE, LATITUDE DOUBLE, IMAGE VARCHAR(400))");
            Cursor kq = db.GetData("SELECT * FROM CuaHang");
            if (!kq.moveToFirst()) {
                try {
                    int dem = 0;// biến thể hiện vị trí dòng
                    int vt = 0;// biến thể hiện địa điểm thứ mấy
                    String data = "";
                    String TenDiaDiem = "";
                    String DiaChi = "";
                    Double KinhDo = null;
                    Double ViDo = null;
                    String Hinh = "";

                    while ((data = reader.readLine()) != null) {
                        if (dem == vt * 6)
                            TenDiaDiem = data;
                        if (dem == vt * 6 + 1)
                            DiaChi = data;
                        if (dem == vt * 6 + 2)
                            KinhDo = Double.parseDouble(data);
                        if (dem == vt * 6 + 3)
                            ViDo = Double.parseDouble(data);
                        if (dem == vt * 6 + 4)
                            Hinh = data;
                        if (dem == vt * 6 + 5) {
                            // thêm dữ liệu
                            db.QueryData("INSERT INTO CuaHang VALUES(null, '" + TenDiaDiem + "', '" + DiaChi + "', '" + KinhDo + "', '" + ViDo + "', '" + Hinh + "')");
                            vt++;
                        }
                        dem++;
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

        }
    }

    public void taoDBKaraoke() {
        // đọc file
        StringBuffer sbuffer = new StringBuffer();
        InputStream is = this.getResources().openRawResource(R.raw.karaoke);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        if (is != null) {
            // Tạo bảng
            db.QueryData("CREATE TABLE IF NOT EXISTS Karaoke(ID INTEGER PRIMARY KEY AUTOINCREMENT, NAME NVARCHAR(150), ADDRESS NVARCHAR(150), LONGITUDE DOUBLE, LATITUDE DOUBLE, IMAGE VARCHAR(400))");
            Cursor kq = db.GetData("SELECT * FROM Karaoke");
            if (!kq.moveToFirst()) {
                try {
                    int dem = 0;// biến thể hiện vị trí dòng
                    int vt = 0;// biến thể hiện địa điểm thứ mấy
                    String data = "";
                    String TenDiaDiem = "";
                    String DiaChi = "";
                    Double KinhDo = null;
                    Double ViDo = null;
                    String Hinh = "";

                    while ((data = reader.readLine()) != null) {
                        if (dem == vt * 6)
                            TenDiaDiem = data;
                        if (dem == vt * 6 + 1)
                            DiaChi = data;
                        if (dem == vt * 6 + 2)
                            KinhDo = Double.parseDouble(data);
                        if (dem == vt * 6 + 3)
                            ViDo = Double.parseDouble(data);
                        if (dem == vt * 6 + 4)
                            Hinh = data;
                        if (dem == vt * 6 + 5) {
                            // thêm dữ liệu
                            db.QueryData("INSERT INTO Karaoke VALUES(null, '" + TenDiaDiem + "', '" + DiaChi + "', '" + KinhDo + "', '" + ViDo + "', '" + Hinh + "')");
                            vt++;
                        }
                        dem++;
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }

    }

    public void taoDBKhachSan() {
        // đọc file
        StringBuffer sbuffer = new StringBuffer();
        InputStream is = this.getResources().openRawResource(R.raw.khachsan);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        if (is != null) {
            // Tạo bảng
            db.QueryData("CREATE TABLE IF NOT EXISTS KhachSan(ID INTEGER PRIMARY KEY AUTOINCREMENT, NAME NVARCHAR(150), ADDRESS NVARCHAR(150), LONGITUDE DOUBLE, LATITUDE DOUBLE, IMAGE VARCHAR(400))");
            Cursor kq = db.GetData("SELECT * FROM KhachSan");
            if (!kq.moveToFirst()) {
                try {
                    int dem = 0;// biến thể hiện vị trí dòng
                    int vt = 0;// biến thể hiện địa điểm thứ mấy
                    String data = "";
                    String TenDiaDiem = "";
                    String DiaChi = "";
                    Double KinhDo = null;
                    Double ViDo = null;
                    String Hinh = "";

                    while ((data = reader.readLine()) != null) {
                        if (dem == vt * 6)
                            TenDiaDiem = data;
                        if (dem == vt * 6 + 1)
                            DiaChi = data;
                        if (dem == vt * 6 + 2)
                            KinhDo = Double.parseDouble(data);
                        if (dem == vt * 6 + 3)
                            ViDo = Double.parseDouble(data);
                        if (dem == vt * 6 + 4)
                            Hinh = data;
                        if (dem == vt * 6 + 5) {
                            // thêm dữ liệu
                            db.QueryData("INSERT INTO KhachSan VALUES(null, '" + TenDiaDiem + "', '" + DiaChi + "', '" + KinhDo + "', '" + ViDo + "', '" + Hinh + "')");
                            vt++;
                        }
                        dem++;
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }

    }

    public void taoDBKhuChoiGame() {
        // đọc file
        StringBuffer sbuffer = new StringBuffer();
        InputStream is = this.getResources().openRawResource(R.raw.khuchoigame);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        if (is != null) {
            // Tạo bảng
            db.QueryData("CREATE TABLE IF NOT EXISTS KhuChoiGame(ID INTEGER PRIMARY KEY AUTOINCREMENT, NAME NVARCHAR(150), ADDRESS NVARCHAR(150), LONGITUDE DOUBLE, LATITUDE DOUBLE, IMAGE VARCHAR(400))");
            Cursor kq = db.GetData("SELECT * FROM KhuChoiGame");
            if (!kq.moveToFirst()) {
                try {
                    int dem = 0;// biến thể hiện vị trí dòng
                    int vt = 0;// biến thể hiện địa điểm thứ mấy
                    String data = "";
                    String TenDiaDiem = "";
                    String DiaChi = "";
                    Double KinhDo = null;
                    Double ViDo = null;
                    String Hinh = "";

                    while ((data = reader.readLine()) != null) {
                        if (dem == vt * 6)
                            TenDiaDiem = data;
                        if (dem == vt * 6 + 1)
                            DiaChi = data;
                        if (dem == vt * 6 + 2)
                            KinhDo = Double.parseDouble(data);
                        if (dem == vt * 6 + 3)
                            ViDo = Double.parseDouble(data);
                        if (dem == vt * 6 + 4)
                            Hinh = data;
                        if (dem == vt * 6 + 5) {
                            // thêm dữ liệu
                            db.QueryData("INSERT INTO KhuChoiGame VALUES(null, '" + TenDiaDiem + "', '" + DiaChi + "', '" + KinhDo + "', '" + ViDo + "', '" + Hinh + "')");
                            vt++;
                        }
                        dem++;
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }

    }

    public void taoDBNganHang() {
        // đọc file
        StringBuffer sbuffer = new StringBuffer();
        InputStream is = this.getResources().openRawResource(R.raw.nganhang);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        if (is != null) {
            // Tạo bảng
            db.QueryData("CREATE TABLE IF NOT EXISTS NganHang(ID INTEGER PRIMARY KEY AUTOINCREMENT, NAME NVARCHAR(150), ADDRESS NVARCHAR(150), LONGITUDE DOUBLE, LATITUDE DOUBLE, IMAGE VARCHAR(400))");
            Cursor kq = db.GetData("SELECT * FROM NganHang");
            if (!kq.moveToFirst()) {
                try {
                    int dem = 0;// biến thể hiện vị trí dòng
                    int vt = 0;// biến thể hiện địa điểm thứ mấy
                    String data = "";
                    String TenDiaDiem = "";
                    String DiaChi = "";
                    Double KinhDo = null;
                    Double ViDo = null;
                    String Hinh = "";

                    while ((data = reader.readLine()) != null) {
                        if (dem == vt * 6)
                            TenDiaDiem = data;
                        if (dem == vt * 6 + 1)
                            DiaChi = data;
                        if (dem == vt * 6 + 2)
                            KinhDo = Double.parseDouble(data);
                        if (dem == vt * 6 + 3)
                            ViDo = Double.parseDouble(data);
                        if (dem == vt * 6 + 4)
                            Hinh = data;
                        if (dem == vt * 6 + 5) {
                            // thêm dữ liệu
                            db.QueryData("INSERT INTO NganHang VALUES(null, '" + TenDiaDiem + "', '" + DiaChi + "', '" + KinhDo + "', '" + ViDo + "', '" + Hinh + "')");
                            vt++;
                        }
                        dem++;
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }

    }

    public void taoDBNhaHang() {
        // đọc file
        StringBuffer sbuffer = new StringBuffer();
        InputStream is = this.getResources().openRawResource(R.raw.nhahang);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        if (is != null) {
            // Tạo bảng
            db.QueryData("CREATE TABLE IF NOT EXISTS NhaHang(ID INTEGER PRIMARY KEY AUTOINCREMENT, NAME NVARCHAR(150), ADDRESS NVARCHAR(150), LONGITUDE DOUBLE, LATITUDE DOUBLE, IMAGE VARCHAR(400))");
            Cursor kq = db.GetData("SELECT * FROM NhaHang");
            if (!kq.moveToFirst()) {
                try {
                    int dem = 0;// biến thể hiện vị trí dòng
                    int vt = 0;// biến thể hiện địa điểm thứ mấy
                    String data = "";
                    String TenDiaDiem = "";
                    String DiaChi = "";
                    Double KinhDo = null;
                    Double ViDo = null;
                    String Hinh = "";

                    while ((data = reader.readLine()) != null) {
                        if (dem == vt * 6)
                            TenDiaDiem = data;
                        if (dem == vt * 6 + 1)
                            DiaChi = data;
                        if (dem == vt * 6 + 2)
                            KinhDo = Double.parseDouble(data);
                        if (dem == vt * 6 + 3)
                            ViDo = Double.parseDouble(data);
                        if (dem == vt * 6 + 4)
                            Hinh = data;
                        if (dem == vt * 6 + 5) {
                            // thêm dữ liệu
                            db.QueryData("INSERT INTO NhaHang VALUES(null, '" + TenDiaDiem + "', '" + DiaChi + "', '" + KinhDo + "', '" + ViDo + "', '" + Hinh + "')");
                            vt++;
                        }
                        dem++;
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }

    }

    public void taoDBNhaThuoc() {
        // đọc file
        StringBuffer sbuffer = new StringBuffer();
        InputStream is = this.getResources().openRawResource(R.raw.nhathuoc);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        if (is != null) {
            // Tạo bảng
            db.QueryData("CREATE TABLE IF NOT EXISTS NhaThuoc(ID INTEGER PRIMARY KEY AUTOINCREMENT, NAME NVARCHAR(150), ADDRESS NVARCHAR(150), LONGITUDE DOUBLE, LATITUDE DOUBLE, IMAGE VARCHAR(400))");
            Cursor kq = db.GetData("SELECT * FROM NhaThuoc");
            if (!kq.moveToFirst()) {
                try {
                    int dem = 0;// biến thể hiện vị trí dòng
                    int vt = 0;// biến thể hiện địa điểm thứ mấy
                    String data = "";
                    String TenDiaDiem = "";
                    String DiaChi = "";
                    Double KinhDo = null;
                    Double ViDo = null;
                    String Hinh = "";

                    while ((data = reader.readLine()) != null) {
                        if (dem == vt * 6)
                            TenDiaDiem = data;
                        if (dem == vt * 6 + 1)
                            DiaChi = data;
                        if (dem == vt * 6 + 2)
                            KinhDo = Double.parseDouble(data);
                        if (dem == vt * 6 + 3)
                            ViDo = Double.parseDouble(data);
                        if (dem == vt * 6 + 4)
                            Hinh = data;
                        if (dem == vt * 6 + 5) {
                            // thêm dữ liệu
                            db.QueryData("INSERT INTO NhaThuoc VALUES(null, '" + TenDiaDiem + "', '" + DiaChi + "', '" + KinhDo + "', '" + ViDo + "', '" + Hinh + "')");
                            vt++;
                        }
                        dem++;
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }

    }

    public void taoDBQuanAnVat() {
        // đọc file
        StringBuffer sbuffer = new StringBuffer();
        InputStream is = this.getResources().openRawResource(R.raw.quananvat);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        if (is != null) {
            // Tạo bảng
            db.QueryData("CREATE TABLE IF NOT EXISTS QuanAnVat(ID INTEGER PRIMARY KEY AUTOINCREMENT, NAME NVARCHAR(150), ADDRESS NVARCHAR(150), LONGITUDE DOUBLE, LATITUDE DOUBLE, IMAGE VARCHAR(400))");
            Cursor kq = db.GetData("SELECT * FROM QuanAnVat");
            if (!kq.moveToFirst()) {
                try {
                    int dem = 0;// biến thể hiện vị trí dòng
                    int vt = 0;// biến thể hiện địa điểm thứ mấy
                    String data = "";
                    String TenDiaDiem = "";
                    String DiaChi = "";
                    Double KinhDo = null;
                    Double ViDo = null;
                    String Hinh = "";

                    while ((data = reader.readLine()) != null) {
                        if (dem == vt * 6)
                            TenDiaDiem = data;
                        if (dem == vt * 6 + 1)
                            DiaChi = data;
                        if (dem == vt * 6 + 2)
                            KinhDo = Double.parseDouble(data);
                        if (dem == vt * 6 + 3)
                            ViDo = Double.parseDouble(data);
                        if (dem == vt * 6 + 4)
                            Hinh = data;
                        if (dem == vt * 6 + 5) {
                            // thêm dữ liệu
                            db.QueryData("INSERT INTO QuanAnVat VALUES(null, '" + TenDiaDiem + "', '" + DiaChi + "', '" + KinhDo + "', '" + ViDo + "', '" + Hinh + "')");
                            vt++;
                        }
                        dem++;
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }

    }

    public void taoDBQuanChay() {
        // đọc file
        StringBuffer sbuffer = new StringBuffer();
        InputStream is = this.getResources().openRawResource(R.raw.quanchay);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        if (is != null) {
            // Tạo bảng
            db.QueryData("CREATE TABLE IF NOT EXISTS QuanChay(ID INTEGER PRIMARY KEY AUTOINCREMENT, NAME NVARCHAR(150), ADDRESS NVARCHAR(150), LONGITUDE DOUBLE, LATITUDE DOUBLE, IMAGE VARCHAR(400))");
            Cursor kq = db.GetData("SELECT * FROM QuanChay");
            if (!kq.moveToFirst()) {
                try {
                    int dem = 0;// biến thể hiện vị trí dòng
                    int vt = 0;// biến thể hiện địa điểm thứ mấy
                    String data = "";
                    String TenDiaDiem = "";
                    String DiaChi = "";
                    Double KinhDo = null;
                    Double ViDo = null;
                    String Hinh = "";

                    while ((data = reader.readLine()) != null) {
                        if (dem == vt * 6)
                            TenDiaDiem = data;
                        if (dem == vt * 6 + 1)
                            DiaChi = data;
                        if (dem == vt * 6 + 2)
                            KinhDo = Double.parseDouble(data);
                        if (dem == vt * 6 + 3)
                            ViDo = Double.parseDouble(data);
                        if (dem == vt * 6 + 4)
                            Hinh = data;
                        if (dem == vt * 6 + 5) {
                            // thêm dữ liệu
                            db.QueryData("INSERT INTO QuanChay VALUES(null, '" + TenDiaDiem + "', '" + DiaChi + "', '" + KinhDo + "', '" + ViDo + "', '" + Hinh + "')");
                            vt++;
                        }
                        dem++;
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }

    }

    public void taoDBRapChieuPhim() {
        // đọc file
        StringBuffer sbuffer = new StringBuffer();
        InputStream is = this.getResources().openRawResource(R.raw.rapchieuphim);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        if (is != null) {
            // Tạo bảng
            db.QueryData("CREATE TABLE IF NOT EXISTS RapChieuPhim(ID INTEGER PRIMARY KEY AUTOINCREMENT, NAME NVARCHAR(150), ADDRESS NVARCHAR(150), LONGITUDE DOUBLE, LATITUDE DOUBLE, IMAGE VARCHAR(400))");
            Cursor kq = db.GetData("SELECT * FROM RapChieuPhim");
            if (!kq.moveToFirst()) {
                try {
                    int dem = 0;// biến thể hiện vị trí dòng
                    int vt = 0;// biến thể hiện địa điểm thứ mấy
                    String data = "";
                    String TenDiaDiem = "";
                    String DiaChi = "";
                    Double KinhDo = null;
                    Double ViDo = null;
                    String Hinh = "";

                    while ((data = reader.readLine()) != null) {
                        if (dem == vt * 6)
                            TenDiaDiem = data;
                        if (dem == vt * 6 + 1)
                            DiaChi = data;
                        if (dem == vt * 6 + 2)
                            KinhDo = Double.parseDouble(data);
                        if (dem == vt * 6 + 3)
                            ViDo = Double.parseDouble(data);
                        if (dem == vt * 6 + 4)
                            Hinh = data;
                        if (dem == vt * 6 + 5) {
                            // thêm dữ liệu
                            db.QueryData("INSERT INTO RapChieuPhim VALUES(null, '" + TenDiaDiem + "', '" + DiaChi + "', '" + KinhDo + "', '" + ViDo + "', '" + Hinh + "')");
                            vt++;
                        }
                        dem++;
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }

    }

    public void taoDBSieuThi() {
        // đọc file
        StringBuffer sbuffer = new StringBuffer();
        InputStream is = this.getResources().openRawResource(R.raw.sieuthi);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        if (is != null) {
            // Tạo bảng
            db.QueryData("CREATE TABLE IF NOT EXISTS SieuThi(ID INTEGER PRIMARY KEY AUTOINCREMENT, NAME NVARCHAR(150), ADDRESS NVARCHAR(150), LONGITUDE DOUBLE, LATITUDE DOUBLE, IMAGE VARCHAR(400))");
            Cursor kq = db.GetData("SELECT * FROM SieuThi");
            if (!kq.moveToFirst()) {
                try {
                    int dem = 0;// biến thể hiện vị trí dòng
                    int vt = 0;// biến thể hiện địa điểm thứ mấy
                    String data = "";
                    String TenDiaDiem = "";
                    String DiaChi = "";
                    Double KinhDo = null;
                    Double ViDo = null;
                    String Hinh = "";

                    while ((data = reader.readLine()) != null) {
                        if (dem == vt * 6)
                            TenDiaDiem = data;
                        if (dem == vt * 6 + 1)
                            DiaChi = data;
                        if (dem == vt * 6 + 2)
                            KinhDo = Double.parseDouble(data);
                        if (dem == vt * 6 + 3)
                            ViDo = Double.parseDouble(data);
                        if (dem == vt * 6 + 4)
                            Hinh = data;
                        if (dem == vt * 6 + 5) {
                            // thêm dữ liệu
                            db.QueryData("INSERT INTO SieuThi VALUES(null, '" + TenDiaDiem + "', '" + DiaChi + "', '" + KinhDo + "', '" + ViDo + "', '" + Hinh + "')");
                            vt++;
                        }
                        dem++;
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }

    }

    public void taoDBTramXang() {
        // đọc file
        StringBuffer sbuffer = new StringBuffer();
        InputStream is = this.getResources().openRawResource(R.raw.tramxang);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        if (is != null) {
            // Tạo bảng
            db.QueryData("CREATE TABLE IF NOT EXISTS TramXang(ID INTEGER PRIMARY KEY AUTOINCREMENT, NAME NVARCHAR(150), ADDRESS NVARCHAR(150), LONGITUDE DOUBLE, LATITUDE DOUBLE, IMAGE VARCHAR(400))");
            Cursor kq = db.GetData("SELECT * FROM TramXang");
            if (!kq.moveToFirst()) {
                try {
                    int dem = 0;// biến thể hiện vị trí dòng
                    int vt = 0;// biến thể hiện địa điểm thứ mấy
                    String data = "";
                    String TenDiaDiem = "";
                    String DiaChi = "";
                    Double KinhDo = null;
                    Double ViDo = null;
                    String Hinh = "";

                    while ((data = reader.readLine()) != null) {
                        if (dem == vt * 6)
                            TenDiaDiem = data;
                        if (dem == vt * 6 + 1)
                            DiaChi = data;
                        if (dem == vt * 6 + 2)
                            KinhDo = Double.parseDouble(data);
                        if (dem == vt * 6 + 3)
                            ViDo = Double.parseDouble(data);
                        if (dem == vt * 6 + 4)
                            Hinh = data;
                        if (dem == vt * 6 + 5) {
                            // thêm dữ liệu
                            db.QueryData("INSERT INTO TramXang VALUES(null, '" + TenDiaDiem + "', '" + DiaChi + "', '" + KinhDo + "', '" + ViDo + "', '" + Hinh + "')");
                            vt++;
                        }
                        dem++;
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

        }
    }

    public ArrayList<PlaceModel> layDiaDiemTheoLoai(String table) {
        Double latitude = 0.0;
        Double longitude = 0.0;

//        mMap.clear();
        ArrayList<PlaceModel> pList = new ArrayList<>();

        Cursor kq = db.GetData("SELECT * FROM "+table+"");
        while (kq.moveToNext())
        {
            String ten = kq.getString(1);
            String diachi = kq.getString(2);
            latitude = kq.getDouble(3);
            longitude = kq.getDouble(4);
            String hinh = kq.getString(5);

            PlaceModel p = new PlaceModel(ten, diachi, latitude, longitude, hinh);
            pList.add(p);

        }
        return pList;
    }

    public ArrayList<PlaceModel> layTatCaDiaDiem()
    {
        ArrayList<PlaceModel> pListAll = new ArrayList<>();
        String[]loais = new String[] { "Bar", "NganHang", "RapChieuPhim", "Cafe", "QuanAnVat", "TrungTamAnhNgu",
                "KhuChoiGame", "BenhVien", "KhachSan", "Karaoke", "TrungTamThuongMai", "TramXang",
                "ChuaDen", "NhaThuoc", "CongVienVuiChoi", "NhaHang", "TruongHoc", "SieuThi", "QuanChay", "VienThong" };
        for (String loai : loais) {
            System.out.println(loai);
            ArrayList<PlaceModel> arrTemp = new ArrayList<>();
            arrTemp = layDiaDiemTheoLoai(loai);
            for(PlaceModel p: arrTemp){
                pListAll.add(p);
            }
        }
        return pListAll;
    }

    public void goListCategory(ArrayList<PlaceModel> pList)
    {
        Intent intent = new Intent(MapsActivity.this, ListPlaceActivity.class);

        String listSerializedToJson = new Gson().toJson(pList);
        intent.putExtra("category", listSerializedToJson);
        startActivity(intent);
    }

    public void goSearch(ArrayList<PlaceModel> pList)
    {
        Intent intent = new Intent(MapsActivity.this, SearchActivity.class);

        String listSerializedToJson = new Gson().toJson(pList);
        intent.putExtra("category", listSerializedToJson);
        startActivity(intent);
    }

    //nhận dữ liệu chọn bên list và add marker
    @Override
    protected void onResume()
    {
        super.onResume();
    }

    public void loadDiaDiem(View v) {
        switch (v.getId()) {
            case R.id.btnBar:
                //xử lý load bar
                goListCategory(layDiaDiemTheoLoai("Bar"));
                break;
            case R.id.btnBank:
                //xử lý load ngân hàng
                goListCategory(layDiaDiemTheoLoai("NganHang"));
                break;
            case R.id.btnCinema:
                //xử lý load rạp phim
                goListCategory(layDiaDiemTheoLoai("RapChieuPhim"));
                break;
            case R.id.btnCoffe:
                //xử lý load cafffe
                goListCategory(layDiaDiemTheoLoai("Cafe"));
                break;
            case R.id.btnEat:
                //xử lý quán ăn vặt
                goListCategory(layDiaDiemTheoLoai("QuanAnVat"));
                break;
            case R.id.btnEnglish:
                //xử lý load trung tâm anh ngữ
                goListCategory(layDiaDiemTheoLoai("TrungTamAnhNgu"));
                break;
            case R.id.btnGame:
                //xứ lý khu vui chơi game
                goListCategory(layDiaDiemTheoLoai("KhuChoiGame"));
                break;
            case R.id.btnHospital:
                //xứ lý bệnh viện
                goListCategory(layDiaDiemTheoLoai("BenhVien"));
                break;
            case R.id.btnHotel:
                //xử lý load khách sạn
                goListCategory(layDiaDiemTheoLoai("KhachSan"));
                break;
            case R.id.btnKaraoke:
                //xử lý karaoke
                goListCategory(layDiaDiemTheoLoai("Karaoke"));
                break;
            case R.id.btnMail:
                //xử lý trung tâm mua sắm
                goListCategory(layDiaDiemTheoLoai("TrungTamThuongMai"));
                break;
            case R.id.btnOil:
                //xử lý trạm xăng
                goListCategory(layDiaDiemTheoLoai("TramXang"));
                break;
            case R.id.btnPagoda:
                //xử lý đền chùa
                goListCategory(layDiaDiemTheoLoai("ChuaDen"));
                break;
            case R.id.btnPara:
                //xử lý nhà thuốc
                goListCategory(layDiaDiemTheoLoai("NhaThuoc"));
                break;
            case R.id.btnPark:
                //xử lý công viên giải trí
                goListCategory(layDiaDiemTheoLoai("CongVienVuiChoi"));
                break;
            case R.id.btnRes:
                //xử lý load nhà hàng
                goListCategory(layDiaDiemTheoLoai("NhaHang"));
                break;
            case R.id.btnSchool:
                //xử lý trường học
                goListCategory(layDiaDiemTheoLoai("TruongHoc"));
                break;
            case R.id.btnSuper:
                //xứ lý siêu thị
                goListCategory(layDiaDiemTheoLoai("SieuThi"));
                break;
            case R.id.btnVega:
                //xử lý quán chay
                goListCategory(layDiaDiemTheoLoai("QuanChay"));
                break;
            case R.id.btnWifi:
                //xử lý viễn thông
                goListCategory(layDiaDiemTheoLoai("VienThong"));
                break;

        }

    }

    public void chiDuong(View v){
        mMap.clear();
        if(getIntent().getStringExtra("oneplace") != null) {
            String SerializedToJson = getIntent().getExtras().getString("oneplace");
            PlaceModel place = new Gson().fromJson(SerializedToJson, new TypeToken<PlaceModel>() {
            }.getType());
            //Tìm đường
                LatLng origin = new LatLng(this.latitude, this.longitude);
                LatLng destination = new LatLng(place.Latitude, place.Longitude);
                System.out.println("Các cậu"+ origin);
                System.out.println("Các cậu"+ destination);
                directionFinder = new DirectionFinder(directionFinderListener, origin, destination);
                System.out.println("Các cậu"+directionFinder.origin);
                System.out.println("Các cậu"+directionFinder.destination);

                try {
                    directionFinder.nameDestin = place.Name;
                    directionFinder.execute();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
        }
    }

    //Vẽ phạm vi
//    public void showCircleToGoogleMap(LatLng position, float radius) {
//        if (position == null) {
//            return;
//        }
//        CircleOptions circleOptions = new CircleOptions();
//        circleOptions.center(position);
//        //Radius in meters
//        circleOptions.radius(radius * 1000);
//        circleOptions.fillColor(getResources()
//                .getColor(R.color.circle_on_map));
//        circleOptions.strokeColor(getResources()
//                .getColor(R.color.circle_on_map));
//        circleOptions.strokeWidth(0);
//        if (mGoogleMap != null) {
//            mGoogleMap.addCircle(circleOptions);
//        }
//    }
}


