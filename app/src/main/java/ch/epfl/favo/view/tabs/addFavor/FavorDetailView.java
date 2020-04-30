package ch.epfl.favo.view.tabs.addFavor;

import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

import ch.epfl.favo.R;
import ch.epfl.favo.favor.Favor;
import ch.epfl.favo.favor.FavorStatus;
import ch.epfl.favo.util.CommonTools;
import ch.epfl.favo.util.DependencyFactory;
import ch.epfl.favo.util.FavorFragmentFactory;
import ch.epfl.favo.viewmodel.FavorDataController;

import static androidx.navigation.Navigation.findNavController;

@SuppressLint("NewApi")
public class FavorDetailView extends Fragment {

  private FavorStatus favorStatus;
  private Favor currentFavor;
  private FloatingActionButton locationAccessBtn;
  private Button acceptAndCancelFavorBtn;
  private Button chatBtn;
  private TextView statusText;
  private FavorDataController favorViewModel;

  public FavorDetailView() {
    // create favor detail from a favor
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    // inflate view
    View rootView = inflater.inflate(R.layout.fragment_favor_accept_view, container, false);
    setupButtons(rootView);
    statusText = rootView.findViewById(R.id.status_text_accept_view);

    favorViewModel =
        (FavorDataController)
            new ViewModelProvider(requireActivity())
                .get(DependencyFactory.getCurrentViewModelClass());
    if (currentFavor == null && getArguments() != null) {
      String favorId = getArguments().getString(FavorFragmentFactory.FAVOR_ARGS);
      setupFavorListener(rootView, favorId);
    }
    else{
      displayFromFavor(rootView, Objects.requireNonNull(currentFavor)); //TODO: fix in case current favor is null
    }

    return rootView;
  }
  public FavorDataController getViewModel(){
    return favorViewModel;
  }

  private void setupFavorListener(View rootView, String favorId) {

    getViewModel()
        .setObservedFavor(favorId)
        .observe(
            getViewLifecycleOwner(),
            favor -> {
              try {
                currentFavor = favor;
                displayFromFavor(rootView, currentFavor);
              } catch (Exception e) {
                CommonTools.showSnackbar(rootView, getString(R.string.error_database_sync));
                enableButtons(false);
              }
            });
  }

  private void setupButtons(View rootView) {
    acceptAndCancelFavorBtn = rootView.findViewById(R.id.accept_button);
    locationAccessBtn = rootView.findViewById(R.id.location_accept_view_btn);
    chatBtn = rootView.findViewById(R.id.chat_button_accept_view);

    locationAccessBtn.setOnClickListener(
        v ->
            findNavController(requireActivity(), R.id.nav_host_fragment)
                .popBackStack(R.id.nav_map, false));

    // If clicking for the first time, then accept the favor
    acceptAndCancelFavorBtn.setOnClickListener(
        v -> {
          if (currentFavor.getStatusId() == FavorStatus.REQUESTED.toInt()) {
            acceptFavor();
          } else {
            cancelFavor();
          }
        });

    chatBtn.setOnClickListener(
        v -> {
          Bundle favorBundle = new Bundle();
          favorBundle.putParcelable("FAVOR_ARGS", currentFavor);
          Navigation.findNavController(requireView())
              .navigate(R.id.action_nav_favorDetailView_to_chatView, favorBundle);
        });
  }

  private void cancelFavor() {
    currentFavor.setStatusIdToInt(FavorStatus.CANCELLED_ACCEPTER);
    CompletableFuture completableFuture =
            getViewModel().updateFavor(currentFavor);
    completableFuture.thenAccept(successfullyCancelledConsumer());
    completableFuture.exceptionally(favorFailedToBeAcceptedConsumer());
  }

  private Consumer successfullyCancelledConsumer() {
    return o -> {
      CommonTools.showSnackbar(getView(), getString(R.string.favor_cancel_success_msg));

      // update UI
      currentFavor.setStatusIdToInt(FavorStatus.CANCELLED_ACCEPTER);
      favorStatus = verifyFavorHasBeenAccepted(currentFavor);
      // updateDisplayFromViewStatus();
    };
  }

