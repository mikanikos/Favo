package ch.epfl.favo.view.tabs.favorList;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.paging.PagedList;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.firebase.ui.firestore.paging.FirestorePagingAdapter;
import com.firebase.ui.firestore.paging.FirestorePagingOptions;
import com.firebase.ui.firestore.paging.LoadingState;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;

import ch.epfl.favo.MainActivity;
import ch.epfl.favo.R;
import ch.epfl.favo.favor.Favor;
import ch.epfl.favo.favor.FavorUtil;
import ch.epfl.favo.util.CommonTools;
import ch.epfl.favo.util.DependencyFactory;

import static ch.epfl.favo.util.CommonTools.hideSoftKeyboard;

/**
 * View will contain list of favors requested and accepted, currently active and in the past. The
 * list will contain clickable items that will expand to give more information about them. This
 * object is a simple {@link Fragment} subclass.
 */
public class MyFavorsPage extends Fragment {

  private TextView tipTextView;
  private SearchView searchView;
  private RadioGroup radioGroup;
  private RadioButton activeToggle;
  private RadioButton archivedToggle;

  private RecyclerView mRecycler;
  private SwipeRefreshLayout mSwipeRefreshLayout;

  private static final int PREFETCH_DISTANCE = 10;
  private static final int PAGE_SIZE = 20;
  private static final char END_CODE = '\uf8ff';

  private final PagedList.Config pagingConfig =
      new PagedList.Config.Builder()
          .setEnablePlaceholders(false)
          .setPrefetchDistance(PREFETCH_DISTANCE)
          .setPageSize(PAGE_SIZE)
          .build();

  private FirestorePagingAdapter<Favor, FavorViewHolder> adapter;

  private FirestorePagingOptions<Favor> activeFavorsOptions;
  private FirestorePagingOptions<Favor> archiveFavorsOptions;

  private String lastQuery;
  private Query searchQuery;

  public MyFavorsPage() {
    // Required empty public constructor
  }

  @Override
  public void onCreate(Bundle bundle) {
    super.onCreate(bundle);
    setHasOptionsMenu(true);
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    View rootView = inflater.inflate(R.layout.fragment_favorpage, container, false);

    // initialize fields
    tipTextView = rootView.findViewById(R.id.tip);
    radioGroup = rootView.findViewById(R.id.radio_toggle);
    activeToggle = rootView.findViewById(R.id.active_toggle);
    archivedToggle = rootView.findViewById(R.id.archived_toggle);

    mRecycler = rootView.findViewById(R.id.paging_recycler);
    mRecycler.addItemDecoration(
        new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));
    mSwipeRefreshLayout = rootView.findViewById(R.id.swipe_refresh_layout);

    Query baseQuery =
        FavorUtil.getSingleInstance()
            .getAllUserFavors(DependencyFactory.getCurrentFirebaseUser().getUid());

    Query listQuery = baseQuery.orderBy(Favor.POSTED_TIME, Query.Direction.DESCENDING);
    searchQuery = baseQuery.orderBy(Favor.TITLE, Query.Direction.ASCENDING);

    activeFavorsOptions =
        createFirestorePagingOptions(listQuery.whereEqualTo(Favor.IS_ARCHIVED, false));
    archiveFavorsOptions =
        createFirestorePagingOptions(listQuery.whereEqualTo(Favor.IS_ARCHIVED, true));

    // setup methods
    setupSwitchButtons();
    setupAdapter();
    setupView();

