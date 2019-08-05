package com.logixcess.smarttaxiapplication.Utils;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.location.Location;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.logixcess.smarttaxiapplication.Fragments.MapFragment;
import com.logixcess.smarttaxiapplication.Models.Order;
import com.logixcess.smarttaxiapplication.Models.RoutePoints;
import com.logixcess.smarttaxiapplication.Models.SharedRide;
import com.logixcess.smarttaxiapplication.Models.UserFareRecord;
import com.logixcess.smarttaxiapplication.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import java.util.Map;

import static com.logixcess.smarttaxiapplication.Fragments.MapFragment.new_order;

public class FareCalculation
{

    /*public double getBaseFare(String vehicle)
    {
        double base_fair = 0;
        switch(vehicle)
        {
            case Helper.VEHICLE_CAR:
                base_fair =  50;
                break;
            case Helper.VEHICLE_MINI:
                base_fair =  30;
                break;
            case Helper.VEHICLE_NANO:
                base_fair =  20;
                break;
            case Helper.VEHICLE_VIP:
                base_fair =  60;
                break;
            case Helper.VEHICLE_THREE_WHEELER:
                base_fair =  30;
                break;
        }
        return base_fair;
    }*/
    
   // public double getCost()
   // {
      //  return Constants.BASE_FAIR_PER_KM * Double.parseDouble(MapFragment.new_order.getTotal_kms());
   // }
    
    
    
    public double getBaseFare2(String vehicle)
    {
        double base_fare = 0;
        switch(vehicle)
        {
            case Helper.VEHICLE_CAR:
                base_fare =  70;
                break;
            case Helper.VEHICLE_MINI:
                base_fare =  65;
                break;
            case Helper.VEHICLE_NANO:
                base_fare =  60;
                break;
            case Helper.VEHICLE_VIP:
                base_fare =  100;
                break;
            case Helper.VEHICLE_THREE_WHEELER:
                base_fare =  50;
                break;
        }
        return base_fare;
        //return getCostTotal(base_fare, Double.parseDouble(MapFragment.new_order.getTotal_kms()) - 1);
    }
    
    public double getCostSingleRide()
    {
        double km = Double.valueOf(new_order.getTotal_kms());
        /*
         * 6.4 =) 7
         *   km * 1000 - 1000;
         *   km * base * .6
         *   km + base
         * */
        double lessBase = Constants.BASE_FAIR_PER_KM * .6;
        lessBase = lessBase / 1000;
        km = km * 1000;
        double lessKM = km - 1000;
        lessKM = lessKM * lessBase;
        return lessKM + Constants.BASE_FAIR_PER_KM;

    }
    
    public SharedRide calculateFareForSharedRide(List<Order> ordersInSharedRide, SharedRide currentSharedRide, Location myLocation, String vehicleType) {
        // contains record of points every 10 seconds
        HashMap<String,List<RoutePoints>> allPoints = currentSharedRide.getAllJourneyPoints();
        // contains record of user fare
        HashMap<String,UserFareRecord> allFareRecords = currentSharedRide.getPassengerFares();
        // passenger count
       
        for (Map.Entry<String, Boolean> entry : currentSharedRide.getPassengers().entrySet()) {
            String key = entry.getKey();
            Order order = getOrderByID(key,ordersInSharedRide);
            if(order == null)
                continue;
            int totalOnRide = getActiveRideUsersCount(currentSharedRide);
            if(totalOnRide < 1)
                return currentSharedRide;
            boolean isOnRide = isDriverOnRide(order.getOrder_id(),currentSharedRide);
    
            // in case : for initial point
            if(!allPoints.containsKey(key)){
                List<RoutePoints> routePoints = new ArrayList<>();
                
                    RoutePoints rP = new RoutePoints(order.getPickupLat(),order.getPickupLong());
                    routePoints.add(rP);
                    allPoints.put(key,routePoints);
                
            }
            
            if(!isOnRide)
                continue;
            if(allPoints.containsKey(key)){
                List<RoutePoints> latLngList = allPoints.get(key);
                
                
                if(latLngList != null){
                    
                    Location lastPointLocation = new Location("lastPoint");
                    lastPointLocation.setLatitude(latLngList.get(latLngList.size() - 1).getLatitude());
                    lastPointLocation.setLongitude(latLngList.get(latLngList.size() - 1).getLongitude());
                    double totalDistanceFromPrevPoint = lastPointLocation.distanceTo(myLocation);
                    if(totalDistanceFromPrevPoint > 0) {
                        latLngList.add(new RoutePoints(myLocation.getLatitude()
                                , myLocation.getLongitude(), totalDistanceFromPrevPoint,totalOnRide));
                            allPoints.put(key, latLngList);
                    }
                    UserFareRecord fareRecord = currentSharedRide.getPassengerFares().get(key);
                    fareRecord = calculateFareOfSingleVehicle(latLngList,fareRecord,
                            totalOnRide, vehicleType,myLocation,key,ordersInSharedRide);
                        allFareRecords.put(key,fareRecord);
                }
            }
        }
        currentSharedRide.setPassengerFares(allFareRecords);
        currentSharedRide.setAllJourneyPoints(allPoints);
        return currentSharedRide;
    }
    
 
    
