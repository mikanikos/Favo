package ch.epfl.favo.viewmodel;

import android.graphics.Bitmap;
import android.location.Location;

import androidx.lifecycle.LiveData;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import ch.epfl.favo.favor.Favor;

/**
 * This class is the sole dependency of all the fragments and activities. It contains Util classes
 * as members.
 */
public interface IFavorViewModel {
  CompletableFuture requestFavor(final Favor favor);

  // Upload/download pictures
  void uploadOrUpdatePicture(Favor favor, Bitmap picture);

  CompletableFuture acceptFavor(final Favor favor);

  CompletableFuture completeFavor(final Favor favor, boolean isRequested);

  CompletableFuture cancelFavor(final Favor favor, boolean isRequested);
  CompletableFuture deleteFavor(final Favor favor);

  CompletableFuture reEnableFavor(final Favor favor);

  CompletableFuture<Bitmap> downloadPicture(final Favor favor);

  LiveData<Map<String, Favor>> getFavorsAroundMe(Location loc, double radius); // used in map view

  LiveData<Map<String, Favor>> getFavorsAroundMe(); // used in nearbylistview

  LiveData<Favor> setObservedFavor(String favorId);

  LiveData<Favor> getObservedFavor();

  void setFavorValue(Favor favor);

  void setShowObservedFavor(Boolean show);

  boolean isShowObservedFavor();
}
