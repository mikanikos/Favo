package ch.epfl.favo.user;

import android.content.res.Resources;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.Query;

import java.util.concurrent.CompletableFuture;

public interface IUserUtil {

  CompletableFuture<Void> postUser(User user);

  CompletableFuture<Void> changeActiveFavorCount(String userId, boolean isRequested, int change);

  CompletableFuture<Void> updateUser(User user);

  CompletableFuture<User> findUser(String id) throws Resources.NotFoundException;

  CompletableFuture retrieveUserRegistrationToken(User user);

  DocumentReference getCurrentUserReference(String userId);

  Query getAllUserFavors(String userId);
}