  // Verifies favor hasn't already been accepted
  private FavorStatus verifyFavorHasBeenAccepted(Favor favor) {
    FavorStatus favorStatus = FavorStatus.toEnum(favor.getStatusId());
    if (favor.getAccepterId() != null) {
      if (favorStatus.equals(FavorStatus.ACCEPTED)
          && !favor.getAccepterId().equals(DependencyFactory.getCurrentFirebaseUser().getUid())) {
        favorStatus = FavorStatus.ACCEPTED_BY_OTHER;
      }
    }
    return favorStatus;
  }

  private void acceptFavor() {
    // update DB with accepted status
    currentFavor.setStatusIdToInt(FavorStatus.ACCEPTED);
    currentFavor.setAccepterId(DependencyFactory.getCurrentFirebaseUser().getUid());
    CompletableFuture updateFavorFuture =
            getViewModel().updateFavor(currentFavor);
    updateFavorFuture.thenAccept(
        o -> CommonTools.showSnackbar(getView(), getString(R.string.favor_respond_success_msg)));
    updateFavorFuture.exceptionally(favorFailedToBeAcceptedConsumer());
  }

  private Function favorFailedToBeAcceptedConsumer() {
    return e -> {
      // if already accepted then change the status display and disable all the buttons
      CommonTools.showSnackbar(getView(), getString(R.string.update_favor_error));
      return null;
    };
  }

  private void displayFromFavor(View rootView, Favor favor) {

    String timeStr = CommonTools.convertTime(favor.getLocation().getTime());
    String titleStr = favor.getTitle();
    String descriptionStr = favor.getDescription();
    // update status string
    favorStatus = verifyFavorHasBeenAccepted(favor);
    updateDisplayFromViewStatus();

    setupTextView(rootView, R.id.datetime_accept_view, timeStr);
    setupTextView(rootView, R.id.title_accept_view, titleStr);
    setupTextView(rootView, R.id.details_accept_view, descriptionStr);
  }

  private void updateDisplayFromViewStatus() {
    statusText.setText(favorStatus.toString());
    updateButtonDisplay();
    switch (favorStatus) {
      case SUCCESSFULLY_COMPLETED:
        {
          enableButtons(false);
        }
      case ACCEPTED:
        {
          statusText.setBackgroundColor(getResources().getColor(R.color.accepted_status_bg));
          break;
        }

      case REQUESTED:
        {
          statusText.setBackgroundColor(getResources().getColor(R.color.requested_status_bg));
          break;
        }
      default: // includes accepted by other
        enableButtons(false);
        statusText.setBackgroundColor(getResources().getColor(R.color.cancelled_status_bg));
    }
  }

  private void updateButtonDisplay() {
    String displayMessage;
    int backgroundColor;
    Drawable img;
    if (favorStatus == FavorStatus.ACCEPTED) {
      displayMessage = getString(R.string.cancel_accept_button_display);
      backgroundColor = R.color.fui_transparent;
      img = getResources().getDrawable(R.drawable.ic_cancel_24dp);
    } else { // includes ACCEPTED_BY_OTHER
      displayMessage = getResources().getString(R.string.accept_favor);
      img = getResources().getDrawable(R.drawable.ic_thumb_up_24dp);
      backgroundColor = android.R.drawable.btn_default;
    }
    acceptAndCancelFavorBtn.setText(displayMessage);
    acceptAndCancelFavorBtn.setBackgroundResource(backgroundColor);
    acceptAndCancelFavorBtn.setCompoundDrawablesWithIntrinsicBounds(null, null, null, img);
  }

  private void enableButtons(boolean enable) {
    acceptAndCancelFavorBtn.setEnabled(enable);
    chatBtn.setEnabled(enable);
  }

  private void setupTextView(View rootView, int id, String text) {
    TextView textView = rootView.findViewById(id);
    textView.setText(text);
    textView.setKeyListener(null);
  }
}