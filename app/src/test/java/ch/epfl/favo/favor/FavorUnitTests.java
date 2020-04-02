package ch.epfl.favo.favor;

import android.location.Location;

import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import ch.epfl.favo.FakeItemFactory;
import ch.epfl.favo.TestConstants;
import ch.epfl.favo.common.CollectionWrapper;
import ch.epfl.favo.common.NotImplementedException;
import ch.epfl.favo.util.TestUtil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class FavorUnitTests {

  @Test
  public void favorGettersReturnCorrectValues() {

    String title = "Flat tire";
    String description = "Tire popped while turning left on Avenue Rhodanie";
    String requesterId = "2362489";
    Location location = new Location("Dummy provider");
    Favor.Status statusId = Favor.Status.EXPIRED;

    Favor favor = new Favor(title, description, requesterId, location, statusId);

    assertEquals(title, favor.getTitle());
    assertEquals(description, favor.getDescription());
    assertEquals(requesterId, favor.getRequesterId());
    assertEquals(location, favor.getLocation());
    assertEquals(statusId, favor.getStatusId());
    assertNotNull(favor.getPostedTime());
  }

  @Test
  public void favorSettersCorrectlyUpdateValues() {

    Favor favor = new Favor();
    Favor.Status statusId = Favor.Status.CANCELLED_REQUESTER;
    Location location = new Location("Dummy provider 2");
    String id = "1243";
    String accepterId = "2364652";
    favor.setStatusId(statusId);
    favor.setLocation(location);
    favor.setAccepterID(accepterId);


    assertEquals(location, favor.getLocation());
    assertEquals(statusId, favor.getStatusId());
    assertEquals(accepterId, favor.getAccepterID());
  }

  @Test
  public void favorSuccessfullyPostsToDB() {
    CollectionWrapper mock = Mockito.mock(CollectionWrapper.class);
    Mockito.doNothing().when(mock).addDocument(any(Favor.class));

    String title = "Sample Favor";
    String description = TestUtil.generateRandomString(305);
    Location location = new Location("dummy provider");
    String requesterId = "requester Id";
    Favor.Status statusId = Favor.Status.REQUESTED;

    Favor f = new Favor(title, description, requesterId, location, statusId);
    FavorUtil.getSingleInstance().postFavor(f);

    assertNotNull(f);
  }

  @Test
  public void favorCanRetrieveAllFavorsForGivenUser() {

    String userId = TestConstants.USER_ID;
    assertThrows(
        NotImplementedException.class,
        new ThrowingRunnable() {
          @Override
          public void run() throws Throwable {
            FavorUtil.getSingleInstance().retrieveAllFavorsForGivenUser(userId);
          }
        });
  }

  @Test
  public void favorCanRetrieveAllActiveFavorsForGivenUser() {

    String userId = TestConstants.USER_ID;
    assertThrows(
        NotImplementedException.class,
        new ThrowingRunnable() {
          @Override
          public void run() throws Throwable {
            FavorUtil.getSingleInstance().retrieveAllActiveFavorsForGivenUser(userId);
          }
        });
  }

  @Test
  public void favorCanRetrieveAllRequestedFavorsForGivenUser() {

    String userId = TestConstants.USER_ID;
    assertThrows(
        NotImplementedException.class,
        new ThrowingRunnable() {
          @Override
          public void run() throws Throwable {
            FavorUtil.getSingleInstance().retrieveAllRequestedFavorsForGivenUser(userId);
          }
        });
  }

  @Test
  public void favorCanRetrieveAllAcceptedFavorsForGivenUser() {

    String userId = TestConstants.USER_ID;
    assertThrows(
        NotImplementedException.class,
        new ThrowingRunnable() {
          @Override
          public void run() throws Throwable {
            FavorUtil.getSingleInstance().retrieveAllAcceptedFavorsForGivenUser(userId);
          }
        });
  }

  @Test
  public void favorCanRetrieveAllFavorsInGivenRadius() {

    Location loc = TestConstants.LOCATION;
    double radius = TestConstants.RADIUS;
    assertThrows(
        NotImplementedException.class,
        new ThrowingRunnable() {
          @Override
          public void run() throws Throwable {
            FavorUtil.getSingleInstance().retrieveAllFavorsInGivenRadius(loc, radius);
          }
        });
  }

  @Test
  public void describeContentsCorrect() {
    String title = "Flat tire";
    String description = "Tiire popped while turning left on Avenue Rhodanie";
    String requesterId = "2362489";
    Location location = new Location("Dummy provider");
    Favor.Status statusId = Favor.Status.REQUESTED;

    Favor favor = new Favor(title, description, requesterId, location, statusId);
    assertEquals(favor.describeContents(), 0);
  }

  @Test
  public void CreatorArrayCorrect() {
    String title = "Flat tire";
    String description = "Tiire popped while turning left on Avenue Rhodanie";
    String requesterId = "2362489";
    Location location = new Location("Dummy provider");
    Favor.Status statusId = Favor.Status.REQUESTED;
    Favor[] favors = Favor.CREATOR.newArray(3);
    favors[0] = new Favor(title, description, requesterId, location, statusId);
    assertEquals(title, favors[0].getTitle());
    assertEquals(description, favors[0].getDescription());
    assertEquals(requesterId, favors[0].getRequesterId());
    assertEquals(location, favors[0].getLocation());
    assertEquals(statusId, favors[0].getStatusId());
  }

  @Test
  public void getDocumentFunction() throws ExecutionException, InterruptedException {
    // get favor from database
    String favorID = "WEZDZQD78A5SI5Q790SZAL7FW";
    assertThrows(
      IllegalStateException.class,
      () -> FavorUtil.getSingleInstance().retrieveFavor(favorID).get());
    }

  @Test
  public void favorGivesCorrectTransformationToMap(){
    Favor favor = FakeItemFactory.getFavor();
    Map<String,Object> favorMap = favor.toMap();
    Favor favor2 = new Favor(favorMap);
    assertEquals(favor.getTitle(),favor2.getTitle());
    assertEquals(favor.getId(),favor2.getId());
    assertEquals(favor.getDescription(),favor2.getDescription());
    assertEquals(favor.getLocation(),favor2.getLocation());
    assertEquals(favor.getRequesterId(),favor2.getRequesterId());
    assertEquals(favor.getAccepterID(),favor2.getAccepterID());
    assertEquals(favor.getPostedTime(),favor2.getPostedTime());
    assertEquals(favor.getStatusId(),favor2.getStatusId());
  }
}