    private int getActiveRideUsersCount(SharedRide sharedRide) {
        int Count = 0;
        HashMap<String,Boolean> ids = sharedRide.getOrderIDs();
        for(Map.Entry<String, Boolean> entry : ids.entrySet()){
            if(entry.getValue())
                Count++;
        }
        return Count;
    }
    
    
    private UserFareRecord calculateFareOfSingleVehicle(List<RoutePoints> userLatLngs, UserFareRecord fareRecord,
                                                        int totalPassengers, String vehicleType, Location myLocation, String key, List<Order> ordersInSharedRide){
        double baseFare = fareRecord.getBaseFare();
        fareRecord.setBaseFare(baseFare);
        double totalKms = getTotalDistanceTraveled(userLatLngs);
        int totalKmsRecord = fareRecord.getLatLngs().size();
        if(fareRecord.getUserFare() == null){
            RoutePoints cLatLng = userLatLngs.get(userLatLngs.size() - 1);
            String latlngKey = String.valueOf(cLatLng.getLatitude()) + "," + String.valueOf(cLatLng.getLongitude());
            HashMap<String, Double> fares = new HashMap<>();
            fares.put(Helper.getRefinedLatLngKeyForHashMap(latlngKey),baseFare);
            fareRecord.setUserFare(fares);
        }
        if(totalKms > totalKmsRecord){
            fareRecord = insertAnotherKm(userLatLngs.get(userLatLngs.size() - 1),fareRecord,totalPassengers, key,ordersInSharedRide,baseFare);
        }
        return fareRecord;
    }
    
    private UserFareRecord insertAnotherKm(RoutePoints cLatLng, UserFareRecord fareRecord, int totalPassengers, String key, List<Order> ordersInSharedRide, double baseFare) {
        List<RoutePoints> kmLatLng = fareRecord.getLatLngs();
        kmLatLng.add(cLatLng);
        fareRecord.setLatLngs(kmLatLng);
        double currentFare;
        switch (totalPassengers){
            case 1:
                switch (fareRecord.getLatLngs().size()){
                    case 1:
                        currentFare = getFirstKMFare(fareRecord);
                        break;
                    default:
                        currentFare = getRegularKMFare(totalPassengers,fareRecord);
                        break;
                }
                break;
            case 2:
                switch (fareRecord.getLatLngs().size()){
                    case 1:
                        currentFare = getFirstKMFare(fareRecord);
                        break;
                    case 2:
                        currentFare = getSecondKMFare(fareRecord);
                        break;
                    default:
                        currentFare = getRegularKMFare(totalPassengers,fareRecord);
                        break;
                }
                break;
            case 3:
                switch (fareRecord.getLatLngs().size()){
                    case 1:
                        currentFare = getFirstKMFare(fareRecord);
                        break;
                    case 2:
                        currentFare = getSecondKMFare(fareRecord);
                        break;
                    case 3:
                        currentFare = fareRecord.getBaseFare() * .5;
                        break;
                    default:
                        currentFare = getRegularKMFare(totalPassengers,fareRecord);
                        break;
                }
                break;
            default:
                switch (fareRecord.getLatLngs().size()){
                    case 1:
            
                        currentFare = getFirstKMFare(fareRecord);
                        break;
                    case 2:
                        currentFare = getSecondKMFare(fareRecord);
                        break;
                    case 3:
                        currentFare = fareRecord.getBaseFare() * .5;
                        break;
                    case 4:
                        currentFare = fareRecord.getBaseFare() * .4;
                        break;
                    default:
                        currentFare = getRegularKMFare(totalPassengers,fareRecord);
                        break;
                }
                break;
        }
        
        HashMap<String,Double> fare = fareRecord.getUserFare();
        String latlngKey = String.valueOf(cLatLng.getLatitude()) + "," + String.valueOf(cLatLng.getLongitude());
        fare.put(Helper.getRefinedLatLngKeyForHashMap(latlngKey),currentFare);
        
        Order order = getOrderByID(key,ordersInSharedRide);
        if(order != null) {
            HashMap<String, Double> temp = new HashMap<>();
            if (fare.containsValue(0.0)) {
                for (Map.Entry<String, Double> entry : fare.entrySet()) {
                    String k = entry.getKey();
                    Double val = entry.getValue();
                    if(val == 0.0){
                        temp.put(entry.getKey(),baseFare);
                    }else{
                        temp.put(entry.getKey(),entry.getValue());
                    }
                }
            }
            if(temp.size() == fareRecord.getUserFare().size())
                fareRecord.setUserFare(temp);
        }else
            fareRecord.setUserFare(fare);
        return fareRecord;
    }
    
