package ch.epfl.favo;

import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.epfl.favo.favor.Favor;

public class FakeItemFactory {

  public static Favor getFavor() {
    return new Favor(
        TestConstants.FAVOR_ID,
        TestConstants.TITLE,
        TestConstants.DESCRIPTION,
        TestConstants.REQUESTER_ID,
        TestConstants.LOCATION,
        TestConstants.FAVOR_STATUS.toInt());
  }

  public static Favor getFavorWithUrl() {
    Favor favor = getFavor();
    favor.setPictureUrl(TestConstants.PICTURE_URL);
    return favor;
  }

  public static FirebaseUser getUser() {
    return new FakeFirebaseUser(
        TestConstants.NAME, TestConstants.EMAIL, TestConstants.PHOTO_URI, TestConstants.PROVIDER);
  }

  public static List<Favor> getFavorList() {
    return new ArrayList<Favor>() {
      {
        add(getFavor());
        add(getFavor());
        add(getFavor());
      }
    };
  }

  public static Map<String, Favor> getFavorListMap() {
    List<Favor> favorList = getFavorList();
    Map<String, Favor> result = new HashMap<>(favorList.size());
    for (Favor favor : favorList) {
      result.put(favor.getId(), favor);
    }
    return result;
  }
}
