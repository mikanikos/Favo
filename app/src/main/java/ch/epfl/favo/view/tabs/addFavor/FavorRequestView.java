package ch.epfl.favo.view.tabs.addFavor;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.KeyListener;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;

import java.util.Objects;

import ch.epfl.favo.MainActivity;
import ch.epfl.favo.R;
import ch.epfl.favo.favor.Favor;
import ch.epfl.favo.favor.FavorUtil;
import ch.epfl.favo.map.Locator;
import ch.epfl.favo.util.CommonTools;
import ch.epfl.favo.util.DependencyFactory;
import ch.epfl.favo.view.ViewController;

import static android.app.Activity.RESULT_OK;
import static ch.epfl.favo.util.CommonTools.hideKeyboardFrom;

public class FavorRequestView extends Fragment {

  private static final int PICK_IMAGE_REQUEST = 1;
  private static final String FAVOR_ARGS = "FAVOR_ARGS";
  private ImageView mImageView;
  private EditText mTitleView;
  private EditText mDescriptionView;
  private TextView mStatusView;
  private Locator mGpsTracker;
  private Button confirmFavorBtn;
  private Button addPictureBtn;
  private Button cancelFavorBtn;
  private Button editFavorBtn;
  private Favor currentFavor;

  public static FavorRequestView newInstance(Favor favor) {
    FavorRequestView fragment = new FavorRequestView();
    Bundle args = new Bundle();
    args.putParcelable(FAVOR_ARGS, favor);
    fragment.setArguments(args);
    return fragment;
  }

  public FavorRequestView() {
    // Required empty public constructor
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    View rootView = inflater.inflate(R.layout.fragment_favor, container, false);

    setupButtons(rootView);
    // Edit text:
    mTitleView = rootView.findViewById(R.id.title_request_view);
    mDescriptionView = rootView.findViewById(R.id.details);
    mStatusView = rootView.findViewById(R.id.favor_status_text);
    setupView(rootView);
    // Extract other elements
    mImageView = rootView.findViewById(R.id.imageView);

    // Get dependencies
    mGpsTracker =
        DependencyFactory.getCurrentGpsTracker(
            Objects.requireNonNull(getActivity()).getApplicationContext());
    // Inject argument

    if (getArguments() != null) {
      currentFavor = getArguments().getParcelable(FAVOR_ARGS);
      displayFavorInfo();
      setFavorActivatedView(rootView);
    }
    return rootView;
  }

  private void displayFavorInfo() {
    mTitleView.setText(currentFavor.getTitle());
    mDescriptionView.setText(currentFavor.getDescription());
    mStatusView.setText(currentFavor.getStatusId().toString());
  }