    private Order getOrderByID(String key, List<Order> ordersInSharedRide) {
        for (Order order : ordersInSharedRide)
            if(order.getUser_id().equals(key))
                return order;
        
        return null;
    }
    
    private double getFirstKMFare(UserFareRecord fareRecord) {
        return fareRecord.getBaseFare();
    }
    
    private double getSecondKMFare(UserFareRecord fareRecord) {
        return fareRecord.getBaseFare() * .6;
    }
    
    
    private double getRegularKMFare(int totalPassengers, UserFareRecord fareRecord) {
        switch (totalPassengers){
            case 1:
                return fareRecord.getBaseFare() * .6;
            case 2:
                return fareRecord.getBaseFare() * .5;
            case 3:
                return fareRecord.getBaseFare() * .4;
            case 4:
                return fareRecord.getBaseFare() * .3;
            
        }
        
        return fareRecord.getBaseFare() * .6;
    }
    
    
    
    private double getTotalDistanceTraveled(List<RoutePoints> userLatLngs) {
        double totalDistance = 0;
//        Location lastPointLocation = null;
//        Location nextPointLocation = new Location("nextPoint");
//
        for(int i = 0; i < userLatLngs.size(); i++){
            RoutePoints c = userLatLngs.get(i);
            totalDistance = totalDistance + c.getDistanceinmeters();
//            if(lastPointLocation == null){
//                lastPointLocation = new Location("lastPoint");
//                lastPointLocation.setLatitude(c.getLatitude());
//                lastPointLocation.setLongitude(c.getLongitude());
//            }else{
//                nextPointLocation.setLatitude(c.getLatitude());
//                nextPointLocation.setLongitude(c.getLongitude());
//                totalDistance += lastPointLocation.distanceTo(nextPointLocation);
//                lastPointLocation.setLatitude(c.getLatitude());
//                lastPointLocation.setLongitude(c.getLongitude());
//            }
            
        }
        return totalDistance / 1000; // in kms
    }
    
    
    
    private boolean isDriverOnRide(String key, SharedRide sharedRide){
        // checking if driver is on ride
        
        HashMap<String,Boolean> ids = sharedRide.getOrderIDs();
        for(Map.Entry<String, Boolean> entry : ids.entrySet()){
            if(entry.getKey().equals(key)){
                return entry.getValue();
            }
        }
        return false;
    }
    
    
    public MarkerOptions getVehicleMarkerOptions(Context context,LatLng latLng, String vehicleType){
        MarkerOptions markerOptions = new MarkerOptions().position(latLng).title("Driver");
        markerOptions.icon(getDrawableByType(context,vehicleType));
        return markerOptions;
    }
    
    private BitmapDescriptor getDrawableByType(Context context, String vehicleType) {
        Drawable drawable = context.getResources().getDrawable(R.drawable.ic_option_nano);
        switch (vehicleType){
            case Helper.VEHICLE_CAR:
                drawable = context.getResources().getDrawable(R.drawable.ic_option_car);
                break;
            case Helper.VEHICLE_MINI:
                drawable = context.getResources().getDrawable(R.drawable.ic_option_mini);
                break;
            case Helper.VEHICLE_NANO:
                drawable = context.getResources().getDrawable(R.drawable.ic_option_nano);
                break;
            case Helper.VEHICLE_THREE_WHEELER:
                drawable = context.getResources().getDrawable(R.drawable.ic_option_three_wheeler);
                break;
            case Helper.VEHICLE_VIP:
                drawable = context.getResources().getDrawable(R.drawable.ic_option_vip);
                break;
        }
        Bitmap driverPin = Helper.convertToBitmap(drawable, 100, 100);
        return BitmapDescriptorFactory.fromBitmap(driverPin);
    }
    
    

}
