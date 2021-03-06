package ch.epfl.favo.user;

import android.location.Location;

import com.google.firebase.auth.FirebaseUser;

import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.Map;

import ch.epfl.favo.FakeItemFactory;
import ch.epfl.favo.TestConstants;
import ch.epfl.favo.exception.IllegalAcceptException;
import ch.epfl.favo.exception.IllegalRequestException;
import ch.epfl.favo.gps.FavoLocation;
import ch.epfl.favo.util.DependencyFactory;
import ch.epfl.favo.view.MockDatabaseWrapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class UserUnitTests {

  @Before
  public void setup() {
    DependencyFactory.setCurrentCollectionWrapper(new MockDatabaseWrapper());
  }

  @Test
  public void userGettersReturnCorrectValues() {

    String id = TestConstants.USER_ID;
    String name = TestConstants.NAME;
    String email = TestConstants.EMAIL;
    String deviceId = TestConstants.DEVICE_ID;
    Date birthDate = TestConstants.BIRTHDAY;
    FavoLocation location = TestConstants.LOCATION;

    User user = new User(id, name, email, deviceId, birthDate, location);

    assertEquals(name, user.getName());
    assertEquals(email, user.getEmail());
    assertEquals(deviceId, user.getDeviceId());
    assertEquals(birthDate, user.getBirthDate());
    assertEquals(0, user.getActiveAcceptingFavors());
    assertEquals(0, user.getActiveRequestingFavors());
    assertEquals(0, user.getRequestedFavors());
    assertEquals(0, user.getAcceptedFavors());
    assertEquals(0, user.getCompletedFavors());
    assertEquals(0, user.getLikes());
    assertEquals(0, user.getDislikes());
    assertEquals(10, (int) user.getBalance());

    // field should initialize null and populate later
    assertNull(user.getNotificationId());
  }

  @Test
  public void userSettersCorrectlyUpdateValues() {

    int testNum = 2;

    User user = new User();
    String pictureUrl = "someUrl";
    int activeAcceptingFavors = User.MAX_ACCEPTING_FAVORS;
    int activeRequestingFavors = User.MAX_REQUESTING_FAVORS;
    int newBalance = 5;
    String temporaryNotificationId = "temporaryNotificationId";
    String temporaryDeviceId = "temporaryDeviceId";
    FavoLocation newLoc = new FavoLocation();
    String testName = "newName";
    double radius = 5;

    user.setActiveAcceptingFavors(activeAcceptingFavors);
    user.setActiveRequestingFavors(activeRequestingFavors);
    user.setNotificationId(temporaryNotificationId);
    user.setDeviceId(temporaryDeviceId);
    user.setLocation(newLoc);
    user.setRequestedFavors(testNum);
    user.setAcceptedFavors(testNum);
    user.setCompletedFavors(testNum);
    user.setLikes(testNum);
    user.setDislikes(testNum);
    user.setBalance(newBalance);
    user.setProfilePictureUrl(pictureUrl);
    user.setName(testName);
    user.setNotificationRadius(radius);
    user.setChatNotifications(false);
    user.setUpdateNotifications(false);

    assertEquals(activeAcceptingFavors, user.getActiveAcceptingFavors());
    assertEquals(activeRequestingFavors, user.getActiveRequestingFavors());
    assertEquals(temporaryNotificationId, user.getNotificationId());
    assertEquals(temporaryDeviceId, user.getDeviceId());
    assertEquals(testNum, user.getRequestedFavors());
    assertEquals(testNum, user.getAcceptedFavors());
    assertEquals(testNum, user.getCompletedFavors());
    assertEquals(testNum, user.getLikes());
    assertEquals(testNum, user.getDislikes());
    assertEquals((int) newBalance, (int) user.getBalance());
    assertEquals(pictureUrl, user.getProfilePictureUrl());
    assertEquals(testName, user.getName());
    assertEquals((int) radius, (int) user.getNotificationRadius());
    assertFalse(user.isChatNotifications());
    assertFalse(user.isUpdateNotifications());
  }

  @Test
  public void testUserConstructedFromFirebase() {
    FirebaseUser fbUser = FakeItemFactory.getFirebaseUser();
    String deviceId = TestConstants.DEVICE_ID;
    Location loc = TestConstants.LOCATION;
    new User(fbUser, deviceId);
  }

  @Test
  public void userGivesCorrectTransformationToMap() {
    User user =
        new User(
            TestConstants.USER_ID,
            TestConstants.NAME,
            TestConstants.EMAIL,
            TestConstants.DEVICE_ID,
            null,
            null);
    Map<String, Object> userMap = user.toMap();
    User user2 = new User(userMap);
    assertEquals(user.getId(), user2.getId());
    assertEquals(user.getName(), user2.getName());
    assertEquals(user.getEmail(), user2.getEmail());
    assertEquals(user.getDeviceId(), user2.getDeviceId());
    assertEquals(user.getNotificationId(), user2.getNotificationId());
    assertEquals(user.getBirthDate(), user2.getBirthDate());
    assertEquals(user.getLocation(), user2.getLocation());
    assertEquals(user.getActiveRequestingFavors(), user2.getActiveRequestingFavors());
    assertEquals(user.getActiveAcceptingFavors(), user2.getActiveAcceptingFavors());
    assertEquals(user.getRequestedFavors(), user2.getRequestedFavors());
    assertEquals(user.getAcceptedFavors(), user2.getAcceptedFavors());
    assertEquals(user.getCompletedFavors(), user2.getCompletedFavors());
    assertEquals(user.getLikes(), user2.getLikes());
    assertEquals(user.getDislikes(), user2.getDislikes());
    assertEquals((int) user.getBalance(), (int) user2.getBalance());
  }

  @Test
  public void testUserHasMaximumAcceptableAndRequestedFavors() {

    User user = new User();
    assertThrows(
        IllegalAcceptException.class,
        () -> user.setActiveAcceptingFavors(User.MAX_ACCEPTING_FAVORS + 1));
    assertThrows(
        IllegalRequestException.class,
        () -> user.setActiveRequestingFavors(User.MAX_REQUESTING_FAVORS + 1));
    assertThrows(IllegalAcceptException.class, () -> user.setActiveAcceptingFavors(-1));
    assertThrows(IllegalRequestException.class, () -> user.setActiveRequestingFavors(-1));
    user.setActiveAcceptingFavors(User.MAX_ACCEPTING_FAVORS);
    user.setActiveRequestingFavors(User.MAX_REQUESTING_FAVORS);
    assertTrue(user.canAccept());
    assertTrue(user.canRequest());
  }

  @Test
  public void testUserIsSuccessFullyConvertedToMap() {
    User user = FakeItemFactory.getUser();
    Map<String, Object> userMap = user.toMap();
    assertEquals(user.getId(), userMap.get(User.ID));
    assertEquals(user.getName(), userMap.get(User.NAME));
    assertEquals(user.getActiveAcceptingFavors(), userMap.get(User.ACTIVE_ACCEPTING_FAVORS));
    assertEquals(user.getActiveRequestingFavors(), userMap.get(User.ACTIVE_REQUESTING_FAVORS));
    assertEquals(user.getLocation(), userMap.get(User.LOCATION));
    assertEquals(user.getBirthDate(), userMap.get(User.BIRTH_DATE));
    assertEquals(user.getEmail(), userMap.get(User.EMAIL));
  }
}