  private void setupButtons(View rootView) {

    // Button: Request Favor
    confirmFavorBtn = rootView.findViewById(R.id.request_button);
    confirmFavorBtn.setOnClickListener(v -> requestFavor());

    // Button: Add Image
    addPictureBtn = rootView.findViewById(R.id.add_picture_button);
    addPictureBtn.setOnClickListener(v -> openFileChooser());

    // Button: Cancel Favor
    cancelFavorBtn = rootView.findViewById(R.id.cancel_favor_button);
    cancelFavorBtn.setOnClickListener(v -> cancelFavor());

    // Button: Edit favor
    editFavorBtn = rootView.findViewById(R.id.edit_favor_button);
    editFavorBtn.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            // if text is currently "Edit Request"
            if (editFavorBtn.getText().toString().equals(getString(R.string.edit_favor))) {
              startUpdatingActiveFavor();
            } else { // text is currently "Update Request"
              confirmUpdatedFavor();
            }
          }
        });
  }

  private void confirmUpdatedFavor() {
    getFavorFromView();
    FavorUtil.updateFavor(currentFavor);
    // update lists
    ((MainActivity) Objects.requireNonNull(getActivity()))
        .activeFavors.put(currentFavor.getId(), currentFavor);
    ((MainActivity) Objects.requireNonNull(getActivity()))
        .archivedFavors.remove(currentFavor.getId());
    setFavorActivatedView(getView());
    showSnackbar(getString(R.string.favor_edit_success_msg));
  }

  private void setFavorActivatedView(View v) {
    confirmFavorBtn.setVisibility(View.INVISIBLE);
    editFavorBtn.setVisibility(View.VISIBLE);
    editFavorBtn.setText(getString(R.string.edit_favor));
    cancelFavorBtn.setVisibility(View.VISIBLE);
    toggleTextViewsEditable(false);
    updateViewFromStatus();
    CommonTools.hideKeyboardFrom(Objects.requireNonNull(getContext()),v);
  }

  private void setFavorUpdatingView() {
    editFavorBtn.setText(R.string.confirm_favor_edit);
    toggleTextViewsEditable(true);
  }

  private void toggleTextViewsEditable(boolean switchToEditable) {
    if (!switchToEditable) {
      mTitleView.setKeyListener(null);
      mDescriptionView.setKeyListener(null);
    } else {
      mTitleView.setKeyListener((KeyListener) mTitleView.getTag());
      mDescriptionView.setKeyListener((KeyListener) mDescriptionView.getTag());
    }
  }

  private void startUpdatingActiveFavor() {
    cancelFavorBtn.setEnabled(true);
    addPictureBtn.setEnabled(true);
    setFavorUpdatingView();
    toggleTextViewsEditable(true);
  }

  private void cancelFavor() {
    currentFavor.updateStatus(Favor.Status.CANCELLED_REQUESTER);
    FavorUtil.updateFavor(currentFavor);
    MainActivity mainActivity = (MainActivity) getActivity();
    mainActivity.archivedFavors.put(currentFavor.getId(), currentFavor);
    mainActivity.activeFavors.remove(currentFavor.getId());
    CommonTools.hideKeyboardFrom(
              Objects.requireNonNull(getContext()), Objects.requireNonNull(getView()));
    updateViewFromStatus();
    // Show confirmation and minimize keyboard
    showSnackbar(getString(R.string.favor_cancel_success_msg));
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    if (requestCode == PICK_IMAGE_REQUEST
        && resultCode == RESULT_OK
        && data != null
        && data.getData() != null) {
      Uri mImageUri = data.getData(); // path of image
      mImageView.setImageURI(mImageUri);
    } else {
      showSnackbar("Try again!");
    }
  }

  private void requestFavor() {
    getFavorFromView();
    FavorUtil.getSingleInstance().postFavor(currentFavor);

    // Save the favor to local favorList
    ((MainActivity) Objects.requireNonNull(getActivity()))
        .activeFavors.put(currentFavor.getId(), currentFavor);

    // Show confirmation and minimize keyboard
    showSnackbar(getString(R.string.favor_request_success_msg));
    hideKeyboardFrom(Objects.requireNonNull(getContext()), getView());

    setFavorActivatedView(getView());

    updateViewFromStatus();
  }

  private void updateViewFromStatus() {
    mStatusView.setText(currentFavor.getStatusId().getPrettyString());
    switch (currentFavor.getStatusId()) {
      case REQUESTED:
        {
          mStatusView.setBackgroundColor(getResources().getColor(R.color.requested_status_bg));
          addPictureBtn.setEnabled(false);
          break;
        }
      case ACCEPTED:
        {
        }
      case SUCCESSFULLY_COMPLETED:
        {
          mStatusView.setBackgroundColor(getResources().getColor(R.color.accepted_status_bg));
          break;
        }
      default:
        {
          mStatusView.setBackgroundColor(getResources().getColor(R.color.cancelled_status_bg));
          editFavorBtn.setText(R.string.edit_favor);
          cancelFavorBtn.setEnabled(false);
          addPictureBtn.setEnabled(false);
        }
    }
  }

  private void getFavorFromView() {

    // Extract details and post favor to Firebase
    EditText titleElem = Objects.requireNonNull(getView()).findViewById(R.id.title_request_view);
    EditText descElem = Objects.requireNonNull(getView()).findViewById(R.id.details);
    String title = titleElem.getText().toString();
    String desc = descElem.getText().toString();
    Location loc = mGpsTracker.getLocation();
    Favor favor = new Favor(title, desc, null, loc, Favor.Status.REQUESTED);
    if (currentFavor == null) {
      currentFavor = favor;
    } else {
      currentFavor.updateToOther(favor);
    }
    return;
  }

  public void openFileChooser() {
    Intent intent = new Intent();
    intent.setType("image/*");
    intent.setAction(Intent.ACTION_GET_CONTENT);
    startActivityForResult(intent, PICK_IMAGE_REQUEST);
  }

  public void showSnackbar(String errorMessageRes) {
    Snackbar.make(Objects.requireNonNull(getView()), errorMessageRes, Snackbar.LENGTH_LONG).show();
  }

  @SuppressLint("ClickableViewAccessibility")
  private void setupView(View view) {
    ((ViewController) Objects.requireNonNull(getActivity())).setupViewBotDestTab();
    if (mTitleView.getKeyListener() != null) {
      mTitleView.setTag(mTitleView.getKeyListener());
    }
    if (mDescriptionView.getKeyListener() != null) {
      mDescriptionView.setTag(mDescriptionView.getKeyListener());
    }
    //ensure click on view will hide keyboard
    view.findViewById(R.id.constraint_layout_req_view)
            .setOnTouchListener((v, event) -> {
              hideKeyboardFrom(Objects.requireNonNull(getContext()),v);
              return false;
            });
  }

}
