package ch.epfl.favo.view;

import android.location.Location;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import ch.epfl.favo.FakeFirebaseUser;
import ch.epfl.favo.MainActivity;
import ch.epfl.favo.R;
import ch.epfl.favo.common.NoPermissionGrantedException;
import ch.epfl.favo.common.NoPositionFoundException;
import ch.epfl.favo.favor.FavorUtil;
import ch.epfl.favo.map.Locator;
import ch.epfl.favo.util.DependencyFactory;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static ch.epfl.favo.TestConstants.EMAIL;
import static ch.epfl.favo.TestConstants.NAME;
import static ch.epfl.favo.TestConstants.PHOTO_URI;
import static ch.epfl.favo.TestConstants.PROVIDER;

class MockGpsTracker implements Locator {
  @Override
  public Location getLocation() throws NoPermissionGrantedException, NoPositionFoundException {
    return null;
  }
}

@RunWith(AndroidJUnit4.class)
public class AddFavorTest {

  @Rule
  public final ActivityTestRule<MainActivity> activityTestRule =
      new ActivityTestRule<MainActivity>(MainActivity.class) {
        @Override
        protected void beforeActivityLaunched() {
          DependencyFactory.setCurrentFirebaseUser(
              new FakeFirebaseUser(NAME, EMAIL, PHOTO_URI, PROVIDER));
          DependencyFactory.setCurrentGpsTracker(new MockGpsTracker());
          FavorUtil.getSingleInstance().setTestUiMode(true);
        }
      };

  @Rule
  public GrantPermissionRule permissionRule =
      GrantPermissionRule.grant(android.Manifest.permission.ACCESS_FINE_LOCATION);

  @After
  public void tearDown() {
    DependencyFactory.setCurrentFirebaseUser(null);
    FavorUtil.getSingleInstance().setTestUiMode(false);
  }

  @Test
  public void addFavorTest() {
    // Click on fav list tab
    onView(withId(R.id.nav_favor_list_button)).check(matches(isDisplayed())).perform(click());

    getInstrumentation().waitForIdleSync();
    onView(withId(R.id.new_favor)).check(matches(isDisplayed())).perform(click());

    getInstrumentation().waitForIdleSync();

    onView(withId(R.id.request_button))
            .check(matches(isDisplayed()))
            .perform(click());
    getInstrumentation().waitForIdleSync();
    // check snackbar shows
    onView(withId(com.google.android.material.R.id.snackbar_text))
            .check(matches(withText(R.string.favor_request_success_msg)));

  }

}
