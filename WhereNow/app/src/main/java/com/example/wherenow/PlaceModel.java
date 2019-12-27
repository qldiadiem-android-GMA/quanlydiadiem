package com.example.wherenow;

import java.util.ArrayList;

public class PlaceModel {
    public int id;
    public String Name = "";
    public String Address = "";
    public double Latitude = 0;
    public double Longitude = 0;
    public int Category = 0;
    public String PicturePath = "";


    public PlaceModel (int id, String Name, String Address, double Latitude, double Longitude, int Category, String PicturePath) {
        this.id = id;
        this.Name = Name;
        this.Address = Address;
        this.Latitude = Latitude;
        this.Longitude = Longitude;
        this.Category = Category;
        this.PicturePath = PicturePath;
    }

    public PlaceModel (String Name, String Address, double Latitude, double Longitude, int Category, String PicturePath) {
        this.Name = Name;
        this.Address = Address;
        this.Latitude = Latitude;
        this.Longitude = Longitude;
        this.Category = Category;
        this.PicturePath = PicturePath;
    }

    public PlaceModel (String Name, String Address, double Latitude, double Longitude, String PicturePath) {
        this.Name = Name;
        this.Address = Address;
        this.Latitude = Latitude;
        this.Longitude = Longitude;
        this.PicturePath = PicturePath;
    }

}
