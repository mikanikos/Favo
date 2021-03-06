package ch.epfl.favo.view.tabs;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import ch.epfl.favo.MainActivity;
import ch.epfl.favo.R;
import ch.epfl.favo.cache.CacheUtil;
import ch.epfl.favo.exception.NoPermissionGrantedException;
import ch.epfl.favo.exception.NoPositionFoundException;
import ch.epfl.favo.favor.Favor;
import ch.epfl.favo.favor.FavorStatus;
import ch.epfl.favo.gps.FavoLocation;
import ch.epfl.favo.util.CommonTools;
import ch.epfl.favo.util.DependencyFactory;
import ch.epfl.favo.view.tabs.addFavor.FavorEditingView;
import ch.epfl.favo.viewmodel.IFavorViewModel;

import static java.lang.Double.parseDouble;

/**
 * View will contain a map and a favor request pop-up. It is implemented using the {@link Fragment}
 * subclass.
 */
@SuppressLint("NewApi")
public class MapPage extends Fragment
    implements OnMapReadyCallback,
        GoogleMap.OnInfoWindowClickListener,
        GoogleMap.InfoWindowAdapter {

  public static final String LOCATION_ARGUMENT_KEY = "LOCATION_ARGS";
  public static final String LATITUDE_ARGUMENT_KEY = "LATITUDE_ARGS";
  public static final String LONGITUDE_ARGUMENT_KEY = "LONGITUDE_ARGS";
  public static final int NEW_REQUEST = 1;
  public static final int EDIT_EXISTING_LOCATION = 2;
  public static final int SHARE_LOCATION = 3;
  public static final int OBSERVE_LOCATION = 4;
  public static final int OBSERVE_FAVOR = 5;
  private Button doneButton;

  private IFavorViewModel favorViewModel;
  private View view;
  private GoogleMap mMap;
  private Location mLocation;

  private final Map<String, Marker> favorsAroundMe = new HashMap<>();
  private double radiusThreshold;

  private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
  private int defaultZoomLevel;
  private int defaultZoomLevelDif = 2;
  private final ArrayList<Integer> mapStyles =
      new ArrayList<Integer>() {
        {
          add(R.raw.google_map_style_standard);
          add(R.raw.google_map_style_silver);
          add(R.raw.google_map_style_night);
        }
      };
  private static int intentType;
  private boolean mLocationPermissionGranted = false;
  private boolean firstOpenApp = true;
  private final ArrayList<Marker> existAddedNewMarkers = new ArrayList<>();

  public MapPage() {
    // Required empty public constructor
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    getLocationPermission();
    view = inflater.inflate(R.layout.fragment_map, container, false);
    // setup offline map button
    FloatingActionButton offlineBtn = view.findViewById(R.id.offline_map_button);
    offlineBtn.setOnClickListener(this::onOfflineMapClick);

    if (DependencyFactory.isOfflineMode(requireContext())) offlineBtn.setVisibility(View.VISIBLE);
    else offlineBtn.setVisibility(View.GONE);
    favorViewModel =
        (IFavorViewModel)
            new ViewModelProvider(requireActivity())
                .get(DependencyFactory.getCurrentViewModelClass());
    // CRITICAL to prevent later calling from getting null value, DO NOT DELETE IT.
    favorViewModel.ObserveAllUserActiveFavorsAndCurrentUser();
    // setup toggle between map and nearby list
    RadioButton nearbyFavorListToggle = view.findViewById(R.id.list_switch);
    nearbyFavorListToggle.setOnClickListener(this::onToggleClick);

    SupportMapFragment mapFragment =
        (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
    if (mapFragment != null && mLocationPermissionGranted) mapFragment.getMapAsync(this);
    return view;
  }

  @SuppressLint("MissingPermission")
  @Override
  public void onMapReady(GoogleMap googleMap) {
    // set zoomLevel from user preference
    String radiusSetting =
        CacheUtil.getInstance()
            .getValueFromCacheStr(requireContext(), getString(R.string.radius_map_setting_key));
    radiusThreshold =
        (!radiusSetting.isEmpty())
            ? Integer.parseInt(radiusSetting)
            : Integer.parseInt(getString(R.string.default_radius));
    defaultZoomLevel = CommonTools.notificationRadiusToZoomLevel(radiusThreshold);

    // set map style from user preference
    String mapStyleSetting =
        CacheUtil.getInstance()
            .getValueFromCacheStr(requireContext(), getString(R.string.map_style_key));
    if (mapStyleSetting == null || mapStyleSetting.equals("")) mapStyleSetting = "0";
    int mapModeIndex = Integer.parseInt(mapStyleSetting);
    MapStyleOptions mapStyleOptions =
        MapStyleOptions.loadRawResourceStyle(requireContext(), mapStyles.get(mapModeIndex));
    googleMap.setMapStyle(mapStyleOptions);

    mMap = googleMap;
    mMap.clear();
    mMap.setMyLocationEnabled(true);
    mMap.setInfoWindowAdapter(this);
    mMap.setOnInfoWindowClickListener(this);
    mMap.setOnMapLongClickListener(new LongClick());
    mMap.setOnMarkerDragListener(new MarkerDrag());

    try {
      mLocation = DependencyFactory.getCurrentGpsTracker(getContext()).getLocation();
    } catch (NoPermissionGrantedException e) {
      CommonTools.showSnackbar(requireView(), getString(R.string.no_position_permission_tip));
      return;
    } catch (NoPositionFoundException e) {
      CommonTools.showSnackbar(requireView(), getString(R.string.no_position_found_tip));
      return;
    } catch (Exception e) {
      CommonTools.showSnackbar(requireView(), getString(R.string.report_unknown_error));
      return;
    }
    if (intentType != 0) //
    {
      drawMarkerAndFocusOnLocation();
      doneButton.setOnClickListener(
          v -> {
            if (intentType == SHARE_LOCATION) sendLocationToChat(existAddedNewMarkers.get(0));
            else if (intentType == OBSERVE_LOCATION) {
              Navigation.findNavController(requireView()).navigate(R.id.action_nav_map_to_chatView);
            } else if (intentType == OBSERVE_FAVOR) {
              Bundle favorBundle = new Bundle();
              favorBundle.putString(
                  CommonTools.FAVOR_ARGS, favorViewModel.getObservedFavor().getValue().getId());
              Navigation.findNavController(view)
                  .navigate(R.id.action_nav_map_to_favorPublishedView_without_return, favorBundle);
            } else {
              requestFavorOnMarkerLocation(existAddedNewMarkers.get(0));
            }
          });
    } else {
      setupNearbyFavorsListener();
      FloatingActionButton markerNavigationBtn = view.findViewById(R.id.look_through_btn);
      markerNavigationBtn.setOnClickListener(this::onNavgBtnClick);
      // only when the app is firstly opened, center on my location,
      if (firstOpenApp) {
        // Add a marker at my location and move the camera
        LatLng latLng = new LatLng(mLocation.getLatitude(), mLocation.getLongitude());
        focusViewOnLatLng(latLng, false, 0);
        firstOpenApp = false;
      }
    }
  }

  public Marker drawMarkerAndFocusOnLocation() {
    String markerTitle;
    String markerDescription;
    double selectedLongitude;
    double selectedLatitude;
    boolean isEditable;

    if (intentType == OBSERVE_LOCATION || intentType == OBSERVE_FAVOR) {
      markerTitle = "";
      markerDescription = "";
      String latitudeFromChat = getArguments().getString(LATITUDE_ARGUMENT_KEY);
      String longitudeFromChat = getArguments().getString(LONGITUDE_ARGUMENT_KEY);
      selectedLongitude = parseDouble(longitudeFromChat);
      selectedLatitude = parseDouble(latitudeFromChat);
      isEditable = false;
    } else {
      markerTitle = getString(R.string.hint_drag_marker);
      markerDescription = getString(R.string.hint_click_window);
      selectedLongitude = mLocation.getLongitude();
      selectedLatitude = mLocation.getLatitude();
      isEditable = true;
    }

    LatLng markerLocation = new LatLng(selectedLatitude, selectedLongitude);
    float markerColor = BitmapDescriptorFactory.HUE_GREEN;
    Marker marker =
        createMarker(markerLocation, markerColor, markerTitle, markerDescription, isEditable);
    existAddedNewMarkers.add(marker);
    focusViewOnLatLng(markerLocation, false, defaultZoomLevelDif);
    return marker;
  }

  private void setLimitedView() {
    view.findViewById(R.id.toggle).setVisibility(View.GONE);
    view.findViewById(R.id.offline_map_button).setVisibility(View.GONE);
    view.findViewById(R.id.look_through_btn).setVisibility(View.GONE);

    ((MainActivity) requireActivity()).hideBottomNavigation();

    doneButton = requireView().findViewById(R.id.button_location_from_request_view);
    doneButton.setVisibility(View.VISIBLE);
    setupToolbar(intentType);
  }

  private void setupToolbar(int intentType) {
    Toolbar toolbar = requireActivity().findViewById(R.id.toolbar_main_activity);
    toolbar.setNavigationIcon(null);
    toolbar.setBackgroundColor(getResources().getColor(R.color.material_green_500));
    toolbar.setTitleTextColor(Color.WHITE);

    switch (intentType) {
      case NEW_REQUEST:
        {
          toolbar.setTitle(R.string.map_request_favor);
          break;
        }
      case EDIT_EXISTING_LOCATION:
        {
          toolbar.setTitle(R.string.map_edit_favor_loc);
          break;
        }
      case SHARE_LOCATION:
        {
          toolbar.setTitle(R.string.map_share_loc);
          break;
        }
      case OBSERVE_LOCATION:
      case OBSERVE_FAVOR:
        {
          toolbar.setTitle(R.string.map_observe_loc);
          break;
        }
    }
  }

  @Override
  public void onStart() {
    super.onStart();
    if (getArguments() != null) intentType = getArguments().getInt(LOCATION_ARGUMENT_KEY);
    if (intentType != 0) { // if intent is to edit, request, share, or observe
      setLimitedView();
    }
  }

  private class MarkerDrag implements GoogleMap.OnMarkerDragListener {

    @Override
    public void onMarkerDragStart(Marker marker) {}

    @Override
    public void onMarkerDrag(Marker marker) {
      mMap.animateCamera(
          CameraUpdateFactory.newLatLngZoom(
              marker.getPosition(), defaultZoomLevel + defaultZoomLevelDif));
    }

    @Override
    public void onMarkerDragEnd(Marker marker) {
      existAddedNewMarkers.get(0).setPosition(marker.getPosition());
    }
  }

  private class LongClick implements GoogleMap.OnMapLongClickListener {
    @Override
    public void onMapLongClick(LatLng latLng) {
      if (intentType != 0) return;
      // at most one new marker is allowed
      if (existAddedNewMarkers.size() != 0) {
        for (Marker m : existAddedNewMarkers) m.remove();
        existAddedNewMarkers.clear();
      }
      String markerTitle = getString(R.string.hint_drag_marker);
      String markerDescription = getString(R.string.hint_click_window);
      float markerColor = BitmapDescriptorFactory.HUE_GREEN;
      Marker marker = createMarker(latLng, markerColor, markerTitle, markerDescription, true);
      marker.showInfoWindow();
      existAddedNewMarkers.add(marker);
      mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
    }
  }

  private void setupNearbyFavorsListener() {
    favorViewModel
        .getFavorsAroundMe(mLocation, radiusThreshold)
        .observe(
            getViewLifecycleOwner(),
            stringFavorMap -> {
              try {
                for (Marker marker : favorsAroundMe.values()) marker.remove();
                favorsAroundMe.clear();
                drawFavorMarkers(new ArrayList<>(stringFavorMap.values()));
              } catch (Exception e) {
                CommonTools.showSnackbar(
                    requireView(), getString(R.string.nearby_favors_exception));
              }
            });
  }

  private void sendLocationToChat(Marker marker) {

    Bundle chatBundle = new Bundle();
    chatBundle.putParcelable(LOCATION_ARGUMENT_KEY, marker.getPosition());
    Navigation.findNavController(requireView())
        .navigate(R.id.action_nav_map_to_chatView, chatBundle);
  }

  private void requestFavorOnMarkerLocation(Marker marker) {
    Favor preparedFavor = getArguments().getParcelable(CommonTools.FAVOR_VALUE_ARGS);
    preparedFavor.getLocation().setLatitude(marker.getPosition().latitude);
    preparedFavor.getLocation().setLongitude(marker.getPosition().longitude);
    int change = (intentType == NEW_REQUEST) ? 1 : 0;
    // post to DB
    CompletableFuture<Void> postFavorFuture = getViewModel().requestFavor(preparedFavor, change);
    postFavorFuture.whenComplete(
        (aVoid, throwable) -> {
          if (throwable != null)
            CommonTools.showSnackbar(requireView(), getString(R.string.update_favor_error));
          else {
            CommonTools.showSnackbar(
                requireView(),
                getString(CommonTools.getSnackbarMessageForRequestedFavor(requireContext())));
            // jump to favorPublished view
            Bundle favorBundle = new Bundle();
            favorBundle.putString(CommonTools.FAVOR_ARGS, preparedFavor.getId());
            Navigation.findNavController(requireView())
                .navigate(R.id.action_nav_map_to_favorPublishedView_via_RequestView, favorBundle);
          }
        });

    if (DependencyFactory.isOfflineMode(requireContext())) {
      CommonTools.showSnackbar(requireView(), getString(R.string.save_draft_message));
    }
  }

  public IFavorViewModel getViewModel() {
    return favorViewModel;
  }

  private Marker drawFavorMarker(Favor favor, boolean isRequested, boolean isEdited) {
    LatLng latLng =
        new LatLng(favor.getLocation().getLatitude(), favor.getLocation().getLongitude());
    float markerColor =
        isRequested ? BitmapDescriptorFactory.HUE_ORANGE : BitmapDescriptorFactory.HUE_VIOLET;
    String markerTitle =
        (isEdited) && favor.getTitle().equals("")
            ? getString(R.string.hint_drag_marker)
            : favor.getTitle();
    String markerDescription =
        isEdited && favor.getDescription().equals("")
            ? getString(R.string.hint_click_window)
            : favor.getDescription();
    Marker marker = createMarker(latLng, markerColor, markerTitle, markerDescription, isEdited);
    marker.setTag(
        new ArrayList<Object>() {
          {
            add(favor.getId());
            add(false);
          }
        });
    if (!isRequested) {
      favorsAroundMe.put(favor.getId(), marker);
    }
    return marker;
  }

  private Marker createMarker(
      LatLng latLng,
      float markerColor,
      String markerTitle,
      String markerDescription,
      boolean isDraggable) {
    Marker marker =
        mMap.addMarker(
            new MarkerOptions()
                .position(latLng)
                .title(markerTitle)
                .icon(BitmapDescriptorFactory.defaultMarker(markerColor))
                .snippet(markerDescription));
    marker.setDraggable(isDraggable);
    if (isDraggable) marker.showInfoWindow();
    return marker;
  }

  private void focusViewOnLatLng(LatLng location, boolean animate, int zoomDif) {
    if (animate)
      mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, defaultZoomLevel + zoomDif));
    else mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, defaultZoomLevel + zoomDif));
  }

  private void onOfflineMapClick(View view) {
    new AlertDialog.Builder(requireContext())
        .setTitle(R.string.offline_mode_dialog_title)
        .setMessage(R.string.offline_mode_instructions)
        .setPositiveButton(android.R.string.yes, null)
        .setNeutralButton(
            R.string.offline_mode_dialog_link,
            (dialogInterface, i) -> {
              Intent browserIntent =
                  new Intent(
                      Intent.ACTION_VIEW, Uri.parse(getString(R.string.download_offline_map)));
              startActivity(browserIntent);
            })
        .show();
  }

  private void onToggleClick(View view) {
    Navigation.findNavController(view).navigate(R.id.action_nav_map_to_nearby_favor_list);
  }

  private void onNavgBtnClick(View view) {
    for (Marker marker : favorsAroundMe.values()) {
      ArrayList tagArray = (ArrayList) marker.getTag();
      boolean visited = (boolean) tagArray.get(1);
      if (!visited) {
        // mark that this favor has been visited
        tagArray.set(1, true);
        marker.showInfoWindow();
        mMap.animateCamera(
            CameraUpdateFactory.newLatLngZoom(marker.getPosition(), defaultZoomLevel));
        return;
      }
    }
    // if all visited, mark all favors not visited, another cycle
    for (Marker marker : favorsAroundMe.values()) {
      ((ArrayList) marker.getTag()).set(1, false);
    }
    Toast.makeText(requireContext(), getString(R.string.finish_visit_marker), Toast.LENGTH_SHORT)
        .show();
  }

  private void drawFavorMarkers(List<Favor> favors) {
    for (Favor favor : favors) {
      drawFavorMarker(favor, false, false);
    }
  }

  private void centerViewOnMyLocation() {}

  /** Request location permission, so that we can get the location of the device. */
  private void getLocationPermission() {
    if (ContextCompat.checkSelfPermission(
            requireActivity().getApplicationContext(),
            android.Manifest.permission.ACCESS_FINE_LOCATION)
        != PackageManager.PERMISSION_GRANTED) {
      requestPermissions(
          new String[] {android.Manifest.permission.ACCESS_FINE_LOCATION},
          PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
      mLocationPermissionGranted = false;
    } else {
      mLocationPermissionGranted = true;
    }
  }

  @Override
  public void onRequestPermissionsResult(
      int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    if (requestCode
        == PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION) { // If fragment_favor_published_view is
      // cancelled, the result arrays
      // are empty.
      if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        mLocationPermissionGranted = true;
        // if get permission, then refresh the map view
        Navigation.findNavController(view).navigate(R.id.action_global_nav_map);
      }
    }
  }

  @Override
  public View getInfoWindow(Marker marker) {
    View mWindow = getLayoutInflater().inflate(R.layout.map_info_window, null);
    String title = marker.getTitle();
    TextView titleUi = mWindow.findViewById(R.id.title);
    setSpannableString(title, titleUi);

    String snippet = marker.getSnippet();
    TextView snippetUi = mWindow.findViewById(R.id.snippet);
    setSpannableString(snippet, snippetUi);
    return mWindow;
  }

  private void setSpannableString(String content, TextView textview) {
    if (content != null) {
      SpannableString snippetText = new SpannableString(content);
      snippetText.setSpan(new ForegroundColorSpan(Color.BLACK), 0, content.length(), 0);
      textview.setText(snippetText);
    }
  }

  @Override
  public View getInfoContents(Marker marker) {
    return null;
  }

  @Override
  public void onInfoWindowClick(Marker marker) {
    if (intentType != 0) return;
    Bundle favorBundle = new Bundle();
    if (marker.getTitle().equals(getString(R.string.hint_drag_marker))
        && marker.getSnippet().equals(getString(R.string.hint_click_window))) {
      navigateToEditPage(marker, favorBundle);
    } else {
      List<Object> markerInfo = (List<Object>) marker.getTag();
      String favorId = markerInfo.get(0).toString();
      favorBundle.putString(CommonTools.FAVOR_ARGS, favorId);
      Navigation.findNavController(view)
          .navigate(R.id.action_nav_map_to_favorPublishedView, favorBundle);
    }
  }

  private void navigateToEditPage(Marker marker, Bundle favorBundle) {
    FavoLocation loc = new FavoLocation(mLocation);
    loc.setLatitude(marker.getPosition().latitude);
    loc.setLongitude(marker.getPosition().longitude);
    Favor preparedFavor =
        new Favor(
            "", "", DependencyFactory.getCurrentFirebaseUser().getUid(), loc, FavorStatus.EDIT, 0);
    favorBundle.putParcelable(CommonTools.FAVOR_VALUE_ARGS, preparedFavor);
    favorBundle.putString(FavorEditingView.FAVOR_SOURCE_KEY, FavorEditingView.FAVOR_SOURCE_MAP);
    Navigation.findNavController(view)
        .navigate(R.id.action_nav_map_to_favorEditingView, favorBundle);
  }
}
