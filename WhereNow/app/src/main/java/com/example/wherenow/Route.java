package com.example.wherenow;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

public class Route {
    public Distance distance;
    public Duration duration;
    public String endName;
    public String endAddress;
    public LatLng endLocation;
    public String startName;
    public String startAddress;
    public LatLng startLocation;

    public List<LatLng> points;
}
