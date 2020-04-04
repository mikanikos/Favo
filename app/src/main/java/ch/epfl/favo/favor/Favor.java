package ch.epfl.favo.favor;

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import ch.epfl.favo.common.DatabaseWrapper;
import ch.epfl.favo.common.Document;

/**
 * Class contains all the information relevant to a single favor. Relevant info includes tile,
 * description, requester, accepter, location and status
 */
public class Favor implements Parcelable, Document {
  public enum Status {
    REQUESTED("Requested"),
    EDIT("Edit mode"), // temporary state used for logic in the request view
    ACCEPTED("Accepted"),
    EXPIRED("Expired"),
    CANCELLED_REQUESTER("Cancelled by requester"),
    CANCELLED_ACCEPTER("Cancelled by accepter"),
    SUCCESSFULLY_COMPLETED("Completed succesfully");

    private String customDisplay;

    Status(String custom) {
      this.customDisplay = custom;
    }

    public String getPrettyString() {
      return customDisplay;
    }
  }

  // String constants for Map conversion
  public static final String ID = "ID";
  public static final String TITLE = "Title";
  public static final String DESCRIPTION = "Description";
  public static final String REQUESTER_ID = "RequesterID";
  public static final String ACCEPTER_ID = "AccepterID";
  public static final String LOCATION = "Location";
  public static final String POSTED_TIME = "Posted time";
  public static final String STATUS_ID = "Status ID";

  public static final Creator<Favor> CREATOR =
      new Creator<Favor>() {
        @Override
        public Favor createFromParcel(Parcel in) {
          return new Favor(in);
        }

        @Override
        public Favor[] newArray(int size) {
          return new Favor[size];
        }
      };
  private String id;
  private String title;
  private String description;
  private String requesterId;
  private String accepterId;
  private Location location;
  private Date postedTime;
  private Status statusId;

  public Favor() {}

  public Favor( // not including id
      String title, String description, String requesterId, Location location, Status statusId) {
    String id = DatabaseWrapper.generateRandomId();
    setParameters(id, title, description, requesterId, location, statusId);
  }

  public Favor( //includes id
      String id,
      String title,
      String description,
      String requesterId,
      Location location,
      Status statusId) {
    setParameters(id, title, description, requesterId, location, statusId);
  }

  private void setParameters(
      String id,
      String title,
      String description,
      String requesterId,
      Location location,
      Status statusId) {
    setId(id);
    this.title = title;
    this.description = description;
    this.requesterId = requesterId;
    this.location = location;
    this.postedTime = new Date();
    this.statusId = statusId;
    this.accepterId = null;
  }

  /**
   * Constructor from map
   *
   * @param map
   */
  public Favor(Map<String, Object> map) {
    this.id = (String) map.get(ID);
    this.title = (String) map.get(TITLE);
    this.description = (String) map.get(DESCRIPTION);
    this.requesterId = (String) map.get(REQUESTER_ID);
    this.accepterId = (String) map.get(ACCEPTER_ID);
    this.location = (Location) map.get(LOCATION);
    this.postedTime = (Date) map.get(POSTED_TIME);
    this.statusId = (Status) map.get(STATUS_ID);
  }

  /**
   * Parcelable implementaion allows us to pass favor to fragment
   *
   * @param in
   */
  protected Favor(Parcel in) {
    title = in.readString();
    description = in.readString();
    requesterId = in.readString();
    accepterId = in.readString();
    location = in.readParcelable(Location.class.getClassLoader());
    try {
      statusId = Status.valueOf(in.readString());
    } catch (Exception e) { // null pointer
      statusId = null;
    }
  }

  @Override
  public String getId() {
    return id;
  }

  private void setId(String id) {
    this.id = id;
  }

  @Override
  public Map<String, Object> toMap() {
    return new HashMap<String, Object>() {
      {
        put(ID, id);
        put(TITLE, title);
        put(DESCRIPTION, description);
        put(REQUESTER_ID, requesterId);
        put(ACCEPTER_ID, accepterId);
        put(LOCATION, location);
        put(POSTED_TIME, postedTime);
        put(STATUS_ID, statusId);
      }
    };
  }

  public String getTitle() {
    return title;
  }

  public String getDescription() {
    return description;
  }

  public String getRequesterId() {
    return requesterId;
  }

  public String getAccepterID() {
    return accepterId;
  }

  public Date getPostedTime() {
    return postedTime;
  }

  void setAccepterID(String accepterID) {
    this.accepterId = accepterID;
  }

  /**
   * Status ID can be: 0 for requested 1 for accepted 2 for completed succesfully 3 for expired 4
   * for completed unsuccessfully
   *
   * @return statusID
   */
  public Status getStatusId() {
    return statusId;
  }

  void setStatusId(Status statusId) {
    this.statusId = statusId;
  }

  public Location getLocation() {
    return location;
  }

  void setLocation(Location location) {
    this.location = location;
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(title);
    dest.writeString(description);
    dest.writeString(requesterId);
    dest.writeString(accepterId);
    dest.writeParcelable(location, flags);
    dest.writeString(statusId.toString());
  }

  public void updateToOther(Favor other) {
    // we take all the values except for the ID

    this.title = other.getTitle();
    this.description = other.getDescription();
    this.location = other.getLocation();
    this.postedTime = other.getPostedTime();
    this.requesterId = other.getRequesterId();
    this.statusId = other.getStatusId();
  }

  public void updateStatus(Status newStatus) {
    this.statusId = newStatus;
  }
}
