package ch.epfl.favo.util;

import org.junit.Test;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;

import ch.epfl.favo.database.DatabaseWrapper;
import ch.epfl.favo.view.tabs.MapPage;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class CommonToolsTests {

  @Test
  public void ConvertTimeTest() {
    Date date = new Date();
    Format format = new SimpleDateFormat("yyyy/MM/dd HH:mm");
    String time = format.format(date);
    assertEquals(CommonTools.convertTime(date), time);
  }

  @Test
  public void databaseUtilCorrectlyGeneratesIds() {
    int ID_LENGTH = 28;
    String id1 = DatabaseWrapper.generateRandomId();
    String id2 = DatabaseWrapper.generateRandomId();
    assertEquals(ID_LENGTH, id1.length());
    assertEquals(ID_LENGTH, id2.length());
    assertNotEquals(id1, id2);
  }

  @Test
  public void RadiusToZoomLevelTest() {
    MapPage mapPage = new MapPage();
    assertEquals(17, CommonTools.notificationRadiusToZoomLevel(1));
    assertEquals(15, CommonTools.notificationRadiusToZoomLevel(5));
    assertEquals(14, CommonTools.notificationRadiusToZoomLevel(10));
    assertEquals(13, CommonTools.notificationRadiusToZoomLevel(25));
  }
}
