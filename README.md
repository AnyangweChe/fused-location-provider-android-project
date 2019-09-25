# Saving GPS Location to Database from Android Fused Location Provider API. Android Project
In this tutorial, i will show you how to get GPS location from your android device and save to a database. The project is divided into two parts. The mobile code and the server side code.


![full project image](https://miro.medium.com/max/1080/1*vv6xz-RhZA1mtNsqq0rhyg.png)

# Creating the Android Project
The app we’re gonna build will have two simple screens Realtime Fused Location Screen and Realtime Location Screen.
1. In Android Studio, create a new project from File ⇒ New Project and fill all the required details.
2. Open build.gradle and add volley library support by adding
implementation ‘com.android.volley:volley:1.1.0’, implementation ‘com.google.android.gms:play-services-location:17.0.0’ under dependencies.
3. Now open AndroidManifest.xml and add INTERNET permission, ACCESS_COARSE_LOCATION permision and ACCESS_FINE_LOCATION permission. Also add other activities (RealtimeActivity and MainActivity) which we are going to create shortly.

4. Create an activity class named MainActivity.java under activity package. In this class
connectGoogleClient() — Method verifies google play service and connects GoogleApiClient

The requestLocationUpdate() — Method starts location updates with sLocationRequest as the location request object and sLocationCallback object to get results of the location request. These objects are initialized when GoogleApiClient is connected i.e in the connected callback of GoogleApiClient.

The buildGoogleApiClient() — Method builds the GoogleApiClient object and adds the ConnectionCallbacks ( where sLocationRequest and sLocationCallback are initialized), the OnConnectionFailedListener, the api we want to use i.e the Location Api and build. We call connectGoogleClient() method to connect.

The locationSettingsRequest() — Method builds the location settings and adds a location request. The method checks location service settings. if location service is not activated, in the OnFailureListener callback, a ResolvableApiException with startResolutionForResult is started which shows a dialog to enable location service. Response from the dialog is handled in onActivityResult. If location service is activated, requestLocationUpdate() is called in the OnSuccessListener callback of the SettingClient


![location service dialog](https://miro.medium.com/max/1080/1*dYCl34diMKFEzrMPqrrA7Q.png)


9. Create an activity class named RealtimeActivity.java under activity package.
In this class

The url for the server is “http://192.168.42.85/RealtimeLocation/addlocation.php" replace the ip address with that of your server or with your domain name

The unique identifier for app installation UUID.randomUUID().toString(). This identifier is used to store location update for each request sent to the server.

connectGoogleClient() — Method verifies google play service and connects GoogleApiClient
The requestLocationUpdate() — Method starts location updates with mLocationRequest as the location request object and mLocationCallback object to get results of the location request. These objects are initialized when GoogleApiClient is connected i.e in the connected callback of GoogleApiClient.

The buildGoogleApiClient() — Method builds the GoogleApiClient object and adds the ConnectionCallbacks ( where mLocationRequest and mLocationCallback are initialized), the OnConnectionFailedListener, the api we want to use i.e the Location Api and build. We call connectGoogleClient() method to connect.

The locationSettingsRequest() — Method builds the location settings and adds a location request. The method checks location service settings. if location service is not activated, in the OnFailureListener callback, a ResolvableApiException with startResolutionForResult is started which shows a dialog to enable location service. Response from the dialog is handled in onActivityResult. If location service is activated, requestLocationUpdate() is called in the OnSuccessListener callback of the SettingsClient

The saveLocation() — Method send location updates to the server which is called in mLocationCallback of Fused location client. It takes 3 parameters. The uniqueId for each app instance, longitude and latitude. The location update is sent to the server using the volley library. Response from the server is returned in json and the arrayadapter is updated.


![location service dialog](https://miro.medium.com/max/1080/1*Y1PPYhcNQ2iVWgFZThmGqw.png)