    return rootView;
  }

  private void setupAdapter() {
    adapter = createFirestorePagingAdapter(activeFavorsOptions);

    adapter.registerAdapterDataObserver(
        new RecyclerView.AdapterDataObserver() {
          @Override
          public void onItemRangeInserted(int positionStart, int itemCount) {
            setEmptyListText();
          }

          @Override
          public void onItemRangeRemoved(int positionStart, int itemCount) {
            setEmptyListText();
          }
        });

    mRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
    mRecycler.setAdapter(adapter);
    mSwipeRefreshLayout.setOnRefreshListener(adapter::refresh);
  }

  private void setEmptyListText() {
    int totalNumberOfItems = adapter.getItemCount();
    if (totalNumberOfItems == 0) {
      tipTextView.setVisibility(View.VISIBLE);
    } else {
      tipTextView.setVisibility(View.INVISIBLE);
    }
  }

  private void setupSwitchButtons() {
    activeToggle.setOnCheckedChangeListener(
        (buttonView, isChecked) -> {
          if (isChecked) {
            tipTextView.setText(R.string.favor_no_active_favor);
            displayFavorList(activeFavorsOptions);
          }
        });

    archivedToggle.setOnCheckedChangeListener(
        (buttonView, isChecked) -> {
          if (isChecked) {
            tipTextView.setText(R.string.favor_no_archived_favor);
            displayFavorList(archiveFavorsOptions);
          }
        });
  }

  private FirestorePagingAdapter<Favor, FavorViewHolder> createFirestorePagingAdapter(
      FirestorePagingOptions<Favor> options) {
    return new FirestorePagingAdapter<Favor, FavorViewHolder>(options) {
      @NonNull
      @Override
      public FavorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view =
            LayoutInflater.from(parent.getContext())
                .inflate(R.layout.favor_list_item, parent, false);

        view.setOnClickListener(
            v -> {
              int itemPosition = mRecycler.getChildLayoutPosition(view);
              DocumentSnapshot doc = getItem(itemPosition);
              if (doc != null && doc.exists()) {
                Favor favor = doc.toObject(Favor.class);
                if (favor != null) {
                  Bundle favorBundle = new Bundle();
                  favorBundle.putString(CommonTools.FAVOR_ARGS, favor.getId());
                  Navigation.findNavController(requireView())
                      .navigate(R.id.action_nav_favorlist_to_favorPublishedView, favorBundle);
                }
              }
            });

        return new FavorViewHolder(view);
      }

      @Override
      protected void onBindViewHolder(
          @NonNull FavorViewHolder holder, int position, @NonNull Favor model) {
        holder.bind(requireContext(), model);
      }

      @Override
      protected void onLoadingStateChanged(@NonNull LoadingState state) {
        switch (state) {
          case LOADING_INITIAL:
            mSwipeRefreshLayout.setRefreshing(true);
            break;
          case LOADED:
          case FINISHED:
            mSwipeRefreshLayout.setRefreshing(false);
            break;
          case ERROR:
            Toast.makeText(
                    getContext(),
                    getString(R.string.favors_retrieval_failed_message),
                    Toast.LENGTH_SHORT)
                .show();

            break;
        }
      }
    };
  }

  private FirestorePagingOptions<Favor> createFirestorePagingOptions(Query query) {
    return new FirestorePagingOptions.Builder<Favor>()
        .setLifecycleOwner(this)
        .setQuery(query, pagingConfig, Favor.class)
        .build();
  }

  private void displayFavorList(FirestorePagingOptions<Favor> options) {
    adapter.updateOptions(options);
    mRecycler.setAdapter(adapter);
  }

  @SuppressLint("ClickableViewAccessibility")
  private void setupView() {
    // ensure click on view will hide keyboard
    mRecycler.setOnTouchListener(
        (v, event) -> {
          hideSoftKeyboard(requireActivity());
          return false;
        });
  }

  @Override
  public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {

    // Inflate the menu; this adds items to the action bar if it is present.
    inflater.inflate(R.menu.favor_list_menu, menu);
    super.onCreateOptionsMenu(menu, inflater);

    setupSearch(menu);
  }

  private void setupSearch(Menu menu) {
    MenuItem searchMenuItem = menu.findItem(R.id.search_item);
    searchView = (SearchView) searchMenuItem.getActionView();
    searchView.setIconifiedByDefault(true);
    searchView.setQueryHint(getString(R.string.query_hint));

    setOnMenuItemActions(searchMenuItem);
    setOnQueryTextListeners();

    if (lastQuery != null) {
      searchView.post(() -> searchView.setQuery(lastQuery, false));
      searchMenuItem.expandActionView();
    }
  }

  private void setOnQueryTextListeners() {
    searchView.setOnQueryTextListener(
        new SearchView.OnQueryTextListener() {
          @Override
          public boolean onQueryTextSubmit(String query) {
            return false;
          }

          @Override
          public boolean onQueryTextChange(String newText) {

            // complex condition to prevent modification of last query when callback is fired on
            // fragment replacement and on back button pressed
            if (radioGroup.getVisibility() == View.VISIBLE
                || searchView.isIconified()
                || !isVisible()) {
              // Don't call setSearchQuery when SearchView is collapsing/collapsed
              return true;
            }

            Query query;
            if (newText.equals("")) {
              query = searchQuery;
            } else {
              query =
                  searchQuery
                      .whereGreaterThanOrEqualTo(Favor.TITLE, newText)
                      .whereLessThanOrEqualTo(Favor.TITLE, newText + END_CODE);
            }

            lastQuery = newText;
            displayFavorList(createFirestorePagingOptions(query));

            return true;
          }
        });
  }

  private void setOnMenuItemActions(MenuItem searchMenuItem) {
    searchMenuItem.setOnActionExpandListener(
        new MenuItem.OnActionExpandListener() {

          @Override
          public boolean onMenuItemActionExpand(MenuItem item) {
            radioGroup.setVisibility(View.INVISIBLE);
            ((MainActivity) (requireActivity())).hideBottomNavigation();
            tipTextView.setText(R.string.query_failed);

            Query query;
            if (lastQuery == null || lastQuery.equals("")) {
              query = searchQuery;
              lastQuery = "";
            } else {
              query =
                  searchQuery
                      .whereGreaterThanOrEqualTo(Favor.TITLE, lastQuery)
                      .whereLessThanOrEqualTo(Favor.TITLE, lastQuery + END_CODE);
            }

            displayFavorList(createFirestorePagingOptions(query));
            return true;
          }

          @Override
          public boolean onMenuItemActionCollapse(MenuItem item) {
            radioGroup.setVisibility(View.VISIBLE);
            ((MainActivity) (requireActivity())).showBottomNavigation();
            lastQuery = null;

            if (activeToggle.isChecked()) {
              tipTextView.setText(R.string.favor_no_active_favor);
              displayFavorList(activeFavorsOptions);
            } else {
              tipTextView.setText(R.string.favor_no_archived_favor);
              displayFavorList(archiveFavorsOptions);
            }

            return true;
          }
        });
  }
}
