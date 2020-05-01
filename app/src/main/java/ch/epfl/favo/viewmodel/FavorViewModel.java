package ch.epfl.favo.viewmodel;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.location.Location;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.MetadataChanges;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import ch.epfl.favo.common.FavoLocation;
import ch.epfl.favo.favor.Favor;
import ch.epfl.favo.favor.FavorStatus;
import ch.epfl.favo.favor.FavorUtil;
import ch.epfl.favo.user.IUserUtil;
import ch.epfl.favo.util.DependencyFactory;
import ch.epfl.favo.util.PictureUtil;

@SuppressLint("NewApi")
public class FavorViewModel extends ViewModel implements FavorDataController {
  String TAG = "FIRESTORE_VIEW_MODEL";
  private Location mCurrentLocation;
  private double mRadius = -1.0;

  private MutableLiveData<Map<String, Favor>> activeFavorsAroundMe = new MutableLiveData<>();

  MutableLiveData<Favor> observedFavor = new MutableLiveData<>();
  // MediatorLiveData<Favor> observedFavor = new MediatorLiveData<>();

  public FavorUtil getFavorRepository() {
    return DependencyFactory.getCurrentFavorRepository();
  }

  public IUserUtil getUserRepository() {
    return DependencyFactory.getCurrentUserRepository();
  }

  private PictureUtil getPictureUtility() {
    return DependencyFactory.getCurrentPictureUtility();
  }

  // save address to firebase
  @Override
  public CompletableFuture requestFavor(Favor favor) {
    return changeActiveFavorCount(
            true, 1) // if user can request favor then post it in the favor collection
        .thenCompose((f) -> getFavorRepository().requestFavor(favor));
  }

  public CompletableFuture updateFavor(
      Favor favor, boolean isRequested, int activeFavorsCountChange) {
    return changeActiveFavorCount(isRequested, activeFavorsCountChange)
        .thenCompose(o -> getFavorRepository().updateFavor(favor));
  }

  /**
   * Tries to update the number of active favors for a given user. Detailed implementation in
   * UserUtil
   *
   * @param isRequested is/are the favors requested?
   * @param change number of favors being updated
   * @return Can be completed exceptionally
   */
  private CompletableFuture changeActiveFavorCount(boolean isRequested, int change) {
    return getUserRepository().changeActiveFavorCount(isRequested, change);
  }

  // Upload/download pictures
  @Override
  public void uploadOrUpdatePicture(Favor favor, Bitmap picture) {
    CompletableFuture<String> pictureUrl = getPictureUtility().uploadPicture(picture);
    pictureUrl.thenAccept(url -> FavorUtil.getSingleInstance().updateFavorPhoto(favor, url));
  }

  @Override
  public CompletableFuture<Bitmap> downloadPicture(Favor favor) throws RuntimeException {
    String url = favor.getPictureUrl();
    if (url == null) {
      return new CompletableFuture<Bitmap>() {
        {
          completeExceptionally(new RuntimeException("Invalid picture url in Favor"));
        }
      };
    } else {
      return getPictureUtility().downloadPicture(url);
    }
  }

  @Override
  public LiveData<Map<String, Favor>> getFavorsAroundMe(Location loc, double radiusInKm) {
    if (mCurrentLocation == null) mCurrentLocation = loc;
    if (mRadius == -1) mRadius = radiusInKm;
    if (activeFavorsAroundMe.getValue() == null
        || (mCurrentLocation.distanceTo(loc)) > 1000 * radiusInKm) {
      getFavorRepository()
          .getNearbyFavors(loc, radiusInKm)
          .addSnapshotListener(
              MetadataChanges.EXCLUDE,
              (queryDocumentSnapshots, e) -> {
                activeFavorsAroundMe.postValue(
                    getNearbyFavorsFromQuery(loc, radiusInKm, queryDocumentSnapshots, e));
              });
    }
    return getFavorsAroundMe();
  }

  public LiveData<Map<String, Favor>> getFavorsAroundMe() {
    return activeFavorsAroundMe;
  }

  public Map<String, Favor> getNearbyFavorsFromQuery(
      Location loc,
      double radius,
      QuerySnapshot queryDocumentSnapshots,
      FirebaseFirestoreException e) {
    handleException(e);
    List<Favor> favorsList = queryDocumentSnapshots.toObjects(Favor.class);

    Map<String, Favor> favorsMap = new HashMap<>();
    // Filter latitude because Firebase only filters longitude
    double latDif = Math.toDegrees(radius / FavoLocation.EARTH_RADIUS);
    for (Favor favor : favorsList) {
      if (!favor.getRequesterId().equals(DependencyFactory.getCurrentFirebaseUser().getUid())
          && favor.getStatusId() == FavorStatus.REQUESTED.toInt()
          && favor.getLocation().getLatitude() > loc.getLatitude() - latDif
          && favor.getLocation().getLatitude() < loc.getLatitude() + latDif) {
        favorsMap.put(favor.getId(), favor);
      }
    }
    return favorsMap;
  }

  public void handleException(FirebaseFirestoreException e) {
    if (e != null) {
      Log.w(TAG, "Listen Failed", e);
      throw new RuntimeException(e.getMessage());
    }
  }

  @Override
  public LiveData<Favor> setObservedFavor(String favorId) {
    if (getObservedFavor().getValue() != null
        && getObservedFavor().getValue().getId().equals(favorId)) {
      return getObservedFavor(); // if request hasn't changed then return original
    }
    observedFavor.postValue(null);
    getFavorRepository()
        .getFavorReference(favorId)
        .addSnapshotListener(
            MetadataChanges.EXCLUDE,
            (documentSnapshot, e) -> {
              handleException(e);
              observedFavor.postValue(documentSnapshot.toObject(Favor.class));
            });
    return getObservedFavor();
  }

  @Override
  public LiveData<Favor> getObservedFavor() {
    return observedFavor;
  }
}
